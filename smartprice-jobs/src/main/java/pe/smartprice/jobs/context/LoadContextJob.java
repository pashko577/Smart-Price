package pe.smartprice.jobs.context;

import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.coalesce;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.concat_ws;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.date_format;
import static org.apache.spark.sql.functions.element_at;
import static org.apache.spark.sql.functions.exp;
import static org.apache.spark.sql.functions.explode;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.log;
import static org.apache.spark.sql.functions.make_date;
import static org.apache.spark.sql.functions.round;
import static org.apache.spark.sql.functions.split;
import static org.apache.spark.sql.functions.to_date;
import static org.apache.spark.sql.functions.to_timestamp;
import static org.apache.spark.sql.functions.trunc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import pe.smartprice.jobs.common.AppConfig;
import pe.smartprice.jobs.common.Hashes;
import pe.smartprice.jobs.common.Sparks;
import pe.smartprice.jobs.mongo.MongoWriter;

/**
 * Indicadores de contexto para comparar el mercado mayorista con los supermercados y la inflación.
 *
 * <p>Escribe en {@code price_indexes} (un documento por fuente, indicador y fecha):
 * <ul>
 *   <li>{@code QHAWARINA}: índice diario de precios de supermercados (Plaza Vea, Metro, Wong),
 *       base 10-02-2026 = 100. Archivo: data/raw/qhawarina/*.json</li>
 *   <li>{@code BCRP_INEI}: IPC de Lima Metropolitana (INEI, publicado por el BCRP), base dic. 2021 = 100.
 *       Archivos: data/raw/bcrp/*.json</li>
 *   <li>{@code SMARTPRICE}: índices del Gran Mercado Mayorista calculados aquí con la fórmula de Jevons
 *       (media geométrica de los cambios de precio de cada producto), igual que Qhawarina:
 *       mensual con base ene. 2024 = 100 y diario con base 10-02-2026 = 100.</li>
 * </ul>
 * Cada archivo de origen se registra también en {@code source_files} con su SHA-256.
 *
 * <p>Uso: {@code load-context}
 */
public final class LoadContextJob {

    static final String JOB = "load_context";
    static final LocalDate MONTHLY_BASE = LocalDate.of(2024, 1, 1);
    static final LocalDate DAILY_BASE = LocalDate.of(2026, 2, 10);   // base del índice de Qhawarina

    /** Indicadores de Qhawarina que se cargan, con su etiqueta. */
    static final Map<String, String> QHAWARINA_INDICATORS = Map.of(
            "index_all", "Supermercados: índice general",
            "index_food", "Supermercados: alimentos",
            "index_verduras", "Supermercados: verduras",
            "index_frutas", "Supermercados: frutas");

    private LoadContextJob() {
    }

