package pe.smartprice.jobs.gmml;

import java.time.LocalDate;

/** Una fila de la tabla de precios de un boletín diario del GMML. */
public record GmmlRow(
        LocalDate observedAt,
        String productName,
        String footnote,
        Double volumeTPrev,
        Double volumeT,
        Double volumeTAvg7,
        Double volumeTAvg4Wd,
        String unit,
        Double kgEquivalent,
        Double pricePrev,
        Double price,
        Double priceAvg7,
        Double pricePerKg,
        String sourceFile,
        String sourceSha256,
        int page,
        String extractorVersion) {
}
