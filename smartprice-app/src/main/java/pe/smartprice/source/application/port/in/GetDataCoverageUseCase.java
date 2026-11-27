package pe.smartprice.source.application.port.in;

import java.util.List;

import pe.smartprice.source.domain.DataCoverage;
import pe.smartprice.source.domain.ProcessingRun;

/** Puerto de entrada: qué archivos alimentan el sistema, qué periodo cubren y cómo se procesaron. */
public interface GetDataCoverageUseCase {

    DataCoverage coverage();

    /** Ejecuciones recientes del Streaming de boletines diarios. */
    List<ProcessingRun> recentRuns(int limit);
}
