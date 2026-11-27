package pe.smartprice.shared.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import org.bson.Document;

/**
 * Lectura segura de documentos BSON escritos por Spark.
 * Las fechas de observación se guardan a las 00:00 UTC, así que se convierten en UTC.
 */
public final class Docs {

    private Docs() {
    }

    /** Valor en una ruta con puntos, por ejemplo "price.avg". */
    public static Object get(Document doc, String path) {
        Object current = doc;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Document d)) {
                return null;
            }
            current = d.get(part);
        }
        return current;
    }

    public static Double dbl(Document doc, String path) {
        return get(doc, path) instanceof Number n ? n.doubleValue() : null;
    }

    public static long lng(Document doc, String path) {
        return get(doc, path) instanceof Number n ? n.longValue() : 0L;
    }

    public static String str(Document doc, String path) {
        Object v = get(doc, path);
        return v == null ? null : v.toString();
    }

    public static LocalDate date(Document doc, String path) {
        Object v = get(doc, path);
        if (v instanceof Date d) {
            return d.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }
        if (v instanceof String s && !s.isBlank()) {
            return LocalDate.parse(s.substring(0, 10));
        }
        return null;
    }

    public static Instant instant(Document doc, String path) {
        return get(doc, path) instanceof Date d ? d.toInstant() : null;
    }

    /** Inicio del día en UTC, para comparar con observed_at. */
    public static Date utc(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
