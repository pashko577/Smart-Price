package pe.smartprice.source.application.service;

import java.util.Comparator;
import java.util.List;

import pe.smartprice.source.application.port.in.GetDataCoverageUseCase;
import pe.smartprice.source.application.port.out.ProcessingRunPort;
import pe.smartprice.source.application.port.out.SourceFilePort;
import pe.smartprice.source.domain.DataCoverage;
import pe.smartprice.source.domain.ProcessingRun;
import pe.smartprice.source.domain.SourceFile;

public class DataCoverageService implements GetDataCoverageUseCase {

    static final int MAX_RUNS = 100;

    private final SourceFilePort files;
    private final ProcessingRunPort runs;

    public DataCoverageService(SourceFilePort files, ProcessingRunPort runs) {
        this.files = files;
        this.runs = runs;
    }

    @Override
    public DataCoverage coverage() {
        return DataCoverage.of(files.findAll().stream()
                .sorted(Comparator.comparing(SourceFile::firstDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList());
    }

    @Override
    public List<ProcessingRun> recentRuns(int limit) {
        return runs.findRecent(Math.max(1, Math.min(limit, MAX_RUNS)));
    }
}
