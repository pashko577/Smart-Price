package pe.smartprice.alert.application.service;

import java.time.LocalDate;
import java.util.List;

import pe.smartprice.alert.application.port.in.ListAlertsUseCase;
import pe.smartprice.alert.application.port.out.AlertPort;
import pe.smartprice.alert.domain.AlertFilter;
import pe.smartprice.alert.domain.AlertStats;
import pe.smartprice.alert.domain.AlertType;
import pe.smartprice.alert.domain.PriceAlert;
import pe.smartprice.alert.domain.Severity;

/** Casos de uso de alertas. Sin dependencias de Spring ni de MongoDB. */
public class AlertService implements ListAlertsUseCase {

    public static final String DEFAULT_MARKET = "GMML";
    /** Severidad mínima que se muestra en el dashboard. */
    static final Severity DASHBOARD_MIN_SEVERITY = Severity.MEDIUM;

    private final AlertPort port;

    public AlertService(AlertPort port) {
        this.port = port;
    }

    @Override
    public List<PriceAlert> list(AlertFilter filter) {
        if (!filter.supplyOnly()) {
            return port.find(DEFAULT_MARKET, filter);
        }
        // "Menor abastecimiento" es una regla de dominio: se filtra aquí y luego se aplica el límite.
        return port.findMatching(DEFAULT_MARKET, filter).stream().filter(filter::matches).limit(filter.limit()).toList();
    }

    @Override
    public AlertStats stats(AlertFilter filter, LocalDate since) {
        AlertFilter all = filter.forStats();
        List<PriceAlert> alerts = port.findMatching(DEFAULT_MARKET, all).stream().filter(all::matches).toList();
        return new AlertStats(since, alerts.size(),
                count(alerts, Severity.HIGH), count(alerts, Severity.MEDIUM), count(alerts, Severity.LOW),
                alerts.stream().filter(a -> a.type() == AlertType.PRICE_SPIKE).count(),
                alerts.stream().filter(a -> a.type() == AlertType.PRICE_DROP).count());
    }

    @Override
    public List<PriceAlert> recentRelevant(int limit) {
        return port.find(DEFAULT_MARKET, new AlertFilter(DASHBOARD_MIN_SEVERITY, null, null, null, limit));
    }

    @Override
    public AlertStats statsSince(LocalDate since) {
        List<PriceAlert> alerts = port.findSince(DEFAULT_MARKET, since);
        return new AlertStats(since, alerts.size(),
                count(alerts, Severity.HIGH), count(alerts, Severity.MEDIUM), count(alerts, Severity.LOW),
                alerts.stream().filter(a -> a.type() == AlertType.PRICE_SPIKE).count(),
                alerts.stream().filter(a -> a.type() == AlertType.PRICE_DROP).count());
    }

    @Override
    public LocalDate lastAlertDate() {
        return port.lastAlertDate(DEFAULT_MARKET).orElse(LocalDate.now());
    }

    private static long count(List<PriceAlert> alerts, Severity s) {
        return alerts.stream().filter(a -> a.severity() == s).count();
    }
}
