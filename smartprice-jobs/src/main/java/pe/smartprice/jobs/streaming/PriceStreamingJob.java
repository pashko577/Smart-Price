package pe.smartprice.jobs.streaming;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.concat_ws;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.date_format;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.round;
import static org.apache.spark.sql.functions.struct;
import static org.apache.spark.sql.functions.to_date;
import static org.apache.spark.sql.functions.to_timestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.spark.api.java.function.VoidFunction2;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.streaming.Trigger;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;

import pe.smartprice.jobs.analytics.IqrAlertRules;
import pe.smartprice.jobs.common.AppConfig;
import pe.smartprice.jobs.common.Sparks;
import pe.smartprice.jobs.mongo.MongoWriter;

/**
 * Spark Structured Streaming: procesa cada boletín diario del GMML en cuanto aparece su CSV en
 * {@code data/streaming/incoming/} (lo deja ahí el comando {@code ingest-pdf}).
 *
 * <p>Por cada micro-lote:
 * <ol>
 *   <li>Valida las filas y toma el precio por kg (precio / equivalencia en kg, redondeado como SISAP).</li>
 *   <li>Separa las observaciones nuevas de las que ya existen en MongoDB (no duplica; SISAP manda).
 *       En las existentes compara el precio: es la validación cruzada PDF vs SISAP.</li>
 *   <li>Guarda las nuevas en {@code wholesale_prices} con {@code source.system = GMML_PDF}.</li>
 *   <li>Calcula sus alertas con las mismas reglas IQR del Batch, usando el rango normal de cada producto
 *       (documentos ALERT_BASELINE) y el historial reciente leído de MongoDB.</li>
 *   <li>Registra la ejecución en {@code processing_logs}.</li>
 * </ol>
 *
 * <p>Uso: {@code stream-prices} (se queda escuchando) o {@code stream-prices --once}
 * (procesa lo pendiente y termina, útil para pruebas y demostraciones).
 */
public final class PriceStreamingJob {

    public static final String JOB = "streaming_prices";
    public static final String JOB_VERSION = "stream_v1";
    static final String MARKET_CODE = "GMML";
    static final String MARKET_NAME = "Gran Mercado Mayorista de Lima";
    static final String CHECKPOINT = "data/streaming/checkpoint";
    /** Días de historial que se leen para el precio anterior y el volumen promedio de 30 días. */
    static final int HISTORY_DAYS = 45;
    /** Tolerancia de la validación cruzada PDF vs SISAP (soles). */
    static final double PRICE_TOLERANCE = 0.011;

    private PriceStreamingJob() {
    }

    static StructType csvSchema() {
        return new StructType()
                .add("observed_at", DataTypes.StringType)
                .add("product_name", DataTypes.StringType)
                .add("footnote", DataTypes.StringType)
                .add("volume_t_prev", DataTypes.DoubleType)
                .add("volume_t", DataTypes.DoubleType)
                .add("volume_t_avg7", DataTypes.DoubleType)
                .add("volume_t_avg4_wd", DataTypes.DoubleType)
                .add("unit", DataTypes.StringType)
                .add("kg_equivalent", DataTypes.DoubleType)
                .add("price_prev", DataTypes.DoubleType)
                .add("price", DataTypes.DoubleType)
                .add("price_avg7", DataTypes.DoubleType)
                .add("price_per_kg", DataTypes.DoubleType)
                .add("source_file", DataTypes.StringType)
                .add("source_sha256", DataTypes.StringType)
                .add("page", DataTypes.IntegerType)
                .add("extractor_version", DataTypes.StringType);
    }

