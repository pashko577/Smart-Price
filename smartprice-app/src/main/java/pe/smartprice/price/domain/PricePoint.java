package pe.smartprice.price.domain;

import java.time.LocalDate;

/**
 * Precio promedio de un producto en un día y volumen ingresado al mercado.
 *
 * @param price   soles por kg (o por litro o unidad, según el producto); puede ser nulo
 * @param volumeT toneladas ingresadas; puede ser nulo
 */
public record PricePoint(LocalDate date, Double price, Double volumeT) {

    public PricePoint {
        if (date == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        if (price != null && price < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
    }
}
