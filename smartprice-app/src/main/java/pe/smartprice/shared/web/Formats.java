package pe.smartprice.shared.web;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;

/** Formatos de presentación para las plantillas Thymeleaf: ${@fmt.pct(x)}. */
@Component("fmt")
public class Formats {

    private static final Locale ES_PE = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM yyyy", ES_PE);

    private static DecimalFormat df(String pattern) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(ES_PE));
    }

    /** "S/ 3.06" */
    public String soles(Double v) {
        return v == null ? "—" : "S/ " + df("#,##0.00").format(v);
    }

    /** "+12.3 %" / "−4.0 %" (con signo menos tipográfico). */
    public String pct(Double v) {
        if (v == null) {
            return "—";
        }
        String s = df("0.0").format(Math.abs(v));
        return (v > 0 ? "+" : v < 0 ? "−" : "") + s + " %";
    }

    public String num(Double v, int decimals) {
        return v == null ? "—" : df(decimals == 0 ? "#,##0" : "#,##0." + "0".repeat(decimals)).format(v);
    }

    public String num(Long v) {
        return v == null ? "—" : df("#,##0").format(v);
    }

    public String date(LocalDate d) {
        return d == null ? "—" : DATE.format(d);
    }

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final java.time.ZoneId LIMA = java.time.ZoneId.of("America/Lima");

    /** Fecha y hora en hora de Lima. */
    public String instant(java.time.Instant i) {
        return i == null ? "—" : DATE_TIME.format(i.atZone(LIMA));
    }

    public String shortDate(LocalDate d) {
        return d == null ? "—" : SHORT.format(d);
    }

    /** Clase CSS según el signo de la variación (subida = precio más caro). */
    public String tone(Double v) {
        if (v == null || Math.abs(v) < 5) {
            return "tone-neutral";
        }
        return v > 0 ? "tone-up" : "tone-down";
    }
}
