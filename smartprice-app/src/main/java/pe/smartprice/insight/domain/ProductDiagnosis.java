package pe.smartprice.insight.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import pe.smartprice.alert.domain.PriceAlert;
import pe.smartprice.alert.domain.Severity;
import pe.smartprice.analytics.domain.MonthlyPrice;
import pe.smartprice.analytics.domain.ProductSummary;

/**
 * Diagnóstico automático de un producto: qué está pasando con su precio, por qué (volumen, historial)
 * y cómo reaccionó el sistema (alertas). Se arma solo con resultados que ya calculó Spark.
 *
 * @param alerts      alertas del producto en la ventana de análisis, de la más reciente a la más antigua
 * @param headline    una frase que resume la situación
 * @param reasons     la explicación, punto por punto
 */
public record ProductDiagnosis(
        ProductSummary summary,
        Situation situation,
        List<PriceAlert> alerts,
        String headline,
        List<String> reasons) {

    /** Correlación precio-volumen a partir de la cual se considera que el precio reacciona a la oferta. */
    public static final double SUPPLY_SENSITIVE_CORR = -0.3;
    /** Posición en el rango histórico (0 = mínimo, 1 = máximo) que se considera "cerca del máximo". */
    public static final double NEAR_HIGH = 0.8;
    public static final double NEAR_LOW = 0.2;
    /** Variación anual a partir de la cual el producto está "mucho más caro que hace un año". */
    public static final double YOY_HIGH_PCT = 20.0;

    public ProductDiagnosis {
        alerts = List.copyOf(alerts);
        reasons = List.copyOf(reasons);
    }

    public String productName() {
        return summary.productName();
    }

    public long highAlerts() {
        return alerts.stream().filter(a -> a.severity() == Severity.HIGH).count();
    }

    /** Hubo al menos una subida atípica acompañada de una caída fuerte del volumen. */
    public boolean supplyExplained() {
        return alerts.stream().anyMatch(PriceAlert::isExplainedBySupplyDrop);
    }

    public boolean nearHistoricalHigh() {
        Double p = summary.positionInHistoricalRange();
        return p != null && p >= NEAR_HIGH;
    }

    public boolean nearHistoricalLow() {
        Double p = summary.positionInHistoricalRange();
        return p != null && p <= NEAR_LOW;
    }

    public boolean muchHigherThanLastYear() {
        return summary.varPctYoy() != null && summary.varPctYoy() >= YOY_HIGH_PCT;
    }

    public boolean supplySensitive() {
        return summary.corrPriceVolume() != null && summary.corrPriceVolume() <= SUPPLY_SENSITIVE_CORR;
    }

    /**
     * Vale la pena vigilarlo: alza fuerte con alertas altas recientes, o alza en su máximo histórico.
     */
    public boolean worthWatching() {
        return situation == Situation.STRONG_RISE && (highAlerts() > 0 || nearHistoricalHigh())
                || situation.isRising() && nearHistoricalHigh() && muchHigherThanLastYear();
    }

    /** Puntaje para ordenar la lista de vigilancia (mayor = más urgente). */
    public double watchScore() {
        double v = summary.varPct30d() == null ? 0 : summary.varPct30d();
        return v + 15 * highAlerts() + (nearHistoricalHigh() ? 10 : 0) + (supplyExplained() ? 5 : 0);
    }

    /**
     * @param alertsInWindow alertas del producto en los últimos 30 días (cualquier severidad)
     * @param monthly        serie mensual del producto (puede estar vacía); se usa para la tendencia del volumen
     */
    public static ProductDiagnosis of(ProductSummary s, List<PriceAlert> alertsInWindow, List<MonthlyPrice> monthly) {
        List<PriceAlert> alerts = alertsInWindow.stream()
                .sorted(Comparator.comparing(PriceAlert::observedAt).reversed()).toList();
        Situation situation = Situation.of(s.varPct30d());
        String name = s.productName();

        // Frases cortas: un dato por línea.
        String change = Text.pct(s.varPct30d()) + " frente al mes anterior.";
        String headline = switch (situation) {
            case STRONG_RISE -> name + " está en alza fuerte: " + change;
            case RISE -> name + " está en alza: " + change;
            case STABLE -> name + " se mantiene estable: " + change;
            case FALL -> name + " está bajando: " + change;
            case STRONG_FALL -> name + " está en baja fuerte: " + change;
            case UNKNOWN -> name + ": sin datos suficientes de 30 días.";
        };

        List<String> reasons = new ArrayList<>();
        ProductDiagnosis probe = new ProductDiagnosis(s, situation, alerts, headline, List.of());

        // 1. Nivel frente a su propio historial.
        Double pos = s.positionInHistoricalRange();
        if (pos != null) {
            String where = probe.nearHistoricalHigh() ? ", cerca del máximo"
                    : probe.nearHistoricalLow() ? ", cerca del mínimo" : "";
            reasons.add("Último precio " + Text.soles(s.lastPrice()) + ": " + Text.num(pos * 100, 0)
                    + " % de su rango (" + Text.soles(s.minAll()) + " – " + Text.soles(s.maxAll()) + ")" + where + ".");
        }

        // 2. Frente a hace un año.
        if (s.varPctYoy() != null) {
            reasons.add("Frente a hace un año: " + Text.pct(s.varPctYoy()) + ".");
        }

        // 3. Volumen: tendencia mensual (si se conoce) y relación histórica con el precio.
        volumeTrend(monthly).ifPresent(reasons::add);
        PriceAlert supplyCase = alerts.stream().filter(PriceAlert::isExplainedBySupplyDrop)
                .min(Comparator.comparing(PriceAlert::volumeChangePct)).orElse(null);
        Double corr = s.corrPriceVolume();
        if (supplyCase != null) {
            reasons.add("Sube por menor abastecimiento: el " + Text.date(supplyCase.observedAt()) + ", precio "
                    + Text.pct(supplyCase.changePct()) + " con volumen " + Text.pct(supplyCase.volumeChangePct()) + ".");
        } else if (corr != null && corr <= SUPPLY_SENSITIVE_CORR) {
            reasons.add("Sensible a la oferta: si entra menos producto, sube (corr. " + Text.num(corr, 2) + ").");
        } else if (corr != null && corr >= 0.3) {
            reasons.add("Precio y volumen suben juntos (corr. " + Text.num(corr, 2) + "): no es falta de oferta.");
        } else if (corr != null) {
            reasons.add("Relación débil con el volumen (corr. " + Text.num(corr, 2) + ").");
        }

        // 4. Cómo reaccionó el sistema.
        if (alerts.isEmpty()) {
            reasons.add("El sistema no generó alertas en 30 días.");
        } else {
            PriceAlert last = alerts.get(0);
            reasons.add(Text.count(alerts.size(), "alerta", "alertas") + " en 30 días (" + probe.highAlerts()
                    + " altas). Última: " + Text.date(last.observedAt()) + ", " + Text.pct(last.changePct()) + ".");
        }

        return new ProductDiagnosis(s, situation, alerts, headline, reasons);
    }

    /** "Volumen promedio: 260 t/día en nov. 2026, 12.5 % menos que en oct. 2026 (297 t/día)." */
    static java.util.Optional<String> volumeTrend(List<MonthlyPrice> monthly) {
        List<MonthlyPrice> withVolume = monthly.stream().filter(m -> m.avgVolumeT() != null)
                .sorted(Comparator.comparing(MonthlyPrice::month)).toList();
        if (withVolume.size() < 2) {
            return java.util.Optional.empty();
        }
        MonthlyPrice last = withVolume.get(withVolume.size() - 1);
        MonthlyPrice prev = withVolume.get(withVolume.size() - 2);
        if (prev.avgVolumeT() == 0) {
            return java.util.Optional.empty();
        }
        double change = (last.avgVolumeT() - prev.avgVolumeT()) / prev.avgVolumeT() * 100;
        String direction = Math.abs(change) < 5 ? "casi igual que"
                : change < 0 ? Text.num(Math.abs(change), 1) + " % menos que" : Text.num(change, 1) + " % más que";
        return java.util.Optional.of("Volumen: " + Text.num(last.avgVolumeT(), 0) + " t/día en "
                + Text.month(last.month()) + (last.isPartial() ? " (parcial)" : "") + ", " + direction + " en "
                + Text.month(prev.month()) + ".");
    }
}
