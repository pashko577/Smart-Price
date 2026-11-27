package pe.smartprice.alert.domain;

import java.time.LocalDate;

/**
 * Variación diaria atípica de un producto, detectada por Apache Spark (método IQR) en el Batch o en el Streaming.
 *
 * @param lowerBound   límite inferior de la variación normal (%): Q1 − k·IQR
 * @param upperBound   límite superior de la variación normal (%): Q3 + k·IQR
 * @param k            multiplicador del IQR con el que se calculó la alerta (nulo = {@link #DEFAULT_K})
 * @param minChangePct variación mínima exigida para alertar (nulo = {@link #DEFAULT_MIN_CHANGE_PCT})
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
        Double volumeAvg30dT,
        Double k,
        Double minChangePct) {

    /** Una caída del ingreso de al menos este porcentaje se considera una señal de menor abastecimiento. */
    public static final double SUPPLY_DROP_THRESHOLD_PCT = 30.0;
    /** Parámetros por defecto del job Batch (BatchAnalyticsJob). */
    public static final double DEFAULT_K = 3.0;
    public static final double DEFAULT_MIN_CHANGE_PCT = 15.0;
    /** Umbrales de severidad: distancia al límite, medida en número de IQR. */
    public static final double HIGH_IQR_DISTANCE = 3.0;
    public static final double MEDIUM_IQR_DISTANCE = 1.0;

    /** Alerta sin los parámetros del cálculo (se asumen los valores por defecto del Batch). */
    public PriceAlert(String id, String productName, String marketCode, LocalDate observedAt, AlertType type,
                      Severity severity, Double previousPrice, Double currentPrice, Double changePct,
                      Double lowerBound, Double upperBound, Double volumeT, Double volumeAvg30dT) {
        this(id, productName, marketCode, observedAt, type, severity, previousPrice, currentPrice, changePct,
                lowerBound, upperBound, volumeT, volumeAvg30dT, null, null);
    }

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

    public double effectiveK() {
        return k == null ? DEFAULT_K : k;
    }

    public double effectiveMinChangePct() {
        return minChangePct == null ? DEFAULT_MIN_CHANGE_PCT : minChangePct;
    }

    /**
     * Rango intercuartílico (IQR) de la variación diaria del producto, deducido de los límites:
     * upper − lower = (Q3 − Q1) + 2k·IQR = IQR · (1 + 2k).
     */
    public Double iqr() {
        if (lowerBound == null || upperBound == null) {
            return null;
        }
        return (upperBound - lowerBound) / (1 + 2 * effectiveK());
    }

    /** Primer cuartil (Q1) de la variación diaria: lower + k·IQR. */
    public Double q1() {
        Double iqr = iqr();
        return iqr == null ? null : lowerBound + effectiveK() * iqr;
    }

    /** Tercer cuartil (Q3) de la variación diaria: upper − k·IQR. */
    public Double q3() {
        Double iqr = iqr();
        return iqr == null ? null : upperBound - effectiveK() * iqr;
    }

    /** Cuánto se salió la variación del rango normal, en puntos porcentuales (0 si no se salió). */
    public Double excessPoints() {
        if (changePct == null || lowerBound == null || upperBound == null) {
            return null;
        }
        if (changePct > upperBound) {
            return changePct - upperBound;
        }
        if (changePct < lowerBound) {
            return lowerBound - changePct;
        }
        return 0.0;
    }

    /**
     * Distancia al límite medida en número de IQR: la misma cuenta que hace Spark para la severidad.
     * Si el IQR es 0 (precio que casi nunca cambia), Spark usa la variación absoluta.
     */
    public Double iqrDistance() {
        Double excess = excessPoints();
        Double iqr = iqr();
        if (excess == null || iqr == null) {
            return null;
        }
        return iqr > 1e-9 ? excess / iqr : Math.abs(changePct);
    }
}