    public static void run(String[] args) throws IOException {
        String uri = AppConfig.mongoUri();
        String db = AppConfig.mongoDatabase();
        Path qhDir = Path.of("data/raw/qhawarina");
        Path bcrpDir = Path.of("data/raw/bcrp");

        SparkSession spark = Sparks.local("SmartPrice-Context");
        try {
            List<Dataset<Row>> parts = new ArrayList<>();
            List<Row> files = new ArrayList<>();

            // 1. Qhawarina (supermercados, diario)
            for (Path f : jsonFiles(qhDir)) {
                Dataset<Row> series = spark.read().option("multiLine", true).json(f.toString())
                        .select(explode(col("series")).alias("s")).select("s.*");
                for (Map.Entry<String, String> ind : QHAWARINA_INDICATORS.entrySet()) {
                    parts.add(series.select(
                            lit("QHAWARINA").alias("source"),
                            lit(ind.getKey()).alias("indicator"),
                            lit(ind.getValue()).alias("label"),
                            lit("DAILY").alias("frequency"),
                            to_date(col("date")).alias("date"),
                            col(ind.getKey()).cast("double").alias("value"),
                            lit("10-02-2026 = 100").alias("base_label"),
                            col("interpolated").cast("boolean").alias("interpolated"),
                            lit(f.getFileName().toString()).alias("source_file")));
                }
                files.add(fileRow(f, "QHAWARINA", "download", series.count()));
            }

            // 2. BCRP / INEI (IPC Lima Metropolitana, mensual)
            for (Path f : jsonFiles(bcrpDir)) {
                String code = f.getFileName().toString().split("_")[0];
                Dataset<Row> raw = spark.read().option("multiLine", true).json(f.toString());
                String label = raw.select(element_at(col("config.series.name"), 1)).first().getString(0)
                        .replaceFirst("^.* - ", "")
                        .replace("Índice de Precios al Consumidor (IPC)", "IPC Lima Metropolitana")
                        .replace("IPC Sin Alimentos y Bebidas", "IPC Lima sin alimentos y bebidas");
                Dataset<Row> periods = raw.select(explode(col("periods")).alias("p"))
                        .select(split(col("p.name"), "\\.").alias("ym"), element_at(col("p.values"), 1).alias("v"))
                        .select(make_date(element_at(col("ym"), 2).cast("int"), monthNumber(element_at(col("ym"), 1)),
                                lit(1)).alias("date"), col("v").cast("double").alias("value"))
                        .where(col("value").isNotNull());
                parts.add(periods.select(
                        lit("BCRP_INEI").alias("source"),
                        lit(code).alias("indicator"),
                        lit(label).alias("label"),
                        lit("MONTHLY").alias("frequency"),
                        col("date"),
                        col("value"),
                        lit("dic. 2021 = 100").alias("base_label"),
                        lit(false).alias("interpolated"),
                        lit(f.getFileName().toString()).alias("source_file")));
                files.add(fileRow(f, "BCRP_INEI", "api_download", periods.count()));
            }

            // 3. Índices del mercado mayorista (Jevons) calculados desde wholesale_prices
            Dataset<Row> prices = spark.read().format("mongodb")
                    .option("connection.uri", uri).option("database", db)
                    .option("collection", "wholesale_prices").load()
                    .select(to_date(col("observed_at")).alias("date"), col("product_name"),
                            col("price.avg").alias("price"))
                    .where(col("price").isNotNull().and(col("price").gt(0)))
                    .cache();

            Dataset<Row> monthlyPrices = prices.withColumn("month", trunc(col("date"), "month"))
                    .groupBy("product_name", "month").agg(avg("price").alias("price"));
            parts.add(jevons(monthlyPrices.withColumnRenamed("month", "date"), MONTHLY_BASE,
                    "gmml_jevons_monthly", "Mercado mayorista GMML (59 productos)", "MONTHLY", "ene. 2024 = 100"));
            parts.add(jevons(prices.where(col("date").geq(lit(DAILY_BASE.toString()))), DAILY_BASE,
                    "gmml_jevons_daily", "Mercado mayorista GMML (59 productos)", "DAILY", "10-02-2026 = 100"));

            Dataset<Row> all = parts.stream().reduce(Dataset::unionByName).orElseThrow()
                    .where(col("value").isNotNull())
                    .select(
                            concat_ws("|", col("source"), col("indicator"),
                                    date_format(col("date"), "yyyy-MM-dd")).alias("_id"),
                            col("source"), col("indicator"), col("label"), col("frequency"),
                            to_timestamp(col("date")).alias("date"),
                            round(col("value"), 4).alias("value"),
                            col("base_label"), col("interpolated"), col("source_file"),
                            lit(JOB).alias("job"),
                            current_timestamp().alias("generated_at"))
                    .cache();

            MongoWriter.upsert(all, uri, db, "price_indexes");
            MongoWriter.upsert(spark.createDataFrame(files, fileSchema())
                    .withColumn("loaded_at", current_timestamp()), uri, db, "source_files");

            System.out.println("price_indexes por fuente e indicador:");
            all.groupBy("source", "indicator", "frequency")
                    .agg(count("*").alias("puntos"),
                            date_format(org.apache.spark.sql.functions.min("date"), "yyyy-MM-dd").alias("desde"),
                            date_format(org.apache.spark.sql.functions.max("date"), "yyyy-MM-dd").alias("hasta"))
                    .orderBy("source", "indicator").show(false);
            System.out.println("Archivos registrados en source_files: " + files.size());
        } finally {
            spark.stop();
        }
    }

    /**
     * Índice de Jevons: 100 × media geométrica de (precio / precio base) de los productos que tienen
     * precio en la fecha base. Entrada: product_name, date, price.
     */
    static Dataset<Row> jevons(Dataset<Row> prices, LocalDate base, String indicator, String label,
                               String frequency, String baseLabel) {
        Dataset<Row> basePrices = prices.where(col("date").equalTo(lit(base.toString()).cast("date")))
                .select(col("product_name").alias("b_product"), col("price").alias("base_price"));
        return prices.join(basePrices, col("product_name").equalTo(col("b_product")))
                .groupBy("date")
                .agg(exp(avg(log(col("price").divide(col("base_price"))))).multiply(100).alias("value"),
                        count("*").alias("products"))
                .select(lit("SMARTPRICE").alias("source"), lit(indicator).alias("indicator"),
                        lit(label).alias("label"), lit(frequency).alias("frequency"), col("date"),
                        col("value"), lit(baseLabel).alias("base_label"), lit(false).alias("interpolated"),
                        lit("wholesale_prices").alias("source_file"));
    }

    /** "Ene" -> 1 ... "Dic" -> 12 (abreviaturas del BCRP; acepta "Set" y "Sep"). */
    static Column monthNumber(Column abbr) {
        String[] names = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
        Column result = lit(null).cast("int");
        for (int i = names.length - 1; i >= 0; i--) {
            result = org.apache.spark.sql.functions.when(abbr.equalTo(names[i]), lit(i + 1)).otherwise(result);
        }
        return coalesce(org.apache.spark.sql.functions.when(abbr.equalTo("Set"), lit(9)), result);
    }

    static List<Path> jsonFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
    }

    static Row fileRow(Path f, String system, String method, long rows) throws IOException {
        return RowFactory.create(f.getFileName().toString(), system, "CONTEXT",
                Hashes.sha256(Files.readAllBytes(f)), f.getParent().getFileName().toString(), rows, method);
    }

    static StructType fileSchema() {
        return new StructType()
                .add("_id", DataTypes.StringType)
                .add("system", DataTypes.StringType)
                .add("market_code", DataTypes.StringType)
                .add("sha256", DataTypes.StringType)
                .add("period_label", DataTypes.StringType)
                .add("rows", DataTypes.LongType)
                .add("acquisition_method", DataTypes.StringType);
    }
}
