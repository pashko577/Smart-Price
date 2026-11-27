/*
 * Comparador de precios (Apache ECharts).
 *
 * - Un solo bloque de filtros arriba: periodo (rápido o con fechas), productos (hasta 6), grupos listos,
 *   agrupación (día, semana, mes) y medida (precio, índice base 100, volumen). Todo lo de abajo usa el mismo corte.
 * - Cada producto conserva su color mientras está elegido (el color sigue al producto, no a su posición).
 * - La URL guarda los filtros: la comparación se puede compartir o volver a abrir.
 * - Al recargar, los gráficos mantienen el dibujo anterior atenuado (sin parpadeo).
 */
window.SmartPriceCompare = (function () {
    "use strict";

    var cfg, root, max, firstDate, lastDate;
    var state = { products: [], from: null, to: null, days: 365, group: "WEEK", measure: "price" };
    var slots = {};           // producto -> número de color (1..6)
    var data = null;          // última respuesta de la API
    var sortKey = null, sortDir = -1;
    var charts = {};
    var req = 0;

    var soles = new Intl.NumberFormat("es-PE", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    var dec1 = new Intl.NumberFormat("es-PE", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
    var dec2 = new Intl.NumberFormat("es-PE", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    var int0 = new Intl.NumberFormat("es-PE", { maximumFractionDigits: 0 });
    var dateFmt = new Intl.DateTimeFormat("es-PE", { day: "numeric", month: "short", year: "numeric", timeZone: "UTC" });
    var NB = " ";

    function $(id) { return document.getElementById(id); }
    function css(name) { return getComputedStyle(document.documentElement).getPropertyValue(name).trim(); }
    function fmtDate(iso) { return iso ? dateFmt.format(new Date(iso + "T00:00:00Z")) : "—"; }
    function fmtSol(v) { return v == null ? "—" : "S/" + NB + soles.format(v); }
    function fmtPct(v) { return v == null ? "—" : (v > 0.05 ? "+" : v < -0.05 ? "−" : "") + dec1.format(Math.abs(v)) + NB + "%"; }
    function tone(v) { return v == null || Math.abs(v) < 5 ? "tone-neutral" : v > 0 ? "tone-up" : "tone-down"; }
    function isoMinusDays(iso, days) {
        var d = new Date(iso + "T00:00:00Z");
        d.setUTCDate(d.getUTCDate() - days + 1);
        return d.toISOString().slice(0, 10);
    }

    // ---------- colores: el producto conserva su color mientras siga elegido ----------
    function assignSlots() {
        Object.keys(slots).forEach(function (p) { if (state.products.indexOf(p) === -1) delete slots[p]; });
        state.products.forEach(function (p) {
            if (slots[p]) return;
            for (var i = 1; i <= max; i++) {
                var used = Object.keys(slots).some(function (k) { return slots[k] === i; });
                if (!used) { slots[p] = i; return; }
            }
        });
    }
    function colorOf(p) { return css("--cat-" + (slots[p] || 1)); }

    // ---------- estado <-> URL ----------
    function readUrl() {
        var p = new URLSearchParams(location.search);
        var products = p.getAll("products").filter(Boolean);
        var preset = p.get("preset") && document.querySelector('#f-preset option[value="' + CSS.escape(p.get("preset")) + '"]');
        state.products = products.length ? products.slice(0, max)
            : preset ? preset.dataset.products.split("|").slice(0, max)
            : (root.dataset.default ? root.dataset.default.split("|").slice(0, max) : []);
        if (p.get("group")) state.group = p.get("group").toUpperCase();
        if (p.get("measure")) state.measure = p.get("measure");
        if (p.get("from") || p.get("to")) {
            state.days = null;
            state.from = p.get("from") || firstDate;
            state.to = p.get("to") || lastDate;
        } else {
            state.days = p.has("days") ? parseInt(p.get("days"), 10) : 365;
            applyDays();
        }
    }
    function applyDays() {
        state.to = lastDate;
        state.from = state.days > 0 ? isoMinusDays(lastDate, state.days) : firstDate;
        if (state.from < firstDate) state.from = firstDate;
    }
    function query(extra) {
        var p = new URLSearchParams();
        state.products.forEach(function (x) { p.append("products", x); });
        p.set("from", state.from);
        p.set("to", state.to);
        p.set("group", state.group);
        Object.keys(extra || {}).forEach(function (k) { p.set(k, extra[k]); });
        return p;
    }
    function writeUrl() {
        var p = new URLSearchParams();
        state.products.forEach(function (x) { p.append("products", x); });
        if (state.days === null) { p.set("from", state.from); p.set("to", state.to); }
        else if (state.days !== 365) p.set("days", state.days);
        if (state.group !== "WEEK") p.set("group", state.group);
        if (state.measure !== "price") p.set("measure", state.measure);
        history.replaceState(null, "", location.pathname + "?" + p.toString());
        $("dl-points").href = cfg.csv + "?" + query({ kind: "points" });
        $("dl-stats").href = cfg.csv + "?" + query({ kind: "stats" });
    }

    // ---------- controles ----------
    function syncControls() {
        $("f-from").value = state.from;
        $("f-to").value = state.to;
        document.querySelectorAll("#period-presets button").forEach(function (b) {
            b.setAttribute("aria-pressed", String(state.days !== null && parseInt(b.dataset.days, 10) === state.days));
        });
        document.querySelectorAll("#group-buttons button").forEach(function (b) {
            b.setAttribute("aria-pressed", String(b.dataset.group === state.group));
        });
        document.querySelectorAll("#measure-buttons button").forEach(function (b) {
            b.setAttribute("aria-pressed", String(b.dataset.measure === state.measure));
        });
        assignSlots();
        var chips = $("chips");
        chips.innerHTML = "";
        state.products.forEach(function (p) {
            var chip = document.createElement("span");
            chip.className = "pchip";
            chip.setAttribute("role", "listitem");
            var dot = document.createElement("i");
            dot.style.background = colorOf(p);
            var name = document.createElement("a");
            name.href = cfg.product + "?name=" + encodeURIComponent(p);
            name.textContent = p;
            var x = document.createElement("button");
            x.type = "button";
            x.setAttribute("aria-label", "Quitar " + p);
            x.title = "Quitar " + p;
            x.textContent = "×";
            x.addEventListener("click", function () { removeProduct(p); });
            chip.append(dot, name, x);
            chips.appendChild(chip);
        });
        if (!state.products.length) {
            var hint = document.createElement("span");
            hint.className = "muted";
            hint.textContent = "Agrega al menos un producto.";
            chips.appendChild(hint);
        }
        var full = state.products.length >= max;
        $("f-add").disabled = full;
        $("f-add").options[0].textContent = full ? "Máximo " + max + " productos" : "Elige un producto…";
        Array.prototype.forEach.call($("f-add").options, function (o) {
            if (o.value) o.disabled = state.products.indexOf(o.value) !== -1;
        });
    }
    function removeProduct(p) {
        state.products = state.products.filter(function (x) { return x !== p; });
        refresh();
    }

    function bind() {
        document.querySelectorAll("#period-presets button").forEach(function (b) {
            b.addEventListener("click", function () { state.days = parseInt(b.dataset.days, 10); applyDays(); refresh(); });
        });
        ["f-from", "f-to"].forEach(function (id) {
            $(id).addEventListener("change", function () {
                var f = $("f-from").value, t = $("f-to").value;
                if (!f || !t) return;
                if (f > t) { var tmp = f; f = t; t = tmp; }
                state.days = null; state.from = f; state.to = t;
                refresh();
            });
        });
        $("f-add").addEventListener("change", function (e) {
            var v = e.target.value;
            e.target.value = "";
            if (v && state.products.indexOf(v) === -1 && state.products.length < max) {
                state.products.push(v);
                refresh();
            }
        });
        $("f-preset").addEventListener("change", function (e) {
            var opt = e.target.selectedOptions[0];
            e.target.value = "";
            if (!opt || !opt.dataset.products) return;
            state.products = opt.dataset.products.split("|").slice(0, max);
            refresh();
        });
        document.querySelectorAll("#group-buttons button").forEach(function (b) {
            b.addEventListener("click", function () { state.group = b.dataset.group; refresh(); });
        });
        document.querySelectorAll("#measure-buttons button").forEach(function (b) {
            b.addEventListener("click", function () { state.measure = b.dataset.measure; syncControls(); writeUrl(); render(); });
        });
        $("copy-link").addEventListener("click", function () {
            var done = function () { $("cmp-status").textContent = "Enlace copiado"; setTimeout(function () { $("cmp-status").textContent = ""; }, 2500); };
            if (navigator.clipboard) navigator.clipboard.writeText(location.href).then(done, function () { window.prompt("Copia el enlace:", location.href); });
            else window.prompt("Copia el enlace:", location.href);
        });
        document.querySelectorAll("#stats-table th").forEach(function (th) {
            th.tabIndex = 0;
            th.setAttribute("role", "button");
            var sortBy = function () {
                var k = th.dataset.key;
                if (sortKey === k) sortDir = -sortDir; else { sortKey = k; sortDir = k === "product" || k === "category" ? 1 : -1; }
                renderTable();
            };
            th.addEventListener("click", sortBy);
            th.addEventListener("keydown", function (e) { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); sortBy(); } });
        });
        window.addEventListener("resize", function () { Object.keys(charts).forEach(function (k) { charts[k].resize(); }); });
        document.addEventListener("smartprice:theme", function () { syncControls(); render(); });
    }

    // ---------- datos ----------
    function refresh() {
        syncControls();
        writeUrl();
        if (!state.products.length) { data = null; render(); return; }
        var id = ++req;
        $("cmp-results").classList.add("is-loading");
        $("cmp-status").textContent = "Cargando…";
        fetch(cfg.api + "?" + query())
            .then(function (r) {
                return r.json().then(function (body) {
                    if (!r.ok) throw new Error(body && body.error ? body.error : "HTTP " + r.status);
                    return body;
                });
            })
            .then(function (body) {
                if (id !== req) return;     // llegó una respuesta más nueva
                data = body;
                $("cmp-error").hidden = true;
                $("cmp-status").textContent = state.products.length + (state.products.length === 1 ? " producto · " : " productos · ")
                    + fmtDate(body.from) + " a " + fmtDate(body.to);
                render();
            })
            .catch(function (e) {
                if (id !== req) return;
                $("cmp-error").textContent = "No se pudo comparar: " + e.message;
                $("cmp-error").hidden = false;
                $("cmp-status").textContent = "";
            })
            .then(function () { if (id === req) $("cmp-results").classList.remove("is-loading"); });
    }

    // ---------- dibujo ----------
    function theme() {
        return { text: css("--text-primary"), muted: css("--text-secondary"), grid: css("--grid"), axis: css("--axis"),
            surface: css("--surface-1"), up: css("--div-up"), down: css("--div-down"), font: getComputedStyle(document.body).fontFamily };
    }
    function chart(id) {
        if (!charts[id]) charts[id] = echarts.init($(id), null, { renderer: "svg" });
        return charts[id];
    }

    function render() {
        var series = data ? data.series : [];
        renderKpis(series);
        renderReading();
        renderMain(series);
        renderBars("chart-rank", series, function (s) { return s.stats.changePct; });
        renderBars("chart-yoy", series, function (s) { return s.stats.vsPrevYearPct; });
        renderTable();
    }

    function setKpi(id, s, value, note) {
        $(id).textContent = s ? value : "—";
        $(id + "-n").textContent = s ? note : "";
    }
    function pick(series, f, better) {
        var best = null;
        series.forEach(function (s) { var v = f(s); if (v != null && (best === null || better(v, f(best)))) best = s; });
        return best;
    }
    function renderKpis(series) {
        var rise = pick(series, function (s) { return s.stats.changePct; }, function (a, b) { return a > b; });
        var fall = pick(series, function (s) { return s.stats.changePct; }, function (a, b) { return a < b; });
        var vol = pick(series, function (s) { return s.stats.volatilityPct; }, function (a, b) { return a > b; });
        var cheap = pick(series, function (s) { return s.stats.avgPrice; }, function (a, b) { return a < b; });
        if (rise && rise.stats.changePct <= 0) rise = null;
        if (fall && fall.stats.changePct >= 0) fall = null;
        setKpi("k-rise", rise, rise && fmtPct(rise.stats.changePct), rise && rise.product + ": " + fmtSol(rise.stats.firstPrice) + " → " + fmtSol(rise.stats.lastPrice));
        setKpi("k-fall", fall, fall && fmtPct(fall.stats.changePct), fall && fall.product + ": " + fmtSol(fall.stats.firstPrice) + " → " + fmtSol(fall.stats.lastPrice));
        setKpi("k-vol", vol, vol && dec1.format(vol.stats.volatilityPct) + NB + "%", vol && vol.product);
        setKpi("k-cheap", cheap, cheap && fmtSol(cheap.stats.avgPrice), cheap && cheap.product);
        if (!rise) $("k-rise-n").textContent = series.length ? "Ninguno subió en el periodo" : "";
        if (!fall) $("k-fall-n").textContent = series.length ? "Ninguno bajó en el periodo" : "";
    }

    function renderReading() {
        var box = $("reading");
        if (!box) return;   // la vista ya no muestra la lectura en texto: los indicadores y gráficos la resumen
        box.innerHTML = "";
        var lines = data && data.reading && data.reading.length ? data.reading
            : [state.products.length ? "Sin datos de precio para este periodo." : "Agrega productos para ver la lectura."];
        lines.forEach(function (t) { var p = document.createElement("p"); p.textContent = t; box.appendChild(p); });
    }

    var MEASURES = {
        price: { title: "Precio promedio por kg", unit: "Soles por kg", get: function (p) { return p.price; }, fmt: fmtSol, axis: function (v) { return "S/ " + soles.format(v); } },
        index: { title: "Índice base 100", unit: "100 = primer precio del periodo", get: function (p) { return p.index; }, fmt: function (v) { return v == null ? "—" : dec1.format(v); }, axis: function (v) { return int0.format(v); } },
        volume: { title: "Volumen ingresado", unit: "Toneladas por día (promedio)", get: function (p) { return p.volumeT; }, fmt: function (v) { return v == null ? "—" : int0.format(v) + NB + "t"; }, axis: function (v) { return int0.format(v) + " t"; } }
    };
    var GROUP_LABEL = { DAY: "por día", WEEK: "promedio por semana", MONTH: "promedio por mes" };

    function renderMain(series) {
        var t = theme(), m = MEASURES[state.measure];
        $("t-main").textContent = m.title;
        $("main-sub").textContent = m.unit + " · " + GROUP_LABEL[state.group];
        var dates = [];
        series.forEach(function (s) { s.points.forEach(function (p) { if (dates.indexOf(p.date) === -1) dates.push(p.date); }); });
        dates.sort();
        var opt = {
            animationDuration: 300,
            textStyle: { fontFamily: t.font, color: t.muted },
            grid: { left: 64, right: 24, top: 48, bottom: 64 },
            legend: { top: 0, left: 0, icon: "roundRect", itemWidth: 14, itemHeight: 4, textStyle: { color: t.text, fontSize: 13 } },
            tooltip: {
                trigger: "axis", confine: true, backgroundColor: t.surface, borderColor: t.grid,
                textStyle: { color: t.text, fontFamily: t.font, fontSize: 13 },
                valueFormatter: m.fmt,
                axisPointer: { type: "line", lineStyle: { color: t.axis } }
            },
            xAxis: {
                type: "category", data: dates, boundaryGap: false,
                axisLine: { lineStyle: { color: t.axis } }, axisTick: { show: false },
                axisLabel: { color: t.muted, hideOverlap: true, formatter: fmtDate }
            },
            yAxis: {
                type: "value", scale: state.measure !== "volume", splitNumber: 5,
                axisLabel: { color: t.muted, formatter: m.axis },
                splitLine: { lineStyle: { color: t.grid } }
            },
            dataZoom: [{ type: "inside", throttle: 50 }, { type: "slider", height: 18, bottom: 8, borderColor: t.grid,
                textStyle: { color: t.muted }, labelFormatter: function (i) { return fmtDate(dates[i]); } }],
            series: series.map(function (s) {
                var byDate = {};
                s.points.forEach(function (p) { byDate[p.date] = m.get(p); });
                return {
                    name: s.product, type: "line", showSymbol: false, symbolSize: 8, connectNulls: true,
                    data: dates.map(function (d) { return byDate[d] === undefined ? null : byDate[d]; }),
                    lineStyle: { width: 2, color: colorOf(s.product) },
                    itemStyle: { color: colorOf(s.product), borderColor: t.surface, borderWidth: 2 },
                    emphasis: { focus: "series" },
                    markLine: state.measure === "index" && s === series[0] ? { silent: true, symbol: "none",
                        lineStyle: { color: t.axis, type: "solid" }, label: { show: false }, data: [{ yAxis: 100 }] } : undefined
                };
            })
        };
        chart("chart-main").setOption(opt, true);

        // Precios de escalas muy distintas (ajo a S/ 7, papa a S/ 1): el índice base 100 los compara mejor.
        var avgs = series.map(function (s) { return s.stats.avgPrice; }).filter(function (v) { return v > 0; });
        var spread = avgs.length > 1 ? Math.max.apply(null, avgs) / Math.min.apply(null, avgs) : 1;
        var hint = $("main-hint");
        hint.hidden = !(state.measure === "price" && spread >= 3);
        if (!hint.hidden) {
            hint.innerHTML = "";
            hint.append("Escalas muy distintas (" + int0.format(spread) + "×): ");
            var b = document.createElement("button");
            b.type = "button";
            b.className = "link-btn";
            b.textContent = "ver como índice base 100";
            b.addEventListener("click", function () { document.querySelector("#measure-buttons [data-measure=index]").click(); });
            hint.append(b);
        }
    }

    function renderBars(id, series, f) {
        var t = theme();
        var rows = series.filter(function (s) { return f(s) != null; })
            .sort(function (a, b) { return f(a) - f(b); });   // la mayor queda arriba
        $(id).style.height = Math.max(160, rows.length * 44 + 40) + "px";
        chart(id).resize();
        chart(id).setOption({
            animationDuration: 300,
            textStyle: { fontFamily: t.font, color: t.muted },
            grid: { left: 8, right: 64, top: 8, bottom: 8, containLabel: true },
            tooltip: { trigger: "item", backgroundColor: t.surface, borderColor: t.grid, textStyle: { color: t.text, fontFamily: t.font },
                formatter: function (p) { return p.name + ": " + fmtPct(p.value); } },
            xAxis: { type: "value", axisLabel: { show: false }, splitLine: { show: false } },
            yAxis: { type: "category", data: rows.map(function (s) { return s.product; }), axisTick: { show: false },
                axisLine: { lineStyle: { color: t.axis } }, axisLabel: { color: t.text, fontSize: 13 } },
            series: [{
                type: "bar", barMaxWidth: 22,
                data: rows.map(function (s) {
                    var v = f(s);
                    return { value: v, itemStyle: { color: v >= 0 ? t.up : t.down, borderRadius: v >= 0 ? [0, 4, 4, 0] : [4, 0, 0, 4] } };
                }),
                label: { show: true, position: "right", color: t.text, fontWeight: 600, formatter: function (p) { return fmtPct(p.value); } }
            }],
            graphic: rows.length ? [] : [{ type: "text", left: "center", top: "middle", style: { text: "Sin datos", fill: t.muted } }]
        }, true);
    }

    function renderTable() {
        var tbody = document.querySelector("#stats-table tbody");
        var ths = Array.prototype.slice.call(document.querySelectorAll("#stats-table th"));
        tbody.innerHTML = "";
        var series = data ? data.series.slice() : [];
        if (sortKey) {
            series.sort(function (a, b) {
                var va = sortKey === "product" ? a.product : sortKey === "category" ? a.category : a.stats[sortKey];
                var vb = sortKey === "product" ? b.product : sortKey === "category" ? b.category : b.stats[sortKey];
                if (va == null) return 1;
                if (vb == null) return -1;
                return (typeof va === "string" ? va.localeCompare(vb, "es") : va - vb) * sortDir;
            });
        }
        ths.forEach(function (th) {
            th.setAttribute("aria-sort", th.dataset.key === sortKey ? (sortDir > 0 ? "ascending" : "descending") : "none");
        });
        series.forEach(function (s) {
            var st = s.stats;
            var cells = [
                function (td) {
                    var dot = document.createElement("i"); dot.className = "series-key"; dot.style.background = colorOf(s.product);
                    var a = document.createElement("a"); a.href = cfg.product + "?name=" + encodeURIComponent(s.product); a.textContent = s.product;
                    td.append(dot, a);
                },
                fmtSol(st.firstPrice), fmtSol(st.lastPrice),
                { text: fmtPct(st.changePct), cls: "delta " + tone(st.changePct) },
                { text: fmtSol(st.minPrice), sub: st.minDate ? fmtDate(st.minDate) : "" },
                { text: fmtSol(st.maxPrice), sub: st.maxDate ? fmtDate(st.maxDate) : "" },
                fmtSol(st.avgPrice),
                { text: fmtPct(st.vsPrevYearPct), cls: "delta " + tone(st.vsPrevYearPct) }
            ];   // categoría, volatilidad, volumen y correlación: en el CSV de resumen
            var tr = document.createElement("tr");
            cells.forEach(function (c, i) {
                var td = document.createElement("td");
                var th = ths[i];
                if (th.classList.contains("num")) td.className = "num";
                if (th.classList.contains("opt")) td.classList.add("cell-opt");
                td.setAttribute("data-label", th.textContent.trim());
                if (i === 0) td.classList.add("cell-primary");
                if (typeof c === "function") c(td);
                else if (typeof c === "object") {
                    var sp = document.createElement("span");
                    if (c.cls) sp.className = c.cls;
                    sp.textContent = c.text;
                    td.appendChild(sp);
                    if (c.sub) { var sm = document.createElement("small"); sm.className = "cell-sub"; sm.textContent = c.sub; td.appendChild(sm); }
                }
                else td.textContent = c;
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        });
    }

    function init(options) {
        cfg = options;
        root = $("cmp-root");
        max = parseInt(root.dataset.max, 10) || 6;
        firstDate = root.dataset.first;
        lastDate = root.dataset.last;
        readUrl();
        bind();
        refresh();
    }

    return { init: init };
})();
