package pe.smartprice.jobs.analytics;

import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.desc;
import static org.apache.spark.sql.functions.max;
import static org.apache.spark.sql.functions.min;
import static org.apache.spark.sql.functions.round;
import static org.apache.spark.sql.functions.to_date;
import static org.apache.spark.sql.functions.year;

import java.util.List;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import pe.smartprice.jobs.common.Sparks;

/**
 * Primera exploración del histórico SISAP: rango de fechas y variación del precio promedio anual.
 *
 * <p>Uso: {@code explore-sisap [csv_entrada]}
 */
public final class ExploreSisapJob {

    private ExploreSisapJob() {
    }

    public static void run(String[] args) {
        String input = args.length > 0 ? args[0] : "data/interim/sisap_prices.csv";
        SparkSession spark = Sparks.local("SmartPrice-SISAP-Explore");
        try {
            StructType schema = new StructType()
                    .add("observed_at", DataTypes.StringType)
                    .add("product_name", DataTypes.StringType)
                    .add("price_avg_per_unit", DataTypes.DoubleType)
                    .add("volume_t", DataTypes.DoubleType);

            Dataset<Row> df = spark.read().option("header", true).option("encoding", "UTF-8")
                    .schema(schema).csv(input)
                    .withColumn("observed_at", to_date(col("observed_at")));

            System.out.printf("Filas: %d | Productos: %d%n",
                    df.count(), df.select("product_name").distinct().count());
            df.agg(min("observed_at").alias("desde"), max("observed_at").alias("hasta")).show();

            Dataset<Row> yearly = df.withColumn("year", year(col("observed_at")))
                    .groupBy("product_name")
                    .pivot("year", List.of(2024, 2025, 2026))
                    .agg(round(avg("price_avg_per_unit"), 2))
                    .withColumn("var_2024_2026_pct",
                            round(col("2026").minus(col("2024")).divide(col("2024")).multiply(100), 1));

            System.out.println("Mayores subidas de precio promedio 2024 -> 2026:");
            yearly.orderBy(desc("var_2024_2026_pct")).show(8, false);
            System.out.println("Mayores bajadas:");
            yearly.orderBy(col("var_2024_2026_pct")).show(5, false);
        } finally {
            spark.stop();
        }
    }
}