    public static void run(String[] args) throws TimeoutException, StreamingQueryException {
        boolean once = args.length > 0 && "--once".equals(args[0]);
        String uri = AppConfig.mongoUri();
        String db = AppConfig.mongoDatabase();

        SparkSession spark = Sparks.local("SmartPrice-Price-Streaming");
        try {
            Dataset<Row> stream = spark.readStream()
                    .schema(csvSchema())
                    .option("header", true)
                    .option("maxFilesPerTrigger", 1)   // un boletín por micro-lote
                    .csv(IngestPdfJob.INCOMING.toString());

            VoidFunction2<Dataset<Row>, Long> handler = (batch, batchId) -> processBatch(batch, batchId, uri, db);

            StreamingQuery query = stream.writeStream()
                    .queryName("smartprice-gmml")
                    .option("checkpointLocation", CHECKPOINT)
                    .trigger(once ? Trigger.AvailableNow() : Trigger.ProcessingTime("15 seconds"))
                    .foreachBatch(handler)
                    .start();

            System.out.printf("Streaming activo: vigilando %s (checkpoint en %s)%s%n",
                    IngestPdfJob.INCOMING, CHECKPOINT,
                    once ? " · modo --once: procesa lo pendiente y termina" : " · Ctrl+C para detener");
            query.awaitTermination();
        } finally {
            spark.stop();
        }
    }

    static void processBatch(Dataset<Row> batch, long batchId, String uri, String db) {
        Instant started = Instant.now();
        SparkSession spark = batch.sparkSession();

        Dataset<Row> valid = batch
                .where(col("price_per_kg").isNotNull().and(col("price_per_kg").gt(0))
                        .and(col("observed_at").isNotNull()).and(col("product_name").isNotNull()))
                .select(
                        concat_ws("|", col("observed_at"), lit(MARKET_CODE), col("product_name")).alias("_id"),
                        to_date(col("observed_at")).alias("date"),
                        lit(MARKET_CODE).alias("market_code"),
                        col("product_name"),
                        round(col("price_per_kg"), 2).alias("price"),
                        col("volume_t"),
                        col("source_file"),
                        col("extractor_version"))
                .dropDuplicates("_id")
                .cache();

        long rowsRead = batch.count();

        // 0. Solo productos del catálogo del proyecto (los que tienen rango normal calculado por el Batch).
        //    El boletín trae productos que no están en la selección de SISAP; guardarlos crearía series de un día.
        List<String> catalog = catalog(uri, db);
        List<String> outside = List.of();
        if (!catalog.isEmpty()) {
            Dataset<Row> inCatalog = valid.where(col("product_name").isin(catalog.toArray())).cache();
            outside = valid.where(org.apache.spark.sql.functions.not(col("product_name").isin(catalog.toArray())))
                    .select("product_name").distinct().as(Encoders.STRING()).collectAsList();
            valid.unpersist();
            valid = inCatalog;
        }

        List<Row> validRows = valid.select("_id", "price", "source_file").collectAsList();
        if (validRows.isEmpty()) {
            System.out.printf("[lote %d] sin filas válidas del catálogo (%d leídas, %d productos fuera del catálogo)%n",
                    batchId, rowsRead, outside.size());
            valid.unpersist();
            return;
        }

        // 1. ¿Qué observaciones ya existen? (SISAP u otro boletín) -> no se duplican; se validan.
        List<String> ids = validRows.stream().map(r -> r.getString(0)).toList();
        Map<String, Double> existing = existingPrices(uri, db, ids);
        int matches = 0;
        List<String> mismatches = new ArrayList<>();
        for (Row r : validRows) {
            Double stored = existing.get(r.getString(0));
            if (existing.containsKey(r.getString(0))) {
                if (stored != null && Math.abs(stored - r.getDouble(1)) <= PRICE_TOLERANCE) {
                    matches++;
                } else {
                    mismatches.add(r.getString(0) + " (PDF " + r.getDouble(1) + " / guardado " + stored + ")");
                }
            }
        }

        Dataset<Row> existingIds = spark.createDataset(new ArrayList<>(existing.keySet()), Encoders.STRING())
                .toDF("_id");
        Dataset<Row> fresh = valid.join(existingIds, valid.col("_id").equalTo(existingIds.col("_id")), "left_anti")
                .cache();
        long newRows = fresh.count();

        // 2. Guardar las observaciones nuevas.
        if (newRows > 0) {
            MongoWriter.upsert(fresh.select(
                    col("_id"),
                    to_timestamp(col("date")).alias("observed_at"),
                    col("market_code"),
                    lit(MARKET_NAME).alias("market_name"),
                    col("product_name"),
                    struct(col("price").alias("avg"), lit("PEN").alias("currency"),
                            lit("kg|l|unit").alias("unit_basis")).alias("price"),
                    col("volume_t"),
                    struct(lit("GMML_PDF").alias("system"), col("source_file").alias("file"),
                            col("extractor_version").alias("reader_version")).alias("source"),
                    current_timestamp().alias("ingested_at")), uri, db, "wholesale_prices");
        }

        // 3. Alertas de las observaciones nuevas, con las mismas reglas que el Batch.
        long alertCount = newRows > 0 ? alertsForNewRows(spark, fresh, uri, db) : 0;

        // 4. Registro de la ejecución.
        List<String> files = validRows.stream().map(r -> r.getString(2)).distinct().toList();
        logRun(uri, db, batchId, files, rowsRead, validRows.size(), outside, newRows, existing.size(), matches,
                mismatches, alertCount, started);

        System.out.printf("[lote %d] %s%n    leídas %d, del catálogo %d (fuera del catálogo: %d), nuevas %d, "
                        + "ya existían %d (coinciden con lo guardado: %d, difieren: %d) | alertas %d%n",
                batchId, String.join(", ", files), rowsRead, validRows.size(), outside.size(), newRows,
                existing.size(), matches, mismatches.size(), alertCount);
        mismatches.stream().limit(5).forEach(m -> System.out.println("    difiere: " + m));

        fresh.unpersist();
        valid.unpersist();
    }

