package pe.smartprice.alert.domain;

public enum AlertType {
    PRICE_SPIKE, PRICE_DROP;

    public static AlertType parseOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return AlertType.valueOf(value.trim().toUpperCase());
    }
}
