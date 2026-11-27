package pe.smartprice.analytics.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import pe.smartprice.analytics.application.port.in.GetProductAnalyticsUseCase;
import pe.smartprice.analytics.application.port.out.AnalyticsPort;
import pe.smartprice.analytics.domain.MonthlyPrice;
import pe.smartprice.analytics.domain.ProductSummary;

/** Casos de uso de indicadores por producto. Sin dependencias de Spring ni de MongoDB. */
public class ProductAnalyticsService implements GetProductAnalyticsUseCase {

    public static final String DEFAULT_MARKET = "GMML";

    private final AnalyticsPort port;

    public ProductAnalyticsService(AnalyticsPort port) {
        this.port = port;
    }

    @Override
    public List<ProductSummary> summaries() {
        return port.findSummaries(DEFAULT_MARKET).stream()
                .sorted(Comparator.comparing(ProductSummary::productName))
                .toList();
    }

    @Override
    public Optional<ProductSummary> summary(String productName) {
        return port.findSummary(DEFAULT_MARKET, productName);
    }

    @Override
    public List<ProductSummary> topMovers(int limit, boolean rising) {
        if (limit <= 0) {
            return List.of();
        }
        Comparator<ProductSummary> byChange = Comparator.comparingDouble(ProductSummary::varPct30d);
        return port.findSummaries(DEFAULT_MARKET).stream()
                .filter(s -> s.varPct30d() != null)
                .filter(s -> rising ? s.varPct30d() > 0 : s.varPct30d() < 0)
                .sorted(rising ? byChange.reversed() : byChange)
                .limit(limit)
                .toList();
    }

    @Override
    public List<MonthlyPrice> monthly(String productName) {
        return port.findMonthly(DEFAULT_MARKET, productName).stream()
                .sorted(Comparator.comparing(MonthlyPrice::month))
                .toList();
    }
}
