package pe.smartprice.insight.application.port.in;

import java.util.Optional;

import pe.smartprice.insight.domain.AlertExplanation;
import pe.smartprice.insight.domain.MarketAnalysis;
import pe.smartprice.insight.domain.ProductDiagnosis;

/** Puerto de entrada: lectura automática del mercado y de cómo reaccionó el sistema. */
public interface GetMarketAnalysisUseCase {

    /** Días de la ventana de análisis (últimos N días con datos). */
    int WINDOW_DAYS = 30;

    /** Análisis completo de los últimos 30 días. */
    MarketAnalysis analyze();

    /** Diagnóstico de un producto (vacío si no existe). */
    Optional<ProductDiagnosis> diagnose(String productName);

    /**
     * Un ejemplo real para explicar paso a paso cómo se calcula una alerta: la alerta alta más reciente
     * explicada por menor abastecimiento o, si no hay, la alerta alta más reciente.
     */
    Optional<AlertExplanation> workedExample();
}
