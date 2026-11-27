package pe.smartprice.insight.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import pe.smartprice.alert.domain.AlertStats;
import pe.smartprice.alert.domain.PriceAlert;
import pe.smartprice.analytics.domain.ProductSummary;

/**
 * Redacta la lectura del mercado a partir de los indicadores. Reglas de texto puras: mismos datos,
 * mismo texto. No inventa cifras: cada número sale de un resultado de Spark.
 */
public final class MarketNarrative {

    private MarketNarrative() {
    }

    public static List<String> compose(MarketPulse pulse, List<ProductDiagnosis> diagnoses, AlertStats stats,
                                       InflationComparison inflation) {
        // Puntos clave cortos, con la forma "Tema: dato". La vista pone el tema en negrita.
        List<String> p = new ArrayList<>();
        if (pulse.products() == 0) {
            p.add("Sin datos: ejecuta los jobs load-sisap y batch-analytics.");
            return p;
        }

        // 1. El pulso general.
        p.add("Mercado: " + pulse.phase().phrase() + " (al " + Text.date(pulse.asOf()) + "). "
                + pulse.rising() + " de " + pulse.products() + " productos subieron 5 % o más y "
                + pulse.falling() + " bajaron.");
        p.add("Canasta: " + Text.pct(pulse.basketChangePct()) + " en 30 días; el producto típico, "
                + Text.pct(pulse.medianChangePct()) + ".");

        // 2. Quiénes empujan.
        Comparator<ProductDiagnosis> byChange = Comparator.comparing((ProductDiagnosis d) -> d.summary().varPct30d());
        List<String> up = diagnoses.stream().filter(d -> d.situation().isRising()).sorted(byChange.reversed())
                .limit(3).map(d -> d.productName() + " " + Text.pct(d.summary().varPct30d())).toList();
        List<String> down = diagnoses.stream().filter(d -> d.situation().isFalling()).sorted(byChange)
                .limit(3).map(d -> d.productName() + " " + Text.pct(d.summary().varPct30d())).toList();
        if (!up.isEmpty()) {
            p.add("Más subieron: " + String.join(", ", up) + ".");
        }
        if (!down.isEmpty()) {
            p.add("Más bajaron: " + String.join(", ", down) + ".");
        }

        // 3. Cómo reaccionó el sistema y el abastecimiento.
        List<PriceAlert> spikes = diagnoses.stream().flatMap(d -> d.alerts().stream())
                .filter(a -> a.type() == pe.smartprice.alert.domain.AlertType.PRICE_SPIKE).toList();
        List<PriceAlert> supply = spikes.stream().filter(PriceAlert::isExplainedBySupplyDrop).toList();
        if (stats.total() == 0) {
            p.add("Alertas: ninguna variación atípica en 30 días.");
        } else {
            p.add("Alertas: " + stats.total() + " en 30 días (" + stats.high() + " altas)"
                    + (spikes.isEmpty() ? "." : "; " + supply.size() + " de " + spikes.size()
                    + " subidas por menor abastecimiento."));
            // El caso más marcado: la mayor caída del ingreso en toneladas (pesa más un producto de gran volumen).
            PriceAlert strongest = supply.stream()
                    .max(Comparator.comparingDouble(a -> a.volumeAvg30dT() - a.volumeT())).orElse(null);
            if (strongest != null) {
                p.add("Caso clave: " + strongest.productName() + " el " + Text.date(strongest.observedAt()) + ", "
                        + Text.pct(strongest.changePct()) + " con " + Text.tons(strongest.volumeT())
                        + " ingresadas (promedio " + Text.tons(strongest.volumeAvg30dT()) + ").");
            }
        }

        // 4. Frente a su historial.
        long nearHigh = diagnoses.stream().filter(ProductDiagnosis::nearHistoricalHigh).count();
        long nearLow = diagnoses.stream().filter(ProductDiagnosis::nearHistoricalLow).count();
        long yoy = diagnoses.stream().filter(ProductDiagnosis::muchHigherThanLastYear).count();
        p.add("Historial: " + nearHigh + " cerca de su máximo y " + nearLow + " cerca de su mínimo; "
                + yoy + " cuestan 20 % más que hace un año.");

        // 5. Frente a la inflación.
        if (inflation != null && inflation.isComplete()) {
            p.add("Inflación: mayorista " + Text.pct(inflation.wholesalePct()) + " vs IPC de Lima "
                    + Text.pct(inflation.cpiPct()) + " (" + Text.month(java.time.YearMonth.from(inflation.from()))
                    + " – " + Text.month(java.time.YearMonth.from(inflation.to())) + ").");
        }
        return p;
    }

    /** Productos que conviene vigilar, más urgentes primero. */
    public static List<ProductDiagnosis> watchList(List<ProductDiagnosis> diagnoses, int limit) {
        return diagnoses.stream().filter(ProductDiagnosis::worthWatching)
                .sorted(Comparator.comparingDouble(ProductDiagnosis::watchScore).reversed())
                .limit(limit).toList();
    }

    /**
     * Calidad de datos: productos con exactamente los mismos indicadores de precio probablemente son una
     * categoría agregada de otro (por ejemplo "Papa Blanca" y "Papa Yungay" en SISAP).
     */
    public static List<String> qualityNotes(List<ProductSummary> summaries) {
        Map<String, List<String>> twins = new LinkedHashMap<>();
        for (ProductSummary s : summaries) {
            if (s.lastPrice() == null || s.avg30d() == null || s.varPct30d() == null) {
                continue;
            }
            String key = Objects.toString(s.lastPrice()) + "|" + s.avg30d() + "|" + s.varPct30d() + "|" + s.varPctYoy();
            twins.computeIfAbsent(key, k -> new ArrayList<>()).add(s.productName());
        }
        List<String> notes = new ArrayList<>();
        twins.values().stream().filter(names -> names.size() > 1).forEach(names -> notes.add(Text.list(names)
                + " tienen los mismos indicadores de precio: probablemente una agrupa a la otra (no son señales independientes)."));
        return notes;
    }
}
