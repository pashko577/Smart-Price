package pe.smartprice.insight.infrastructure.adapter.in.web;

import java.util.Arrays;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import pe.smartprice.insight.application.port.in.GetMarketAnalysisUseCase;
import pe.smartprice.insight.domain.Situation;

/** Vista "Análisis del mercado": lectura automática, diagnóstico por producto y reacción del sistema. */
@Controller
public class AnalysisController {

    private final GetMarketAnalysisUseCase analysis;

    public AnalysisController(GetMarketAnalysisUseCase analysis) {
        this.analysis = analysis;
    }

    @GetMapping("/analysis")
    public String page(Model model) {
        model.addAttribute("analysis", analysis.analyze());
        model.addAttribute("situations", Arrays.stream(Situation.values()).filter(s -> s != Situation.UNKNOWN).toList());
        model.addAttribute("active", "analysis");
        return "analysis/index";
    }
}
