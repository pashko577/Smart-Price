package pe.smartprice.price.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import pe.smartprice.price.domain.PricePoint;

/** Puerto de salida: acceso a las observaciones diarias de precios. */
public interface PriceHistoryPort {

    /** Puntos entre {@code from} y {@code to} (inclusive), ordenados por fecha ascendente. */
    List<PricePoint> findByProduct(String marketCode, String productName, LocalDate from, LocalDate to);

    Optional<LocalDate> lastObservedDate(String marketCode);

    List<String> productNames(String marketCode);
}
