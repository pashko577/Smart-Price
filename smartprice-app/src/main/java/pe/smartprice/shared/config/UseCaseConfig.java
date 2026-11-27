package pe.smartprice.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pe.smartprice.alert.application.port.in.ListAlertsUseCase;
import pe.smartprice.alert.application.port.out.AlertPort;
import pe.smartprice.alert.application.service.AlertService;
import pe.smartprice.analytics.application.port.in.GetProductAnalyticsUseCase;
import pe.smartprice.analytics.application.port.out.AnalyticsPort;
import pe.smartprice.analytics.application.service.ProductAnalyticsService;
import pe.smartprice.context.application.port.in.GetPriceContextUseCase;
import pe.smartprice.context.application.port.out.IndexSeriesPort;
import pe.smartprice.context.application.service.PriceContextService;
import pe.smartprice.price.application.port.in.GetPriceHistoryUseCase;
import pe.smartprice.price.application.port.out.PriceHistoryPort;
import pe.smartprice.price.application.service.PriceHistoryService;
import pe.smartprice.source.application.port.in.GetDataCoverageUseCase;
import pe.smartprice.source.application.port.out.ProcessingRunPort;
import pe.smartprice.source.application.port.out.SourceFilePort;
import pe.smartprice.source.application.service.DataCoverageService;

/**
 * Ensamblaje de la arquitectura hexagonal: conecta cada caso de uso (núcleo, sin Spring)
 * con el adaptador de salida que implementa su puerto.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    GetPriceHistoryUseCase getPriceHistoryUseCase(PriceHistoryPort port) {
        return new PriceHistoryService(port);
    }

    @Bean
    GetProductAnalyticsUseCase getProductAnalyticsUseCase(AnalyticsPort port) {
        return new ProductAnalyticsService(port);
    }

    @Bean
    ListAlertsUseCase listAlertsUseCase(AlertPort port) {
        return new AlertService(port);
    }

    @Bean
    GetDataCoverageUseCase getDataCoverageUseCase(SourceFilePort files, ProcessingRunPort runs) {
        return new DataCoverageService(files, runs);
    }

    @Bean
    GetPriceContextUseCase getPriceContextUseCase(IndexSeriesPort port) {
        return new PriceContextService(port);
    }
}
