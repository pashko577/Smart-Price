package pe.smartprice.analytics.domain;

import java.time.LocalDate;

/**
 * Resumen actual de un producto, calculado por el job Batch de Spark (PRODUCT_SUMMARY).
 *
 * @param varPct30d       promedio de los últimos 30 días frente a los 30 días anteriores (%)
 * @param varPctYoy       promedio de los últimos 30 días frente al mismo periodo del año anterior (%)
 * @param corrPriceVolume correlación de Pearson entre precio y volumen diarios
 */
public record ProductSummary(
        String productName,
        String marketCode,
        LocalDate firstDate,
        LocalDate lastDate,
        Double lastPrice,
        Double avg30d,
        Double avg365d,
        Double minAll,
        Double maxAll,
        Double lastVolumeT,
        Double varPct30d,
        Double varPctYoy,
        Double corrPriceVolume,
        long observations) {

    public Trend trend30d() {
        return Trend.of(varPct30d);
    }

    public Trend trendYoy() {
        return Trend.of(varPctYoy);
    }

    /** Relación inversa moderada o fuerte: cuando entra menos producto, el precio tiende a subir. */
    public boolean priceReactsToSupply() {
        return corrPriceVolume != null && corrPriceVolume <= -0.3;
    }

    /** Posición del último precio entre el mínimo y el máximo históricos (0 = mínimo, 1 = máximo). */
    public Double positionInHistoricalRange() {
        if (lastPrice == null || minAll == null || maxAll == null || maxAll.equals(minAll)) {
            return null;
        }
        return (lastPrice - minAll) / (maxAll - minAll);
    }
}
