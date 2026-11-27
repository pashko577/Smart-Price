package pe.smartprice.jobs.analytics;

import static org.apache.spark.sql.functions.abs;
import static org.apache.spark.sql.functions.add_months;
import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.concat_ws;
import static org.apache.spark.sql.functions.corr;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.date_format;
import static org.apache.spark.sql.functions.datediff;
import static org.apache.spark.sql.functions.expr;
import static org.apache.spark.sql.functions.last_day;
import static org.apache.spark.sql.functions.lag;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.max;
import static org.apache.spark.sql.functions.max_by;
import static org.apache.spark.sql.functions.min;
import static org.apache.spark.sql.functions.round;
import static org.apache.spark.sql.functions.struct;
import static org.apache.spark.sql.functions.sum;
import static org.apache.spark.sql.functions.to_date;
import static org.apache.spark.sql.functions.to_timestamp;
import static org.apache.spark.sql.functions.trunc;
import static org.apache.spark.sql.functions.when;

import java.util.List;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;
import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import pe.smartprice.jobs.common.AppConfig;
import pe.smartprice.jobs.common.Sparks;
import pe.smartprice.jobs.mongo.MongoWriter;

/**
 * Procesamiento Batch del histórico: lee wholesale_prices de MongoDB y escribe
 * <ul>
 *   <li>{@code price_analytics} con analysis_type = PRICE_MONTHLY (un documento por producto y mes)</li>
 *   <li>{@code price_analytics} con analysis_type = PRODUCT_SUMMARY (un documento por producto)</li>
 *   <li>{@code price_alerts}: variaciones diarias atípicas según IQR por producto</li>
 * </ul>
 *
 * <p>Cada ejecución reemplaza los resultados anteriores de este job (campo {@code job}).
 *
 * <p>Uso: {@code batch-analytics [k_iqr=3] [variacion_minima_pct=15]}
 */
public final class BatchAnalyticsJob {

    public static final String JOB = "batch_analytics";
    public static final String JOB_VERSION = "batch_v1";

    /** Multiplicador del rango intercuartílico por defecto: fuera de [Q1 - k*IQR, Q3 + k*IQR]. */
    static final double DEFAULT_IQR_K = 3.0;
    /**
     * Variación diaria mínima (en %) por defecto para generar una alerta. Evaluación sobre el histórico
     * (59 productos, 35 meses): 10 % -> ~67 alertas/mes, 15 % -> ~47, 20 % -> ~34.
     */
    static final double DEFAULT_MIN_ABS_CHANGE_PCT = 15.0;
    /** Mínimo de días con dato en el mes para marcar el promedio mensual como confiable. */
    static final int MIN_DAYS_MONTH = 15;


    private BatchAnalyticsJob() {
    }

    public static void run(String[] args) {
        double iqrK = args.length > 0 ? Double.parseDouble(args[0]) : DEFAULT_IQR_K;
        double minChangePct = args.length > 1 ? Double.parseDouble(args[1]) : DEFAULT_MIN_ABS_CHANGE_PCT;
        System.out.printf("Parámetros de alertas: k = %.1f, variación mínima = %.1f %%%n", iqrK, minChangePct);

        String uri = AppConfig.mongoUri();
        String db = AppConfig.mongoDatabase();

        SparkSession spark = Sparks.local("SmartPrice-Batch-Analytics");
        try {
            Dataset<Row> prices = spark.read().format("mongodb")
                    .option("connection.uri", uri)
                    .option("database", db)
                    .option("collection", "wholesale_prices")
                    .load()
                    .select(
                            to_date(col("observed_at")).alias("date"),
                            col("market_code"),
                            col("product_name"),
                            col("price.avg").alias("price"),
                            col("volume_t"))
                    .where(col("price").isNotNull())
                    .cache();

            long n = prices.count();
            System.out.println("Observaciones leídas de MongoDB (con precio): " + n);

            Dataset<Row> monthly = monthly(prices);
            Dataset<Row> summary = productSummary(prices);
            Dataset<Row> daily = IqrAlertRules.dailyChanges(prices).cache();
            Dataset<Row> baselines = IqrAlertRules.baselines(daily).cache();
            Dataset<Row> alerts = iqrAlerts(daily, baselines, iqrK, minChangePct);
            Dataset<Row> baselineDocs = IqrAlertRules.toBaselineDocs(baselines, iqrK, minChangePct, JOB, JOB_VERSION);

            long nMonthly = monthly.count();
            long nSummary = summary.count();
            long nAlerts = alerts.count();

            deletePreviousResults(uri, db);
            MongoWriter.upsert(monthly, uri, db, "price_analytics");
            MongoWriter.upsert(summary, uri, db, "price_analytics");
            MongoWriter.upsert(baselineDocs, uri, db, "price_analytics");
            MongoWriter.upsert(alerts, uri, db, "price_alerts");
            createIndexes(uri, db);

            System.out.printf("price_analytics: %d PRICE_MONTHLY + %d PRODUCT_SUMMARY + %d ALERT_BASELINE"
                    + " | price_alerts: %d%n", nMonthly, nSummary, baselines.count(), nAlerts);
            printHighlights(monthly, summary, alerts);
        } finally {
            spark.stop();
        }
    }

