package pe.smartprice.alert.application.port.in;

import java.time.LocalDate;
import java.util.List;

import pe.smartprice.alert.domain.AlertFilter;
import pe.smartprice.alert.domain.AlertStats;
import pe.smartprice.alert.domain.PriceAlert;

/** Puerto de entrada: consulta de alertas de precio. */
public interface ListAlertsUseCase {

    /** Alertas que cumplen el filtro, de la más reciente a la más antigua. */
    List<PriceAlert> list(AlertFilter filter);

    /** Alertas recientes relevantes para el dashboard (severidad media o alta). */
    List<PriceAlert> recentRelevant(int limit);

    AlertStats statsSince(LocalDate since);

    /** Fecha de la alerta más reciente, útil para calcular "últimos N días". */
    LocalDate lastAlertDate();
}
