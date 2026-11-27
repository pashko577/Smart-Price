package pe.smartprice.jobs.common;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Configuración leída del archivo .env de la raíz del proyecto (o de variables de entorno).
 * La URI de MongoDB nunca se imprime completa: usar {@link #maskedMongoUri()}.
 */
public final class AppConfig {

    private static final Dotenv ENV = Dotenv.configure().ignoreIfMissing().load();

    private AppConfig() {
    }

    public static String mongoUri() {
        String uri = ENV.get("MONGODB_URI", "");
        if (uri.isBlank() || uri.contains("PEGA_AQUI") || uri.contains("<db_password>")) {
            throw new IllegalStateException("Configura MONGODB_URI en el archivo .env");
        }
        return uri;
    }

    public static String mongoDatabase() {
        return ENV.get("MONGODB_DATABASE", "smartprice_db");
    }

    public static String maskedMongoUri() {
        return mongoUri().replaceAll("://([^:/]+):[^@]+@", "://$1:****@");
    }
}
