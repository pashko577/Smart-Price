package pe.smartprice.insight.domain;

import java.time.LocalDate;

/**
 * Variación del índice mayorista frente al IPC de Lima en el mismo periodo (ambos con la misma base).
 *
 * @param wholesalePct variación del índice mayorista (Jevons, 59 productos)
 * @param cpiPct       variación del IPC de Lima Metropolitana (INEI)
 */
public record InflationComparison(LocalDate from, LocalDate to, Double wholesalePct, Double cpiPct) {

    /** Diferencia en puntos porcentuales (mayorista − IPC). */
    public Double gapPoints() {
        return wholesalePct == null || cpiPct == null ? null : wholesalePct - cpiPct;
    }

    public boolean isComplete() {
        return from != null && to != null && wholesalePct != null && cpiPct != null;
    }
}
