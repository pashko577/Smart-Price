package pe.smartprice.insight.domain;

import java.util.ArrayList;
import java.util.List;

import pe.smartprice.alert.domain.AlertType;
import pe.smartprice.alert.domain.PriceAlert;
import pe.smartprice.alert.domain.Severity;

/**
 * Cómo reaccionó el sistema ante una variación de precio: la cuenta que hizo Spark, paso a paso,
 * con los números reales de la alerta.
 *
 * @param steps      el razonamiento en orden (variación, rango normal, distancia, volumen)
 * @param conclusion qué hizo el sistema con el resultado
 */
public record AlertExplanation(PriceAlert alert, String title, List<String> steps, String conclusion) {

    public AlertExplanation {
        steps = List.copyOf(steps);
    }

    public static String severityLabel(Severity s) {
        if (s == null) {
            return "sin severidad";
        }
        return switch (s) {
            case HIGH -> "Alta";
            case MEDIUM -> "Media";
            case LOW -> "Baja";
        };
    }

    public static AlertExplanation of(PriceAlert a) {
        boolean spike = a.type() == AlertType.PRICE_SPIKE;
        String title = a.productName() + " · " + Text.date(a.observedAt()) + " · "
                + (spike ? "subida" : "bajada") + " de " + Text.pct(a.changePct());

        List<String> steps = new ArrayList<>();

        // Una línea por paso: el dato y la decisión, sin explicaciones largas.
        // 1. La variación del día.
        steps.add("Precio: " + Text.soles(a.previousPrice()) + " → " + Text.soles(a.currentPrice())
                + " (" + Text.pct(a.changePct()) + ").");

        // 2. El rango normal del producto.
        if (a.iqr() != null) {
            steps.add("Rango normal: " + Text.pct(a.lowerBound()) + " a " + Text.pct(a.upperBound())
                    + " (IQR " + Text.num(a.iqr(), 2) + ", k = " + Text.num(a.effectiveK(), 0) + ").");
        }

        // 3. Cuánto se salió y qué severidad corresponde.
        Double distance = a.iqrDistance();
        if (distance != null && a.iqr() != null) {
            String bound = spike ? "límite superior" : "límite inferior";
            if (a.iqr() > 1e-9) {
                steps.add("Pasó el " + bound + " por " + Text.num(a.excessPoints(), 1) + " puntos ("
                        + Text.num(distance, 1) + " veces el IQR) → severidad " + severityLabel(a.severity()) + ".");
            } else {
                steps.add("Precio casi fijo (IQR = 0): " + Text.num(a.effectiveMinChangePct(), 0)
                        + " % o más → severidad " + severityLabel(a.severity()) + ".");
            }
        }

        // 4. El filtro de variación mínima.
        steps.add("Supera el mínimo de " + Text.num(a.effectiveMinChangePct(), 0) + " %.");

        // 5. El volumen: ¿hay una explicación de abastecimiento?
        Double vol = a.volumeChangePct();
        if (vol == null) {
            steps.add("Volumen: sin dato ese día.");
        } else if (a.isExplainedBySupplyDrop()) {
            steps.add("Volumen: " + Text.tons(a.volumeT()) + " vs " + Text.tons(a.volumeAvg30dT())
                    + " de promedio (" + Text.pct(vol) + ") → \"Menor abastecimiento\".");
        } else if (spike) {
            steps.add("Volumen: " + Text.tons(a.volumeT()) + " (" + Text.pct(vol)
                    + " vs su promedio): no explica la subida.");
        } else {
            steps.add("Volumen: " + Text.tons(a.volumeT()) + " (" + Text.pct(vol) + " vs su promedio)."
                    + (vol >= PriceAlert.SUPPLY_DROP_THRESHOLD_PCT ? " Entró mucho más producto." : ""));
        }

        String conclusion = "Resultado: alerta " + (spike ? "de subida" : "de bajada")
                + " " + severityLabel(a.severity()).toLowerCase()
                + (a.isExplainedBySupplyDrop() ? " + \"Menor abastecimiento\"" : "")
                + ", guardada en price_alerts.";
        return new AlertExplanation(a, title, steps, conclusion);
    }
}
