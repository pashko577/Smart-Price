package pe.smartprice.price.infrastructure.adapter.in.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import pe.smartprice.analytics.domain.ProductCategory;
import pe.smartprice.analytics.domain.ProductSummary;
import pe.smartprice.insight.domain.Situation;
import pe.smartprice.insight.application.port.in.GetMarketAnalysisUseCase;

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
    private final GetMarketAnalysisUseCase diagnosis;

    public ProductController(GetProductAnalyticsUseCase analytics, ListAlertsUseCase alerts,
                             GetMarketAnalysisUseCase diagnosis) {
        this.analytics = analytics;
        this.alerts = alerts;
        this.diagnosis = diagnosis;
    }

    /** Fila del catálogo: el resumen de Spark más su categoría y su situación de 30 días (para filtrar y ordenar). */
    public record ProductRow(ProductSummary summary, ProductCategory category, Situation situation) {
    }

    @GetMapping("/products")
    public String list(Model model) {
        model.addAttribute("rows", analytics.summaries().stream()
                .map(s -> new ProductRow(s, ProductCategory.of(s.productName()), Situation.of(s.varPct30d()))).toList());
        model.addAttribute("categories", Arrays.stream(ProductCategory.values())
                .filter(c -> c != ProductCategory.OTROS).toList());
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
        List<ProductSummary> all = analytics.summaries();
        model.addAttribute("productNames", all.stream().map(ProductSummary::productName).toList());
        model.addAttribute("diagnosis", diagnosis.diagnose(name).orElse(null));
        // "Comparar con…": el producto y hasta 3 de su categoría (los que más cambiaron en el mes).
        ProductCategory category = ProductCategory.of(name);
        List<String> compareWith = new ArrayList<>(List.of(name));
        all.stream().filter(s -> !s.productName().equals(name) && ProductCategory.of(s.productName()) == category)
                .sorted(Comparator.comparing((ProductSummary s) -> s.varPct30d() == null ? 0 : Math.abs(s.varPct30d())).reversed())
                .limit(3).map(ProductSummary::productName).forEach(compareWith::add);
        model.addAttribute("category", category);
        model.addAttribute("compareWith", compareWith);
        model.addAttribute("active", "products");
        return "products/detail";
    }
}
