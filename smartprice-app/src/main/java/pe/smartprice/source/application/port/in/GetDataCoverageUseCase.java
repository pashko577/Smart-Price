package pe.smartprice.source.application.port.in;

import java.util.List;

import pe.smartprice.source.domain.DataCoverage;
import pe.smartprice.source.domain.ProcessingRun;
import pe.smartprice.source.domain.SourceFile;

/** Puerto de entrada: qué archivos alimentan el sistema, qué periodo cubren y cómo se procesaron. */
public interface GetDataCoverageUseCase {

    /** Cobertura del histórico de precios mayoristas (archivos SISAP del GMML). */
    DataCoverage coverage();

    /** Archivos de contexto: índices de supermercados (Qhawarina) e IPC (INEI vía BCRP). */
    List<SourceFile> contextFiles();

    /** Ejecuciones recientes del Streaming de boletines diarios. */
    List<ProcessingRun> recentRuns(int limit);
}
