package pe.smartprice.insight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.smartprice.alert.application.port.in.ListAlertsUseCase;
import pe.smartprice.alert.domain.AlertFilter;
import pe.smartprice.alert.domain.AlertStats;
import pe.smartprice.alert.domain.AlertType;
import pe.smartprice.alert.domain.PriceAlert;
import pe.smartprice.alert.domain.Severity;
import pe.smartprice.analytics.application.port.in.GetProductAnalyticsUseCase;
import pe.smartprice.analytics.domain.MonthlyPrice;
import pe.smartprice.analytics.domain.ProductSummary;
import pe.smartprice.context.application.port.in.GetPriceContextUseCase;
import pe.smartprice.context.domain.IndexPoint;
import pe.smartprice.context.domain.IndexSeries;
import pe.smartprice.context.domain.PriceContext;
import pe.smartprice.insight.application.service.MarketAnalysisService;
import pe.smartprice.insight.domain.AlertExplanation;
import pe.smartprice.insight.domain.MarketAnalysis;
import pe.smartprice.insight.domain.MarketNarrative;
import pe.smartprice.insight.domain.MarketPulse;
import pe.smartprice.insight.domain.ProductDiagnosis;
import pe.smartprice.insight.domain.Situation;

/** Análisis del mercado probado con casos de uso falsos: sin MongoDB, sin Spark, sin Spring. */
class MarketAnalysisTest {

    private static final LocalDate D = LocalDate.of(2026, 11, 26);

    private static ProductSummary summary(String name, Double last, Double var30, Double yoy, Double corr) {
        return new ProductSummary(name, "GMML", LocalDate.of(2024, 1, 1), D, last, last, 1.5, 0.33, 4.23, 200.0,
                var30, yoy, corr, 1000);
    }

    /** Caso real: Zanahoria, 23-11-2026 (+54.95 %, 45 t frente a 275.47 t, rango normal −21.7 % a +22.1 %). */
    private static PriceAlert zanahoria() {
        return new PriceAlert("z", "Zanahoria", "GMML", LocalDate.of(2026, 11, 23), AlertType.PRICE_SPIKE,
                Severity.HIGH, 2.73, 4.23, 54.95, -21.7, 22.1, 45.0, 275.47, 3.0, 15.0);
    }

    private static PriceAlert drop(String product, LocalDate d) {
        return new PriceAlert(product + d, product, "GMML", d, AlertType.PRICE_DROP, Severity.MEDIUM,
                4.98, 3.45, -30.7, -25.0, 24.0, 300.0, 200.0, 3.0, 15.0);
    }

    @Test
    void situacionSegunVariacionDe30Dias() {
        assertEquals(Situation.STRONG_RISE, Situation.of(64.2));
        assertEquals(Situation.STRONG_RISE, Situation.of(20.0));
        assertEquals(Situation.RISE, Situation.of(5.0));
        assertEquals(Situation.STABLE, Situation.of(4.99));
        assertEquals(Situation.STABLE, Situation.of(-4.99));
        assertEquals(Situation.FALL, Situation.of(-5.0));
        assertEquals(Situation.STRONG_FALL, Situation.of(-31.9));
        assertEquals(Situation.UNKNOWN, Situation.of(null));
        assertTrue(Situation.RISE.isRising());
        assertTrue(Situation.STRONG_FALL.isFalling());
    }

    @Test
    void alertaReconstruyeLaCuentaDeSpark() {
        PriceAlert a = zanahoria();
        // upper − lower = IQR · (1 + 2k)  →  43.8 / 7
        assertEquals(6.257, a.iqr(), 0.001);
        assertEquals(-2.929, a.q1(), 0.001);
        assertEquals(3.329, a.q3(), 0.001);
        assertEquals(32.85, a.excessPoints(), 0.001);
        assertEquals(5.25, a.iqrDistance(), 0.01);          // 3 IQR o más → severidad alta, igual que Spark
        assertTrue(a.iqrDistance() >= PriceAlert.HIGH_IQR_DISTANCE);

        // Sin parámetros guardados se asumen los del Batch (k = 3, 15 %).
        PriceAlert legacy = new PriceAlert("x", "X", "GMML", D, AlertType.PRICE_SPIKE, Severity.HIGH,
                1.0, 2.0, 100.0, -21.7, 22.1, null, null);
        assertEquals(3.0, legacy.effectiveK());
        assertEquals(15.0, legacy.effectiveMinChangePct());
        assertEquals(6.257, legacy.iqr(), 0.001);

        // Producto que casi nunca cambia (IQR = 0): Spark usa la variación absoluta.
        PriceAlert flat = new PriceAlert("f", "F", "GMML", D, AlertType.PRICE_SPIKE, Severity.HIGH,
                1.0, 1.2, 20.0, 0.0, 0.0, null, null, 3.0, 15.0);
        assertEquals(20.0, flat.iqrDistance(), 1e-9);
    }

