package pe.smartprice.source.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Cobertura total de un conjunto de archivos de origen. */
public record DataCoverage(List<SourceFile> files, LocalDate firstDate, LocalDate lastDate, long totalRows) {

    public static DataCoverage of(List<SourceFile> files) {
        LocalDate first = files.stream().map(SourceFile::firstDate).min(LocalDate::compareTo).orElse(null);
        LocalDate last = files.stream().map(SourceFile::lastDate).max(LocalDate::compareTo).orElse(null);
        long rows = files.stream().mapToLong(SourceFile::rows).sum();
        return new DataCoverage(List.copyOf(files), first, last, rows);
    }

    public long days() {
        return firstDate == null ? 0 : ChronoUnit.DAYS.between(firstDate, lastDate) + 1;
    }
}
