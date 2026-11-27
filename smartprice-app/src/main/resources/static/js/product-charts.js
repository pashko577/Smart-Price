/*
 * Gráficos del detalle de producto (Apache ECharts).
 *
 * Dos gráficos de una sola serie que comparten el eje de fechas, en lugar de un gráfico con dos ejes Y:
 *   - precio promedio diario (línea, serie 1)
 *   - volumen ingresado (columnas, serie 2)
 * Comparten cursor, tooltip y zoom (echarts.connect). Los colores salen de las variables CSS,
 * así que siguen el tema claro/oscuro.
 */
window.SmartPriceCharts = (function () {
    "use strict";

    var GROUP = "smartprice-product";
    var priceChart, volumeChart, lastData, productName, apiUrl;

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
            price: css("--series-1"),
            volume: css("--series-2"),
            font: getComputedStyle(document.body).fontFamily
        };
    }

    var soles = new Intl.NumberFormat("es-PE", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    var tons = new Intl.NumberFormat("es-PE", { maximumFractionDigits: 0 });
    var dateFmt = new Intl.DateTimeFormat("es-PE", { day: "numeric", month: "short", year: "numeric", timeZone: "UTC" });

    function fmtDate(iso) {
        return dateFmt.format(new Date(iso + "T00:00:00Z"));
    }

    function tooltipHtml(t, date, price, volume) {
        var row = function (color, label, value) {
            return '<div style="display:flex;align-items:center;gap:8px;margin-top:4px">' +
                '<span style="width:10px;height:2px;background:' + color + ';display:inline-block"></span>' +
                '<span style="color:' + t.muted + '">' + label + '</span>' +
                '<span style="margin-left:auto;font-weight:600;padding-left:16px">' + value + "</span></div>";
        };
        return '<div style="font-weight:600">' + fmtDate(date) + "</div>" +
            row(t.price, "Precio", price == null ? "sin dato" : "S/ " + soles.format(price)) +
            row(t.volume, "Volumen", volume == null ? "sin dato" : tons.format(volume) + " t");
    }

    function baseOption(t, dates) {
        return {
            animationDuration: 300,
            textStyle: { fontFamily: t.font, color: t.muted },
            grid: { left: 56, right: 72, top: 16, bottom: 28 },
            xAxis: {
                type: "category",
                data: dates,
                boundaryGap: true,   // igual en ambos gráficos: las fechas quedan alineadas
                axisLine: { lineStyle: { color: t.axis } },
                axisTick: { show: false },
                axisLabel: { color: t.muted, hideOverlap: true, formatter: function (v) { return fmtDate(v); } },
                axisPointer: { show: true, type: "line", lineStyle: { color: t.axis, width: 1 }, label: { show: false } }
            },
            tooltip: {
                trigger: "axis",
                confine: true,
                backgroundColor: t.surface,
                borderColor: t.grid,
                textStyle: { color: t.text, fontFamily: t.font, fontSize: 13 },
                formatter: function (params) {
                    var i = params[0].dataIndex;
                    var p = lastData.points[i];
                    return tooltipHtml(t, p.date, p.price, p.volumeT);
                }
            },
            dataZoom: [{ type: "inside", throttle: 50 }]
        };
    }

    function render() {
        if (!lastData) return;
        var t = theme();
        var pts = lastData.points;
        var dates = pts.map(function (p) { return p.date; });
        var prices = pts.map(function (p) { return p.price; });
        var volumes = pts.map(function (p) { return p.volumeT; });

        // Etiqueta directa solo en el último precio informado (no en cada punto).
        var lastIdx = -1;
        for (var i = prices.length - 1; i >= 0; i--) { if (prices[i] != null) { lastIdx = i; break; } }

        var priceOpt = baseOption(t, dates);
        priceOpt.yAxis = {
            type: "value",
            scale: true,
            splitNumber: 4,
            axisLabel: { color: t.muted, formatter: function (v) { return "S/ " + soles.format(v); } },
            splitLine: { lineStyle: { color: t.grid, width: 1 } }
        };
        priceOpt.series = [{
            name: "Precio",
            type: "line",
            data: prices,
            connectNulls: false,
            showSymbol: false,
            symbolSize: 8,
            lineStyle: { width: 2, color: t.price, cap: "round", join: "round" },
            itemStyle: { color: t.price, borderColor: t.surface, borderWidth: 2 },
            areaStyle: { color: t.price, opacity: 0.10 },
            emphasis: { focus: "none", scale: false },
            markPoint: lastIdx < 0 ? undefined : {
                symbol: "circle",
                symbolSize: 9,
                itemStyle: { color: t.price, borderColor: t.surface, borderWidth: 2 },
                label: {
                    show: true,
                    position: "right",
                    color: t.text,
                    fontWeight: 600,
                    formatter: function () { return "S/ " + soles.format(prices[lastIdx]); }
                },
                data: [{ coord: [dates[lastIdx], prices[lastIdx]] }]
            }
        }];

        var volumeOpt = baseOption(t, dates);
        volumeOpt.grid = { left: 56, right: 72, top: 8, bottom: 28 };
        volumeOpt.yAxis = {
            type: "value",
            splitNumber: 2,
            axisLabel: { color: t.muted, formatter: function (v) { return tons.format(v) + " t"; } },
            splitLine: { lineStyle: { color: t.grid, width: 1 } }
        };
        volumeOpt.series = [{
            name: "Volumen",
            type: "bar",
            data: volumes,
            barMaxWidth: 24,
            barCategoryGap: "20%",
            itemStyle: { color: t.volume, borderRadius: [4, 4, 0, 0] },
            emphasis: { disabled: true }
        }];

        priceChart.setOption(priceOpt, true);
        volumeChart.setOption(volumeOpt, true);
    }

    function load(days) {
        var status = document.getElementById("chart-status");
        var url = apiUrl + "?product=" + encodeURIComponent(productName);
        if (days > 0 && lastData && lastData.to) {
            var to = new Date(lastData.to + "T00:00:00Z");
            to.setUTCDate(to.getUTCDate() - days);
            url += "&from=" + to.toISOString().slice(0, 10);
        } else if (days === 0) {
            url += "&from=2000-01-01";
        }
        status.textContent = "Cargando…";
        return fetch(url)
            .then(function (r) {
                if (!r.ok) throw new Error("HTTP " + r.status);
                return r.json();
            })
            .then(function (data) {
                lastData = data;
                status.textContent = data.points.length + " días · " + fmtDate(data.from) + " a " + fmtDate(data.to) +
                    " · arrastra o usa la rueda del ratón sobre el gráfico para acercar.";
                render();
            })
            .catch(function (e) {
                status.textContent = "No se pudo cargar el historial (" + e.message + ").";
            });
    }

    function init(name, url) {
        productName = name;
        apiUrl = url;
        priceChart = echarts.init(document.getElementById("chart-price"), null, { renderer: "svg" });
        volumeChart = echarts.init(document.getElementById("chart-volume"), null, { renderer: "svg" });
        priceChart.group = GROUP;
        volumeChart.group = GROUP;
        echarts.connect(GROUP);

        window.addEventListener("resize", function () { priceChart.resize(); volumeChart.resize(); });
        document.addEventListener("smartprice:theme", render);
        matchMedia("(prefers-color-scheme: dark)").addEventListener("change", render);

        var buttons = document.querySelectorAll(".segmented button");
        buttons.forEach(function (b) {
            b.addEventListener("click", function () {
                buttons.forEach(function (x) { x.setAttribute("aria-pressed", String(x === b)); });
                load(parseInt(b.dataset.days, 10));
            });
        });

        // Primera carga: último año (el servicio usa 365 días por defecto).
        load(-1);
    }

    return { init: init };
})();
