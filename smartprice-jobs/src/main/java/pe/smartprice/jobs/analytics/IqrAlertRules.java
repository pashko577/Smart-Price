package pe.smartprice.jobs.analytics;

import static org.apache.spark.sql.functions.abs;
import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.concat_ws;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.date_format;
import static org.apache.spark.sql.functions.expr;
import static org.apache.spark.sql.functions.lag;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.round;
import static org.apache.spark.sql.functions.struct;
import static org.apache.spark.sql.functions.to_timestamp;
import static org.apache.spark.sql.functions.when;

import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;

import scala.collection.JavaConverters;
import scala.collection.Seq;

/**
 * Reglas de alerta por variación diaria atípica (IQR), compartidas por el job Batch y el Streaming.
 *
 * <p>Entrada esperada: columnas {@code date, market_code, product_name, price, volume_t}.
 */
public final class IqrAlertRules {

    public static final String METHOD = "IQR_DAILY_CHANGE";

    @SuppressWarnings("deprecation")
    static final Seq<String> KEYS = JavaConverters.asScalaBuffer(List.of("market_code", "product_name"));

    private IqrAlertRules() {
    }

    /** Añade precio anterior, fecha anterior, volumen promedio de los 30 días previos y variación (%). */
    public static Dataset<Row> dailyChanges(Dataset<Row> prices) {
        WindowSpec byProduct = Window.partitionBy("market_code", "product_name").orderBy("date");
        WindowSpec trailing30 = byProduct.rowsBetween(-30, -1);
        return prices
                .withColumn("prev_price", lag("price", 1).over(byProduct))
                .withColumn("prev_date", lag("date", 1).over(byProduct))
                .withColumn("volume_avg_30d", avg("volume_t").over(trailing30))
                .where(col("prev_price").isNotNull().and(col("prev_price").gt(0)))
                .withColumn("change_pct",
                        col("price").minus(col("prev_price")).divide(col("prev_price")).multiply(100));
    }

    /** Q1 y Q3 de la variación diaria de cada producto (rango normal). */
    public static Dataset<Row> baselines(Dataset<Row> daily) {
        return daily.groupBy("market_code", "product_name")
                .agg(expr("percentile_approx(change_pct, 0.25, 10000)").alias("q1"),
                        expr("percentile_approx(change_pct, 0.75, 10000)").alias("q3"));
    }

    /** Filas cuya variación sale de [Q1 - k*IQR, Q3 + k*IQR] y supera la variación mínima. */
    public static Dataset<Row> flag(Dataset<Row> daily, Dataset<Row> baselines, double k, double minChangePct) {
        return daily
                .join(baselines, KEYS)
                .withColumn("iqr", col("q3").minus(col("q1")))
                .withColumn("lower", col("q1").minus(col("iqr").multiply(k)))
                .withColumn("upper", col("q3").plus(col("iqr").multiply(k)))
                .where(col("change_pct").lt(col("lower")).or(col("change_pct").gt(col("upper")))
                        .and(abs(col("change_pct")).geq(minChangePct)))
                // cuántos IQR se aleja del límite (si IQR = 0 se usa la variación absoluta)
                .withColumn("iqr_distance", when(col("iqr").gt(0),
                        when(col("change_pct").gt(col("upper")),
                                col("change_pct").minus(col("upper")).divide(col("iqr")))
                                .otherwise(col("lower").minus(col("change_pct")).divide(col("iqr"))))
                        .otherwise(abs(col("change_pct"))));
    }

    /** Documentos de price_alerts. El _id es el mismo en Batch y Streaming: una alerta por producto y día. */
    public static Dataset<Row> toAlertDocs(Dataset<Row> flagged, double k, double minChangePct,
                                           String job, String jobVersion) {
        return flagged.select(
                concat_ws("|", lit(METHOD), col("market_code"), col("product_name"),
                        date_format(col("date"), "yyyy-MM-dd")).alias("_id"),
                when(col("change_pct").gt(0), lit("PRICE_SPIKE")).otherwise(lit("PRICE_DROP")).alias("alert_type"),
                lit(METHOD).alias("method"),
                col("market_code"),
                col("product_name"),
                to_timestamp(col("date")).alias("observed_at"),
                to_timestamp(col("prev_date")).alias("previous_observed_at"),
                struct(
                        col("price").alias("current"),
                        col("prev_price").alias("previous"),
                        lit("PEN").alias("currency")).alias("price"),
                round(col("change_pct"), 2).alias("change_pct"),
                struct(
                        round(col("q1"), 3).alias("q1"),
                        round(col("q3"), 3).alias("q3"),
                        round(col("lower"), 3).alias("lower"),
                        round(col("upper"), 3).alias("upper"),
                        lit(k).alias("k"),
                        lit(minChangePct).alias("min_abs_change_pct")).alias("bounds"),
                struct(
                        col("volume_t").alias("current_t"),
                        round(col("volume_avg_30d"), 2).alias("avg_30d_t")).alias("volume"),
                when(col("iqr_distance").geq(3), lit("HIGH"))
                        .when(col("iqr_distance").geq(1), lit("MEDIUM"))
                        .otherwise(lit("LOW")).alias("severity"),
                lit("OPEN").alias("status"),
                lit(job).alias("job"),
                lit(jobVersion).alias("job_version"),
                current_timestamp().alias("generated_at"));
    }

    /** Documentos ALERT_BASELINE de price_analytics: el rango normal que reutiliza el Streaming. */
    public static Dataset<Row> toBaselineDocs(Dataset<Row> baselines, double k, double minChangePct,
                                              String job, String jobVersion) {
        String typ = "ALERT_BASELINE";
        return baselines.select(
                concat_ws("|", lit(typ), col("market_code"), col("product_name")).alias("_id"),
                lit(typ).alias("analysis_type"),
                col("market_code"),
                col("product_name"),
                round(col("q1"), 4).alias("q1"),
                round(col("q3"), 4).alias("q3"),
                lit(k).alias("k"),
                lit(minChangePct).alias("min_abs_change_pct"),
                lit(METHOD).alias("method"),
                lit(job).alias("job"),
                lit(jobVersion).alias("job_version"),
                current_timestamp().alias("generated_at"));
    }
}
