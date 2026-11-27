package pe.smartprice.analytics.application.port.out;

import java.util.List;
import java.util.Optional;

import pe.smartprice.analytics.domain.MonthlyPrice;
import pe.smartprice.analytics.domain.ProductSummary;

/** Puerto de salida: lectura de los resultados analíticos escritos por Spark. */
public interface AnalyticsPort {

    List<ProductSummary> findSummaries(String marketCode);

    Optional<ProductSummary> findSummary(String marketCode, String productName);

    List<MonthlyPrice> findMonthly(String marketCode, String productName);
}
