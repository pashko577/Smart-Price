package pe.smartprice.price.application.service;

import java.time.LocalDate;
import java.util.List;

import pe.smartprice.price.application.port.in.GetPriceHistoryUseCase;
import pe.smartprice.price.application.port.out.PriceHistoryPort;
import pe.smartprice.price.domain.PriceHistory;

/** Caso de uso de historial de precios. Sin dependencias de Spring ni de MongoDB. */
public class PriceHistoryService implements GetPriceHistoryUseCase {

    public static final String DEFAULT_MARKET = "GMML";
    static final int DEFAULT_DAYS = 365;
    static final int MAX_DAYS = 366 * 5;

    private final PriceHistoryPort port;

    public PriceHistoryService(PriceHistoryPort port) {
        this.port = port;
    }

    @Override
    public PriceHistory history(String productName, LocalDate from, LocalDate to) {
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        LocalDate end = to != null ? to : port.lastObservedDate(DEFAULT_MARKET).orElse(LocalDate.now());
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS);
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        if (start.plusDays(MAX_DAYS).isBefore(end)) {
            start = end.minusDays(MAX_DAYS);
        }
        return new PriceHistory(productName, DEFAULT_MARKET, start, end,
                port.findByProduct(DEFAULT_MARKET, productName, start, end));
    }

    @Override
    public List<String> productNames() {
        return port.productNames(DEFAULT_MARKET);
    }
}
