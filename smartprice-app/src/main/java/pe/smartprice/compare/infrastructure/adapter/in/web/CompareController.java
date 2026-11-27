package pe.smartprice.compare.infrastructure.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import pe.smartprice.analytics.application.port.in.GetProductAnalyticsUseCase;
import pe.smartprice.analytics.domain.ProductCategory;
import pe.smartprice.analytics.domain.ProductSummary;
import pe.smartprice.compare.application.port.in.ComparePricesUseCase;
import pe.smartprice.compare.domain.ComparedSeries;
import pe.smartprice.compare.domain.Comparison;
import pe.smartprice.compare.domain.Grouping;
import pe.smartprice.compare.domain.Preset;
import pe.smartprice.compare.domain.SeriesStats;

/** Vista "Comparar precios", su API JSON y la descarga en CSV. */
@Controller
public class CompareController {

    private final ComparePricesUseCase compare;
    private final GetProductAnalyticsUseCase analytics;

    public CompareController(ComparePricesUseCase compare, GetProductAnalyticsUseCase analytics) {
        this.compare = compare;
        this.analytics = analytics;
    }

    /** Opción del selector de productos. */
    public record ProductOption(String name, String category, Double lastPrice, Double varPct30d) {
    }

    public record PointDto(String date, Double price, Double volumeT, Double index) {
    }

    public record StatsDto(String firstDate, Double firstPrice, String lastDate, Double lastPrice, Double changePct,
                           Double minPrice, String minDate, Double maxPrice, String maxDate, Double avgPrice,
                           Double amplitudePct, Double volatilityPct, Double avgVolumeT, Double corrPriceVolume,
                           int daysWithPrice, Double prevYearAvgPrice, Double vsPrevYearPct) {
    }

    public record SeriesDto(String product, String category, List<PointDto> points, StatsDto stats) {
    }

    public record ComparisonDto(String from, String to, String grouping, List<SeriesDto> series, List<String> reading) {
    }

    @GetMapping("/compare")
    public String page(Model model) {
        List<ProductSummary> summaries = analytics.summaries();
        Map<String, List<ProductOption>> byCategory = new LinkedHashMap<>();
        for (ProductCategory c : ProductCategory.values()) {
            List<ProductOption> opts = summaries.stream().filter(s -> ProductCategory.of(s.productName()) == c)
                    .map(s -> new ProductOption(s.productName(), c.label(), s.lastPrice(), s.varPct30d())).toList();
            if (!opts.isEmpty()) {
                byCategory.put(c.label(), opts);
            }
        }
        List<Preset> presets = compare.presets();
        model.addAttribute("byCategory", byCategory);
        model.addAttribute("presets", presets);
        model.addAttribute("firstDate", compare.firstDate().toString());
        model.addAttribute("lastDate", compare.lastDate().toString());
        model.addAttribute("defaultProducts", presets.isEmpty() ? List.of() : presets.get(0).products());
        model.addAttribute("maxProducts", ComparePricesUseCase.MAX_PRODUCTS);
        model.addAttribute("active", "compare");
        return "compare/index";
    }

    @GetMapping("/api/compare")
    @ResponseBody
    public ComparisonDto api(@RequestParam(name = "products") List<String> products,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                             @RequestParam(required = false) String group) {
        Comparison c = compare.compare(products, from, to, Grouping.parseOr(group, Grouping.WEEK));
        return new ComparisonDto(c.from().toString(), c.to().toString(), c.grouping().name(),
                c.series().stream().map(CompareController::dto).toList(), c.reading());
    }

    /** Descarga en CSV (UTF-8 con BOM, separador ";" para que Excel en español lo abra en columnas). */
    @GetMapping("/api/compare.csv")
    public ResponseEntity<byte[]> csv(@RequestParam(name = "products") List<String> products,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                      @RequestParam(required = false) String group,
                                      @RequestParam(defaultValue = "points") String kind) {
        Comparison c = compare.compare(products, from, to, Grouping.parseOr(group, Grouping.WEEK));
        List<String> lines = new ArrayList<>();
        if ("stats".equals(kind)) {
            lines.add("producto;categoria;desde;hasta;precio_inicial;precio_final;variacion_pct;minimo;fecha_minimo;"
                    + "maximo;fecha_maximo;promedio;amplitud_pct;volatilidad_diaria_pct;volumen_promedio_t;"
                    + "correlacion_precio_volumen;dias_con_precio;promedio_anio_anterior;vs_anio_anterior_pct");
            for (ComparedSeries s : c.series()) {
                SeriesStats st = s.stats();
                lines.add(String.join(";", s.productName(), s.category().label(), str(c.from()), str(c.to()),
                        num(st.firstPrice()), num(st.lastPrice()), num(st.changePct()), num(st.minPrice()), str(st.minDate()),
                        num(st.maxPrice()), str(st.maxDate()), num(st.avgPrice()), num(st.amplitudePct()),
                        num(st.volatilityPct()), num(st.avgVolumeT()), num(st.corrPriceVolume()),
                        String.valueOf(st.daysWithPrice()), num(st.prevYearAvgPrice()), num(st.vsPrevYearPct())));
            }
        } else {
            lines.add("fecha;producto;categoria;precio_soles_kg;volumen_t;indice_base_100");
            for (ComparedSeries s : c.series()) {
                for (ComparedSeries.Point p : s.points()) {
                    lines.add(String.join(";", str(p.date()), s.productName(), s.category().label(),
                            num(p.price()), num(p.volumeT()), num(p.index())));
                }
            }
        }
        byte[] body = ("﻿" + String.join("\r\n", lines) + "\r\n").getBytes(StandardCharsets.UTF_8);
        String file = "smartprice-comparacion-" + ("stats".equals(kind) ? "resumen-" : "") + c.from() + "_" + c.to() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    static SeriesDto dto(ComparedSeries s) {
        SeriesStats st = s.stats();
        return new SeriesDto(s.productName(), s.category().label(),
                s.points().stream().map(p -> new PointDto(p.date().toString(), p.price(), p.volumeT(), p.index())).toList(),
                new StatsDto(str(st.firstDate()), r(st.firstPrice(), 2), str(st.lastDate()), r(st.lastPrice(), 2),
                        r(st.changePct(), 1), r(st.minPrice(), 2), str(st.minDate()), r(st.maxPrice(), 2), str(st.maxDate()),
                        r(st.avgPrice(), 2), r(st.amplitudePct(), 1), r(st.volatilityPct(), 1), r(st.avgVolumeT(), 1),
                        r(st.corrPriceVolume(), 2), st.daysWithPrice(), r(st.prevYearAvgPrice(), 2), r(st.vsPrevYearPct(), 1)));
    }

    static Double r(Double v, int decimals) {
        if (v == null) {
            return null;
        }
        double f = Math.pow(10, decimals);
        return Math.round(v * f) / f;
    }

    static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    /** Número con coma decimal (Excel en español). */
    static String num(Double v) {
        return v == null ? "" : String.format(Locale.ROOT, "%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", "")
                .replace('.', ',');
    }
}
