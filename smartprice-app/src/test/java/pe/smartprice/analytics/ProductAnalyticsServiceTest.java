package pe.smartprice.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.smartprice.analytics.application.port.out.AnalyticsPort;
import pe.smartprice.analytics.application.service.ProductAnalyticsService;
import pe.smartprice.analytics.domain.MonthlyPrice;
import pe.smartprice.analytics.domain.ProductSummary;
import pe.smartprice.analytics.domain.Trend;

/** Casos de uso probados con un puerto falso: sin MongoDB, sin Spark, sin Spring. */
class ProductAnalyticsServiceTest {

    private static ProductSummary summary(String name, Double var30, Double corr) {
        LocalDate d = LocalDate.of(2026, 11, 26);
        return new ProductSummary(name, "GMML", d.minusYears(2), d, 3.0, 2.9, 1.5, 0.5, 4.0, 200.0,
                var30, null, corr, 1000);
    }

    private static final AnalyticsPort FAKE = new AnalyticsPort() {
        @Override
        public List<ProductSummary> findSummaries(String marketCode) {
            return List.of(summary("Zanahoria", 64.2, -0.32), summary("Ajo Morado", -12.0, 0.1),
                    summary("Apio", 20.0, null), summary("Kion", null, null), summary("Tomate", -30.5, -0.1));
        }

        @Override
        public Optional<ProductSummary> findSummary(String marketCode, String productName) {
            return findSummaries(marketCode).stream().filter(s -> s.productName().equals(productName)).findFirst();
        }

        @Override
        public List<MonthlyPrice> findMonthly(String marketCode, String productName) {
            return List.of(
                    new MonthlyPrice(productName, YearMonth.of(2026, 11), 3.05, 2.0, 4.2, 260.0, 6760.0, 68.2, 113.6, 26, "VALIDATED"),
                    new MonthlyPrice(productName, YearMonth.of(2026, 9), 1.32, 1.0, 1.8, 318.0, 9540.0, 52.7, 5.0, 30, "VALIDATED"),
                    new MonthlyPrice(productName, YearMonth.of(2026, 10), 1.81, 1.3, 2.4, 297.0, 9207.0, 36.9, 62.8, 10, "PARTIAL_MONTH"));
        }
    };

    private final ProductAnalyticsService service = new ProductAnalyticsService(FAKE);

    @Test
    void topMoversOrdenaPorVariacionYExcluyeNulos() {
        List<ProductSummary> up = service.topMovers(5, true);
        assertEquals(List.of("Zanahoria", "Apio"), up.stream().map(ProductSummary::productName).toList());

        List<ProductSummary> down = service.topMovers(5, false);
        assertEquals(List.of("Tomate", "Ajo Morado"), down.stream().map(ProductSummary::productName).toList());

        assertEquals(1, service.topMovers(1, true).size());
        assertTrue(service.topMovers(0, true).isEmpty());
    }

    @Test
    void resumenesOrdenadosPorNombre() {
        assertEquals("Ajo Morado", service.summaries().get(0).productName());
    }

    @Test
    void serieMensualOrdenadaYMesesParciales() {
        List<MonthlyPrice> m = service.monthly("Zanahoria");
        assertEquals(List.of(YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11)),
                m.stream().map(MonthlyPrice::month).toList());
        assertTrue(m.get(1).isPartial());
        assertFalse(m.get(2).isPartial());
    }

    @Test
    void reglasDeDominioDelResumen() {
        ProductSummary z = service.summary("Zanahoria").orElseThrow();
        assertEquals(Trend.UP, z.trend30d());
        assertTrue(z.priceReactsToSupply());            // corr -0.32 <= -0.3
        assertEquals((3.0 - 0.5) / (4.0 - 0.5), z.positionInHistoricalRange(), 1e-9);

        assertEquals(Trend.UNKNOWN, service.summary("Kion").orElseThrow().trend30d());
        assertFalse(service.summary("Apio").orElseThrow().priceReactsToSupply());
        assertEquals(Trend.STABLE, Trend.of(4.9));
        assertEquals(Trend.DOWN, Trend.of(-5.0));
        assertNull(summary("X", 1.0, null).corrPriceVolume());
    }
}
