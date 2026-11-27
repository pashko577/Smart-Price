package pe.smartprice.source.application.port.out;

import java.util.List;

import pe.smartprice.source.domain.SourceFile;

public interface SourceFilePort {

    List<SourceFile> findAll();
}
