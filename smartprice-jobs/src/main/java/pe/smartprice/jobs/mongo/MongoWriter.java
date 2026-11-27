package pe.smartprice.jobs.mongo;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/** Escritura idempotente de DataFrames en MongoDB mediante el conector de Spark. */
public final class MongoWriter {

    private MongoWriter() {
    }

    /**
     * Reemplaza (o inserta) cada documento según su _id. Volver a ejecutar no crea duplicados.
     * La URI se pasa como opción de escritura y no como configuración de la sesión.
     */
    public static void upsert(Dataset<Row> df, String uri, String database, String collection) {
        df.write().format("mongodb").mode("append")
                .option("connection.uri", uri)
                .option("database", database)
                .option("collection", collection)
                .option("operationType", "replace")
                .option("idFieldList", "_id")
                .option("upsertDocument", "true")
                .save();
    }
}
