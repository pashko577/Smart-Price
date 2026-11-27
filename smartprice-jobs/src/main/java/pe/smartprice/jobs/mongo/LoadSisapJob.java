package pe.smartprice.jobs.mongo;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.concat_ws;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.max;
import static org.apache.spark.sql.functions.min;
import static org.apache.spark.sql.functions.struct;
import static org.apache.spark.sql.functions.to_timestamp;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import pe.smartprice.jobs.common.AppConfig;
import pe.smartprice.jobs.common.Sparks;

/**
 * Carga el histórico SISAP (data/interim/sisap_prices.csv) en MongoDB Atlas.
 *
 * <ul>
 *   <li>{@code wholesale_prices}: una observación por fecha, mercado y producto. El _id es
 *       determinista ("2026-11-06|GMML|Aji Rocoto"), así que repetir la carga reemplaza
 *       documentos en lugar de duplicarlos.</li>
 *   <li>{@code source_files}: un documento por archivo original (hash, periodo, filas).</li>
 * </ul>
 *
 * <p>Uso: {@code load-sisap [csv_entrada]}
 */
public final class LoadSisapJob {

    static final String MARKET_CODE = "GMML";

    private LoadSisapJob() {
    }

    public static void run(String[] args) {
        String input = args.length > 0 ? args[0] : "data/interim/sisap_prices.csv";
        String uri = AppConfig.mongoUri();
        String db = AppConfig.mongoDatabase();

        SparkSession spark = Sparks.local("SmartPrice-SISAP-Load");
        try {
            StructType schema = new StructType()
                    .add("observed_at", DataTypes.StringType)
                    .add("product_name", DataTypes.StringType)
                    .add("price_avg_per_unit", DataTypes.DoubleType)
                    .add("volume_t", DataTypes.DoubleType)
                    .add("market", DataTypes.StringType)
                    .add("source_file", DataTypes.StringType)
                    .add("source_sha256", DataTypes.StringType)
                    .add("period_label", DataTypes.StringType)
                    .add("reader_version", DataTypes.StringType);

            Dataset<Row> raw = spark.read().option("header", true).option("encoding", "UTF-8")
                    .schema(schema).csv(input);

            Dataset<Row> prices = raw.select(
                    concat_ws("|", col("observed_at"), lit(MARKET_CODE), col("product_name")).alias("_id"),
                    to_timestamp(col("observed_at")).alias("observed_at"),
                    lit(MARKET_CODE).alias("market_code"),
                    col("market").alias("market_name"),
                    col("product_name"),
                    struct(
                            col("price_avg_per_unit").alias("avg"),
                            lit("PEN").alias("currency"),
                            lit("kg|l|unit").alias("unit_basis")).alias("price"),
                    col("volume_t"),
                    struct(
                            lit("SISAP").alias("system"),
                            col("source_file").alias("file"),
                            col("reader_version")).alias("source"),
                    current_timestamp().alias("ingested_at"));

            Dataset<Row> files = raw
                    .groupBy("source_file", "source_sha256", "period_label", "reader_version")
                    .agg(count("*").alias("rows"),
                            min("observed_at").alias("first_date"),
                            max("observed_at").alias("last_date"))
                    .select(
                            col("source_file").alias("_id"),
                            lit("SISAP").alias("system"),
                            lit(MARKET_CODE).alias("market_code"),
                            col("source_sha256").alias("sha256"),
                            col("period_label"), col("first_date"), col("last_date"), col("rows"),
                            col("reader_version"),
                            lit("manual_export").alias("acquisition_method"),
                            current_timestamp().alias("loaded_at"));

            long n = prices.count();
            System.out.println("Observaciones a cargar: " + n);

            MongoWriter.upsert(prices, uri, db, "wholesale_prices");
            MongoWriter.upsert(files, uri, db, "source_files");

            System.out.printf("Carga completada en %s.wholesale_prices (%d documentos) y %s.source_files.%n",
                    db, n, db);
        } finally {
            spark.stop();
        }
    }
}
