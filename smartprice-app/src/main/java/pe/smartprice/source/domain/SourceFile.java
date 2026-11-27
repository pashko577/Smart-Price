package pe.smartprice.source.domain;

import java.time.Instant;
import java.time.LocalDate;

/** Archivo original del que provienen los datos (trazabilidad). */
public record SourceFile(
        String fileName,
        String system,
        String marketCode,
        String sha256,
        String periodLabel,
        LocalDate firstDate,
        LocalDate lastDate,
        long rows,
        String readerVersion,
        String acquisitionMethod,
        Instant loadedAt) {

    /** Primeros 12 caracteres del hash, suficientes para identificarlo en pantalla. */
    public String shortSha256() {
        return sha256 == null ? "" : sha256.substring(0, Math.min(12, sha256.length()));
    }
}
