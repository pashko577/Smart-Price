package pe.smartprice.context.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Comparación de varias series de índices, todas con base 100 en la misma fecha.
 *
 * @param base      fecha en la que todas las series valen 100
 * @param commonEnd última fecha con dato en todas las series (para comparar variaciones de forma justa)
 */
public record PriceContext(String title, String description, LocalDate base, LocalDate commonEnd,
                           List<IndexSeries> series) {

    /**
     * Compone la comparación: cambia la base de cada serie, calcula la ventana común y recorta todas
     * las series a esa ventana, para que el gráfico compare el mismo periodo (y no muestre meses
     * incompletos de una sola serie).
     */
    public static PriceContext of(String title, String description, LocalDate base, List<IndexSeries> raw) {
        List<IndexSeries> rebased = raw.stream().map(s -> s.rebase(base)).filter(s -> !s.isEmpty()).toList();
        LocalDate end = rebased.stream().map(s -> s.lastDate().orElseThrow())
                .min(LocalDate::compareTo).orElse(null);
        List<IndexSeries> windowed = end == null ? rebased : rebased.stream().map(s -> s.until(end)).toList();
        return new PriceContext(title, description, base, end, windowed);
    }

    /** Variación de cada serie entre la base y la última fecha común. */
    public Optional<Double> changeToCommonEnd(IndexSeries s) {
        return commonEnd == null ? Optional.empty() : s.changePct(base, commonEnd);
    }

    /** Diferencia (puntos porcentuales) entre la serie de referencia y otra, al final común. */
    public Optional<Double> gapPoints(IndexSeries reference, IndexSeries other) {
        Optional<Double> a = changeToCommonEnd(reference);
        Optional<Double> b = changeToCommonEnd(other);
        return a.isPresent() && b.isPresent() ? Optional.of(a.get() - b.get()) : Optional.empty();
    }
}
