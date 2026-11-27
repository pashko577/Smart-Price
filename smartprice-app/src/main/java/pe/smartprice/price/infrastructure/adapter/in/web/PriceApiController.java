package pe.smartprice.price.infrastructure.adapter.in.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.smartprice.analytics.application.port.in.GetProductAnalyticsUseCase;
import pe.smartprice.price.application.port.in.GetPriceHistoryUseCase;
import pe.smartprice.price.domain.PriceHistory;

/**
 * API REST para los gráficos (JavaScript). Expone DTO, nunca documentos de MongoDB.
 */
@RestController
@RequestMapping("/api")
public class PriceApiController {

    private final GetPriceHistoryUseCase prices;
    private final GetProductAnalyticsUseCase analytics;

    public PriceApiController(GetPriceHistoryUseCase prices, GetProductAnalyticsUseCase analytics) {
        this.prices = prices;
        this.analytics = analytics;
    }

    public record PricePointDto(String date, Double price, Double volumeT) {
    }

    public record PriceHistoryDto(String product, String market, String from, String to,
                                  List<PricePointDto> points) {
    }

    public record MonthlyDto(String month, Double avgPrice, Double minPrice, Double maxPrice,
                             Double avgVolumeT, Double varPctVsPrevMonth, Double varPctVsPrevYear,
                             int daysObserved, boolean partial) {
    }

    @GetMapping("/prices")
    public PriceHistoryDto prices(@RequestParam String product,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        PriceHistory h = prices.history(product, from, to);
        return new PriceHistoryDto(h.productName(), h.marketCode(), h.from().toString(), h.to().toString(),
                h.points().stream()
                        .map(p -> new PricePointDto(p.date().toString(), p.price(), p.volumeT()))
                        .toList());
    }

    @GetMapping("/monthly")
    public List<MonthlyDto> monthly(@RequestParam String product) {
        return analytics.monthly(product).stream()
                .map(m -> new MonthlyDto(m.month().toString(), m.avgPrice(), m.minPrice(), m.maxPrice(),
                        m.avgVolumeT(), m.varPctVsPrevMonth(), m.varPctVsPrevYear(), m.daysObserved(),
                        m.isPartial()))
                .toList();
    }
}
