package pe.smartprice.jobs.gmml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import pe.smartprice.jobs.common.CsvWriter;

/**
 * Extrae un boletín PDF del GMML a CSV.
 *
 * <p>Uso: {@code extract-pdf <archivo.pdf> [carpeta_salida]}
 */
public final class ExtractGmmlPdfJob {

    public static final List<String> HEADER = List.of(
            "observed_at", "product_name", "footnote",
            "volume_t_prev", "volume_t", "volume_t_avg7", "volume_t_avg4_wd",
            "unit", "kg_equivalent",
            "price_prev", "price", "price_avg7", "price_per_kg",
            "source_file", "source_sha256", "page", "extractor_version");

    private ExtractGmmlPdfJob() {
    }

    public static void run(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Uso: extract-pdf <archivo.pdf> [carpeta_salida]");
        }
        Path pdf = Path.of(args[0]);
        Path outDir = Path.of(args.length > 1 ? args[1] : "data/interim/gmml_csv");

        List<GmmlRow> rows = GmmlPdfExtractor.extract(pdf);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Sin filas extraídas: " + pdf.getFileName());
        }
        String stem = pdf.getFileName().toString().replaceFirst("\\.pdf$", "");
        Path out = outDir.resolve(stem + ".csv");
        writeCsv(rows, out);
        System.out.printf("%s: %d filas -> %s%n", pdf.getFileName(), rows.size(), out);
    }

    public static void writeCsv(List<GmmlRow> rows, Path out) throws IOException {
        CsvWriter.write(out, HEADER, rows.stream()
                .map(r -> Arrays.<Object>asList(r.observedAt(), r.productName(), r.footnote(),
                        r.volumeTPrev(), r.volumeT(), r.volumeTAvg7(), r.volumeTAvg4Wd(),
                        r.unit(), r.kgEquivalent(), r.pricePrev(), r.price(), r.priceAvg7(), r.pricePerKg(),
                        r.sourceFile(), r.sourceSha256(), r.page(), r.extractorVersion()))
                .toList());
    }
}
