package pe.smartprice.context.application.service;

import java.time.LocalDate;
import java.util.List;

import pe.smartprice.context.application.port.in.GetPriceContextUseCase;
import pe.smartprice.context.application.port.out.IndexSeriesPort;
import pe.smartprice.context.domain.PriceContext;

/**
 * Compone las comparaciones de contexto. El orden de las series es fijo (el mercado mayorista primero)
 * para que cada serie conserve su color en los gráficos.
 */
public class PriceContextService implements GetPriceContextUseCase {

    static final LocalDate MONTHLY_BASE = LocalDate.of(2024, 1, 1);
    static final LocalDate DAILY_BASE = LocalDate.of(2026, 2, 10);

    private final IndexSeriesPort port;

    public PriceContextService(IndexSeriesPort port) {
        this.port = port;
    }

    @Override
    public PriceContext wholesaleVsInflation() {
        return PriceContext.of(
                "Mercado mayorista frente a la inflación",
                "Índices mensuales con base enero 2024 = 100. El IPC mide toda la canasta del consumidor de Lima; "
                        + "el índice mayorista solo 59 productos frescos del Gran Mercado Mayorista.",
                MONTHLY_BASE,
                List.of(port.find("SMARTPRICE", "gmml_jevons_monthly"),
                        port.find("BCRP_INEI", "PN38705PM"),
                        port.find("BCRP_INEI", "PN38706PM")));
    }

    @Override
    public PriceContext wholesaleVsSupermarkets() {
        return PriceContext.of(
                "Mercado mayorista frente a supermercados",
                "Índices diarios con base 10 de febrero de 2026 = 100. Supermercados: Plaza Vea, Metro y Wong "
                        + "(Qhawarina), cuyo índice público no se actualiza desde el 13 de junio de 2026.",
                DAILY_BASE,
                List.of(port.find("SMARTPRICE", "gmml_jevons_daily"),
                        port.find("QHAWARINA", "index_food"),
                        port.find("QHAWARINA", "index_verduras")));
    }
}
