package pe.smartprice.insight.infrastructure.adapter.in.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import pe.smartprice.alert.application.port.in.ListAlertsUseCase;
import pe.smartprice.alert.domain.PriceAlert;
import pe.smartprice.analytics.domain.Trend;
import pe.smartprice.insight.application.port.in.GetMarketAnalysisUseCase;
import pe.smartprice.insight.domain.ProductDiagnosis;
import pe.smartprice.insight.domain.Situation;
import pe.smartprice.insight.domain.Text;
import pe.smartprice.source.application.port.in.GetDataCoverageUseCase;

/**
 * Vista "Cómo funciona": qué hace el sistema ante cada evento, un ejemplo real del cálculo de una alerta,
 * el significado de cada indicador y los parámetros vigentes. Los números de las reglas salen de las mismas
 * constantes que usa el código, para que la explicación no se desincronice.
 */
@Controller
public class SystemController {

    static final int RECENT_RUNS = 5;

    /** Un parámetro del sistema: valor, para qué sirve y dónde está definido. */
    public record Param(String name, String value, String meaning, String definedIn) {
    }

    /** Un evento y la reacción automática del sistema. */
    public record Reaction(String event, String reaction, String rule, String where) {
    }

    private final GetMarketAnalysisUseCase analysis;
    private final GetDataCoverageUseCase coverage;
    private final ListAlertsUseCase alerts;

    public SystemController(GetMarketAnalysisUseCase analysis, GetDataCoverageUseCase coverage,
                            ListAlertsUseCase alerts) {
        this.analysis = analysis;
        this.coverage = coverage;
        this.alerts = alerts;
    }

    @GetMapping("/system")
    public String page(Model model) {
        model.addAttribute("coverage", coverage.coverage());
        model.addAttribute("contextFiles", coverage.contextFiles());
        model.addAttribute("runs", coverage.recentRuns(RECENT_RUNS));
        model.addAttribute("allAlerts", alerts.statsSince(LocalDate.of(2000, 1, 1)));
        model.addAttribute("example", analysis.workedExample().orElse(null));
        model.addAttribute("reactions", reactions());
        model.addAttribute("params", params());
        model.addAttribute("active", "system");
        return "system/index";
    }

    static List<Reaction> reactions() {
        String k = Text.num(PriceAlert.DEFAULT_K, 0);
        String min = Text.num(PriceAlert.DEFAULT_MIN_CHANGE_PCT, 0);
        String supply = Text.num(PriceAlert.SUPPLY_DROP_THRESHOLD_PCT, 0);
        String stable = Text.num(Trend.STABLE_THRESHOLD_PCT, 0);
        return List.of(
                new Reaction("Llega un boletín PDF nuevo",
                        "Spark Streaming lo lee en el siguiente micro-lote y guarda solo los días nuevos.",
                        "Un boletín por micro-lote; revisión cada 15 s; checkpoint para no procesarlo dos veces",
                        "Fuentes → Boletines procesados"),
                new Reaction("El boletín trae un día ya cargado",
                        "No lo duplica: compara su precio con el guardado (SISAP vs PDF).",
                        "_id determinista fecha|mercado|producto; tolerancia S/ 0.011",
                        "Fuentes → Validación cruzada"),
                new Reaction("Llega un producto fuera del catálogo",
                        "Lo ignora y lo anota en el registro de la ejecución.",
                        "Catálogo = los 59 productos con rango normal calculado por el Batch",
                        "processing_logs"),
                new Reaction("Un precio sale de su rango normal",
                        "Genera una alerta de subida o de bajada.",
                        "Fuera de [Q1 − " + k + "·IQR, Q3 + " + k + "·IQR] y al menos " + min + " % de variación",
                        "Dashboard, Alertas, Análisis, detalle del producto"),
                new Reaction("La alerta se aleja mucho del límite",
                        "Le da severidad Alta, Media o Baja según cuántos IQR se alejó.",
                        "Alta: " + Text.num(PriceAlert.HIGH_IQR_DISTANCE, 0) + " IQR o más · Media: de "
                                + Text.num(PriceAlert.MEDIUM_IQR_DISTANCE, 0) + " a " + Text.num(PriceAlert.HIGH_IQR_DISTANCE, 0)
                                + " · Baja: menos de " + Text.num(PriceAlert.MEDIUM_IQR_DISTANCE, 0),
                        "Punto de color + texto (Alta / Media / Baja)"),
                new Reaction("El precio sube y entra mucho menos producto",
                        "La marca como \"Menor abastecimiento\".",
                        "Subida atípica con volumen " + supply + " % o más por debajo de su promedio de 30 días",
                        "Etiqueta en Dashboard, Alertas y Análisis"),
                new Reaction("Un producto cambia poco en 30 días",
                        "Lo muestra como estable (en gris).",
                        "Variación de 30 días menor a " + stable + " % en valor absoluto",
                        "Todas las vistas"),
                new Reaction("Un mes tiene pocos días con dato",
                        "Marca ese mes como parcial.",
                        "Menos de 15 días con precio en el mes",
                        "Detalle del producto → Resumen mensual"),
                new Reaction("Se repite una carga o el Batch",
                        "Reemplaza los documentos: no duplica nada.",
                        "Escritura replace + upsert por _id",
                        "setup-mongo muestra los mismos totales"),
                new Reaction("MongoDB no responde",
                        "Muestra un aviso y \"Sin conexión\"; al volver la base, sigue sin reiniciar.",
                        "El driver de MongoDB reintenta la conexión solo",
                        "Barra lateral y página de error"));
    }

