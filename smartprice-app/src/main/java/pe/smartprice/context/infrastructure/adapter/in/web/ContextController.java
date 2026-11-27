package pe.smartprice.context.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import pe.smartprice.context.application.port.in.GetPriceContextUseCase;
import pe.smartprice.context.domain.IndexSeries;
import pe.smartprice.context.domain.PriceContext;

/** Vista "Contexto" y API JSON de sus gráficos. */
@Controller
public class ContextController {

    private final GetPriceContextUseCase context;

    public ContextController(GetPriceContextUseCase context) {
        this.context = context;
    }

    /** Fila de la tabla resumen: variación de cada serie entre la base y la última fecha común. */
    public record SeriesRow(String label, String source, Double changePct, Double lastValue, String lastDate) {
    }

    public record PointDto(String date, double value) {
    }

    public record SeriesDto(String key, String label, String source, List<PointDto> points) {
    }

    public record ContextDto(String title, String base, String commonEnd, List<SeriesDto> series) {
    }

    @GetMapping("/context")
    public String page(Model model) {
        PriceContext inflation = context.wholesaleVsInflation();
        PriceContext supermarkets = context.wholesaleVsSupermarkets();
        model.addAttribute("inflation", inflation);
        model.addAttribute("inflationRows", rows(inflation));
        model.addAttribute("supermarkets", supermarkets);
        model.addAttribute("supermarketRows", rows(supermarkets));
        model.addAttribute("active", "context");
        return "context/index";
    }

    @GetMapping("/api/context/inflation")
    @ResponseBody
    public ContextDto inflation() {
        return dto(context.wholesaleVsInflation());
    }

    @GetMapping("/api/context/supermarkets")
    @ResponseBody
    public ContextDto supermarkets() {
        return dto(context.wholesaleVsSupermarkets());
    }

    static List<SeriesRow> rows(PriceContext c) {
        return c.series().stream()
                .map(s -> new SeriesRow(s.label(), sourceName(s.source()), c.changeToCommonEnd(s).orElse(null),
                        c.commonEnd() == null ? null : s.valueAt(c.commonEnd()).orElse(null),
                        c.commonEnd() == null ? null : c.commonEnd().toString()))
                .toList();
    }

    static ContextDto dto(PriceContext c) {
        return new ContextDto(c.title(), String.valueOf(c.base()), String.valueOf(c.commonEnd()),
                c.series().stream().map(ContextController::seriesDto).toList());
    }

    static SeriesDto seriesDto(IndexSeries s) {
        return new SeriesDto(s.source() + ":" + s.indicator(), s.label(), sourceName(s.source()),
                s.points().stream().map(p -> new PointDto(p.date().toString(),
                        Math.round(p.value() * 100) / 100.0)).toList());
    }

    static String sourceName(String source) {
        return switch (source) {
            case "SMARTPRICE" -> "SmartPrice (SISAP, MIDAGRI)";
            case "BCRP_INEI" -> "INEI, vía BCRP";
            case "QHAWARINA" -> "Qhawarina";
            default -> source;
        };
    }
}
