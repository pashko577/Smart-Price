package pe.smartprice.alert.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import pe.smartprice.alert.domain.AlertFilter;
import pe.smartprice.alert.domain.PriceAlert;

/** Puerto de salida: lectura de las alertas escritas por Spark. */
public interface AlertPort {

    /** Ordenadas por fecha descendente. */
    List<PriceAlert> find(String marketCode, AlertFilter filter);

    /** Todas las alertas desde una fecha (para estadísticas). */
    List<PriceAlert> findSince(String marketCode, LocalDate since);

    Optional<LocalDate> lastAlertDate(String marketCode);
}