    // ------------------------------------------------------------------ mensual

    static Dataset<Row> monthly(Dataset<Row> prices) {
        Dataset<Row> m = prices
                .withColumn("month", trunc(col("date"), "month"))
                .groupBy("market_code", "product_name", "month")
                .agg(round(avg("price"), 4).alias("avg_price"),
                        min("price").alias("min_price"),
                        max("price").alias("max_price"),
                        round(avg("volume_t"), 2).alias("avg_volume_t"),
                        round(sum("volume_t"), 2).alias("total_volume_t"),
                        count("*").alias("days_observed"));

        // Mes anterior y mismo mes del año anterior mediante auto-join (robusto ante meses faltantes).
        Dataset<Row> prev = m.select(col("market_code").alias("p_market"), col("product_name").alias("p_product"),
                add_months(col("month"), 1).alias("p_month"), col("avg_price").alias("prev_month_avg"));
        Dataset<Row> prevYear = m.select(col("market_code").alias("y_market"), col("product_name").alias("y_product"),
                add_months(col("month"), 12).alias("y_month"), col("avg_price").alias("prev_year_avg"));

        Dataset<Row> joined = m
                .join(prev, col("market_code").equalTo(col("p_market"))
                        .and(col("product_name").equalTo(col("p_product")))
                        .and(col("month").equalTo(col("p_month"))), "left")
                .join(prevYear, col("market_code").equalTo(col("y_market"))
                        .and(col("product_name").equalTo(col("y_product")))
                        .and(col("month").equalTo(col("y_month"))), "left");

        String typ = "PRICE_MONTHLY";
        return joined.select(
                concat_ws("|", lit(typ), col("market_code"), col("product_name"),
                        date_format(col("month"), "yyyy-MM")).alias("_id"),
                lit(typ).alias("analysis_type"),
                col("market_code"),
                col("product_name"),
                struct(
                        date_format(col("month"), "yyyy-MM").alias("label"),
                        to_timestamp(col("month")).alias("from"),
                        to_timestamp(last_day(col("month"))).alias("to")).alias("period"),
                struct(
                        col("avg_price").alias("avg"),
                        col("min_price").alias("min"),
                        col("max_price").alias("max"),
                        lit("PEN").alias("currency"),
                        lit("kg|l|unit").alias("unit_basis")).alias("price"),
                struct(
                        col("avg_volume_t").alias("avg_t"),
                        col("total_volume_t").alias("total_t")).alias("volume"),
                pct(col("avg_price"), col("prev_month_avg")).alias("var_pct_vs_prev_month"),
                pct(col("avg_price"), col("prev_year_avg")).alias("var_pct_vs_prev_year"),
                col("days_observed"),
                when(col("days_observed").geq(MIN_DAYS_MONTH), lit("VALIDATED"))
                        .otherwise(lit("PARTIAL_MONTH")).alias("data_quality_status"),
                lit(JOB).alias("job"),
                lit(JOB_VERSION).alias("job_version"),
                current_timestamp().alias("generated_at"));
    }

    // ------------------------------------------------------------------ resumen por producto

    static Dataset<Row> productSummary(Dataset<Row> prices) {
        Dataset<Row> last = prices.groupBy("market_code", "product_name")
                .agg(max("date").alias("last_date"),
                        max_by(col("price"), col("date")).alias("last_price"),
                        max_by(col("volume_t"), col("date")).alias("last_volume_t"),
                        min("price").alias("min_price_all"),
                        max("price").alias("max_price_all"),
                        min("date").alias("first_date"),
                        count("*").alias("observations"),
                        round(corr("price", "volume_t"), 3).alias("corr_price_volume"));

        Dataset<Row> withLast = prices.join(last, IqrAlertRules.KEYS);

        Dataset<Row> windows = withLast.groupBy("market_code", "product_name")
                .agg(round(avg(when(datediff(col("last_date"), col("date")).lt(30), col("price"))), 4).alias("avg_30d"),
                        round(avg(when(datediff(col("last_date"), col("date")).between(30, 59), col("price"))), 4)
                                .alias("avg_prev_30d"),
                        round(avg(when(datediff(col("last_date"), col("date")).lt(365), col("price"))), 4).alias("avg_365d"),
                        round(avg(when(datediff(col("last_date"), col("date")).between(365, 394), col("price"))), 4)
                                .alias("avg_30d_year_ago"));

        Dataset<Row> s = last.join(windows, IqrAlertRules.KEYS);

        String typ = "PRODUCT_SUMMARY";
        return s.select(
                concat_ws("|", lit(typ), col("market_code"), col("product_name")).alias("_id"),
                lit(typ).alias("analysis_type"),
                col("market_code"),
                col("product_name"),
                struct(
                        to_timestamp(col("first_date")).alias("from"),
                        to_timestamp(col("last_date")).alias("to")).alias("period"),
                struct(
                        col("last_price").alias("last"),
                        col("avg_30d"),
                        col("avg_365d"),
                        col("min_price_all").alias("min_all"),
                        col("max_price_all").alias("max_all"),
                        lit("PEN").alias("currency"),
                        lit("kg|l|unit").alias("unit_basis")).alias("price"),
                col("last_volume_t"),
                pct(col("avg_30d"), col("avg_prev_30d")).alias("var_pct_30d"),
                pct(col("avg_30d"), col("avg_30d_year_ago")).alias("var_pct_yoy"),
                col("corr_price_volume"),
                col("observations"),
                lit("VALIDATED").alias("data_quality_status"),
                lit(JOB).alias("job"),
                lit(JOB_VERSION).alias("job_version"),
                current_timestamp().alias("generated_at"));
    }

