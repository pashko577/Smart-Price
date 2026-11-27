package pe.smartprice.compare.application.port.in;

import java.time.LocalDate;
import java.util.List;

import pe.smartprice.compare.domain.Comparison;
import pe.smartprice.compare.domain.Grouping;
import pe.smartprice.compare.domain.Preset;

/** Puerto de entrada: comparar el precio de varios productos en un mismo periodo. */
public interface ComparePricesUseCase {

    /** Máximo de productos por comparación (los colores del gráfico siguen distinguibles hasta 6). */
    int MAX_PRODUCTS = 6;
    /** Periodo por defecto: el último año con datos. */
    int DEFAULT_DAYS = 365;

    /**
     * @param from inclusive; nulo = {@code to} menos 365 días
     * @param to   inclusive; nulo = último día con datos
     * @throws IllegalArgumentException si no hay productos, hay más de 6, alguno no existe o el periodo es inválido
     */
    Comparison compare(List<String> products, LocalDate from, LocalDate to, Grouping grouping);

    /** Grupos listos para comparar: canasta básica, mayores subidas y bajadas, una por categoría. */
    List<Preset> presets();

    /** Primer y último día con datos (para limitar el selector de fechas). */
    LocalDate firstDate();

    LocalDate lastDate();
}