    /** Precio guardado (price.avg) de los _id que ya existen en wholesale_prices. */
    static Map<String, Double> existingPrices(String uri, String db, List<String> ids) {
        Map<String, Double> out = new HashMap<>();
        try (MongoClient client = MongoClients.create(uri)) {
            MongoCollection<Document> c = client.getDatabase(db).getCollection("wholesale_prices");
            for (Document d : c.find(Filters.in("_id", ids)).projection(Projections.include("price.avg"))) {
                Document price = d.get("price", Document.class);
                Object v = price == null ? null : price.get("avg");
                out.put(d.getString("_id"), v instanceof Number n ? n.doubleValue() : null);
            }
        }
        return out;
    }

    static long alertsForNewRows(SparkSession spark, Dataset<Row> fresh, String uri, String db) {
        Row range = fresh.agg(org.apache.spark.sql.functions.min("date"), org.apache.spark.sql.functions.max("date"))
                .first();
        LocalDate from = range.getDate(0).toLocalDate().minusDays(HISTORY_DAYS);
        LocalDate to = range.getDate(1).toLocalDate();
        List<String> products = fresh.select("product_name").distinct().as(Encoders.STRING()).collectAsList();

        // Historial reciente de esos productos (ya incluye las filas nuevas recién guardadas).
        String productList = products.stream().map(p -> "\"" + p.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(","));
        String pipeline = "[{\"$match\": {\"market_code\": \"" + MARKET_CODE + "\", "
                + "\"product_name\": {\"$in\": [" + productList + "]}, "
                + "\"observed_at\": {\"$gte\": {\"$date\": \"" + from + "T00:00:00Z\"}, "
                + "\"$lte\": {\"$date\": \"" + to + "T00:00:00Z\"}}}}]";
        Dataset<Row> history = spark.read().format("mongodb")
                .option("connection.uri", uri).option("database", db)
                .option("collection", "wholesale_prices")
                .option("aggregation.pipeline", pipeline)
                .load()
                .select(to_date(col("observed_at")).alias("date"), col("market_code"), col("product_name"),
                        col("price.avg").alias("price"), col("volume_t"))
                .where(col("price").isNotNull());

        Dataset<Row> baselines = spark.read().format("mongodb")
                .option("connection.uri", uri).option("database", db)
                .option("collection", "price_analytics")
                .option("aggregation.pipeline",
                        "[{\"$match\": {\"analysis_type\": \"ALERT_BASELINE\", \"market_code\": \"" + MARKET_CODE + "\"}}]")
                .load();
        if (baselines.isEmpty()) {
            System.out.println("    Aviso: no hay rangos normales (ALERT_BASELINE). Ejecuta antes batch-analytics.");
            return 0;
        }
        Row params = baselines.select("k", "min_abs_change_pct").first();
        double k = params.getDouble(0);
        double minChange = params.getDouble(1);

        // Variación diaria sobre el historial y solo se conservan las filas nuevas.
        Dataset<Row> newKeys = fresh.select(col("date").alias("n_date"), col("product_name").alias("n_product"));
        Dataset<Row> daily = IqrAlertRules.dailyChanges(history);
        Dataset<Row> dailyNew = daily.join(newKeys,
                daily.col("date").equalTo(newKeys.col("n_date"))
                        .and(daily.col("product_name").equalTo(newKeys.col("n_product"))), "left_semi");

        Dataset<Row> alerts = IqrAlertRules.toAlertDocs(
                IqrAlertRules.flag(dailyNew, baselines.select("market_code", "product_name", "q1", "q3"), k, minChange),
                k, minChange, JOB, JOB_VERSION).cache();
        long n = alerts.count();
        if (n > 0) {
            MongoWriter.upsert(alerts, uri, db, "price_alerts");
            alerts.select(date_format(col("observed_at"), "yyyy-MM-dd").alias("fecha"), col("product_name"),
                            col("alert_type"), col("change_pct"), col("severity"))
                    .orderBy(col("severity"), col("product_name")).show(20, false);
        }
        alerts.unpersist();
        return n;
    }

