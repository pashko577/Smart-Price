package pe.smartprice.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.smartprice.alert.application.port.out.AlertPort;
import pe.smartprice.alert.application.service.AlertService;
import pe.smartprice.alert.domain.AlertFilter;
import pe.smartprice.alert.domain.AlertStats;
import pe.smartprice.alert.domain.AlertType;
import pe.smartprice.alert.domain.PriceAlert;
import pe.smartprice.alert.domain.Severity;

class AlertServiceTest {

    private static PriceAlert alert(String product, LocalDate d, AlertType t, Severity s, Double vol, Double volAvg) {
        return new PriceAlert(product + d, product, "GMML", d, t, s, 2.73, 4.23, 54.95, -21.7, 22.1, vol, volAvg);
    }

    private static final LocalDate D = LocalDate.of(2026, 11, 23);

    @Test
    void reglaDeAbastecimiento() {
        // Caso real: Zanahoria 23-11-2026, 45 t frente a un promedio de 275 t.
        PriceAlert zanahoria = alert("Zanahoria", D, AlertType.PRICE_SPIKE, Severity.HIGH, 45.0, 275.47);
        assertEquals(-83.66, zanahoria.volumeChangePct(), 0.01);
        assertTrue(zanahoria.isExplainedBySupplyDrop());

        assertFalse(alert("A", D, AlertType.PRICE_SPIKE, Severity.HIGH, 250.0, 275.0).isExplainedBySupplyDrop());
        assertFalse(alert("A", D, AlertType.PRICE_DROP, Severity.HIGH, 45.0, 275.0).isExplainedBySupplyDrop());
        assertNull(alert("A", D, AlertType.PRICE_SPIKE, Severity.HIGH, null, 275.0).volumeChangePct());
        assertNull(alert("A", D, AlertType.PRICE_SPIKE, Severity.HIGH, 10.0, 0.0).volumeChangePct());
    }

    @Test
    void severidadYFiltro() {
        assertTrue(Severity.HIGH.isAtLeast(Severity.MEDIUM));
        assertFalse(Severity.LOW.isAtLeast(Severity.MEDIUM));
        assertNull(Severity.parseOrNull(" "));
        assertEquals(Severity.HIGH, Severity.parseOrNull("high"));

        AlertFilter f = new AlertFilter(null, "  ", null, null, 0);
        assertNull(f.productName());
        assertEquals(AlertFilter.MAX_LIMIT, f.limit());
    }

    @Test
    void dashboardPideSeveridadMediaOAltaYEstadisticas() {
        List<AlertFilter> received = new ArrayList<>();
        AlertPort fake = new AlertPort() {
            @Override
            public List<PriceAlert> find(String marketCode, AlertFilter filter) {
                received.add(filter);
                return List.of();
            }

            @Override
            public List<PriceAlert> findSince(String marketCode, LocalDate since) {
                return List.of(
                        alert("A", D, AlertType.PRICE_SPIKE, Severity.HIGH, 1.0, 1.0),
                        alert("B", D, AlertType.PRICE_DROP, Severity.MEDIUM, 1.0, 1.0),
                        alert("C", D, AlertType.PRICE_SPIKE, Severity.LOW, 1.0, 1.0),
                        alert("D", D, AlertType.PRICE_SPIKE, Severity.HIGH, 1.0, 1.0));
            }

            @Override
            public Optional<LocalDate> lastAlertDate(String marketCode) {
                return Optional.of(D);
            }
        };
        AlertService service = new AlertService(fake);

        service.recentRelevant(8);
        assertEquals(Severity.MEDIUM, received.get(0).minSeverity());
        assertEquals(8, received.get(0).limit());

        AlertStats s = service.statsSince(D.minusDays(29));
        assertEquals(4, s.total());
        assertEquals(2, s.high());
        assertEquals(1, s.medium());
        assertEquals(1, s.low());
        assertEquals(3, s.spikes());
        assertEquals(1, s.drops());
        assertEquals(D, service.lastAlertDate());
    }
}
