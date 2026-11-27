package pe.smartprice.analytics.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Categoría de un producto del Gran Mercado Mayorista, deducida de su nombre en SISAP.
 * Sirve para filtrar, agrupar y comparar productos parecidos (por ejemplo, todas las papas).
 */
public enum ProductCategory {
    PAPAS("Papas", List.of("papa ")),
    RAICES("Raíces y tubérculos", List.of("beterraga", "camote", "olluco", "yuca", "zanahoria")),
    AJIES("Ajíes y pimiento", List.of("aji ", "pimiento")),
    BULBOS("Cebollas, ajos y kion", List.of("cebolla", "ajo ", "kion")),
    HOJAS("Hojas y tallos", List.of("apio", "brocoli", "col china", "culantro", "espinaca", "lechuga", "pacchoy",
            "alcachofa", "esparrago")),
    VAINAS("Menestras verdes", List.of("arveja", "frejol", "frijol", "haba", "pallar", "vainita", "holantau")),
    MAIZ("Maíz", List.of("choclo", "maiz")),
    FRUTOS("Frutos y frutas", List.of("tomate", "zapallo", "limon", "manzana", "maracuya")),
    OTROS("Otros", List.of());

    private final String label;
    private final List<String> prefixes;

    ProductCategory(String label, List<String> prefixes) {
        this.label = label;
        this.prefixes = prefixes;
    }

    public String label() {
        return label;
    }

    /** Clave corta para URLs y filtros ("papas", "raices", …). */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ProductCategory of(String productName) {
        if (productName == null) {
            return OTROS;
        }
        String n = Normalizer.normalize(productName, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).trim() + " ";
        for (ProductCategory c : values()) {
            for (String p : c.prefixes) {
                if (n.startsWith(p)) {
                    return c;
                }
            }
        }
        return OTROS;
    }

    public static ProductCategory parseOrNull(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        for (ProductCategory c : values()) {
            if (c.key().equalsIgnoreCase(key.trim())) {
                return c;
            }
        }
        return null;
    }
}