    /** Productos con rango normal (ALERT_BASELINE); vacío si todavía no se ejecutó el Batch. */
    static List<String> catalog(String uri, String db) {
        try (MongoClient client = MongoClients.create(uri)) {
            return client.getDatabase(db).getCollection("price_analytics")
                    .distinct("product_name", Filters.eq("analysis_type", "ALERT_BASELINE"), String.class)
                    .into(new ArrayList<>());
        }
    }

    static void logRun(String uri, String db, long batchId, List<String> files, long rowsRead, long rowsValid,
                       List<String> outsideCatalog, long rowsNew, long rowsExisting, int matches,
                       List<String> mismatches, long alerts, Instant started) {
        Instant finished = Instant.now();
        Document log = new Document("_id", JOB + "|" + started.toEpochMilli() + "|" + batchId)
                .append("job", JOB)
                .append("job_version", JOB_VERSION)
                .append("batch_id", batchId)
                .append("files", files)
                .append("rows_read", rowsRead)
                .append("rows_valid", rowsValid)
                .append("products_outside_catalog", outsideCatalog)
                .append("rows_new", rowsNew)
                .append("rows_existing", rowsExisting)
                .append("existing_price_matches", matches)
                .append("existing_price_mismatches", mismatches.size())
                .append("mismatch_examples", mismatches.stream().limit(10).toList())
                .append("alerts_generated", alerts)
                .append("started_at", Date.from(started))
                .append("finished_at", Date.from(finished))
                .append("duration_ms", finished.toEpochMilli() - started.toEpochMilli())
                .append("status", "SUCCEEDED");
        try (MongoClient client = MongoClients.create(uri)) {
            client.getDatabase(db).getCollection("processing_logs").insertOne(log);
        }
    }
}
