package pe.smartprice.source.infrastructure.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import pe.smartprice.source.application.port.in.GetDataCoverageUseCase;

/** Fuentes y trazabilidad: qué archivos alimentan el sistema. */
@Controller
public class SourceController {

    static final int RECENT_RUNS = 10;

    private final GetDataCoverageUseCase coverage;

    public SourceController(GetDataCoverageUseCase coverage) {
        this.coverage = coverage;
    }

    @GetMapping("/sources")
    public String sources(Model model) {
        model.addAttribute("coverage", coverage.coverage());
        model.addAttribute("runs", coverage.recentRuns(RECENT_RUNS));
        model.addAttribute("active", "sources");
        return "sources/list";
    }
}