    @Test
    void explicacionPasoAPaso() {
        AlertExplanation e = AlertExplanation.of(zanahoria());
        // Los importes y porcentajes llevan espacio duro ( ) para no partirse en dos líneas.
        assertEquals("Zanahoria · 23/11/2026 · subida de +55.0 %", e.title());
        assertEquals(5, e.steps().size());
        assertTrue(e.steps().get(0).contains("S/ 2.73") && e.steps().get(0).contains("S/ 4.23"));
        assertTrue(e.steps().get(2).contains("límite superior por 32.") &&e.steps().get(2).contains("veces el IQR"), e.steps().get(2));
        assertTrue(e.steps().get(2).contains("severidad Alta"));
        assertTrue(e.steps().get(4).contains("Menor abastecimiento"), e.steps().get(4));
        assertTrue(e.conclusion().contains("price_alerts"));
    }

    @Test
    void pulsoDelMercado() {
        MarketPulse p = MarketPulse.of(D, List.of(
                summary("Zanahoria", 3.04, 64.2, 95.7, -0.32),
                summary("Apio", 4.7, 12.0, 30.0, -0.1),
                summary("Tomate", 2.0, 1.0, 0.0, 0.0),
                summary("Aji Rocoto", 3.0, -31.9, -10.0, -0.4),
                summary("Kion", 3.6, null, null, null)));
        assertEquals(5, p.products());
        assertEquals(1, p.strongRise());
        assertEquals(1, p.rise());
        assertEquals(2, p.rising());
        assertEquals(1, p.stable());
        assertEquals(1, p.falling());
        assertEquals(1, p.unknown());
        // media geométrica de 1.642, 1.12, 1.01 y 0.681
        assertEquals(6.05, p.basketChangePct(), 0.01);
        assertEquals(6.5, p.medianChangePct(), 1e-9);
        assertEquals(MarketPulse.Phase.RISING, p.phase());
        assertEquals(6, p.distribution().size());
        assertEquals(List.of("Aji Rocoto"), p.distribution().get(0).products());
        assertEquals(List.of("Zanahoria"), p.distribution().get(5).products());
        assertEquals(1, p.maxBucket());
    }

    @Test
    void diagnosticoDeUnProducto() {
        List<MonthlyPrice> monthly = List.of(
                new MonthlyPrice("Zanahoria", YearMonth.of(2026, 10), 1.81, 1.48, 2.5, 297.16, 9211.0, 36.9, 62.8, 31, "VALIDATED"),
                new MonthlyPrice("Zanahoria", YearMonth.of(2026, 11), 3.05, 2.04, 4.23, 260.0, 6760.0, 68.2, 113.6, 26, "VALIDATED"));
        ProductDiagnosis d = ProductDiagnosis.of(summary("Zanahoria", 4.0, 64.2, 95.7, -0.32),
                List.of(drop("Zanahoria", LocalDate.of(2026, 11, 10)), zanahoria()), monthly);

        assertEquals(Situation.STRONG_RISE, d.situation());
        assertTrue(d.headline().startsWith("Zanahoria está en alza fuerte"));
        assertEquals(LocalDate.of(2026, 11, 23), d.alerts().get(0).observedAt());   // la más reciente primero
        assertEquals(1, d.highAlerts());
        assertTrue(d.supplyExplained());
        assertTrue(d.nearHistoricalHigh());                                          // 4.0 en un rango 0.33–4.23
        assertTrue(d.muchHigherThanLastYear());
        assertTrue(d.worthWatching());
        assertTrue(d.reasons().stream().anyMatch(r -> r.contains("12.5 % menos que en oct. 2026")), d.reasons().toString());
        assertTrue(d.reasons().stream().anyMatch(r -> r.contains("menor abastecimiento")));
        assertTrue(d.reasons().stream().anyMatch(r -> r.contains("2 alertas")));

        ProductDiagnosis calm = ProductDiagnosis.of(summary("Tomate", 2.0, 1.0, 0.0, 0.0), List.of(), List.of());
        assertEquals(Situation.STABLE, calm.situation());
        assertFalse(calm.worthWatching());
        assertTrue(calm.reasons().stream().anyMatch(r -> r.contains("no generó alertas")));
    }

    @Test
    void notasDeCalidadDetectanProductosGemelos() {
        List<String> notes = MarketNarrative.qualityNotes(List.of(
                summary("Papa Blanca", 1.83, 64.27, 119.25, -0.02),
                summary("Papa Yungay", 1.83, 64.27, 119.25, 0.047),
                summary("Papa Huayro", 2.5, 10.0, 5.0, 0.1)));
        assertEquals(1, notes.size());
        assertTrue(notes.get(0).startsWith("Papa Blanca y Papa Yungay"));
    }

