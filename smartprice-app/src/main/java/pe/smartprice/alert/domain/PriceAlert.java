package pe.smartprice.alert.domain;

import java.time.LocalDate;

/**
 * Variación diaria atípica de un producto, detectada por el job Batch de Spark (método IQR).
 *
 * @param lowerBound límite inferior de la variación normal (%)
 * @param upperBound límite superior de la variación normal (%)
 */
public record PriceAlert(
        String id,
        String productName,
        String marketCode,
        LocalDate observedAt,
        AlertType type,
        Severity severity,
        Double previousPrice,
        Double currentPrice,
        Double changePct,
        Double lowerBound,
        Double upperBound,
        Double volumeT,
        Double volumeAvg30dT) {

    /** Una caída del ingreso de al menos este porcentaje se considera una señal de menor abastecimiento. */
    public static final double SUPPLY_DROP_THRESHOLD_PCT = 30.0;

    /** Variación del volumen del día frente al promedio de los 30 días anteriores (%). */
    public Double volumeChangePct() {
        if (volumeT == null || volumeAvg30dT == null || volumeAvg30dT == 0) {
            return null;
        }
        return (volumeT - volumeAvg30dT) / volumeAvg30dT * 100;
    }

    /**
     * Regla de negocio: una subida de precio acompañada de una caída fuerte del volumen
     * tiene una explicación probable de abastecimiento.
     */
    public boolean isExplainedBySupplyDrop() {
        Double v = volumeChangePct();
        return type == AlertType.PRICE_SPIKE && v != null && v <= -SUPPLY_DROP_THRESHOLD_PCT;
    }
}
