package pe.smartprice.price.infrastructure.adapter.in.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import pe.smartprice.alert.application.port.in.ListAlertsUseCase;
import pe.smartprice.alert.domain.AlertFilter;
import pe.smartprice.alert.domain.Severity;
import pe.smartprice.analytics.application.port.in.GetProductAnalyticsUseCase;
import pe.smartprice.analytics.domain.MonthlyPrice;

/**
 * Catálogo de productos y detalle de un producto.
 * El nombre va como parámetro (?name=) porque algunos contienen "/" ("Maiz Marlo / Coronta De Maiz").
 */
@Controller
public class ProductController {

    static final int MONTHS_IN_TABLE = 12;
    static final int PRODUCT_ALERTS = 15;

    private final GetProductAnalyticsUseCase analytics;
    private final ListAlertsUseCase alerts;

    public ProductController(GetProductAnalyticsUseCase analytics, ListAlertsUseCase alerts) {
        this.analytics = analytics;
        this.alerts = alerts;
    }

    @GetMapping("/products")
    public String list(Model model) {
        model.addAttribute("summaries", analytics.summaries());
        model.addAttribute("active", "products");
        return "products/list";
    }

    @GetMapping("/product")
    public String detail(@RequestParam("name") String name, Model model, RedirectAttributes redirect) {
        var summary = analytics.summary(name);
        if (summary.isEmpty()) {
            redirect.addFlashAttribute("error", "No existe el producto \"" + name + "\".");
            return "redirect:/products";
        }
        List<MonthlyPrice> monthly = new ArrayList<>(analytics.monthly(name));
        Collections.reverse(monthly); // del más reciente al más antiguo

        model.addAttribute("summary", summary.get());
        model.addAttribute("monthly", monthly.subList(0, Math.min(MONTHS_IN_TABLE, monthly.size())));
        model.addAttribute("alerts", alerts.list(new AlertFilter(Severity.MEDIUM, name, null, null, PRODUCT_ALERTS)));
        model.addAttribute("productNames", analytics.summaries().stream().map(s -> s.productName()).toList());
        model.addAttribute("active", "products");
        return "products/detail";
    }
}
