package pe.smartprice.alert.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Criterios de búsqueda de alertas.
 *
 * @param minSeverity severidad mínima (nulo = todas)
 * @param productName producto exacto (nulo = todos)
 * @param type        tipo de alerta (nulo = ambos)
 * @param from        fecha mínima inclusive (nulo = sin límite)
 * @param limit       máximo de resultados
 * @param to          fecha máxima inclusive (nulo = sin límite)
 * @param products    solo estos productos, por ejemplo los de una categoría (vacío = todos)
 * @param supplyOnly  solo subidas explicadas por menor abastecimiento
 */
public record AlertFilter(Severity minSeverity, String productName, AlertType type, LocalDate from, int limit,
                          LocalDate to, List<String> products, boolean supplyOnly) {

    public static final int MAX_LIMIT = 500;

    public AlertFilter {
        if (productName != null && productName.isBlank()) {
            productName = null;
        }
        if (limit <= 0 || limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
        products = products == null ? List.of() : List.copyOf(products);
    }

    /** Filtro básico, sin fecha máxima, sin lista de productos y sin el filtro de abastecimiento. */
    public AlertFilter(Severity minSeverity, String productName, AlertType type, LocalDate from, int limit) {
        this(minSeverity, productName, type, from, limit, null, List.of(), false);
    }

    /** El mismo filtro sin severidad mínima ni límite: para contar todas las alertas que cumple. */
    public AlertFilter forStats() {
        return new AlertFilter(null, productName, type, from, MAX_LIMIT, to, products, supplyOnly);
    }

    /** ¿La alerta cumple este filtro? (el adaptador ya filtra en la base; esto sirve para las reglas de dominio). */
    public boolean matches(PriceAlert a) {
        return (!supplyOnly || a.isExplainedBySupplyDrop())
                && (minSeverity == null || (a.severity() != null && a.severity().isAtLeast(minSeverity)))
                && (productName == null || productName.equals(a.productName()))
                && (type == null || type == a.type())
                && (from == null || !a.observedAt().isBefore(from))
                && (to == null || !a.observedAt().isAfter(to))
                && (products.isEmpty() || products.contains(a.productName()));
    }
}