    static List<Param> params() {
        return List.of(
                new Param("k (multiplicador del IQR)", Text.num(PriceAlert.DEFAULT_K, 0),
                        "Qué tan ancho es el rango normal de la variación diaria", "BatchAnalyticsJob"),
                new Param("Variación mínima para alertar", Text.num(PriceAlert.DEFAULT_MIN_CHANGE_PCT, 0) + " %",
                        "Evita alertas en productos cuyo precio casi nunca cambia (IQR = 0)", "BatchAnalyticsJob"),
                new Param("Severidad alta / media", Text.num(PriceAlert.HIGH_IQR_DISTANCE, 0) + " / "
                        + Text.num(PriceAlert.MEDIUM_IQR_DISTANCE, 0) + " IQR",
                        "Distancia al límite del rango normal", "IqrAlertRules"),
                new Param("Menor abastecimiento", Text.num(PriceAlert.SUPPLY_DROP_THRESHOLD_PCT, 0) + " %",
                        "Caída del volumen frente a su promedio de 30 días que explica una subida", "PriceAlert"),
                new Param("Umbral de estabilidad", "±" + Text.num(Trend.STABLE_THRESHOLD_PCT, 0) + " %",
                        "Por debajo, una variación se considera estable", "Trend"),
                new Param("Alza o baja fuerte", "±" + Text.num(Situation.STRONG_PCT, 0) + " %",
                        "Variación de 30 días que se considera fuerte", "Situation"),
                new Param("Cerca del máximo histórico", Text.num(ProductDiagnosis.NEAR_HIGH * 100, 0) + " % del rango",
                        "Posición del último precio entre el mínimo y el máximo desde 2024", "ProductDiagnosis"),
                new Param("Sensible al abastecimiento", "correlación ≤ " + Text.num(ProductDiagnosis.SUPPLY_SENSITIVE_CORR, 1),
                        "Relación inversa entre precio y volumen diarios", "ProductSummary"),
                new Param("Mes parcial", "menos de 15 días", "Mes con pocos datos: su promedio es menos confiable",
                        "BatchAnalyticsJob"),
                new Param("Tolerancia de la validación cruzada", "S/ 0.011",
                        "Diferencia máxima entre el precio del PDF y el guardado", "PriceStreamingJob"),
                new Param("Historial que lee el Streaming", "45 días",
                        "Para calcular el precio anterior y el volumen promedio de los días nuevos", "PriceStreamingJob"),
                new Param("Frecuencia del Streaming", "cada 15 s",
                        "Cada cuánto revisa la carpeta de boletines nuevos", "PriceStreamingJob"));
    }
}
