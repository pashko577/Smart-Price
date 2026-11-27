package pe.smartprice.compare.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import pe.smartprice.insight.domain.Text;

/**
 * Resultado de comparar varios productos en un mismo periodo.
 *
 * @param from     primer día del periodo
 * @param to       último día del periodo
 * @param series   un elemento por producto, en el orden pedido
 */
public record Comparison(LocalDate from, LocalDate to, Grouping grouping, List<ComparedSeries> series) {

    public Comparison {
        series = List.copyOf(series);
    }

    /** Productos ordenados por variación en el periodo, de la mayor subida a la mayor bajada. */
    public List<ComparedSeries> ranking() {
        return series.stream().filter(s -> s.stats().changePct() != null)
                .sorted(Comparator.comparing((ComparedSeries s) -> s.stats().changePct()).reversed()).toList();
    }

    public Optional<ComparedSeries> biggestRise() {
        return max(s -> s.stats().changePct()).filter(s -> s.stats().changePct() > 0);
    }

    public Optional<ComparedSeries> biggestFall() {
        return min(s -> s.stats().changePct()).filter(s -> s.stats().changePct() < 0);
    }

    public Optional<ComparedSeries> mostVolatile() {
        return max(s -> s.stats().volatilityPct());
    }

    public Optional<ComparedSeries> cheapest() {
        return min(s -> s.stats().avgPrice());
    }

    /**
     * Lectura automática de la comparación, con reglas fijas: quién subió y bajó más, quién se movió más
     * día a día y cómo está cada uno frente al mismo periodo del año anterior.
     */
    public List<String> reading() {
        List<String> out = new ArrayList<>();
        if (series.isEmpty()) {
            return out;
        }
        String period = "del " + Text.date(from) + " al " + Text.date(to);
        if (series.size() == 1) {
            ComparedSeries s = series.get(0);
            SeriesStats st = s.stats();
            if (st.changePct() != null) {
                out.add(s.productName() + " pasó de " + Text.soles(st.firstPrice()) + " a " + Text.soles(st.lastPrice())
                        + " por kg " + period + " (" + Text.pct(st.changePct()) + "). Su mínimo fue "
                        + Text.soles(st.minPrice()) + " el " + Text.date(st.minDate()) + " y su máximo "
                        + Text.soles(st.maxPrice()) + " el " + Text.date(st.maxDate()) + ".");
            }
        } else {
            List<String> parts = new ArrayList<>();
            biggestRise().ifPresent(s -> parts.add("la mayor subida fue de " + s.productName() + " ("
                    + Text.pct(s.stats().changePct()) + ", de " + Text.soles(s.stats().firstPrice()) + " a "
                    + Text.soles(s.stats().lastPrice()) + ")"));
            biggestFall().ifPresent(s -> parts.add("la mayor bajada, de " + s.productName() + " ("
                    + Text.pct(s.stats().changePct()) + ")"));
            if (!parts.isEmpty()) {
                out.add("Entre los productos elegidos, " + Text.list(parts) + ", " + period + ".");
            }
            mostVolatile().ifPresent(s -> out.add(s.productName() + " fue el más inestable: su precio varió en promedio "
                    + Text.num(s.stats().volatilityPct(), 1) + Text.NBSP + "% de un día a otro."));
        }
        List<String> yoy = series.stream().filter(s -> s.stats().vsPrevYearPct() != null)
                .sorted(Comparator.comparing((ComparedSeries s) -> s.stats().vsPrevYearPct()).reversed())
                .map(s -> s.productName() + " " + Text.pct(s.stats().vsPrevYearPct())).toList();
        if (!yoy.isEmpty()) {
            out.add("Frente al mismo periodo del año anterior, el precio promedio varió así: " + Text.list(yoy) + ".");
        }
        series.stream().filter(s -> s.stats().corrPriceVolume() != null && s.stats().corrPriceVolume() <= -0.3)
                .map(ComparedSeries::productName).reduce((a, b) -> a + ", " + b)
                .ifPresent(names -> out.add("En " + names + ", cuando entró menos producto el precio tendió a subir "
                        + "(correlación precio-volumen negativa en el periodo)."));
        return out;
    }

    private Optional<ComparedSeries> max(Function<ComparedSeries, Double> f) {
        return series.stream().filter(s -> f.apply(s) != null).max(Comparator.comparing(f));
    }

    private Optional<ComparedSeries> min(Function<ComparedSeries, Double> f) {
        return series.stream().filter(s -> f.apply(s) != null).min(Comparator.comparing(f));
    }
}
