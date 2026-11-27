package pe.smartprice.insight.domain;

import pe.smartprice.analytics.domain.Trend;

/**
 * Situación de un producto según la variación de su precio promedio de los últimos 30 días frente a
 * los 30 días anteriores. Usa el mismo umbral de "estable" que el resto de la aplicación (±5 %).
 */
public enum Situation {
    STRONG_RISE("Alza fuerte", "sube 20 % o más", "up"),
    RISE("Alza", "sube entre 5 % y 20 %", "up"),
    STABLE("Estable", "varía menos de 5 %", "neutral"),
    FALL("Baja", "baja entre 5 % y 20 %", "down"),
    STRONG_FALL("Baja fuerte", "baja 20 % o más", "down"),
    UNKNOWN("Sin dato", "sin datos suficientes para comparar", "neutral");

    /** A partir de esta variación (%) el alza o la baja se considera fuerte. */
    public static final double STRONG_PCT = 20.0;

    private final String label;
    private final String rule;
    private final String tone;

    Situation(String label, String rule, String tone) {
        this.label = label;
        this.rule = rule;
        this.tone = tone;
    }

    public String label() {
        return label;
    }

    /** La regla en palabras, para explicarla en pantalla. */
    public String rule() {
        return rule;
    }

    /** up / down / neutral: dirección del precio (subir = más caro). */
    public String tone() {
        return tone;
    }

    public boolean isRising() {
        return this == STRONG_RISE || this == RISE;
    }

    public boolean isFalling() {
        return this == STRONG_FALL || this == FALL;
    }

    public static Situation of(Double varPct30d) {
        if (varPct30d == null) {
            return UNKNOWN;
        }
        if (varPct30d >= STRONG_PCT) {
            return STRONG_RISE;
        }
        if (varPct30d >= Trend.STABLE_THRESHOLD_PCT) {
            return RISE;
        }
        if (varPct30d <= -STRONG_PCT) {
            return STRONG_FALL;
        }
        if (varPct30d <= -Trend.STABLE_THRESHOLD_PCT) {
            return FALL;
        }
        return STABLE;
    }
}
