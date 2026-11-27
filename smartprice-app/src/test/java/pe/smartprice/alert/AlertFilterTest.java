package pe.smartprice.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.smartprice.alert.application.port.out.AlertPort;
import pe.smartprice.alert.application.service.AlertService;
import pe.smartprice.alert.domain.AlertFilter;
import pe.smartprice.alert.domain.AlertStats;
import pe.smartprice.alert.domain.AlertType;
import pe.smartprice.alert.domain.PriceAlert;
import pe.smartprice.alert.domain.Severity;

/** Filtros nuevos de alertas: fecha máxima, lista de productos (categoría) y "solo menor abastecimiento". */
class AlertFilterTest {

    private static final LocalDate D = LocalDate.of(2026, 11, 23);

    private static PriceAlert alert(String product, LocalDate d, AlertType t, Severity s, Double vol, Double avg) {
        return new PriceAlert(product + d, product, "GMML", d, t, s, 2.0, 3.0, 50.0, -20.0, 20.0, vol, avg);
    }

    private static final List<PriceAlert> ALL = List.of(
            alert("Zanahoria", D, AlertType.PRICE_SPIKE, Severity.HIGH, 45.0, 275.0),        // menor abastecimiento
            alert("Zanahoria", D.minusDays(10), AlertType.PRICE_SPIKE, Severity.LOW, 260.0, 270.0),
            alert("Papa Yungay", D.minusDays(3), AlertType.PRICE_DROP, Severity.MEDIUM, 900.0, 600.0),
            alert("Tomate", D.minusDays(40), AlertType.PRICE_SPIKE, Severity.HIGH, 50.0, 200.0)); // menor abastecimiento

    @Test
    void filtroCompletoEnElDominio() {
        AlertFilter supplyInNov = new AlertFilter(null, null, null, LocalDate.of(2026, 11, 1), 50,
                LocalDate.of(2026, 11, 30), List.of(), true);
        assertEquals(1, ALL.stream().filter(supplyInNov::matches).count());     // solo la zanahoria del 23/11

        AlertFilter raices = new AlertFilter(null, null, null, null, 50, null, List.of("Zanahoria", "Yuca Amarilla"), false);
        assertEquals(2, ALL.stream().filter(raices::matches).count());

        AlertFilter hasta = new AlertFilter(null, null, null, null, 50, D.minusDays(5), List.of(), false);
        assertTrue(hasta.matches(ALL.get(1)));
        assertFalse(hasta.matches(ALL.get(0)));

        AlertFilter high = new AlertFilter(Severity.HIGH, null, null, null, 1);
        AlertFilter stats = high.forStats();
        assertEquals(null, stats.minSeverity());                                 // el conteo incluye todas las severidades
        assertEquals(AlertFilter.MAX_LIMIT, stats.limit());
    }

    @Test
    void servicioAplicaMenorAbastecimientoYCuentaConLosFiltros() {
        AlertPort port = new AlertPort() {
            public List<PriceAlert> find(String m, AlertFilter f) { return ALL.stream().filter(f::matches).limit(f.limit()).toList(); }

            public List<PriceAlert> findMatching(String m, AlertFilter f) {
                // como MongoDB: filtra todo menos la regla de abastecimiento, sin límite
                AlertFilter db = new AlertFilter(f.minSeverity(), f.productName(), f.type(), f.from(), AlertFilter.MAX_LIMIT,
                        f.to(), f.products(), false);
                return ALL.stream().filter(db::matches).toList();
            }

            public List<PriceAlert> findSince(String m, LocalDate since) { return ALL; }

            public java.util.Optional<LocalDate> lastAlertDate(String m) { return java.util.Optional.of(D); }
        };
        AlertService service = new AlertService(port);

        AlertFilter supply = new AlertFilter(null, null, null, null, 1, null, List.of(), true);
        List<PriceAlert> list = service.list(supply);
        assertEquals(1, list.size());                                            // el límite se aplica después de la regla
        assertTrue(list.get(0).isExplainedBySupplyDrop());

        AlertStats st = service.stats(new AlertFilter(Severity.HIGH, null, null, null, 50, null, List.of(), true), D);
        assertEquals(2, st.total());                                             // las 2 por menor abastecimiento
        assertEquals(2, st.high());
        assertEquals(2, st.spikes());
    }
}
