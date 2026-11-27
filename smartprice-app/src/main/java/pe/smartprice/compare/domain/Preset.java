package pe.smartprice.compare.domain;

import java.util.List;

/**
 * Un grupo de productos listo para comparar con un clic (la canasta básica, las mayores subidas,
 * una categoría…).
 *
 * @param key      identificador para la URL (?preset=…)
 * @param hint     explicación breve de cómo se eligieron los productos
 */
public record Preset(String key, String label, String hint, List<String> products) {

    public Preset {
        products = List.copyOf(products);
    }
}
