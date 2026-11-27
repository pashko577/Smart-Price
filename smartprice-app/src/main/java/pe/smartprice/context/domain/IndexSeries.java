package pe.smartprice.context.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Serie de un índice de precios (mayorista, supermercados o IPC), ordenada por fecha.
 *
 * @param source    QHAWARINA, BCRP_INEI o SMARTPRICE
 * @param baseLabel base original del índice (por ejemplo "dic. 2021 = 100")
 */
public record IndexSeries(String source, String indicator, String label, String frequency, String baseLabel,
                          List<IndexPoint> points) {

    public IndexSeries {
        points = points.stream().sorted(Comparator.comparing(IndexPoint::date)).toList();
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }

    public Optional<LocalDate> firstDate() {
        return points.isEmpty() ? Optional.empty() : Optional.of(points.get(0).date());
    }

    public Optional<LocalDate> lastDate() {
        return points.isEmpty() ? Optional.empty() : Optional.of(points.get(points.size() - 1).date());
    }

    /** Último valor disponible en {@code date} o antes. */
    public Optional<Double> valueAt(LocalDate date) {
        Double v = null;
        for (IndexPoint p : points) {
            if (p.date().isAfter(date)) {
                break;
            }
            v = p.value();
        }
        return Optional.ofNullable(v);
    }

    /**
     * Cambia la base para que la serie valga 100 en {@code base} (o en el primer punto posterior
     * si no hay dato ese día). Así índices con bases distintas se pueden comparar en un mismo eje.
     */
    public IndexSeries rebase(LocalDate base) {
        Optional<IndexPoint> ref = points.stream().filter(p -> !p.date().isBefore(base)).findFirst();
        if (ref.isEmpty()) {
            return new IndexSeries(source, indicator, label, frequency, baseLabel, List.of());
        }
        double factor = 100.0 / ref.get().value();
        return new IndexSeries(source, indicator, label, frequency, ref.get().date() + " = 100",
                points.stream().filter(p -> !p.date().isBefore(ref.get().date()))
                        .map(p -> new IndexPoint(p.date(), p.value() * factor)).toList());
    }

    /** Solo los puntos hasta {@code end} inclusive. */
    public IndexSeries until(LocalDate end) {
        return new IndexSeries(source, indicator, label, frequency, baseLabel,
                points.stream().filter(p -> !p.date().isAfter(end)).toList());
    }

    /** Variación (%) entre dos fechas; vacía si falta alguno de los dos valores. */
    public Optional<Double> changePct(LocalDate from, LocalDate to) {
        Optional<Double> a = valueAt(from);
        Optional<Double> b = valueAt(to);
        if (a.isEmpty() || b.isEmpty() || firstDate().map(d -> d.isAfter(from)).orElse(true)) {
            return Optional.empty();
        }
        return Optional.of((b.get() - a.get()) / a.get() * 100);
    }
}
