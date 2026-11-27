package pe.smartprice.compare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.smartprice.analytics.application.port.in.GetProductAnalyticsUseCase;
import pe.smartprice.analytics.domain.MonthlyPrice;
import pe.smartprice.analytics.domain.ProductCategory;
import pe.smartprice.analytics.domain.ProductSummary;
import pe.smartprice.compare.application.service.PriceComparisonService;
import pe.smartprice.compare.domain.ComparedSeries;
import pe.smartprice.compare.domain.Comparison;
import pe.smartprice.compare.domain.Grouping;
import pe.smartprice.compare.domain.Preset;
import pe.smartprice.compare.domain.SeriesStats;
import pe.smartprice.price.application.port.in.GetPriceHistoryUseCase;
import pe.smartprice.price.domain.PriceHistory;
import pe.smartprice.price.domain.PricePoint;

/** Comparador probado con casos de uso falsos: sin MongoDB, sin Spring. */
class PriceComparisonTest {

    private static final LocalDate END = LocalDate.of(2026, 11, 26);

    /** Los 59 productos de SISAP: todos deben caer en una categoría conocida. */
    private static final List<String> ALL_PRODUCTS = List.of("Aji Amarillo Seco", "Aji Escabeche", "Aji Montaña",
            "Aji Panca", "Aji Paprika", "Aji Rocoto", "Ajo Criollo O Napuri", "Ajo Morado", "Alcachofa", "Apio",
            "Arveja Verde Americana", "Arveja Verde Blanca Serrana", "Beterraga", "Brocoli", "Camote Amarillo",
            "Camote Morado", "Cebolla Cabeza Blanca", "Cebolla Cabeza Roja", "Cebolla China", "Choclo ( Tipo Cusco)",
            "Col China / Longapa", "Culantro", "Esparrago", "Espinaca", "Frejolito Chino", "Frijol Verde Canario",
            "Haba Verde Serrana", "Holantau / Organica", "Kion", "Lechuga Americana", "Lechuga Criolla Seda",
            "Lechuga Romana Hidroponica", "Limon Sutil Bolsa", "Limon Sutil Cajon", "Maiz Marlo / Coronta De Maiz",
            "Maiz Morado", "Manzana Cte / Para Agua", "Maracuya", "Olluco Largo", "Pacchoy", "Pallar Verde Serrucho",
            "Papa Amarilla", "Papa Blanca", "Papa Canchan", "Papa Color", "Papa Huamantanga", "Papa Huayro",
            "Papa Negra Andina", "Papa Peruanita", "Papa Unica", "Papa Yungay", "Pimiento Morron", "Tomate",
            "Vainita Americana", "Yuca Amarilla", "Zanahoria", "Zapallo Italiano", "Zapallo Loche", "Zapallo Macre");

    @Test
    void cadaProductoTieneCategoria() {
        assertEquals(59, ALL_PRODUCTS.size());
        ALL_PRODUCTS.forEach(p -> assertTrue(ProductCategory.of(p) != ProductCategory.OTROS, p));
        assertEquals(ProductCategory.PAPAS, ProductCategory.of("Papa Yungay"));
        assertEquals(ProductCategory.AJIES, ProductCategory.of("Aji Montaña"));
        assertEquals(ProductCategory.BULBOS, ProductCategory.of("Ajo Morado"));   // "Ajo", no "Ají"
        assertEquals(ProductCategory.RAICES, ProductCategory.of("Zanahoria"));
        assertEquals(10, ALL_PRODUCTS.stream().filter(p -> ProductCategory.of(p) == ProductCategory.PAPAS).count());
        assertEquals(ProductCategory.MAIZ, ProductCategory.parseOrNull("maiz"));
        assertNull(ProductCategory.parseOrNull(""));
    }

    @Test
    void agrupaPorSemanaYMesConIndiceBase100() {
        List<PricePoint> daily = List.of(
                new PricePoint(LocalDate.of(2026, 11, 2), 2.0, 100.0),   // lunes
                new PricePoint(LocalDate.of(2026, 11, 3), 4.0, 300.0),
                new PricePoint(LocalDate.of(2026, 11, 9), 4.5, null),    // lunes siguiente
                new PricePoint(LocalDate.of(2026, 11, 10), null, 50.0));
        ComparedSeries week = ComparedSeries.of("Zanahoria", daily, List.of(), Grouping.WEEK);
        assertEquals(2, week.points().size());
        assertEquals(LocalDate.of(2026, 11, 2), week.points().get(0).date());
        assertEquals(3.0, week.points().get(0).price(), 1e-9);              // promedio de 2.0 y 4.0
        assertEquals(200.0, week.points().get(0).volumeT(), 1e-9);
        assertEquals(100.0, week.points().get(0).index(), 1e-9);            // base 100
        assertEquals(150.0, week.points().get(1).index(), 1e-9);            // 4.5 / 3.0
        ComparedSeries month = ComparedSeries.of("Zanahoria", daily, List.of(), Grouping.MONTH);
        assertEquals(1, month.points().size());
        assertEquals(LocalDate.of(2026, 11, 1), month.points().get(0).date());
    }

