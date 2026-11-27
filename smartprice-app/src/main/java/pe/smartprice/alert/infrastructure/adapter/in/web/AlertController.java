package pe.smartprice.alert.infrastructure.adapter.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pe.smartprice.alert.application.port.in.ListAlertsUseCase;
import pe.smartprice.alert.domain.AlertFilter;
import pe.smartprice.alert.domain.AlertType;
import pe.smartprice.alert.domain.Severity;
import pe.smartprice.analytics.application.port.in.GetProductAnalyticsUseCase;

/** Listado de alertas con filtros. */
@Controller
public class AlertController {

    static final int DEFAULT_DAYS = 90;

    private final ListAlertsUseCase alerts;
    private final GetProductAnalyticsUseCase analytics;
    private final int pageSize;

    public AlertController(ListAlertsUseCase alerts, GetProductAnalyticsUseCase analytics,
                           @Value("${smartprice.alerts.page-size:50}") int pageSize) {
        this.alerts = alerts;
        this.analytics = analytics;
        this.pageSize = pageSize;
    }

    @GetMapping("/alerts")
    public String list(@RequestParam(defaultValue = "MEDIUM") String severity,
                       @RequestParam(required = false) String product,
                       @RequestParam(required = false) String type,
                       @RequestParam(defaultValue = "" + DEFAULT_DAYS) int days,
                       Model model) {
        Severity minSeverity = "ALL".equalsIgnoreCase(severity) ? null : Severity.parseOrNull(severity);
        var since = days > 0 ? alerts.lastAlertDate().minusDays(days - 1L) : null;
        var filter = new AlertFilter(minSeverity, product, AlertType.parseOrNull(type), since, pageSize);

        model.addAttribute("alerts", alerts.list(filter));
        model.addAttribute("stats", alerts.statsSince(since != null ? since : alerts.lastAlertDate().minusYears(10)));
        model.addAttribute("productNames", analytics.summaries().stream().map(s -> s.productName()).toList());
        model.addAttribute("severity", minSeverity == null ? "ALL" : minSeverity.name());
        model.addAttribute("product", product == null ? "" : product);
        model.addAttribute("type", type == null ? "" : type);
        model.addAttribute("days", days);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("active", "alerts");
        return "alerts/list";
    }
}
