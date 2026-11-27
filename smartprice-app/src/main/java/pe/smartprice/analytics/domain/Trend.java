package pe.smartprice.analytics.domain;

/** Dirección de la variación de precio frente a un periodo de referencia. */
public enum Trend {
    UP, DOWN, STABLE, UNKNOWN;

    /** Umbral (en %) por debajo del cual una variación se considera estable. */
    public static final double STABLE_THRESHOLD_PCT = 5.0;

    public static Trend of(Double changePct) {
        if (changePct == null) {
            return UNKNOWN;
        }
        if (changePct >= STABLE_THRESHOLD_PCT) {
            return UP;
        }
        if (changePct <= -STABLE_THRESHOLD_PCT) {
            return DOWN;
        }
        return STABLE;
    }
}
