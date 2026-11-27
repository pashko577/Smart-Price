package pe.smartprice.shared.web;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pe.smartprice.alert.application.port.in.ListAlertsUseCase;
import pe.smartprice.analytics.application.port.in.GetProductAnalyticsUseCase;
import pe.smartprice.analytics.domain.ProductSummary;

/**
 * Estado que muestra la barra lateral en todas las páginas: fecha de los datos y alertas altas recientes.
 * Se usa desde la plantilla como {@code ${@nav.status()}}.
 *
 * <p>Se guarda en memoria unos segundos para no consultar MongoDB en cada página, y nunca lanza
 * excepciones: si la base no responde, la barra lateral lo indica y la página sigue mostrándose.
 */
@Component("nav")
public class NavStatus {

    private static final Logger LOG = LoggerFactory.getLogger(NavStatus.class);
    static final Duration TTL_OK = Duration.ofSeconds(60);
    static final Duration TTL_OFFLINE = Duration.ofSeconds(15);
    static final int ALERT_WINDOW_DAYS = 30;

    /**
     * @param online     MongoDB respondió
     * @param dataDate   último día con indicadores calculados
     * @param highAlerts alertas de severidad alta en los últimos 30 días con datos
     */
    public record Status(boolean online, LocalDate dataDate, long highAlerts, int products) {
    }

    private record Cached(Status status, Instant until) {
    }

    private final GetProductAnalyticsUseCase analytics;
    private final ListAlertsUseCase alerts;
    private final AtomicReference<Cached> cache = new AtomicReference<>();

    public NavStatus(GetProductAnalyticsUseCase analytics, ListAlertsUseCase alerts) {
        this.analytics = analytics;
        this.alerts = alerts;
    }

    public Status status() {
        Cached c = cache.get();
        if (c != null && Instant.now().isBefore(c.until())) {
            return c.status();
        }
        Status s;
        try {
            var summaries = analytics.summaries();
            LocalDate dataDate = ProductSummary.latestDate(summaries).orElse(null);
            LocalDate asOf = dataDate != null ? dataDate : alerts.lastAlertDate();
            long high = alerts.statsSince(asOf.minusDays(ALERT_WINDOW_DAYS - 1L)).high();
            s = new Status(true, dataDate, high, summaries.size());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo leer el estado para la barra lateral: {}", e.getMessage());
            s = new Status(false, null, 0, 0);
        }
        cache.set(new Cached(s, Instant.now().plus(s.online() ? TTL_OK : TTL_OFFLINE)));
        return s;
    }
}
