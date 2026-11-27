package pe.smartprice.compare.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Cómo se agrupan los precios diarios para comparar: por día, por semana (lunes) o por mes. */
public enum Grouping {
    DAY("Día"),
    WEEK("Semana"),
    MONTH("Mes");

    private final String label;

    Grouping(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Primer día del grupo al que pertenece la fecha (el lunes de su semana, el día 1 de su mes). */
    public LocalDate bucket(LocalDate date) {
        return switch (this) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    public static Grouping parseOr(String value, Grouping fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        for (Grouping g : values()) {
            if (g.name().equalsIgnoreCase(value.trim())) {
                return g;
            }
        }
        throw new IllegalArgumentException("Agrupación desconocida: " + value + " (usa DAY, WEEK o MONTH)");
    }
}
