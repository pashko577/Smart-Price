package pe.smartprice.price.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Serie diaria de precios de un producto en un mercado, ordenada por fecha. */
public record PriceHistory(String productName, String marketCode, LocalDate from, LocalDate to,
                           List<PricePoint> points) {

    public PriceHistory {
        Objects.requireNonNull(productName, "productName");
        points = List.copyOf(points);
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    /** Último día con precio informado. */
    public Optional<PricePoint> latest() {
        for (int i = points.size() - 1; i >= 0; i--) {
            if (points.get(i).price() != null) {
                return Optional.of(points.get(i));
            }
        }
        return Optional.empty();
    }
}
