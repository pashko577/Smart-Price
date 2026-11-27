package pe.smartprice.analytics.infrastructure.adapter.in.web;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import pe.smartprice.alert.application.port.in.ListAlertsUseCase;
import pe.smartprice.analytics.application.port.in.GetProductAnalyticsUseCase;
import pe.smartprice.analytics.domain.ProductSummary;
import pe.smartprice.source.application.port.in.GetDataCoverageUseCase;

/** Dashboard de inteligencia de precios. */
@Controller
public class DashboardController {

    static final int MOVERS = 5;
    static final int ALERT_WINDOW_DAYS = 30;

    private final GetProductAnalyticsUseCase analytics;
    private final ListAlertsUseCase alerts;
    private final GetDataCoverageUseCase coverage;
    private final int alertLimit;

    public DashboardController(GetProductAnalyticsUseCase analytics, ListAlertsUseCase alerts,
                               GetDataCoverageUseCase coverage,
                               @Value("${smartprice.alerts.dashboard-limit:8}") int alertLimit) {
        this.analytics = analytics;
        this.alerts = alerts;
        this.coverage = coverage;
        this.alertLimit = alertLimit;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        var summaries = analytics.summaries();
        // Misma referencia que Alertas, Análisis y la barra lateral: el último día con datos.
        LocalDate lastAlert = ProductSummary.latestDate(summaries).orElseGet(alerts::lastAlertDate);

        model.addAttribute("productCount", summaries.size());
        model.addAttribute("coverage", coverage.coverage());
        model.addAttribute("risers", analytics.topMovers(MOVERS, true));
        model.addAttribute("fallers", analytics.topMovers(MOVERS, false));
        model.addAttribute("alertStats", alerts.statsSince(lastAlert.minusDays(ALERT_WINDOW_DAYS - 1)));
        model.addAttribute("alertWindowDays", ALERT_WINDOW_DAYS);
        model.addAttribute("recentAlerts", alerts.recentRelevant(alertLimit));
        model.addAttribute("risingCount", summaries.stream()
                .filter(s -> s.varPct30d() != null && s.varPct30d() >= 5).count());
        model.addAttribute("fallingCount", summaries.stream()
                .filter(s -> s.varPct30d() != null && s.varPct30d() <= -5).count());
        model.addAttribute("active", "dashboard");
        return "dashboard/index";
    }
}