    @Test
    void servicioComponeElAnalisisCompleto() {
        GetProductAnalyticsUseCase analytics = new GetProductAnalyticsUseCase() {
            final List<ProductSummary> all = List.of(MarketAnalysisTest.summary("Zanahoria", 4.0, 64.2, 95.7, -0.32),
                    MarketAnalysisTest.summary("Arveja Verde Blanca Serrana", 3.45, -12.0, 5.0, 0.1),
                    MarketAnalysisTest.summary("Tomate", 2.0, 1.0, 0.0, 0.0));

            public List<ProductSummary> summaries() { return all; }

            public Optional<ProductSummary> summary(String n) {
                return all.stream().filter(s -> s.productName().equals(n)).findFirst();
            }

            public List<ProductSummary> topMovers(int limit, boolean rising) { return List.of(); }

            public List<MonthlyPrice> monthly(String n) { return List.of(); }
        };
        List<PriceAlert> alerts = List.of(zanahoria(), drop("Arveja Verde Blanca Serrana", LocalDate.of(2026, 11, 25)),
                drop("Arveja Verde Blanca Serrana", LocalDate.of(2026, 9, 1)));    // fuera de la ventana
        ListAlertsUseCase alertsUseCase = new ListAlertsUseCase() {
            public List<PriceAlert> list(AlertFilter f) {
                return alerts.stream()
                        .filter(a -> f.from() == null || !a.observedAt().isBefore(f.from()))
                        .filter(a -> f.productName() == null || a.productName().equals(f.productName()))
                        .filter(a -> f.minSeverity() == null || a.severity().isAtLeast(f.minSeverity()))
                        .toList();
            }

            public List<PriceAlert> recentRelevant(int limit) { return alerts; }

            public AlertStats statsSince(LocalDate since) {
                List<PriceAlert> in = list(new AlertFilter(null, null, null, since, 500));
                long spikes = in.stream().filter(a -> a.type() == AlertType.PRICE_SPIKE).count();
                return new AlertStats(since, in.size(), 1, in.size() - 1, 0, spikes, in.size() - spikes);
            }

            public LocalDate lastAlertDate() { return LocalDate.of(2026, 11, 25); }
        };
        LocalDate base = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2026, 10, 1);
        GetPriceContextUseCase context = new GetPriceContextUseCase() {
            public PriceContext wholesaleVsInflation() {
                return PriceContext.of("t", "d", base, List.of(
                        new IndexSeries("SMARTPRICE", "gmml_jevons_monthly", "GMML", "MONTHLY", "",
                                List.of(new IndexPoint(base, 100), new IndexPoint(end, 139.8))),
                        new IndexSeries("BCRP_INEI", "PN38705PM", "IPC", "MONTHLY", "",
                                List.of(new IndexPoint(base, 110), new IndexPoint(end, 119.35)))));
            }

            public PriceContext wholesaleVsSupermarkets() { return wholesaleVsInflation(); }
        };

        MarketAnalysisService service = new MarketAnalysisService(analytics, alertsUseCase, context);
        MarketAnalysis a = service.analyze();

        assertEquals(D, a.asOf());
        assertEquals(LocalDate.of(2026, 10, 28), a.windowStart());
        assertEquals("Zanahoria", a.diagnoses().get(0).productName());          // mayor variación primero
        assertEquals(2, a.reactions().size());                                   // la de septiembre queda fuera
        assertEquals("Arveja Verde Blanca Serrana", a.reactions().get(0).alert().productName()); // más reciente primero
        assertEquals(1, a.supplyExplainedSpikes());
        assertEquals(39.8, a.inflation().wholesalePct(), 0.01);
        assertEquals(8.5, a.inflation().cpiPct(), 0.01);
        assertEquals(List.of("Zanahoria"), a.watchList().stream().map(ProductDiagnosis::productName).toList());
        assertTrue(a.narrative().get(0).startsWith("Mercado: en fase de alza (al 26/11/2026)"),
                a.narrative().get(0));
        assertTrue(a.narrative().stream().anyMatch(p -> p.contains("Zanahoria el 23/11/2026")));
        assertTrue(a.narrative().stream().anyMatch(p -> p.contains("IPC de Lima +8.5 %")));

        assertTrue(service.diagnose("Zanahoria").isPresent());
        assertTrue(service.diagnose("No existe").isEmpty());
        assertEquals("Zanahoria", service.workedExample().orElseThrow().alert().productName());
        assertNull(new MarketAnalysisService(analytics, alertsUseCase, new GetPriceContextUseCase() {
            public PriceContext wholesaleVsInflation() { return PriceContext.of("t", "d", base, List.of()); }

            public PriceContext wholesaleVsSupermarkets() { return wholesaleVsInflation(); }
        }).analyze().inflation().wholesalePct());
    }
}
