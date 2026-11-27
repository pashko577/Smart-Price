package pe.smartprice.alert.domain;

import java.time.LocalDate;

/**
 * Criterios de búsqueda de alertas.
 *
 * @param minSeverity severidad mínima (nulo = todas)
 * @param productName producto exacto (nulo = todos)
 * @param type        tipo de alerta (nulo = ambos)
 * @param from        fecha mínima inclusive (nulo = sin límite)
 * @param limit       máximo de resultados
 */
public record AlertFilter(Severity minSeverity, String productName, AlertType type, LocalDate from, int limit) {

    public static final int MAX_LIMIT = 500;

    public AlertFilter {
        if (productName != null && productName.isBlank()) {
            productName = null;
        }
        if (limit <= 0 || limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
    }
}
