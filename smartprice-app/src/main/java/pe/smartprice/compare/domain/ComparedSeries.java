package pe.smartprice.compare.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import pe.smartprice.analytics.domain.ProductCategory;
import pe.smartprice.price.domain.PricePoint;

/**
 * Un producto dentro de una comparación: su serie agrupada (día, semana o mes) y sus estadísticas.
 *
 * @param points serie agrupada: precio y volumen promedio de cada grupo, e índice base 100
 *               (precio ÷ primer precio del periodo × 100) para comparar productos de precios distintos
 */
public record ComparedSeries(String productName, ProductCategory category, List<Point> points, SeriesStats stats) {

    /** Un punto de la serie agrupada. */
    public record Point(LocalDate date, Double price, Double volumeT, Double index) {
    }

    public ComparedSeries {
        points = List.copyOf(points);
    }

    /**
     * @param window       precios diarios del periodo comparado
     * @param previousYear precios diarios del mismo periodo un año antes (para la comparación anual)
     */
    public static ComparedSeries of(String productName, List<PricePoint> window, List<PricePoint> previousYear,
                                    Grouping grouping) {
        return new ComparedSeries(productName, ProductCategory.of(productName), aggregate(window, grouping),
                SeriesStats.of(window, previousYear));
    }

    static List<Point> aggregate(List<PricePoint> daily, Grouping grouping) {
        Map<LocalDate, double[]> acc = new TreeMap<>(); // [suma precio, n precio, suma volumen, n volumen]
        for (PricePoint p : daily) {
            double[] a = acc.computeIfAbsent(grouping.bucket(p.date()), d -> new double[4]);
            if (p.price() != null) {
                a[0] += p.price();
                a[1]++;
            }
            if (p.volumeT() != null) {
                a[2] += p.volumeT();
                a[3]++;
            }
        }
        List<Point> out = new ArrayList<>();
        Double base = null;
        for (Map.Entry<LocalDate, double[]> e : acc.entrySet()) {
            double[] a = e.getValue();
            Double price = a[1] == 0 ? null : round(a[0] / a[1], 4);
            Double volume = a[3] == 0 ? null : round(a[2] / a[3], 2);
            if (base == null && price != null && price > 0) {
                base = price;
            }
            Double index = price == null || base == null ? null : round(price / base * 100, 2);
            out.add(new Point(e.getKey(), price, volume, index));
        }
        return out;
    }

    static double round(double v, int decimals) {
        double f = Math.pow(10, decimals);
        return Math.round(v * f) / f;
    }
}
