package pe.smartprice.analytics.domain;

import java.time.YearMonth;

/** Indicadores mensuales de un producto, calculados por el job Batch de Spark (PRICE_MONTHLY). */
public record MonthlyPrice(
        String productName,
        YearMonth month,
        Double avgPrice,
        Double minPrice,
        Double maxPrice,
        Double avgVolumeT,
        Double totalVolumeT,
        Double varPctVsPrevMonth,
        Double varPctVsPrevYear,
        int daysObserved,
        String dataQualityStatus) {

    public boolean isPartial() {
        return !"VALIDATED".equals(dataQualityStatus);
    }

    public Trend trendVsPrevMonth() {
        return Trend.of(varPctVsPrevMonth);
    }
}
