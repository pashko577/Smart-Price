package pe.smartprice.insight.application.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import pe.smartprice.alert.application.port.in.ListAlertsUseCase;
import pe.smartprice.alert.domain.AlertFilter;
import pe.smartprice.alert.domain.AlertStats;
import pe.smartprice.alert.domain.PriceAlert;
import pe.smartprice.alert.domain.Severity;
import pe.smartprice.analytics.application.port.in.GetProductAnalyticsUseCase;
import pe.smartprice.analytics.domain.ProductSummary;
import pe.smartprice.context.application.port.in.GetPriceContextUseCase;
import pe.smartprice.context.domain.IndexSeries;
import pe.smartprice.context.domain.PriceContext;
import pe.smartprice.insight.application.port.in.GetMarketAnalysisUseCase;
import pe.smartprice.insight.domain.AlertExplanation;
import pe.smartprice.insight.domain.InflationComparison;
import pe.smartprice.insight.domain.MarketAnalysis;
import pe.smartprice.insight.domain.MarketNarrative;
import pe.smartprice.insight.domain.MarketPulse;
import pe.smartprice.insight.domain.ProductDiagnosis;

/**
 * Compone el análisis a partir de los casos de uso de los otros módulos (sus puertos de entrada).
 * No lee MongoDB directamente ni depende de Spring: se prueba con implementaciones falsas.
 */
public class MarketAnalysisService implements GetMarketAnalysisUseCase {

    /** Cuántas reacciones del sistema se explican paso a paso. */
    static final int REACTIONS = 8;
    static final int WATCH_LIST = 6;

    private final GetProductAnalyticsUseCase analytics;
    private final ListAlertsUseCase alerts;
    private final GetPriceContextUseCase context;

    public MarketAnalysisService(GetProductAnalyticsUseCase analytics, ListAlertsUseCase alerts,
                                 GetPriceContextUseCase context) {
        this.analytics = analytics;
        this.alerts = alerts;
        this.context = context;
    }

    @Override
    public MarketAnalysis analyze() {
        List<ProductSummary> summaries = analytics.summaries();
        LocalDate asOf = ProductSummary.latestDate(summaries).orElseGet(alerts::lastAlertDate);
        LocalDate since = asOf.minusDays(WINDOW_DAYS - 1L);

        List<PriceAlert> windowAlerts = alerts.list(new AlertFilter(null, null, null, since, AlertFilter.MAX_LIMIT));
        Map<String, List<PriceAlert>> byProduct = windowAlerts.stream()
                .collect(Collectors.groupingBy(PriceAlert::productName));

        List<ProductDiagnosis> diagnoses = summaries.stream()
                .map(s -> ProductDiagnosis.of(s, byProduct.getOrDefault(s.productName(), List.of()), List.of()))
                .sorted(Comparator.comparing((ProductDiagnosis d) -> d.summary().varPct30d(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        MarketPulse pulse = MarketPulse.of(asOf, summaries);
        AlertStats stats = alerts.statsSince(since);
        InflationComparison inflation = inflation();

        List<AlertExplanation> reactions = windowAlerts.stream()
                .filter(a -> a.severity() != null && a.severity().isAtLeast(Severity.MEDIUM))
                .sorted(Comparator.comparing(PriceAlert::observedAt).reversed()
                        .thenComparing(a -> -Math.abs(a.changePct() == null ? 0 : a.changePct())))
                .limit(REACTIONS)
                .map(AlertExplanation::of)
                .toList();

        return new MarketAnalysis(asOf, since, pulse,
                MarketNarrative.compose(pulse, diagnoses, stats, inflation),
                diagnoses,
                MarketNarrative.watchList(diagnoses, WATCH_LIST),
                reactions, stats, inflation,
                MarketNarrative.qualityNotes(summaries),
                pe.smartprice.insight.domain.CategoryStat.of(diagnoses));
    }

    @Override
    public Optional<ProductDiagnosis> diagnose(String productName) {
        return analytics.summary(productName).map(s -> {
            LocalDate since = s.lastDate() == null ? null : s.lastDate().minusDays(WINDOW_DAYS - 1L);
            List<PriceAlert> productAlerts = since == null ? List.of()
                    : alerts.list(new AlertFilter(null, productName, null, since, AlertFilter.MAX_LIMIT));
            return ProductDiagnosis.of(s, productAlerts, analytics.monthly(productName));
        });
    }

    @Override
    public Optional<AlertExplanation> workedExample() {
        List<PriceAlert> high = alerts.list(new AlertFilter(Severity.HIGH, null, null, null, 100));
        return high.stream().filter(PriceAlert::isExplainedBySupplyDrop).findFirst()
                .or(() -> high.stream().findFirst())
                .map(AlertExplanation::of);
    }

    /** Mayorista (índice de Jevons de SmartPrice) frente al IPC total de Lima, con la misma base. */
    InflationComparison inflation() {
        PriceContext c = context.wholesaleVsInflation();
        Optional<IndexSeries> wholesale = c.series().stream().filter(s -> "SMARTPRICE".equals(s.source())).findFirst();
        Optional<IndexSeries> cpi = c.series().stream().filter(s -> "PN38705PM".equals(s.indicator())).findFirst();
        if (wholesale.isEmpty() || cpi.isEmpty() || c.commonEnd() == null) {
            return new InflationComparison(null, null, null, null);
        }
        return new InflationComparison(c.base(), c.commonEnd(),
                c.changeToCommonEnd(wholesale.get()).orElse(null), c.changeToCommonEnd(cpi.get()).orElse(null));
    }
}
