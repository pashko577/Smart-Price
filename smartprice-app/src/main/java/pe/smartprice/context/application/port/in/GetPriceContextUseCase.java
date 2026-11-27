package pe.smartprice.context.application.port.in;

import pe.smartprice.context.domain.PriceContext;

/** Puerto de entrada: el mercado mayorista frente a la inflación y a los supermercados. */
public interface GetPriceContextUseCase {

    /** Índice mayorista GMML frente al IPC de Lima, mensual, base ene. 2024 = 100. */
    PriceContext wholesaleVsInflation();

    /** Índice mayorista GMML frente a los supermercados (Qhawarina), diario, base 10-02-2026 = 100. */
    PriceContext wholesaleVsSupermarkets();
}
