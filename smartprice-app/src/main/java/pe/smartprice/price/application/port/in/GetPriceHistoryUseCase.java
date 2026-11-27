package pe.smartprice.price.application.port.in;

import java.time.LocalDate;
import java.util.List;

import pe.smartprice.price.domain.PriceHistory;

/** Puerto de entrada: historial diario de precios y catálogo de productos. */
public interface GetPriceHistoryUseCase {

    /**
     * @param from inclusive; si es nulo, se usa {@code to} menos 365 días
     * @param to   inclusive; si es nulo, se usa la última fecha disponible
     */
    PriceHistory history(String productName, LocalDate from, LocalDate to);

    List<String> productNames();
}
