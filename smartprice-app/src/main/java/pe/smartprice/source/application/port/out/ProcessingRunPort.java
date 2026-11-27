package pe.smartprice.source.application.port.out;

import java.util.List;

import pe.smartprice.source.domain.ProcessingRun;

/** Puerto de salida: ejecuciones registradas por el Streaming. */
public interface ProcessingRunPort {

    /** Las más recientes primero. */
    List<ProcessingRun> findRecent(int limit);
}
