package pe.smartprice.source.domain;

import java.time.Instant;
import java.util.List;

/** Una ejecución (micro-lote) del Streaming de boletines, registrada en processing_logs. */
public record ProcessingRun(
        String id,
        String job,
        Instant startedAt,
        long durationMs,
        List<String> files,
        long rowsRead,
        long rowsValid,
        int productsOutsideCatalog,
        long rowsNew,
        long rowsExisting,
        long priceMatches,
        long priceMismatches,
        long alertsGenerated,
        String status) {

    public ProcessingRun {
        files = files == null ? List.of() : List.copyOf(files);
    }

    /** Validación cruzada: todas las observaciones ya existentes coincidieron con el precio guardado. */
    public boolean crossCheckPassed() {
        return priceMismatches == 0;
    }
}
