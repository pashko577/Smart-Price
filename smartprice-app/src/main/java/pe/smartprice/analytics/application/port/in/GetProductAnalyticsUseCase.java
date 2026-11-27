package pe.smartprice.analytics.application.port.in;

import java.util.List;
import java.util.Optional;

import pe.smartprice.analytics.domain.MonthlyPrice;
import pe.smartprice.analytics.domain.ProductSummary;

/** Puerto de entrada: indicadores calculados por Spark para cada producto. */
public interface GetProductAnalyticsUseCase {

    /** Todos los resúmenes, ordenados por nombre de producto. */
    List<ProductSummary> summaries();

    Optional<ProductSummary> summary(String productName);

    /** Productos con mayor subida (o bajada) en los últimos 30 días; excluye los que no tienen variación. */
    List<ProductSummary> topMovers(int limit, boolean rising);

    /** Serie mensual de un producto, ordenada por mes ascendente. */
    List<MonthlyPrice> monthly(String productName);
}
