package pe.smartprice.jobs.sisap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import pe.smartprice.jobs.common.CsvWriter;

/**
 * Une todas las exportaciones data/raw/sisap/sisap_*.xls en un único CSV en formato largo.
 *
 * <p>Uso: {@code sisap-to-csv [carpeta_entrada] [csv_salida]}
 */
public final class SisapToCsvJob {

    public static final List<String> HEADER = List.of(
            "observed_at", "product_name", "price_avg_per_unit", "volume_t", "market",
            "source_file", "source_sha256", "period_label", "reader_version");

    private SisapToCsvJob() {
    }

    public static void run(String[] args) throws IOException {
        Path inDir = Path.of(args.length > 0 ? args[0] : "data/raw/sisap");
        Path outFile = Path.of(args.length > 1 ? args[1] : "data/interim/sisap_prices.csv");

        List<Path> files;
        try (Stream<Path> s = Files.list(inDir)) {
            files = s.filter(p -> p.getFileName().toString().matches("sisap_.*\\.xls")).sorted().toList();
        }
        if (files.isEmpty()) {
            throw new IllegalStateException("No hay archivos sisap_*.xls en " + inDir);
        }

        List<SisapRow> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Path f : files) {
            List<SisapRow> rows = SisapReader.read(f);
            int dupes = 0;
            for (SisapRow r : rows) {
                if (seen.add(r.observedAt() + "|" + r.productName())) {
                    all.add(r);
                } else {
                    dupes++;
                }
            }
            System.out.printf("%s: %d filas (%d duplicadas descartadas)%n", f.getFileName(), rows.size(), dupes);
        }

        all.sort(Comparator.comparing(SisapRow::observedAt).thenComparing(SisapRow::productName));
        List<List<Object>> csvRows = all.stream()
                .map(r -> Arrays.<Object>asList(r.observedAt(), r.productName(), r.priceAvgPerUnit(),
                        r.volumeT(), r.market(), r.sourceFile(), r.sourceSha256(), r.periodLabel(),
                        r.readerVersion()))
                .toList();
        CsvWriter.write(outFile, HEADER, csvRows);

        long products = all.stream().map(SisapRow::productName).distinct().count();
        long nulls = all.stream().filter(r -> r.priceAvgPerUnit() == null).count();
        System.out.printf("%nTotal: %d filas | %d productos | %s a %s | precios vacíos: %d%nSalida: %s%n",
                all.size(), products, all.get(0).observedAt(), all.get(all.size() - 1).observedAt(),
                nulls, outFile);
    }
}
