package pe.smartprice.context.application.port.out;

import pe.smartprice.context.domain.IndexSeries;

/** Puerto de salida: series de índices escritas por el job load-context. */
public interface IndexSeriesPort {

    /** Serie completa; vacía (sin puntos) si no existe. */
    IndexSeries find(String source, String indicator);
}
