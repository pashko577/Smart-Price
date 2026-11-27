package pe.smartprice.alert.domain;

import java.time.LocalDate;

/** Conteo de alertas desde una fecha. */
public record AlertStats(LocalDate since, long total, long high, long medium, long low,
                         long spikes, long drops) {
}
