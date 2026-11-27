package pe.smartprice.compare.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import pe.smartprice.price.domain.PricePoint;

/**
 * Estadísticas del precio diario de un producto en el periodo comparado. Se calculan sobre los
 * precios DIARIOS (no sobre los agrupados), para que no cambien al agrupar por semana o mes.
 *
 * @param changePct       variación del último precio frente al primero del periodo (%)
 * @param amplitudePct    distancia entre el máximo y el mínimo, relativa al mínimo (%)
 * @param volatilityPct   desviación estándar de las variaciones diarias (%): cuánto se mueve día a día
 * @param corrPriceVolume correlación de Pearson entre precio y volumen diarios (nula si hay pocos datos)
 * @param prevYearAvgPrice precio promedio en el mismo periodo del año anterior
 * @param vsPrevYearPct   promedio del periodo frente al promedio del mismo periodo del año anterior (%)
 */
public record SeriesStats(
        LocalDate firstDate,
        Double firstPrice,
        LocalDate lastDate,
        Double lastPrice,
        Double changePct,
        Double minPrice,
        LocalDate minDate,
        Double maxPrice,
        LocalDate maxDate,
        Double avgPrice,
        Double amplitudePct,
        Double volatilityPct,
        Double avgVolumeT,
        Double corrPriceVolume,
        int daysWithPrice,
        Double prevYearAvgPrice,
        Double vsPrevYearPct) {

    /** Mínimo de días con precio y volumen para que la correlación tenga sentido. */
    static final int MIN_DAYS_FOR_CORR = 10;

    public static SeriesStats of(List<PricePoint> window, List<PricePoint> previousYear) {
        List<PricePoint> priced = window.stream().filter(p -> p.price() != null)
                .sorted(Comparator.comparing(PricePoint::date)).toList();
        if (priced.isEmpty()) {
            return new SeriesStats(null, null, null, null, null, null, null, null, null, null, null, null,
                    avg(window.stream().map(PricePoint::volumeT).toList()), null, 0, null, null);
        }
        PricePoint first = priced.get(0);
        PricePoint last = priced.get(priced.size() - 1);
        PricePoint min = priced.stream().min(Comparator.comparing(PricePoint::price)).orElseThrow();
        PricePoint max = priced.stream().max(Comparator.comparing(PricePoint::price)).orElseThrow();
        Double avgPrice = avg(priced.stream().map(PricePoint::price).toList());

        List<Double> dailyChanges = new ArrayList<>();
        for (int i = 1; i < priced.size(); i++) {
            double prev = priced.get(i - 1).price();
            if (prev > 0) {
                dailyChanges.add((priced.get(i).price() - prev) / prev * 100);
            }
        }

        Double prevYearAvg = avg(previousYear.stream().map(PricePoint::price).toList());
        return new SeriesStats(
                first.date(), first.price(), last.date(), last.price(),
                pct(last.price(), first.price()),
                min.price(), min.date(), max.price(), max.date(),
                avgPrice,
                pct(max.price(), min.price()),
                stdDev(dailyChanges),
                avg(window.stream().map(PricePoint::volumeT).toList()),
                correlation(priced),
                priced.size(),
                prevYearAvg,
                pct(avgPrice, prevYearAvg));
    }

    static Double pct(Double current, Double reference) {
        if (current == null || reference == null || reference == 0) {
            return null;
        }
        return (current - reference) / reference * 100;
    }

    static Double avg(List<Double> values) {
        double sum = 0;
        int n = 0;
        for (Double v : values) {
            if (v != null) {
                sum += v;
                n++;
            }
        }
        return n == 0 ? null : sum / n;
    }

    /** Desviación estándar muestral; nula con menos de dos valores. */
    static Double stdDev(List<Double> values) {
        if (values.size() < 2) {
            return null;
        }
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double ss = values.stream().mapToDouble(v -> (v - mean) * (v - mean)).sum();
        return Math.sqrt(ss / (values.size() - 1));
    }

    /** Pearson entre precio y volumen de los días que tienen ambos datos. */
    static Double correlation(List<PricePoint> points) {
        List<PricePoint> both = points.stream().filter(p -> p.price() != null && p.volumeT() != null).toList();
        int n = both.size();
        if (n < MIN_DAYS_FOR_CORR) {
            return null;
        }
        double mx = both.stream().mapToDouble(PricePoint::price).average().orElse(0);
        double my = both.stream().mapToDouble(PricePoint::volumeT).average().orElse(0);
        double sxy = 0;
        double sxx = 0;
        double syy = 0;
        for (PricePoint p : both) {
            double dx = p.price() - mx;
            double dy = p.volumeT() - my;
            sxy += dx * dy;
            sxx += dx * dx;
            syy += dy * dy;
        }
        return sxx == 0 || syy == 0 ? null : sxy / Math.sqrt(sxx * syy);
    }
}
