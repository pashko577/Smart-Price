package pe.smartprice.jobs.mongo;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bson.Document;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;

import pe.smartprice.jobs.common.AppConfig;

/**
 * Comandos de administración de MongoDB sin Spark.
 *
 * <ul>
 *   <li>{@code check-mongo}: ping y prueba de escritura en smartprice_db.</li>
 *   <li>{@code setup-mongo}: crea índices y muestra un resumen de la carga.</li>
 * </ul>
 */
public final class MongoSetupJob {

    private MongoSetupJob() {
    }

    private static MongoClient client() {
        return MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(AppConfig.mongoUri()))
                .applyToClusterSettings(b -> b.serverSelectionTimeout(15, TimeUnit.SECONDS))
                .build());
    }

    public static void check() {
        System.out.println("Conectando a: " + AppConfig.maskedMongoUri());
        try (MongoClient client = client()) {
            client.getDatabase("admin").runCommand(new Document("ping", 1));
            System.out.println("Ping OK: el cluster responde y las credenciales son válidas.");
            MongoDatabase db = client.getDatabase(AppConfig.mongoDatabase());
            MongoCollection<Document> c = db.getCollection("_connection_check");
            c.insertOne(new Document("ok", true));
            c.drop();
            System.out.println("Escritura OK en '" + AppConfig.mongoDatabase() + "'.");
        }
    }

    public static void setup() {
        try (MongoClient client = client()) {
            MongoDatabase db = client.getDatabase(AppConfig.mongoDatabase());
            MongoCollection<Document> prices = db.getCollection("wholesale_prices");

            // Consultas previstas: historial de un producto, precios de un día, filtro por mercado.
            prices.createIndex(Indexes.compoundIndex(Indexes.ascending("product_name"),
                    Indexes.descending("observed_at")), new IndexOptions().name("product_date"));
            prices.createIndex(Indexes.descending("observed_at"), new IndexOptions().name("date"));
            prices.createIndex(Indexes.compoundIndex(Indexes.ascending("market_code"),
                    Indexes.descending("observed_at")), new IndexOptions().name("market_date"));

            long total = prices.countDocuments();
            int products = prices.distinct("product_name", String.class).into(new ArrayList<>()).size();
            Date first = prices.find().sort(Sorts.ascending("observed_at")).first().getDate("observed_at");
            Date last = prices.find().sort(Sorts.descending("observed_at")).first().getDate("observed_at");
            long nulls = prices.countDocuments(new Document("price.avg", null));
            Document stats = db.runCommand(new Document("collStats", "wholesale_prices"));
            List<String> indexes = prices.listIndexes().map(d -> d.getString("name")).into(new ArrayList<>());

            System.out.printf("wholesale_prices: %d documentos | %d productos | %s a %s%n",
                    total, products, utcDate(first), utcDate(last));
            System.out.println("Precios nulos: " + nulls);
            System.out.printf("Tamaño: datos %.1f MB, almacenamiento %.1f MB, índices %.1f MB%n",
                    mb(stats, "size"), mb(stats, "storageSize"), mb(stats, "totalIndexSize"));
            System.out.println("Índices: " + indexes);
            System.out.println("source_files: " + db.getCollection("source_files").countDocuments() + " archivos");

            Document doc = prices.find(new Document("_id", "2026-11-06|GMML|Aji Rocoto")).first();
            if (doc != null) {
                doc.remove("ingested_at");
                System.out.println("\nEjemplo: " + doc.toJson());
            }

            MongoCollection<Document> analytics = db.getCollection("price_analytics");
            MongoCollection<Document> alerts = db.getCollection("price_alerts");
            System.out.printf("%nprice_analytics: %d PRICE_MONTHLY, %d PRODUCT_SUMMARY%n",
                    analytics.countDocuments(new Document("analysis_type", "PRICE_MONTHLY")),
                    analytics.countDocuments(new Document("analysis_type", "PRODUCT_SUMMARY")));
            System.out.printf("price_alerts: %d (HIGH %d, MEDIUM %d, LOW %d)%n", alerts.countDocuments(),
                    alerts.countDocuments(new Document("severity", "HIGH")),
                    alerts.countDocuments(new Document("severity", "MEDIUM")),
                    alerts.countDocuments(new Document("severity", "LOW")));
            Document alert = alerts.find(new Document("product_name", "Zanahoria"))
                    .sort(Sorts.descending("observed_at")).first();
            if (alert != null) {
                alert.remove("generated_at");
                System.out.println("\nEjemplo de alerta: " + alert.toJson());
            }
        }
    }

    /** Las fechas de observación se guardan a las 00:00 UTC: se muestran en UTC, no en hora local. */
    private static LocalDate utcDate(Date d) {
        return d.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }

    private static double mb(Document stats, String key) {
        return ((Number) stats.get(key)).doubleValue() / 1e6;
    }
}
