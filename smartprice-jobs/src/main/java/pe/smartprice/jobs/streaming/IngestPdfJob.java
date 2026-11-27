package pe.smartprice.jobs.streaming;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import pe.smartprice.jobs.gmml.ExtractGmmlPdfJob;
import pe.smartprice.jobs.gmml.GmmlPdfExtractor;
import pe.smartprice.jobs.gmml.GmmlRow;

/**
 * Productor de eventos del Streaming: extrae un boletín PDF del GMML y deja el CSV en la carpeta
 * que vigila Spark Structured Streaming.
 *
 * <p>El CSV se escribe primero en una carpeta temporal y luego se mueve de forma atómica:
 * la fuente de archivos de Spark exige que cada archivo aparezca completo.
 *
 * <p>Uso: {@code ingest-pdf <boletin.pdf> [<boletin2.pdf> ...]}
 */
public final class IngestPdfJob {

    static final Path STAGING = Path.of("data/streaming/staging");
    public static final Path INCOMING = Path.of("data/streaming/incoming");

    private IngestPdfJob() {
    }

    public static void run(String[] args) throws IOException {
        if (args.length == 0) {
            throw new IllegalArgumentException("Uso: ingest-pdf <boletin.pdf> [<boletin2.pdf> ...]");
        }
        Files.createDirectories(STAGING);
        Files.createDirectories(INCOMING);
        for (String arg : args) {
            Path pdf = Path.of(arg);
            List<GmmlRow> rows = GmmlPdfExtractor.extract(pdf);
            if (rows.isEmpty()) {
                System.out.printf("%s: sin filas, se omite%n", pdf.getFileName());
                continue;
            }
            String stem = pdf.getFileName().toString().replaceFirst("\\.pdf$", "");
            Path tmp = STAGING.resolve(stem + ".csv");
            ExtractGmmlPdfJob.writeCsv(rows, tmp);
            Path target = INCOMING.resolve(stem + ".csv");
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("%s: %d filas del %s -> %s%n", pdf.getFileName(), rows.size(),
                    rows.get(0).observedAt(), target);
        }
    }
}
