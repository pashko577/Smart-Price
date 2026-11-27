package pe.smartprice.jobs.gmml;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import pe.smartprice.jobs.common.Hashes;

/**
 * Extrae la tabla de precios de un boletín diario del Gran Mercado Mayorista de Lima (MIDAGRI).
 *
 * <p>Cada fila de datos del PDF tiene la forma:
 * {@code Producto [nota/] volAyer volHoy volProm7 volProm4 Unidad [de varias palabras] equivKg precioAyer precioHoy precioProm7}
 * donde los volúmenes pueden ser ":" (sin dato). La fila se interpreta de derecha a izquierda.
 */
public final class GmmlPdfExtractor {

    public static final String EXTRACTOR_VERSION = "gmml_v1";

    private static final Map<String, Integer> MESES = Map.ofEntries(
            Map.entry("enero", 1), Map.entry("febrero", 2), Map.entry("marzo", 3), Map.entry("abril", 4),
            Map.entry("mayo", 5), Map.entry("junio", 6), Map.entry("julio", 7), Map.entry("agosto", 8),
            Map.entry("setiembre", 9), Map.entry("septiembre", 9), Map.entry("octubre", 10),
            Map.entry("noviembre", 11), Map.entry("diciembre", 12));

    private static final Pattern HEADER_DATE = Pattern.compile("(\\d{1,2}) de (\\p{L}+) de (\\d{4})");
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:[.,]\\d+)?");
    private static final Pattern VOLUME = Pattern.compile("-?\\d+(?:[.,]\\d+)?|:");
    private static final Pattern UNIT_WORD = Pattern.compile("\\p{L}+");
    private static final Pattern FOOTNOTE = Pattern.compile("^(.*?)\\s+(\\d+)/$");

    private GmmlPdfExtractor() {
    }

    public static List<GmmlRow> extract(Path pdf) throws IOException {
        byte[] raw = Files.readAllBytes(pdf);
        String sha256 = Hashes.sha256(raw);
        String fileName = pdf.getFileName().toString();

        List<GmmlRow> rows = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(raw)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            for (int page = 1; page <= doc.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(doc);
                LocalDate date = parseDate(text);
                for (String line : text.split("\\R")) {
                    GmmlRow row = parseLine(line.strip(), date, fileName, sha256, page);
                    if (row != null) {
                        rows.add(row);
                    }
                }
            }
        }
        return rows;
    }

    static LocalDate parseDate(String text) {
        Matcher m = HEADER_DATE.matcher(text);
        if (!m.find()) {
            return null;
        }
        Integer month = MESES.get(m.group(2).toLowerCase());
        return month == null ? null
                : LocalDate.of(Integer.parseInt(m.group(3)), month, Integer.parseInt(m.group(1)));
    }

    /** Devuelve null si la línea no es una fila de datos. */
    static GmmlRow parseLine(String line, LocalDate date, String file, String sha256, int page) {
        String[] t = line.split("\\s+");
        int n = t.length;
        // Mínimo: nombre(1) + 4 volúmenes + unidad(1) + 4 números = 10 tokens.
        if (n < 10) {
            return null;
        }
        // 4 números finales: equivalencia kg, precio ayer, precio hoy, promedio 7 días.
        for (int i = n - 4; i < n; i++) {
            if (!NUMBER.matcher(t[i]).matches()) {
                return null;
            }
        }
        // Unidad: una o más palabras justo antes de los números.
        int unitEnd = n - 4;
        int unitStart = unitEnd;
        while (unitStart > 0 && UNIT_WORD.matcher(t[unitStart - 1]).matches()) {
            unitStart--;
        }
        if (unitStart == unitEnd || unitStart < 5) {
            return null;
        }
        // 4 volúmenes antes de la unidad.
        for (int i = unitStart - 4; i < unitStart; i++) {
            if (!VOLUME.matcher(t[i]).matches()) {
                return null;
            }
        }
        String nameRaw = String.join(" ", Arrays.copyOfRange(t, 0, unitStart - 4));
        String unit = String.join(" ", Arrays.copyOfRange(t, unitStart, unitEnd));

        String name = nameRaw;
        String footnote = null;
        Matcher fm = FOOTNOTE.matcher(nameRaw);
        if (fm.matches()) {
            name = fm.group(1);
            footnote = fm.group(2);
        }

        Double kg = num(t[n - 4]);
        Double price = num(t[n - 2]);
        Double perKg = (price != null && kg != null && kg != 0)
                ? BigDecimal.valueOf(price / kg).setScale(4, RoundingMode.HALF_EVEN).doubleValue()
                : null;

        return new GmmlRow(date, name, footnote,
                num(t[unitStart - 4]), num(t[unitStart - 3]), num(t[unitStart - 2]), num(t[unitStart - 1]),
                unit, kg, num(t[n - 3]), price, num(t[n - 1]), perKg,
                file, sha256, page, EXTRACTOR_VERSION);
    }

    private static Double num(String token) {
        return ":".equals(token) ? null : Double.valueOf(token.replace(",", ""));
    }
}
