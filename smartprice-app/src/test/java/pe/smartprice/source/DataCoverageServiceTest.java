package pe.smartprice.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.smartprice.source.application.service.DataCoverageService;
import pe.smartprice.source.domain.DataCoverage;
import pe.smartprice.source.domain.ProcessingRun;
import pe.smartprice.source.domain.SourceFile;

class DataCoverageServiceTest {

    private static SourceFile file(String name, LocalDate from, LocalDate to, long rows) {
        return new SourceFile(name, "SISAP", "GMML", "abc", "", from, to, rows, "sisap_v1", "manual_export", null);
    }

    @Test
    void coberturaSumaArchivosYOrdenaPorFecha() {
        var files = List.of(
                file("b.xls", LocalDate.of(2024, 6, 1), LocalDate.of(2024, 10, 31), 9027),
                file("a.xls", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 5, 31), 8968));
        var service = new DataCoverageService(() -> files, limit -> List.of());

        DataCoverage c = service.coverage();
        assertEquals("a.xls", c.files().get(0).fileName());
        assertEquals(17995, c.totalRows());
        assertEquals(LocalDate.of(2024, 1, 1), c.firstDate());
        assertEquals(305, c.days());
    }

    @Test
    void limitaLasEjecucionesYValidaLaComparacionCruzada() {
        List<Integer> requested = new ArrayList<>();
        var service = new DataCoverageService(List::of, limit -> {
            requested.add(limit);
            return List.of(
                    new ProcessingRun("1", "streaming_prices", Instant.now(), 8000, List.of("b.pdf"),
                            72, 54, 18, 0, 54, 54, 0, 0, "SUCCEEDED"),
                    new ProcessingRun("2", "streaming_prices", Instant.now(), 8000, null,
                            72, 54, 18, 0, 54, 52, 2, 0, "SUCCEEDED"));
        });

        List<ProcessingRun> runs = service.recentRuns(1000);
        assertEquals(100, requested.get(0));          // tope de 100
        assertTrue(runs.get(0).crossCheckPassed());
        assertFalse(runs.get(1).crossCheckPassed());
        assertTrue(runs.get(1).files().isEmpty());    // nulo -> lista vacía
    }
}
