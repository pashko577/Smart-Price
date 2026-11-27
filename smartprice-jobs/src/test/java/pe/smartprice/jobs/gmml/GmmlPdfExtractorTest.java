package pe.smartprice.jobs.gmml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class GmmlPdfExtractorTest {

    private static final LocalDate D = LocalDate.of(2026, 11, 6);

    private static GmmlRow parse(String line) {
        return GmmlPdfExtractor.parseLine(line, D, "f.pdf", "sha", 2);
    }

    @Test
    void filaConUnidadDeUnaPalabra() {
        GmmlRow r = parse("Aji Rocoto 168 187 120 178 Cajon 18.00 60.00 55.00 48.75");
        assertEquals("Aji Rocoto", r.productName());
        assertEquals("Cajon", r.unit());
        assertEquals(18.0, r.kgEquivalent());
        assertEquals(55.0, r.price());
        assertEquals(3.0556, r.pricePerKg());
        assertEquals(187.0, r.volumeT());
    }

    @Test
    void filaConUnidadDeDosPalabrasYVolumenesVacios() {
        GmmlRow r = parse("Zanahoria 219 208 248 306 Saco Chico 65.00 177.50 237.50 171.07");
        assertEquals("Saco Chico", r.unit());
        assertEquals(3.6538, r.pricePerKg());

        GmmlRow acelga = parse("Acelga : : : : Atado 1.00 3.75 3.75 3.80");
        assertNull(acelga.volumeT());
        assertEquals(3.75, acelga.price());
    }

    @Test
    void separaLaNotaAlPie() {
        GmmlRow r = parse("Papa Unica 2/ 56 141 86 : Kilogramo 1.00 1.93 2.05 1.80");
        assertEquals("Papa Unica", r.productName());
        assertEquals("2", r.footnote());
        assertNull(r.volumeTAvg4Wd());
    }

    @Test
    void ignoraEncabezadosYPieDePagina() {
        assertNull(parse("Ayer Hoy Últimos 7 Últ. 4 medida en kg Ayer Hoy Últimos"));
        assertNull(parse("Fuente : EMMSA - Gran Mercado Mayorista de Lima"));
    }

    @Test
    void leeLaFechaDelEncabezado() {
        assertEquals(D, GmmlPdfExtractor.parseDate("viernes, 6 de Noviembre de 2026 GMML - Abastecimiento"));
    }
}
