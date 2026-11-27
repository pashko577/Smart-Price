/*
 * Comportamiento común de todas las páginas: tema claro/oscuro y barra lateral.
 *  - Escritorio: la barra lateral se puede contraer (solo iconos); el estado se guarda en localStorage.
 *  - Móvil (≤ 960 px): la barra lateral es un panel que se abre con el botón de menú y se cierra con
 *    el fondo, el botón X o la tecla Esc.
 * Los gráficos (ECharts) escuchan "resize" y "smartprice:theme" para redibujarse.
 */
(function () {
    "use strict";

    var root = document.documentElement;
    var sidebar = document.getElementById("sidebar");
    var openBtn = document.getElementById("sb-open");
    var closeBtn = document.getElementById("sb-close");
    var backdrop = document.getElementById("sb-backdrop");
    var collapseBtn = document.getElementById("sb-collapse");
    var mobile = window.matchMedia("(max-width: 960px)");

    // ---------- tema ----------
    function currentTheme() {
        return root.getAttribute("data-theme")
            || (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
    }

    function syncThemeLabel() {
        root.setAttribute("data-theme-effective", currentTheme());
    }

    document.querySelectorAll(".js-theme-toggle").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var next = currentTheme() === "dark" ? "light" : "dark";
            root.setAttribute("data-theme", next);
            localStorage.setItem("smartprice-theme", next);
            syncThemeLabel();
            document.dispatchEvent(new CustomEvent("smartprice:theme"));
        });
    });
    syncThemeLabel();

    // ---------- barra lateral: contraer (escritorio) ----------
    function notifyResize() {
        // La transición del ancho dura ~180 ms: se avisa a los gráficos cuando termina.
        setTimeout(function () { window.dispatchEvent(new Event("resize")); }, 220);
    }

    function syncCollapse() {
        var collapsed = root.classList.contains("sb-collapsed");
        if (collapseBtn) {
            collapseBtn.setAttribute("aria-pressed", collapsed ? "true" : "false");
            collapseBtn.querySelector(".sb-label").textContent = collapsed ? "Expandir menú" : "Contraer menú";
        }
    }

    if (collapseBtn) {
        collapseBtn.addEventListener("click", function () {
            var collapsed = root.classList.toggle("sb-collapsed");
            localStorage.setItem("smartprice-sidebar", collapsed ? "collapsed" : "expanded");
            syncCollapse();
            notifyResize();
        });
    }
    syncCollapse();

    // ---------- barra lateral: panel (móvil) ----------
    function setOpen(open) {
        root.classList.toggle("sb-open", open);
        if (openBtn) openBtn.setAttribute("aria-expanded", open ? "true" : "false");
        if (open && closeBtn) closeBtn.focus();
        if (!open && openBtn && mobile.matches) openBtn.focus();
    }

    if (openBtn) openBtn.addEventListener("click", function () { setOpen(true); });
    if (closeBtn) closeBtn.addEventListener("click", function () { setOpen(false); });
    if (backdrop) backdrop.addEventListener("click", function () { setOpen(false); });
    document.addEventListener("keydown", function (e) {
        if (e.key === "Escape" && root.classList.contains("sb-open")) setOpen(false);
    });
    mobile.addEventListener("change", function () { root.classList.remove("sb-open"); });

    // Al elegir una sección en móvil, el panel se cierra.
    if (sidebar) {
        sidebar.querySelectorAll(".sb-link").forEach(function (a) {
            a.addEventListener("click", function () { if (mobile.matches) root.classList.remove("sb-open"); });
        });
    }

    var reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");

    // ---------- tablas: vista en tarjetas (pantallas angostas) y scroll propio (tablas largas) ----------
    // Cada celda recibe la etiqueta de su columna (data-label), que el CSS muestra cuando la tabla
    // se convierte en tarjetas. La primera celda con un enlace (o la primera) es el título de la tarjeta.
    document.querySelectorAll("main .table-wrap").forEach(function (wrap) {
        var table = wrap.querySelector("table");
        if (!table || table.hasAttribute("data-keep-table")) return;
        var ths = Array.prototype.slice.call(table.querySelectorAll("thead th"));
        var heads = ths.map(function (th) { return th.textContent.replace(/\s+/g, " ").trim(); });
        // class="opt" en el encabezado: dato secundario que se omite en las tarjetas de móvil
        var optional = ths.map(function (th) { return th.classList.contains("opt"); });
        if (!heads.length) return;
        var rows = table.querySelectorAll("tbody tr");
        rows.forEach(function (tr) {
            var cells = Array.prototype.slice.call(tr.cells);
            cells.forEach(function (td, i) {
                if (heads[i]) td.setAttribute("data-label", heads[i]);
                if (optional[i]) td.classList.add("cell-opt");
                var text = td.textContent.replace(/\s+/g, " ").trim();
                if (!text) td.classList.add("cell-empty");
                if (text.length > 70) td.classList.add("cell-wide");
            });
            var primary = cells.filter(function (td) { return td.querySelector("a"); })[0] || cells[0];
            if (primary) primary.classList.add("cell-primary");
        });
        table.classList.add("as-cards");
        wrap.classList.add("has-cards");
        if (rows.length > 25) {
            wrap.classList.add("scroll-y");
            wrap.setAttribute("tabindex", "0"); // se puede desplazar con el teclado
            wrap.setAttribute("aria-label", "Tabla con desplazamiento: " + rows.length + " filas");
        }
    });

    // Con la tabla desplazada de lado, la primera columna (fija por CSS) proyecta una sombra que
    // indica que hay columnas ocultas a la izquierda.
    document.querySelectorAll("main .table-wrap").forEach(function (wrap) {
        var update = function () { wrap.classList.toggle("is-scrolled-x", wrap.scrollLeft > 2); };
        wrap.addEventListener("scroll", update, { passive: true });
        update();
    });

    // ---------- índice de la página: marca la sección que se está leyendo ----------
    var toc = document.querySelector(".toc");
    if (toc && "IntersectionObserver" in window) {
        var links = Array.prototype.slice.call(toc.querySelectorAll('a[href^="#"]'));
        var visible = {};
        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (e) { visible[e.target.id] = e.isIntersecting; });
            var current = null;
            links.forEach(function (a) {
                if (!current && visible[a.getAttribute("href").slice(1)]) current = a;
            });
            links.forEach(function (a) {
                if (a === current) a.setAttribute("aria-current", "true"); else a.removeAttribute("aria-current");
            });
            // en móvil el índice es una fila deslizable: se mueve hasta la sección actual
            if (current && toc.scrollWidth > toc.clientWidth) {
                toc.scrollTo({ left: current.offsetLeft - 16, behavior: reduceMotion.matches ? "auto" : "smooth" });
            }
        }, { rootMargin: "-140px 0px -50% 0px" });
        links.forEach(function (a) {
            var target = document.getElementById(a.getAttribute("href").slice(1));
            if (target) observer.observe(target);
        });
    }

    // ---------- botón "Volver arriba" ----------
    var toTop = document.createElement("button");
    toTop.type = "button";
    toTop.className = "to-top";
    toTop.setAttribute("aria-label", "Volver arriba");
    toTop.title = "Volver arriba";
    toTop.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 19V5M5 12l7-7 7 7"/></svg>';
    document.body.appendChild(toTop);
    toTop.addEventListener("click", function () {
        window.scrollTo({ top: 0, behavior: reduceMotion.matches ? "auto" : "smooth" });
        var main = document.getElementById("main");
        if (main) { main.setAttribute("tabindex", "-1"); main.focus({ preventScroll: true }); }
    });
    var ticking = false;
    function updateToTop() {
        toTop.classList.toggle("show", window.scrollY > 600);
        ticking = false;
    }
    window.addEventListener("scroll", function () {
        if (!ticking) { ticking = true; window.requestAnimationFrame(updateToTop); }
    }, { passive: true });
    updateToTop();
})();
