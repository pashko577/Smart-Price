package pe.smartprice.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.smartprice.context.application.service.PriceContextService;
import pe.smartprice.context.domain.IndexPoint;
import pe.smartprice.context.domain.IndexSeries;
import pe.smartprice.context.domain.PriceContext;

class PriceContextTest {

    private static final LocalDate JAN = LocalDate.of(2024, 1, 1);
    private static final LocalDate FEB = LocalDate.of(2024, 2, 1);
    private static final LocalDate MAR = LocalDate.of(2024, 3, 1);

    private static IndexSeries series(String ind, double... values) {
        LocalDate[] dates = {JAN, FEB, MAR};
        List<IndexPoint> pts = new java.util.ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            pts.add(new IndexPoint(dates[i], values[i]));
        }
        return new IndexSeries("SRC", ind, ind, "MONTHLY", "x", pts);
    }

    @Test
    void cambioDeBaseHaceComparablesIndicesConBasesDistintas() {
        // IPC con base dic. 2021 = 100 (vale 112 en enero de 2024) -> 100 en enero de 2024
        IndexSeries ipc = series("ipc", 112.0, 113.12, 114.24).rebase(JAN);
        assertEquals(100.0, ipc.points().get(0).value(), 1e-9);
        assertEquals(102.0, ipc.points().get(2).value(), 1e-9);
        assertEquals(2.0, ipc.changePct(JAN, MAR).orElseThrow(), 1e-9);
    }

    @Test
    void laVentanaComunEsLaUltimaFechaConDatoEnTodasLasSeries() {
        PriceContext c = PriceContext.of("t", "d", JAN, List.of(
                series("mayorista", 100, 120, 150),   // hasta marzo
                series("ipc", 112, 113.12)));         // solo hasta febrero
        assertEquals(FEB, c.commonEnd());
        IndexSeries wholesale = c.series().get(0);
        IndexSeries ipc = c.series().get(1);
        assertEquals(20.0, c.changeToCommonEnd(wholesale).orElseThrow(), 1e-9);  // no usa marzo
        assertEquals(1.0, c.changeToCommonEnd(ipc).orElseThrow(), 1e-9);
        assertEquals(19.0, c.gapPoints(wholesale, ipc).orElseThrow(), 1e-9);
        // las series se recortan a la ventana común: el gráfico no muestra marzo
        assertEquals(FEB, wholesale.lastDate().orElseThrow());
    }

    @Test
    void basePosteriorAlInicioYSeriesVacias() {
        IndexSeries s = series("x", 100, 110, 121).rebase(FEB);
        assertEquals(FEB, s.firstDate().orElseThrow());
        assertEquals(110.0, s.points().get(1).value(), 1e-9);
        assertTrue(series("x", 100).rebase(MAR).isEmpty());
        assertTrue(s.changePct(JAN, MAR).isEmpty());   // la serie empieza después de "from"
        assertThrows(IllegalArgumentException.class, () -> new IndexPoint(JAN, 0));
    }

    @Test
    void elServicioPideLasSeriesEnOrdenFijo() {
        List<String> requested = new java.util.ArrayList<>();
        var service = new PriceContextService((source, indicator) -> {
            requested.add(source + ":" + indicator);
            return series(indicator, 100, 101, 102);
        });
        PriceContext c = service.wholesaleVsInflation();
        assertEquals(List.of("SMARTPRICE:gmml_jevons_monthly", "BCRP_INEI:PN38705PM", "BCRP_INEI:PN38706PM"),
                requested);
        assertEquals(3, c.series().size());
        assertEquals("gmml_jevons_monthly", c.series().get(0).indicator());   // el mayorista siempre primero
    }
}
