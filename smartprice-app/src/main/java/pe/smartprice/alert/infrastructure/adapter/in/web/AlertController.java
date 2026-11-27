package pe.smartprice.alert.infrastructure.adapter.in.web;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pe.smartprice.alert.application.port.in.ListAlertsUseCase;
import pe.smartprice.alert.domain.AlertFilter;
import pe.smartprice.alert.domain.AlertType;
import pe.smartprice.alert.domain.Severity;
import pe.smartprice.analytics.application.port.in.GetProductAnalyticsUseCase;
import pe.smartprice.analytics.domain.ProductCategory;
import pe.smartprice.analytics.domain.ProductSummary;

/**
 * Listado de alertas con filtros: severidad, tipo, categoría, producto, periodo (rápido o con fechas
 * exactas) y "solo menor abastecimiento". Los indicadores de arriba cuentan con los mismos filtros.
 */
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
                       @RequestParam(required = false) String category,
                       @RequestParam(defaultValue = "false") boolean supply,
                       @RequestParam(defaultValue = "" + DEFAULT_DAYS) int days,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model) {
        Severity minSeverity = "ALL".equalsIgnoreCase(severity) ? null : Severity.parseOrNull(severity);
        List<ProductSummary> summaries = analytics.summaries();
        // "Últimos N días" se cuenta desde el último día con datos, igual que en el Dashboard y el Análisis.
        LocalDate asOf = ProductSummary.latestDate(summaries).orElseGet(alerts::lastAlertDate);

        // Fechas exactas mandan sobre el periodo rápido.
        boolean custom = from != null || to != null;
        if (custom && from != null && to != null && from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }
        LocalDate since = custom ? from : (days > 0 ? asOf.minusDays(days - 1L) : null);
        LocalDate until = custom ? to : null;

        ProductCategory cat = ProductCategory.parseOrNull(category);
        List<String> inCategory = cat == null ? List.of() : summaries.stream().map(ProductSummary::productName)
                .filter(n -> ProductCategory.of(n) == cat).toList();
        String productName = product == null || product.isBlank() ? null : product;

        AlertFilter filter = new AlertFilter(minSeverity, productName, AlertType.parseOrNull(type), since, pageSize,
                until, inCategory, supply);

        model.addAttribute("alerts", alerts.list(filter));
        model.addAttribute("stats", alerts.stats(filter, since != null ? since : asOf.minusYears(10)));
        model.addAttribute("productNames", summaries.stream()
                .filter(s -> cat == null || ProductCategory.of(s.productName()) == cat)
                .map(ProductSummary::productName).toList());
        model.addAttribute("categories", Arrays.stream(ProductCategory.values()).filter(c -> c != ProductCategory.OTROS).toList());
        model.addAttribute("severity", minSeverity == null ? "ALL" : minSeverity.name());
        model.addAttribute("product", productName == null ? "" : productName);
        model.addAttribute("type", type == null ? "" : type);
        model.addAttribute("category", cat == null ? "" : cat.key());
        model.addAttribute("supply", supply);
        model.addAttribute("days", custom ? -1 : days);
        model.addAttribute("from", from == null ? "" : from.toString());
        model.addAttribute("to", to == null ? "" : to.toString());
        model.addAttribute("asOf", asOf);
        model.addAttribute("filtered", minSeverity != Severity.MEDIUM || productName != null || AlertType.parseOrNull(type) != null
                || cat != null || supply || custom || days != DEFAULT_DAYS);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("active", "alerts");
        return "alerts/list";
    }
}
