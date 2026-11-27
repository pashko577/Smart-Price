package pe.smartprice.jobs.common;

import org.apache.spark.sql.SparkSession;

/** Crea la sesión Spark local usada por todos los jobs. */
public final class Sparks {

    private Sparks() {
    }

    public static SparkSession local(String appName) {
        SparkSession spark = SparkSession.builder()
                .appName(appName)
                .master("local[*]")
                .config("spark.sql.shuffle.partitions", "4")
                // Las fechas de observación no tienen hora: se tratan como 00:00 UTC.
                .config("spark.sql.session.timeZone", "UTC")
                .config("spark.ui.enabled", "false")
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");
        return spark;
    }
}
