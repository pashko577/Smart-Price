package pe.smartprice.insight.domain;

import java.time.LocalDate;
import java.util.List;

import pe.smartprice.alert.domain.AlertStats;

/**
 * Análisis completo del mercado en una ventana de 30 días.
 *
 * @param asOf         último día con datos
 * @param windowStart  primer día de la ventana (asOf − 29 días)
 * @param narrative    la lectura del mercado, en párrafos
 * @param diagnoses    diagnóstico de cada producto, ordenado por variación de 30 días (mayor primero)
 * @param watchList    productos que conviene vigilar, más urgentes primero
 * @param reactions    cómo reaccionó el sistema ante las variaciones atípicas más relevantes
 * @param alertStats   conteo de alertas de la ventana
 * @param inflation    mayorista frente al IPC (puede estar incompleto si no se cargó el contexto)
 * @param qualityNotes advertencias de calidad de datos detectadas automáticamente
 * @param categories   comparación por categoría, de la que más subió a la que más bajó
 */
public record MarketAnalysis(
        LocalDate asOf,
        LocalDate windowStart,
        MarketPulse pulse,
        List<String> narrative,
        List<ProductDiagnosis> diagnoses,
        List<ProductDiagnosis> watchList,
        List<AlertExplanation> reactions,
        AlertStats alertStats,
        InflationComparison inflation,
        List<String> qualityNotes,
        List<CategoryStat> categories) {

    public MarketAnalysis {
        narrative = List.copyOf(narrative);
        diagnoses = List.copyOf(diagnoses);
        watchList = List.copyOf(watchList);
        reactions = List.copyOf(reactions);
        qualityNotes = List.copyOf(qualityNotes);
        categories = List.copyOf(categories);
    }

    /** La mayor variación absoluta entre categorías (para escalar las barras). */
    public double maxCategoryChange() {
        return categories.stream().map(CategoryStat::medianVar30).filter(v -> v != null)
                .mapToDouble(Math::abs).max().orElse(1);
    }

    /** Largo de la barra de una categoría (0–100 % del ancho), proporcional a su variación absoluta. */
    public double barWidth(CategoryStat c) {
        double max = maxCategoryChange();
        return c.medianVar30() == null || max == 0 ? 0 : Math.round(Math.abs(c.medianVar30()) / max * 1000) / 10.0;
    }

    /** Subidas atípicas de la ventana explicadas por una caída fuerte del volumen. */
    public long supplyExplainedSpikes() {
        return diagnoses.stream().flatMap(d -> d.alerts().stream())
                .filter(pe.smartprice.alert.domain.PriceAlert::isExplainedBySupplyDrop).count();
    }
}
