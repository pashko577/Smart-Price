package pe.smartprice.context.domain;

import java.time.LocalDate;

/** Valor de un índice de precios en una fecha (día o primer día del mes). */
public record IndexPoint(LocalDate date, double value) {

    public IndexPoint {
        if (date == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        if (!(value > 0)) {
            throw new IllegalArgumentException("Un índice de precios debe ser positivo");
        }
    }
}
