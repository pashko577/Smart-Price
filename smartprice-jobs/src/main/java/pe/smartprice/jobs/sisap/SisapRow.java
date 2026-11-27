package pe.smartprice.jobs.sisap;

import java.time.LocalDate;

/** Una observación de SISAP en formato largo: un producto en una fecha. */
public record SisapRow(
        LocalDate observedAt,
        String productName,
        Double priceAvgPerUnit,
        Double volumeT,
        String market,
        String sourceFile,
        String sourceSha256,
        String periodLabel,
        String readerVersion) {
}
