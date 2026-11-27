package pe.smartprice.jobs.sisap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class SisapReaderTest {

    private static final Path FIXTURE = Path.of("src/test/resources/sisap/prueba_ajies_2026-11.xls");

    @Test
    void leeLaExportacionEnFormatoLargo() throws Exception {
        List<SisapRow> rows = SisapReader.read(FIXTURE);

        // 26 días (01/11 a 26/11) x 6 ajíes
        assertEquals(156, rows.size());
        assertEquals(6, rows.stream().map(SisapRow::productName).distinct().count());
        assertEquals("01/11/2026 - 30/11/2026", rows.get(0).periodLabel());
    }

    @Test
    void decodificaLatin1YCoincideConElBoletinPdf() throws Exception {
        List<SisapRow> rows = SisapReader.read(FIXTURE);
        LocalDate d = LocalDate.of(2026, 11, 6);

        SisapRow rocoto = find(rows, d, "Aji Rocoto");
        assertEquals(3.06, rocoto.priceAvgPerUnit()); // PDF: S/ 55.00 por cajón de 18 kg
        assertEquals(187.0, rocoto.volumeT());

        // "Aji Montaña" viene en Latin-1 en el archivo original
        assertEquals(5.63, find(rows, d, "Aji Montaña").priceAvgPerUnit());
    }

    @Test
    void convierteLosPuntosSuspensivosANulo() throws Exception {
        List<SisapRow> rows = SisapReader.read(FIXTURE);
        // Aji Paprika no tiene volumen informado
        assertNull(find(rows, LocalDate.of(2026, 11, 6), "Aji Paprika").volumeT());
    }

    private static SisapRow find(List<SisapRow> rows, LocalDate date, String product) {
        return rows.stream()
                .filter(r -> r.observedAt().equals(date) && r.productName().equals(product))
                .findFirst().orElseThrow();
    }
}