    @Test
    void estadisticasDelPeriodo() {
        List<PricePoint> window = new ArrayList<>();
        double[] prices = {1.0, 1.1, 0.99, 1.2, 1.5, 1.4, 1.6, 1.8, 2.0, 1.9, 2.2, 2.0};
        double[] volumes = {400, 380, 410, 350, 300, 320, 290, 270, 250, 260, 230, 240};
        for (int i = 0; i < prices.length; i++) {
            window.add(new PricePoint(END.minusDays(prices.length - 1L - i), prices[i], volumes[i]));
        }
        List<PricePoint> lastYear = List.of(new PricePoint(END.minusYears(1), 1.0, 300.0),
                new PricePoint(END.minusYears(1).minusDays(1), 1.4, 280.0));
        SeriesStats st = SeriesStats.of(window, lastYear);
        assertEquals(1.0, st.firstPrice());
        assertEquals(2.0, st.lastPrice());
        assertEquals(100.0, st.changePct(), 1e-9);
        assertEquals(0.99, st.minPrice());
        assertEquals(2.2, st.maxPrice());
        assertEquals(END.minusDays(1), st.maxDate());
        assertEquals(12, st.daysWithPrice());
        assertEquals(1.2, st.prevYearAvgPrice(), 1e-9);
        assertEquals((st.avgPrice() - 1.2) / 1.2 * 100, st.vsPrevYearPct(), 1e-9);
        assertTrue(st.volatilityPct() > 5, "volatilidad " + st.volatilityPct());
        assertTrue(st.corrPriceVolume() < -0.8, "correlación " + st.corrPriceVolume());   // menos volumen, más precio

        SeriesStats empty = SeriesStats.of(List.of(new PricePoint(END, null, 10.0)), List.of());
        assertNull(empty.changePct());
        assertEquals(0, empty.daysWithPrice());
    }

    @Test
    void servicioComparaValidaYArmaGrupos() {
        GetPriceHistoryUseCase history = new GetPriceHistoryUseCase() {
            public PriceHistory history(String product, LocalDate from, LocalDate to) {
                List<PricePoint> pts = new ArrayList<>();
                double base = product.equals("Zanahoria") ? 1.0 : 3.0;
                double slope = product.equals("Zanahoria") ? 0.004 : -0.002;
                for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                    long i = java.time.temporal.ChronoUnit.DAYS.between(from, d);
                    pts.add(new PricePoint(d, base + slope * i, 200.0));
                }
                return new PriceHistory(product, "GMML", from, to, pts);
            }

            public List<String> productNames() { return List.of("Zanahoria", "Ajo Morado", "Tomate"); }
        };
        GetProductAnalyticsUseCase analytics = new GetProductAnalyticsUseCase() {
            final List<ProductSummary> all = List.of(PriceComparisonTest.summary("Zanahoria", 64.2),
                    PriceComparisonTest.summary("Ajo Morado", -12.0), PriceComparisonTest.summary("Tomate", 1.0));

            public List<ProductSummary> summaries() { return all; }

            public Optional<ProductSummary> summary(String n) { return all.stream().filter(s -> s.productName().equals(n)).findFirst(); }

            public List<ProductSummary> topMovers(int limit, boolean rising) {
                return all.stream().filter(s -> rising ? s.varPct30d() > 0 : s.varPct30d() < 0).limit(limit).toList();
            }

            public List<MonthlyPrice> monthly(String n) { return List.of(); }
        };
        PriceComparisonService service = new PriceComparisonService(history, analytics);

        Comparison c = service.compare(List.of("Zanahoria", "Ajo Morado", "Zanahoria"), END.minusDays(89), END, Grouping.WEEK);
        assertEquals(2, c.series().size());                                  // sin repetidos
        assertEquals(END.minusDays(89), c.from());
        assertEquals("Zanahoria", c.ranking().get(0).productName());        // la que más subió, primero
        assertEquals("Zanahoria", c.biggestRise().orElseThrow().productName());
        assertEquals("Ajo Morado", c.biggestFall().orElseThrow().productName());
        assertTrue(c.series().get(0).stats().vsPrevYearPct() > 0);          // la serie sube: este año más caro
        assertEquals(90, c.series().get(0).stats().daysWithPrice());
        assertTrue(c.reading().get(0).contains("la mayor subida fue de Zanahoria"), c.reading().get(0));

        assertThrows(IllegalArgumentException.class, () -> service.compare(List.of(), null, null, Grouping.DAY));
        assertThrows(IllegalArgumentException.class, () -> service.compare(List.of("Kiwi"), null, null, Grouping.DAY));
        assertThrows(IllegalArgumentException.class,
                () -> service.compare(List.of("Zanahoria"), END, END.minusDays(5), Grouping.DAY));
        assertThrows(IllegalArgumentException.class, () -> service.compare(
                List.of("a", "b", "c", "d", "e", "f", "g"), null, null, Grouping.DAY));
        assertThrows(IllegalArgumentException.class, () -> Grouping.parseOr("ANUAL", Grouping.WEEK));

        List<Preset> presets = service.presets();
        assertEquals("canasta", presets.get(0).key());
        assertEquals(List.of("Tomate", "Zanahoria", "Ajo Morado"), presets.get(0).products()); // los de la canasta que existen
        assertEquals(List.of("Zanahoria", "Tomate"), presets.get(1).products());              // subidas
        assertTrue(presets.stream().anyMatch(p -> p.key().equals("cat-raices")));
    }

    private static ProductSummary summary(String name, Double var30) {
        return new ProductSummary(name, "GMML", LocalDate.of(2024, 1, 1), END, 2.0, 2.0, 1.5, 0.5, 4.0, 100.0,
                var30, 10.0, -0.3, 1000);
    }
}
