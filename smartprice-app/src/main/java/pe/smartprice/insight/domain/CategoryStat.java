package pe.smartprice.insight.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pe.smartprice.alert.domain.Severity;
import pe.smartprice.analytics.domain.ProductCategory;

/**
 * Comparación por categoría (papas, ajíes, hojas…): cómo se movió cada grupo de productos en los
 * últimos 30 días. Se usa la mediana para que un solo producto extremo no arrastre al grupo.
 *
 * @param medianVar30 variación de 30 días del producto típico de la categoría (%)
 * @param medianYoy   variación frente al año anterior del producto típico (%)
 * @param topMover    el producto de la categoría que más cambió en 30 días (en valor absoluto)
 */
public record CategoryStat(ProductCategory category, int products, int rising, int falling,
                           Double medianVar30, Double medianYoy, int alerts, long highAlerts,
                           String topMover, Double topMoverVar30) {

    public static List<CategoryStat> of(List<ProductDiagnosis> diagnoses) {
        Map<ProductCategory, List<ProductDiagnosis>> byCat = new EnumMap<>(ProductCategory.class);
        diagnoses.forEach(d -> byCat.computeIfAbsent(ProductCategory.of(d.productName()), c -> new ArrayList<>()).add(d));
        List<CategoryStat> out = new ArrayList<>();
        byCat.forEach((cat, list) -> {
            ProductDiagnosis top = list.stream().filter(d -> d.summary().varPct30d() != null)
                    .max(Comparator.comparing(d -> Math.abs(d.summary().varPct30d()))).orElse(null);
            out.add(new CategoryStat(cat, list.size(),
                    (int) list.stream().filter(d -> d.situation().isRising()).count(),
                    (int) list.stream().filter(d -> d.situation().isFalling()).count(),
                    median(list.stream().map(d -> d.summary().varPct30d()).toList()),
                    median(list.stream().map(d -> d.summary().varPctYoy()).toList()),
                    list.stream().mapToInt(d -> d.alerts().size()).sum(),
                    list.stream().flatMap(d -> d.alerts().stream()).filter(a -> a.severity() == Severity.HIGH).count(),
                    top == null ? null : top.productName(),
                    top == null ? null : top.summary().varPct30d()));
        });
        out.sort(Comparator.comparing(CategoryStat::medianVar30, Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    static Double median(List<Double> values) {
        List<Double> v = values.stream().filter(Objects::nonNull).sorted().toList();
        if (v.isEmpty()) {
            return null;
        }
        int n = v.size();
        return n % 2 == 1 ? v.get(n / 2) : (v.get(n / 2 - 1) + v.get(n / 2)) / 2;
    }
}
