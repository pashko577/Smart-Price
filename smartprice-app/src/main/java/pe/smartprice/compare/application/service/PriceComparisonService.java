package pe.smartprice.compare.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import pe.smartprice.analytics.application.port.in.GetProductAnalyticsUseCase;
import pe.smartprice.analytics.domain.ProductCategory;
import pe.smartprice.analytics.domain.ProductSummary;
import pe.smartprice.compare.application.port.in.ComparePricesUseCase;
import pe.smartprice.compare.domain.ComparedSeries;
import pe.smartprice.compare.domain.Comparison;
import pe.smartprice.compare.domain.Grouping;
import pe.smartprice.compare.domain.Preset;
import pe.smartprice.price.application.port.in.GetPriceHistoryUseCase;
import pe.smartprice.price.domain.PriceHistory;
import pe.smartprice.price.domain.PricePoint;

/**
 * Compara productos con los casos de uso de los módulos price (historial diario) y analytics
 * (catálogo y variaciones de 30 días). Sin Spring ni MongoDB: se prueba con implementaciones falsas.
 */
public class PriceComparisonService implements ComparePricesUseCase {

    /** Productos de consumo diario en Lima, para la comparación por defecto. */
    static final List<String> BASIC_BASKET = List.of("Papa Yungay", "Cebolla Cabeza Roja", "Tomate", "Zanahoria",
            "Limon Sutil Cajon", "Ajo Morado");
    /** Historial máximo que se lee: el periodo más largo posible más el año anterior para comparar. */
    static final int MAX_YEARS = 4;

    private final GetPriceHistoryUseCase history;
    private final GetProductAnalyticsUseCase analytics;

    public PriceComparisonService(GetPriceHistoryUseCase history, GetProductAnalyticsUseCase analytics) {
        this.history = history;
        this.analytics = analytics;
    }

    @Override
    public Comparison compare(List<String> products, LocalDate from, LocalDate to, Grouping grouping) {
        Set<String> wanted = new LinkedHashSet<>();
        if (products != null) {
            products.stream().filter(p -> p != null && !p.isBlank()).map(String::trim).forEach(wanted::add);
        }
        if (wanted.isEmpty()) {
            throw new IllegalArgumentException("Elige al menos un producto para comparar");
        }
        if (wanted.size() > MAX_PRODUCTS) {
            throw new IllegalArgumentException("Se pueden comparar hasta " + MAX_PRODUCTS + " productos a la vez");
        }
        Set<String> known = new LinkedHashSet<>(history.productNames());
        for (String p : wanted) {
            if (!known.contains(p)) {
                throw new IllegalArgumentException("No existe el producto \"" + p + "\"");
            }
        }
        LocalDate end = to != null ? to : lastDate();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1L);
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        if (start.isBefore(end.minusYears(MAX_YEARS - 1L))) {
            start = end.minusYears(MAX_YEARS - 1L);
        }
        LocalDate prevStart = start.minusYears(1);
        LocalDate prevEnd = end.minusYears(1);

        List<ComparedSeries> series = new ArrayList<>();
        for (String product : wanted) {
            PriceHistory h = history.history(product, prevStart, end);
            final LocalDate s = start;
            List<PricePoint> window = h.points().stream()
                    .filter(p -> !p.date().isBefore(s) && !p.date().isAfter(end)).toList();
            List<PricePoint> previous = h.points().stream()
                    .filter(p -> !p.date().isBefore(prevStart) && !p.date().isAfter(prevEnd)).toList();
            series.add(ComparedSeries.of(product, window, previous, grouping == null ? Grouping.WEEK : grouping));
        }
        return new Comparison(start, end, grouping == null ? Grouping.WEEK : grouping, series);
    }

    @Override
    public List<Preset> presets() {
        List<ProductSummary> all = analytics.summaries();
        Set<String> names = new LinkedHashSet<>();
        all.forEach(s -> names.add(s.productName()));
        List<Preset> out = new ArrayList<>();
        out.add(new Preset("canasta", "Canasta básica", "Papa, cebolla, tomate, zanahoria, limón y ajo",
                BASIC_BASKET.stream().filter(names::contains).toList()));
        out.add(new Preset("subidas", "Mayores subidas (30 días)", "Los 5 productos que más subieron en el último mes",
                analytics.topMovers(5, true).stream().map(ProductSummary::productName).toList()));
        out.add(new Preset("bajadas", "Mayores bajadas (30 días)", "Los 5 productos que más bajaron en el último mes",
                analytics.topMovers(5, false).stream().map(ProductSummary::productName).toList()));
        for (ProductCategory c : ProductCategory.values()) {
            List<ProductSummary> inCat = all.stream().filter(s -> ProductCategory.of(s.productName()) == c).toList();
            if (inCat.isEmpty()) {
                continue;
            }
            // Con más de 6 productos, se eligen los que más se movieron en el último mes.
            List<String> pick = inCat.stream()
                    .sorted(Comparator.comparing((ProductSummary s) -> s.varPct30d() == null ? 0 : Math.abs(s.varPct30d()))
                            .reversed().thenComparing(ProductSummary::productName))
                    .limit(MAX_PRODUCTS).map(ProductSummary::productName).sorted().toList();
            String hint = inCat.size() > MAX_PRODUCTS
                    ? "Los " + MAX_PRODUCTS + " de " + inCat.size() + " que más cambiaron en el último mes"
                    : inCat.size() + (inCat.size() == 1 ? " producto" : " productos");
            out.add(new Preset("cat-" + c.key(), c.label(), hint, pick));
        }
        return out.stream().filter(p -> !p.products().isEmpty()).toList();
    }

    @Override
    public LocalDate firstDate() {
        return analytics.summaries().stream().map(ProductSummary::firstDate).filter(d -> d != null)
                .min(LocalDate::compareTo).orElse(lastDate().minusYears(1));
    }

    @Override
    public LocalDate lastDate() {
        return analytics.lastDataDate().orElse(LocalDate.now());
    }
}
