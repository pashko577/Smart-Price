package pe.smartprice.jobs.sisap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import pe.smartprice.jobs.common.Hashes;

/**
 * Lee una exportación "Precio Promedio, Volumen" de SISAP (MIDAGRI).
 *
 * <p>Aunque la extensión es .xls, el archivo es una tabla HTML codificada en Latin-1:
 * {@code Fecha | Producto 1 (Precio, Volumen) | Producto 2 (Precio, Volumen) | ...}.
 * El resultado se devuelve en formato largo: una fila por fecha y producto.
 */
public final class SisapReader {

    public static final String READER_VERSION = "sisap_v1";
    public static final String MARKET = "Gran Mercado Mayorista de Lima";

    private static final Set<String> NULL_VALUES = Set.of("", "...", ":");
    private static final Pattern DATE = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private SisapReader() {
    }

    public static List<SisapRow> read(Path file) throws IOException {
        byte[] raw = Files.readAllBytes(file);
        Document doc = Jsoup.parse(new String(raw, StandardCharsets.ISO_8859_1));

        List<Element> titles = doc.select("h1");
        if (titles.isEmpty() || !titles.get(0).text().contains("Precio Promedio")) {
            throw new IllegalArgumentException(file.getFileName()
                    + ": no parece una exportación 'Precio Promedio, Volumen' de SISAP");
        }
        String periodLabel = titles.size() > 1 ? titles.get(1).text().trim() : "";

        List<List<String>> rows = new ArrayList<>();
        for (Element tr : doc.select("tr")) {
            // No usar eachText(): omite las celdas vacías y desalinea las columnas.
            rows.add(tr.select("td").stream().map(td -> td.text().strip()).toList());
        }
        if (rows.size() < 3) {
            throw new IllegalArgumentException(file.getFileName() + ": tabla vacía");
        }

        List<String> products = rows.get(0).subList(1, rows.get(0).size()); // Fecha + productos
        int expectedCells = 1 + 2 * products.size();                         // fecha + (precio, volumen)
        String sha256 = Hashes.sha256(raw);
        String fileName = file.getFileName().toString();

        List<SisapRow> out = new ArrayList<>();
        for (List<String> r : rows.subList(2, rows.size())) {
            if (r.size() != expectedCells || !DATE.matcher(r.get(0)).matches()) {
                continue;
            }
            LocalDate date = LocalDate.parse(r.get(0), DMY);
            for (int i = 0; i < products.size(); i++) {
                out.add(new SisapRow(date, products.get(i),
                        toNumber(r.get(1 + 2 * i)), toNumber(r.get(2 + 2 * i)),
                        MARKET, fileName, sha256, periodLabel, READER_VERSION));
            }
        }
        return out;
    }

    static Double toNumber(String value) {
        String v = value.strip().replace(",", "");
        return NULL_VALUES.contains(v) ? null : Double.valueOf(v);
    }
}
