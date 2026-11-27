/*
 * Gráficos de la vista "Contexto" (Apache ECharts).
 *
 * Varias series de índices con la MISMA base (= 100) en un único eje Y: es una sola medida
 * (índice), así que no hace falta un segundo eje. Colores por entidad y en orden fijo:
 * serie 1 = mercado mayorista (azul), 2 = naranja, 3 = verde agua. Leyenda arriba (HTML),
 * etiqueta directa al final de cada línea, línea de referencia en 100 y tooltip con todas las series.
 */
window.SmartPriceContext = (function () {
    "use strict";

    var charts = [];

    function css(name) {
        return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    }

    function theme() {
        return {
            text: css("--text-primary"),
            muted: css("--text-secondary"),
            grid: css("--grid"),
            axis: css("--axis"),
            surface: css("--surface-1"),
            series: [css("--series-1"), css("--series-2"), css("--series-3")],
            font: getComputedStyle(document.body).fontFamily
        };
    }

    var num = new Intl.NumberFormat("es-PE", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
    var monthFmt = new Intl.DateTimeFormat("es-PE", { month: "short", year: "numeric", timeZone: "UTC" });
    var dayFmt = new Intl.DateTimeFormat("es-PE", { day: "numeric", month: "short", year: "numeric", timeZone: "UTC" });

    function fmtDate(iso, grain) {
        var d = new Date(iso + "T00:00:00Z");
        return grain === "month" ? monthFmt.format(d) : dayFmt.format(d);
    }

    function render(c) {
        if (!c.data) return;
        var t = theme();
        // Eje de fechas común: unión ordenada de todas las fechas.
        var dateSet = {};
        c.data.series.forEach(function (s) { s.points.forEach(function (p) { dateSet[p.date] = true; }); });
        var dates = Object.keys(dateSet).sort();

        // Etiqueta directa al final solo si no choca con otra: si dos series terminan casi en el mismo
        // valor, sus etiquetas se pisarían. En ese caso solo se rotula la serie anterior en el orden
        // (la serie 1, el mercado mayorista, siempre lleva etiqueta) y la otra queda en leyenda, tooltip y tabla.
        var lastValues = c.data.series.map(function (s) { return s.points.length ? s.points[s.points.length - 1].value : null; });
        var all = [];
        c.data.series.forEach(function (s) { s.points.forEach(function (p) { all.push(p.value); }); });
        var span = Math.max.apply(null, all) - Math.min.apply(null, all) || 1;
        var labeled = [];
        lastValues.forEach(function (v, i) {
            var clash = labeled.some(function (j) { return Math.abs(lastValues[j] - v) < span * 0.06; });
            if (v != null && !clash) labeled.push(i);
        });

        var series = c.data.series.map(function (s, i) {
            var byDate = {};
            s.points.forEach(function (p) { byDate[p.date] = p.value; });
            var values = dates.map(function (d) { return byDate[d] === undefined ? null : byDate[d]; });
            var last = -1;
            for (var k = values.length - 1; k >= 0; k--) { if (values[k] != null) { last = k; break; } }
            var color = t.series[i % t.series.length];
            return {
                name: s.label,
                type: "line",
                data: values,
                connectNulls: true,
                showSymbol: false,
                symbolSize: 8,
                lineStyle: { width: 2, color: color, cap: "round", join: "round" },
                itemStyle: { color: color, borderColor: t.surface, borderWidth: 2 },
                emphasis: { focus: "none", scale: false },
                // Etiqueta directa solo en el último punto de cada serie.
                markPoint: (last < 0 || labeled.indexOf(i) < 0) ? undefined : {
                    symbol: "circle", symbolSize: 8,
                    itemStyle: { color: color, borderColor: t.surface, borderWidth: 2 },
                    label: { show: true, position: "right", color: t.text, fontWeight: 600,
                             formatter: function () { return num.format(values[last]); } },
                    data: [{ coord: [dates[last], values[last]] }]
                },
                markLine: i !== 0 ? undefined : {
                    silent: true, symbol: "none",
                    lineStyle: { color: t.axis, width: 1, type: "solid" },
                    label: { show: false },   // la base ya figura en el subtítulo
                    data: [{ yAxis: 100 }]
                }
            };
        });

        c.chart.setOption({
            animationDuration: 300,
            textStyle: { fontFamily: t.font, color: t.muted },
            grid: { left: 48, right: 64, top: 16, bottom: 28 },
            legend: { show: false },   // la leyenda es HTML, sobre el gráfico
            tooltip: {
                trigger: "axis",
                confine: true,
                backgroundColor: t.surface,
                borderColor: t.grid,
                textStyle: { color: t.text, fontFamily: t.font, fontSize: 13 },
                formatter: function (params) {
                    var html = '<div style="font-weight:600">' + fmtDate(dates[params[0].dataIndex], c.grain) + "</div>";
                    params.forEach(function (p) {
                        if (p.value == null) return;
                        html += '<div style="display:flex;align-items:center;gap:8px;margin-top:4px">' +
                            '<span style="width:10px;height:2px;background:' + p.color + ';display:inline-block"></span>' +
                            '<span style="color:' + t.muted + '">' + p.seriesName + "</span>" +
                            '<span style="margin-left:auto;font-weight:600;padding-left:16px">' + num.format(p.value) + "</span></div>";
                    });
                    return html;
                }
            },
            xAxis: {
                type: "category",
                data: dates,
                boundaryGap: false,
                axisLine: { lineStyle: { color: t.axis } },
                axisTick: { show: false },
                axisLabel: { color: t.muted, hideOverlap: true, formatter: function (v) { return fmtDate(v, c.grain); } },
                axisPointer: { show: true, type: "line", lineStyle: { color: t.axis, width: 1 }, label: { show: false } }
            },
            yAxis: {
                type: "value",
                scale: true,
                splitNumber: 5,
                axisLabel: { color: t.muted },
                splitLine: { lineStyle: { color: t.grid, width: 1 } }
            },
            series: series
        }, true);
    }

    function init(elementId, url, grain) {
        var chart = echarts.init(document.getElementById(elementId), null, { renderer: "svg" });
        var c = { chart: chart, grain: grain, data: null };
        charts.push(c);
        fetch(url)
            .then(function (r) { if (!r.ok) throw new Error("HTTP " + r.status); return r.json(); })
            .then(function (data) { c.data = data; render(c); })
            .catch(function (e) {
                document.getElementById(elementId).textContent = "No se pudo cargar el gráfico (" + e.message + ").";
            });
        if (charts.length === 1) {
            window.addEventListener("resize", function () { charts.forEach(function (x) { x.chart.resize(); }); });
            document.addEventListener("smartprice:theme", function () { charts.forEach(render); });
            matchMedia("(prefers-color-scheme: dark)").addEventListener("change", function () { charts.forEach(render); });
        }
    }

    return { init: init };
})();
