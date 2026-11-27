package pe.smartprice.price;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.smartprice.price.application.port.out.PriceHistoryPort;
import pe.smartprice.price.application.service.PriceHistoryService;
import pe.smartprice.price.domain.PriceHistory;
import pe.smartprice.price.domain.PricePoint;

class PriceHistoryServiceTest {

    private static final LocalDate LAST = LocalDate.of(2026, 11, 26);

    private final List<LocalDate[]> requested = new ArrayList<>();

    private final PriceHistoryPort fake = new PriceHistoryPort() {
        @Override
        public List<PricePoint> findByProduct(String market, String product, LocalDate from, LocalDate to) {
            requested.add(new LocalDate[] {from, to});
            return List.of(new PricePoint(from, 1.0, 10.0), new PricePoint(to, null, 5.0));
        }

        @Override
        public Optional<LocalDate> lastObservedDate(String marketCode) {
            return Optional.of(LAST);
        }

        @Override
        public List<String> productNames(String marketCode) {
            return List.of("Apio", "Zanahoria");
        }
    };

    private final PriceHistoryService service = new PriceHistoryService(fake);

    @Test
    void sinFechasUsaElUltimoAnoHastaElUltimoDato() {
        PriceHistory h = service.history("Zanahoria", null, null);
        assertEquals(LAST, h.to());
        assertEquals(LAST.minusDays(365), h.from());
        // latest() salta el último punto porque no tiene precio
        assertEquals(LAST.minusDays(365), h.latest().orElseThrow().date());
    }

    @Test
    void validaElRangoYElProducto() {
        assertThrows(IllegalArgumentException.class, () -> service.history(" ", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.history("Zanahoria", LAST, LAST.minusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> new PricePoint(LAST, -1.0, null));
    }

    @Test
    void limitaRangosDemasiadoLargos() {
        service.history("Zanahoria", LocalDate.of(2000, 1, 1), LAST);
        LocalDate from = requested.get(0)[0];
        assertEquals(LAST.minusDays(366 * 5), from);
    }
}
