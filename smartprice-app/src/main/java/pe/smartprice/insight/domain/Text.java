package pe.smartprice.insight.domain;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Formato de números y fechas para los textos que redacta el dominio (es-PE).
 * Es el mismo criterio que usan las plantillas, pero sin depender de Spring.
 */
public final class Text {

    private static final Locale ES_PE = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy", ES_PE);

    private Text() {
    }

    private static DecimalFormat df(String pattern) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(ES_PE));
    }

    /** Espacio que no se parte: "+86 %" y "S/ 3.06" nunca quedan en dos líneas. */
    public static final String NBSP = " ";

    /** "+12.3 %" / "−4.0 %" (signo menos tipográfico). */
    public static String pct(Double v) {
        if (v == null) {
            return "—";
        }
        String s = df("0.0").format(Math.abs(v));
        return (v > 0.05 ? "+" : v < -0.05 ? "−" : "") + s + NBSP + "%";
    }

    /** Porcentaje sin signo: "63 %". */
    public static String share(long part, long total) {
        return total == 0 ? "0 %" : df("0").format(100.0 * part / total) + " %";
    }

    /** "S/ 3.06" */
    public static String soles(Double v) {
        return v == null ? "—" : "S/" + NBSP + df("#,##0.00").format(v);
    }

    public static String num(Double v, int decimals) {
        if (v == null) {
            return "—";
        }
        String s = df(decimals == 0 ? "#,##0" : "#,##0." + "0".repeat(decimals)).format(Math.abs(v));
        return (v < 0 && Math.abs(v) >= Math.pow(10, -decimals) / 2 ? "−" : "") + s;
    }

    /** "45 t" */
    public static String tons(Double v) {
        return v == null ? "sin dato" : num(v, 0) + " t";
    }

    public static String date(LocalDate d) {
        return d == null ? "—" : DATE.format(d);
    }

    /** "nov. 2026" */
    public static String month(YearMonth m) {
        return m == null ? "—" : MONTH.format(m.atDay(1));
    }

    /** "A, B y C" */
    public static String list(List<String> items) {
        if (items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.get(0);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " y " + items.get(items.size() - 1);
    }

    /** "1 producto" / "3 productos" */
    public static String count(long n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }
}
