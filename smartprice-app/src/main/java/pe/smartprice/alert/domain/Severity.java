package pe.smartprice.alert.domain;

/** Severidad de una alerta, de menor a mayor. */
public enum Severity {
    LOW, MEDIUM, HIGH;

    public boolean isAtLeast(Severity other) {
        return compareTo(other) >= 0;
    }

    public static Severity parseOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Severity.valueOf(value.trim().toUpperCase());
    }
}