    // ------------------------------------------------------------------ alertas IQR

    /** Alertas sobre todo el histórico (la lógica vive en {@link IqrAlertRules}, compartida con el Streaming). */
    static Dataset<Row> iqrAlerts(Dataset<Row> daily, Dataset<Row> baselines, double iqrK, double minChangePct) {
        return IqrAlertRules.toAlertDocs(IqrAlertRules.flag(daily, baselines, iqrK, minChangePct),
                iqrK, minChangePct, JOB, JOB_VERSION);
    }

    // ------------------------------------------------------------------ utilidades

    /** Variación porcentual redondeada a 2 decimales; nula si la referencia es nula o cero. */
    static Column pct(Column current, Column reference) {
        return when(reference.isNull().or(reference.equalTo(0)), lit(null).cast("double"))
                .otherwise(round(current.minus(reference).divide(reference).multiply(100), 2));
    }

    private static void deletePreviousResults(String uri, String db) {
        try (MongoClient client = MongoClients.create(uri)) {
            MongoDatabase d = client.getDatabase(db);
            long a = d.getCollection("price_analytics").deleteMany(new Document("job", JOB)).getDeletedCount();
            long b = d.getCollection("price_alerts").deleteMany(new Document("job", JOB)).getDeletedCount();
            System.out.printf("Resultados anteriores eliminados: %d análisis, %d alertas%n", a, b);
        }
    }

    private static void createIndexes(String uri, String db) {
        try (MongoClient client = MongoClients.create(uri)) {
            MongoDatabase d = client.getDatabase(db);
            d.getCollection("price_analytics").createIndex(Indexes.compoundIndex(
                    Indexes.ascending("analysis_type"), Indexes.ascending("product_name"),
                    Indexes.descending("period.from")), new IndexOptions().name("type_product_period"));
            d.getCollection("price_alerts").createIndex(Indexes.descending("observed_at"),
                    new IndexOptions().name("date"));
            d.getCollection("price_alerts").createIndex(Indexes.compoundIndex(
                    Indexes.ascending("product_name"), Indexes.descending("observed_at")),
                    new IndexOptions().name("product_date"));
        }
    }

    private static void printHighlights(Dataset<Row> monthly, Dataset<Row> summary, Dataset<Row> alerts) {
        System.out.println("\nÚltimos 30 días vs 30 días anteriores (mayores subidas):");
        summary.select(col("product_name"), col("price.last").alias("ultimo"), col("price.avg_30d").alias("prom_30d"),
                        col("var_pct_30d"), col("var_pct_yoy"), col("corr_price_volume"))
                .orderBy(col("var_pct_30d").desc_nulls_last()).show(6, false);

        System.out.println("Zanahoria por mes (últimos 6 meses):");
        monthly.where(col("product_name").equalTo("Zanahoria"))
                .select(col("period.label").alias("mes"), col("price.avg").alias("promedio"),
                        col("var_pct_vs_prev_month"), col("var_pct_vs_prev_year"), col("volume.avg_t").alias("vol_prom_t"))
                .orderBy(col("mes").desc()).show(6, false);

        System.out.println("Alertas por severidad:");
        alerts.groupBy("severity").count().orderBy("severity").show(false);

        System.out.println("Alertas más recientes:");
        alerts.select(date_format(col("observed_at"), "yyyy-MM-dd").alias("fecha"), col("product_name"),
                        col("alert_type"), col("price.previous").alias("antes"), col("price.current").alias("ahora"),
                        col("change_pct"), col("severity"))
                .orderBy(col("observed_at").desc()).show(8, false);
    }
}
