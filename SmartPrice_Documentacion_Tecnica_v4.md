# SMARTPRICE

## Documento de propuesta, diseño técnico y plan de implementación

**Versión:** 4.0 (fuentes verificadas el 26-11-2026)\
**Fecha:** 26 de noviembre de 2026\
**Tipo de documento:** Documento de investigación y diseño técnico\
**Modalidad:** Desarrollo individual\
**Entorno de procesamiento:** Local (Windows 11)\
**Tecnologías centrales:** MongoDB Atlas, Apache Spark (API Java) y Java Spring Boot

------------------------------------------------------------------------

## Control del documento

  -----------------------------------------------------------------------
  Elemento                            Descripción
  ----------------------------------- -----------------------------------
  Nombre del proyecto                 SmartPrice

  Título                              Plataforma inteligente de
                                      comparación y análisis de precios

  Dominio                             Comercio, consumo y análisis de
                                      precios

  Ámbito geográfico inicial           Perú, con foco inicial en Lima

  Mercados considerados               Gran Mercado Mayorista de Lima
                                      (núcleo); supermercados como
                                      contexto y extensión

  Procesamiento                       Batch y Streaming

  Prioridad funcional                 Comparación comercial y analítica
                                      de precios

  Modalidad de trabajo                Individual

  Tipo de entrega                     Investigación tecnológica con
                                      demostración funcional
  -----------------------------------------------------------------------

> **Estado de madurez:** este documento define una propuesta y un plan
> técnico. El 26-11-2026 se verificaron los enlaces de todas las
> fuentes y se descargaron muestras de sus archivos (ver sección 3.0).
> Esa verificación cambió la fuente principal del proyecto: el dataset
> *Datero Agrario* de PNDA no contiene precios. Las fuentes adoptadas
> son dos, ambas de MIDAGRI y referidas al **Gran Mercado Mayorista de
> Lima (GMML)**:
>
> - **SISAP**, para el histórico: se exportó a mano el periodo del
>   01-01-2024 al 26-11-2026, con 59 productos y 62 599 observaciones.
> - **Los boletines diarios en PDF**, para los datos nuevos (Streaming) y
>   para validar SISAP.
>
> Los volúmenes, campos y formatos que no se hayan inspeccionado
> directamente se presentan como objetivos o hipótesis de trabajo, no
> como datos confirmados.
>
> **Cambios respecto de la versión anterior:**
>
> - Fuente principal: de PNDA Datero Agrario a SISAP (histórico) y a los boletines PDF diarios del GMML (datos nuevos).
> - Histórico ya descargado y unificado: 62 599 filas, sin huecos de fechas.
> - Validación cruzada: los 54 productos comunes coinciden entre SISAP y el PDF del 06-11-2026.
> - Nueva etapa de extracción de tablas desde PDF con Apache PDFBox, solo para los boletines nuevos.
> - Todo el proyecto pasa a **Java 17**: Apache Spark con su API Java y Spring Boot. Los prototipos en Python se migraron y se verificó que dan resultados idénticos (sección 9.5).
> - Qhawarina se usa solo como contexto histórico, porque no se actualiza desde el 13-06-2026.
> - El comparador de supermercados pasa a ser un módulo secundario, alimentado con una muestra capturada a mano.
> - El alcance del MVP y el cronograma se ajustan a una persona.
> - Se añade la configuración de Hadoop (`winutils.exe`) que Spark necesita en Windows.

------------------------------------------------------------------------

# 1. Resumen ejecutivo

SmartPrice es una propuesta de plataforma de inteligencia de precios de
alimentos en Lima. Su núcleo es el análisis de los **precios y volúmenes
de ingreso del Gran Mercado Mayorista de Lima (GMML)**, que MIDAGRI
publica cada día en PDF. Ese análisis se complementa con índices de
precios de supermercados (Qhawarina), el IPC del INEI y una muestra
controlada de precios de supermercados. Su propósito es organizar y
analizar datos de productos, mercados, precios y fechas para observar
tendencias, detectar variaciones y apoyar decisiones de compra.

El proyecto utilizará **MongoDB Atlas** como base de datos NoSQL y
**Apache Spark**, mediante su API Java, como motor de procesamiento de
datos. El sistema contempla dos modalidades de procesamiento:

-   **Batch:** análisis del histórico de boletines GMML y de los demás
    conjuntos de datos acumulados.
-   **Streaming:** procesamiento de cada boletín nuevo cuando se
    publica (frecuencia diaria) y de eventos de prueba etiquetados como
    sintéticos para la demostración. Ninguna de las fuentes publica
    precios en tiempo real.

La solución incluirá una capa de adquisición de datos, almacenamiento de
datos originales, normalización, procesamiento analítico, API y una
interfaz de usuario.

El enfoque de investigación consiste en comprobar si la integración de
fuentes heterogéneas y el procesamiento con Spark permiten construir
indicadores confiables de comparación y evolución de precios. La
demostración debe documentar la procedencia de los datos, las
transformaciones realizadas y las limitaciones de comparabilidad.

## 1.1. Problema

Los consumidores pueden tener dificultades para comparar precios entre
diferentes establecimientos y mercados, especialmente cuando la
información se encuentra distribuida en distintas fuentes, formatos,
unidades de medida y fechas de observación.

Además, los precios de supermercados y mercados tradicionales no siempre
son directamente comparables: pueden diferir por presentación, calidad,
variedad, unidad comercial, ubicación, disponibilidad y condiciones de
venta.

## 1.2. Propuesta de solución

SmartPrice centralizará observaciones de precios de distintas fuentes y
las procesará para:

1.  Consultar precios mayoristas por producto, categoría y fecha.
2.  Calcular variaciones y tendencias históricas.
3.  Relacionar el volumen de ingreso al mercado con el precio.
4.  Identificar valores atípicos que requieran revisión.
5.  Contrastar la evolución mayorista con los índices de supermercados
    y con el IPC.
6.  En un módulo secundario, comparar una muestra de productos de
    supermercado y estimar el costo de una canasta simple.

## 1.3. Objetivo general

Desarrollar una plataforma de análisis de precios de alimentos mediante
MongoDB Atlas y Apache Spark que permita extraer, integrar, almacenar,
procesar y analizar los precios diarios del Gran Mercado Mayorista de
Lima, contextualizados con índices de supermercados y de inflación, para
generar indicadores que apoyen las decisiones de compra.

## 1.4. Objetivos específicos

-   Identificar y evaluar fuentes de datos públicas y comerciales sobre
    precios.
-   Descargar y documentar conjuntos de datos en formatos como CSV,
    JSON, Excel y PDF.
-   Extraer de forma automatizada y verificable las tablas de precios
    de los boletines PDF diarios de MIDAGRI.
-   Diseñar una base de datos NoSQL para conservar datos originales y
    procesados.
-   Implementar procesos de ingestión y transformación mediante Apache Spark (API Java).
-   Desarrollar un pipeline Batch para el análisis de históricos.
-   Desarrollar un pipeline Structured Streaming para procesar nuevos
    eventos.
-   Normalizar unidades y atributos necesarios para comparar productos
    equivalentes.
-   Calcular indicadores de precios, variaciones y tendencias.
-   Construir una demostración funcional de consulta y visualización.
-   Evaluar calidad de datos, trazabilidad y rendimiento del
    procesamiento.

------------------------------------------------------------------------

# 2. Alcance y límites

## 2.1. Alcance funcional propuesto

Los módulos se dividen en un núcleo obligatorio y extensiones opcionales.

**Núcleo del MVP (obligatorio):**

1.  **Mercado mayorista:** consulta y análisis de los precios y volúmenes
    diarios del GMML por producto, categoría y fecha.
2.  **Inteligencia de precios:** indicadores, promedios, mínimos,
    máximos, variaciones y tendencias.
3.  **Alertas:** identificación de variaciones relevantes y valores
    atípicos, tanto en Batch como en Streaming.
4.  **Contexto:** índices de supermercados (Qhawarina) e IPC (INEI)
    junto a la evolución mayorista.

**Extensiones (solo si sobra tiempo):**

5.  **Comparador de supermercados:** muestra de 30 a 50 productos
    capturados a mano y emparejados manualmente.
6.  **Canasta simple:** costo total de una lista de productos por
    establecimiento.

**Fuera del MVP:** canasta distribuida entre varias tiendas, panel
administrativo, autenticación de usuarios, Kafka y ejecución de jobs
Spark desde la interfaz web.

## 2.2. Alcance técnico

-   Procesamiento local en una computadora con Windows 11.
-   MongoDB Atlas como almacenamiento remoto.
-   Apache Spark 3.5 (API Java) para procesamiento Batch y Structured Streaming.
-   Java 17 en todo el proyecto: jsoup para SISAP, Apache PDFBox para
    los PDF y Maven como herramienta de construcción.
-   Java Spring Boot con arquitectura hexagonal y Thymeleaf para la
    aplicación web.
-   Demostración individual orientada a investigación tecnológica.

## 2.3. Fuera del alcance inicial

-   Garantizar cobertura de todos los supermercados del Perú.
-   Asegurar disponibilidad de precios en tiempo real.
-   Presentar datos sintéticos como si fueran datos reales.
-   Comparar productos distintos solamente porque tienen nombres
    parecidos.
-   Afirmar que un índice agregado contiene el detalle de cada producto.
-   Implementar predicciones de precios sin disponer de historial
    suficiente y validado.
-   Presentar un precio mayorista como si fuera el precio minorista o de
    góndola.

------------------------------------------------------------------------

# 3. Fuentes de datos identificadas

La disponibilidad de una página web no garantiza que exista una API
pública ni que se permita la extracción automatizada. Cada fuente debe
validarse antes de integrarla.

## 3.0. Resultado de la verificación de fuentes (26-11-2026)

Se comprobó cada enlace y se descargaron e inspeccionaron muestras de
los archivos.

| Fuente | Estado del enlace | Contenido real | Decisión |
|---|---|---|---|
| SISAP (MIDAGRI), exportación "Precio Promedio, Volumen" | Funciona en el navegador, aceptando la advertencia del certificado | Tabla HTML (Latin-1) con extensión `.xls`. Precio por kg (o por litro o unidad) y volumen en toneladas, para todos los días | **Fuente histórica principal** (exportación manual) |
| Boletín diario GMML (MIDAGRI, gob.pe) | Activo; publicación diaria al día (19 boletines en noviembre de 2026) | PDF con capa de texto; tablas extraíbles (Apache PDFBox) | **Datos nuevos (Streaming) y validación** |
| Qhawarina (JSON y CSV) | Activo | Índices agregados. JSON del 10-02-2026 al 13-06-2026 (124 días); CSV hasta el 20-03-2026. Licencia CC BY 4.0 | Contexto histórico |
| INEI (índices y series) | Activo | Portales con tablas descargables; falta inspeccionar los Excel | Contexto macro |
| PNDA Datero Agrario | Activo | **No contiene precios.** Son estadísticas de uso del servicio de consultas por celular. Último recurso: mayo de 2020 | Descartada |
| Supermercados | Sin verificar | Sin API pública autorizada conocida | Muestra manual |

Búsqueda complementaria: en PNDA, la consulta "precios mayoristas" solo
devuelve el Datero Agrario y el dataset de combustibles de OSINERGMIN.
No se encontró en PNDA un dataset CSV de precios mayoristas de
alimentos.

Los archivos descargados en la verificación deben copiarse a `data/raw/`
y registrarse con su fecha de descarga.

## 3.1. Qhawarina -- Billion Prices Project (BPP)

**Enlace principal:** https://qhawarina.pe/datos\
**Documentación API indicada:** https://qhawarina.pe/api/docs\
**JSON indicado:**
https://qhawarina.pe/assets/data/daily_price_index.json\
**CSV indicado:**
https://qhawarina.pe/assets/data/csv/precios_diarios.csv

Qhawarina publica indicadores de precios asociados al Billion Prices
Project para Perú. En la investigación preliminar se identificó
información relacionada con Plaza Vea, Metro y Wong, y una referencia
aproximada a 42 000 SKU monitoreados.

### Distinción esencial

La existencia de aproximadamente 42 000 SKU monitoreados no significa
que el archivo público de índices contenga 42 000 fichas individuales
con todos sus precios. La información pública identificada corresponde a
índices agregados. El acceso al feed granular de productos debe
consultarse directamente con el proveedor.

### Estado verificado (26-11-2026)

-   El JSON (`daily_price_index.json`) y el CSV (`precios_diarios.csv`)
    se descargan correctamente.
-   Licencia: **CC BY 4.0**, que permite reutilizar los datos citando la
    fuente.
-   Metodología declarada: índice bilateral de Jevons ponderado con los
    pesos del IPC del INEI, con base 100 el 10-02-2026.
-   Tiendas: Plaza Vea, Metro y Wong. Se emparejan unos 38 000 productos
    por día.
-   El JSON incluye 12 categorías (arroz y cereales, carnes, lácteos,
    verduras, etc.) y 4 productos de ejemplo con su variación. No
    incluye precios por SKU.
-   **Última fecha del JSON: 13-06-2026. Última fecha del CSV:
    20-03-2026.** La fuente no se actualiza desde hace más de cinco
    meses, así que no puede alimentar indicadores actuales.

### Campos del índice (confirmados en el archivo descargado)

  Campo                           Significado
  ------------------------------- -----------------------------------
  `date`                          Fecha de observación
  `index_all`                     Índice general
  `index_food`                    Índice de alimentos
  `index_nonfood`                 Índice no alimentario
  `var_all`                       Variación general
  `cum_pct`                       Variación acumulada
  `n_matched`                     Cantidad de productos emparejados
  `interpolated`                  Indicador de interpolación

El archivo también incluye `index_all_unweighted`, `var_food`,
`yoy_inflation`, `mom_inflation` y un índice por categoría
(`index_arroz_cereales`, `index_carnes`, `index_verduras`, etc.).

### Uso en SmartPrice

-   Incorporar indicadores agregados como contexto de evolución de
    precios.
-   Comparar tendencias generales por categoría, si la fuente lo
    permite.
-   No usar el índice como sustituto de precios individuales por SKU.
-   Solicitar al proveedor información sobre acceso académico al feed
    granular, licencia, frecuencia, campos y condiciones de uso.

### Validaciones pendientes

-   Consultar al proveedor si el índice se seguirá actualizando.
-   Consultar disponibilidad y costo del feed granular por producto.

## 3.2. MIDAGRI -- Gran Mercado Mayorista de Lima (fuente principal)

MIDAGRI publica los precios del GMML por dos vías, que el proyecto usa
de forma complementaria:

  Vía                       Uso en SmartPrice             Obtención
  ------------------------- ----------------------------- ------------------------
  SISAP (exportación)       Histórico (Batch)             Manual, por bloques
  Boletín diario en PDF     Datos nuevos (Streaming) y    Automatizable
                            validación

### Boletines diarios en PDF

**Portal de reportes:**
https://www.gob.pe/institucion/midagri/informes-publicaciones/1211-precios-de-alimentos\
**Colección GMML (un PDF por día):**
https://www.gob.pe/institucion/midagri/colecciones/335-reporte-de-ingreso-y-precios-en-el-gran-mercado-mayorista-de-lima\
**Ejemplo de archivo:**
https://cdn.www.gob.pe/uploads/document/file/10926750/8779792-reporte-de-ingreso-y-precios-en-el-gran-mercado-mayorista-de-lima-06-11-2026.pdf

El Ministerio de Desarrollo Agrario y Riego publica cada día un boletín
de abastecimiento y precios del Gran Mercado Mayorista de Lima. La
fuente primaria de los datos es EMMSA, y el boletín lo elabora
MIDAGRI-DGESEP-DEIA (Área de Comercio Interno).

#### Estado verificado (26-11-2026)

-   La colección está activa y al día: noviembre de 2026 ya tenía 19
    boletines publicados. En la página hay publicaciones mensuales desde,
    al menos, noviembre de 2024.
-   Cada publicación mensual agrupa los PDF diarios del mes.
-   El PDF del 06-11-2026 tiene 3 páginas: una portada (hortalizas,
    legumbres, tubérculos y raíces) y dos páginas de tabla.
-   El PDF tiene **capa de texto** (no es un escaneo). El extractor
    (Apache PDFBox) obtuvo las tablas directamente: 55 filas de productos en la
    página 2 y 17 en la página 3, es decir, 72 productos ese día.

#### Estructura de la tabla (confirmada)

  Columna del PDF              Campo de destino       Ejemplo
  ---------------------------- ---------------------- ------------------
  Productos                    `product_name`         Aji Rocoto
  Masa de ingreso (t) -- Ayer  `volume_t_prev`        168
  Masa de ingreso (t) -- Hoy   `volume_t`             187
  Promedio últimos 7 días      `volume_t_avg7`        120
  Promedio últimos 4 [día]     `volume_t_avg4_wd`     178
  Unidad de medida             `unit`                 Cajon
  Equivalencia en kg           `kg_equivalent`        18.00
  Precio -- Ayer               `price_prev`           60.00
  Precio -- Hoy                `price`                55.00
  Precio promedio 7 días       `price_avg7`           48.75

Reglas observadas en el archivo:

-   El precio está en **soles por unidad de medida**, no por kg. El
    precio por kg se calcula como `price / kg_equivalent`. Por ejemplo:
    Aji Rocoto, S/ 55.00 el cajón de 18 kg, equivale a S/ 3.06 por kg.
-   El carácter `:` significa "sin dato" y debe convertirse a nulo.
-   Algunos nombres incluyen notas al pie (`Papa Unica 2/`,
    `Papa Yungay 1/`), que deben separarse del nombre.
-   La fecha del boletín aparece en el encabezado de cada página
    ("viernes, 6 de Noviembre de 2026") y en el nombre del archivo
    (`...-06-11-2026.pdf`).
-   Las unidades observadas incluyen Kilogramo, Atado, Cajon, Cajon
    Chico, Docena, Saco Chico y Unidad.

#### Otras colecciones de MIDAGRI disponibles

-   Mercado Mayorista de Frutas N.º 2.
-   Mercado Mayorista de Productores.
-   Boletines por producto: papa, arroz, aves, huevo y maíz amarillo
    duro.

Se pueden incorporar con el mismo extractor si su tabla tiene una
estructura parecida. Hay que verificarlo archivo por archivo.

### SISAP: histórico (fuente principal)

**Consulta:** https://sistemas.midagri.gob.pe/sisap/portal2/mayorista/resumenes/consultar/

El enlace original (`sistemas.minagri.gob.pe`) ya no resuelve. El
sistema está en `sistemas.midagri.gob.pe`. El 26-11-2026 su certificado
TLS estaba vencido: el navegador muestra "No seguro", pero la consulta
funciona. Mientras el certificado siga vencido, **la descarga se hace a
mano desde el navegador** y no se automatiza.

#### Procedimiento de exportación

1.  Mercado: Gran Mercado Mayorista de Lima.
2.  Productos: los 59 productos disponibles en la lista, siempre la misma
    selección en todas las descargas.
3.  Variables: Precio Promedio y Volumen.
4.  Pestaña "Intervalo de Tiempo", con las fechas Desde y Hasta.
5.  Consultar y exportar con el menú Herramientas.
6.  Renombrar el archivo como `sisap_AAAA-MM_AAAA-MM.xls` y guardarlo en
    `data/raw/sisap/`.

**Límite observado:** SISAP rechaza las consultas de más de unas 9 000
combinaciones de producto y día ("Demasiados criterios para realizar la
consulta"). 59 productos × 149 días funcionó; 59 × unos 210 días falló.
Por eso el histórico se descarga en bloques de unos 5 meses.

#### Formato del archivo exportado (confirmado)

-   Aunque la extensión es `.xls`, el archivo es una **tabla HTML
    codificada en Latin-1**.
-   El título incluye el periodo, por ejemplo `01/07/2026 - 30/11/2026`.
-   Estructura en formato ancho: `Fecha | Producto 1 (Precio Promedio,
    Volumen) | Producto 2 (...) | ...`.
-   El precio está en **S/ por kg** (o por litro o unidad, según el
    producto), y el volumen en toneladas.
-   Los vacíos se marcan con `...`.
-   Incluye todos los días del intervalo, también fines de semana.
-   Fuente indicada en el archivo: Gran Mercado Mayorista de Lima.
    Elaboración: MIDAGRI-DGESEP-DEIA, Área de Comercio.

#### Histórico descargado (26-11-2026)

  Archivo                       Periodo                    Días   Filas
  ----------------------------- ------------------------ ------ -------
  `sisap_2024-01_2024-05.xls`   01-01-2024 a 31-05-2024     152   8 968
  `sisap_2024-06_2024-10.xls`   01-06-2024 a 31-10-2024     153   9 027
  `sisap_2024-11_2025-03.xls`   01-11-2024 a 31-03-2025     151   8 909
  `sisap_2025-04_2025-08.xls`   01-04-2025 a 31-08-2025     153   9 027
  `sisap_2025-09_2026-01.xls`   01-09-2025 a 31-01-2026     153   9 027
  `sisap_2026-02_2026-06.xls`   01-02-2026 a 30-06-2026     150   8 850
  `sisap_2026-07_2026-11.xls`   01-07-2026 a 26-11-2026     149   8 791
  **Total**                     01-01-2024 a 26-11-2026   1 061  62 599

-   Los periodos son continuos: no hay huecos ni solapamientos.
-   Los 7 archivos tienen los mismos 59 productos.
-   Hay 71 precios vacíos (0,1 %), casi todos en 2024.

#### Validación cruzada SISAP vs PDF

Para el 06-11-2026, los **54 productos que aparecen en las dos fuentes
coinciden** en precio por kg y en volumen. Ejemplo: Aji Rocoto, S/ 3.06
por kg en SISAP, y S/ 55.00 por cajón de 18 kg en el PDF (55.00 / 18 =
3.06). 5 productos solo están en SISAP y 18 solo en el PDF.

### Uso en SmartPrice

-   SISAP: serie histórica diaria del 01-01-2024 al 26-11-2026, para el
    procesamiento Batch.
-   Boletines PDF: incorporación diaria de datos nuevos con Streaming y
    control de calidad contra SISAP.
-   Relacionar el volumen de ingreso con el precio.
-   Detectar variaciones y valores atípicos.
-   Mantener los datos mayoristas separados de los precios minoristas de
    supermercados.

## 3.3. PNDA -- MIDAGRI, Datero Agrario (descartada)

**Ficha:**\
https://www.datosabiertos.gob.pe/dataset/midagri-02-datero-agrario-ministerio-de-desarrollo-agrario-y-riego

En la investigación inicial esta ficha se consideró como posible fuente
de precios históricos. La verificación del 26-11-2026 mostró que **sus
recursos no contienen precios**. Los CSV registran el uso del servicio
de consultas por celular:

``` text
AÑO,MES,CONSULTAS,USUARIO,MODULOS,OPERADOR
2020,Feb,9804,794,PRECIOS,Movistar Peru
```

-   Cada archivo pesa entre 96 y 651 bytes, con 3 a 20 filas.
-   Los recursos van de 2015 a mayo de 2020 y no tienen actualizaciones
    posteriores.
-   Algunos archivos están en ISO-8859-1 y otros en ASCII.

**Decisión:** la fuente se descarta para el análisis de precios.

## 3.4. INEI -- Índice de Precios al Consumidor

**Índices de precios:**\
https://www.inei.gob.pe/estadisticas/indice-tematico/price-indexes/

**Sistema de Series Nacionales:**\
https://www.gob.pe/institucion/inei/pages/32329-consultar-el-sistema-de-series-nacionales

El INEI publica indicadores estadísticos de precios e inflación. Estos
datos sirven para contextualizar el comportamiento general de los
precios.

### Formatos a comprobar

-   Excel.
-   Tablas descargables.
-   Otros formatos disponibles en los portales oficiales.

### Campos de interés

-   Periodo.
-   Indicador.
-   Ámbito geográfico.
-   Categoría de consumo.
-   Índice.
-   Variación porcentual.

### Uso en SmartPrice

El IPC se utilizará como indicador complementario. No representa el
precio individual de un producto en un establecimiento y no debe
compararse directamente con el precio de una presentación específica.

## 3.5. Catálogos de supermercados

Sitios identificados para una evaluación posterior:

-   Plaza Vea: https://www.plazavea.com.pe/
-   Tottus: https://www.tottus.com.pe/
-   Metro: https://www.metro.pe/
-   Wong: https://www.wong.pe/
-   Makro: https://www.makro.plazavea.com.pe/

### Campos objetivo

  Campo                 Descripción
  --------------------- ----------------------------------
  `product_name`        Nombre comercial
  `brand`               Marca
  `sku`                 Código de producto, si existe
  `category`            Categoría
  `presentation`        Presentación
  `quantity`            Cantidad
  `unit`                Unidad de medida
  `regular_price`       Precio regular
  `promotional_price`   Precio promocional
  `store`               Establecimiento
  `captured_at`         Fecha y hora de captura
  `product_url`         URL del producto, si corresponde

Estos campos representan el esquema deseado. Cada sitio puede publicar
información distinta.

### Método de adquisición

Prioridad recomendada:

1.  API o feed oficial, si existe y se autoriza.
2.  Catálogo o archivo descargable autorizado.
3.  Recopilación automatizada de páginas públicas, únicamente si las
    condiciones de uso y restricciones técnicas lo permiten.
4.  Captura manual controlada para una demostración pequeña,
    documentando fecha y procedimiento.

No se debe eludir autenticación, controles de acceso ni restricciones
técnicas.

## 3.6. Aclaración sobre otros datasets de PNDA

Durante la exploración se identificó también un dataset de OSINERGMIN
sobre precios de distribuidores mayoristas y establecimientos de venta
al público. Su ámbito es el de combustibles, no el de alimentos. No será
una fuente central del comparador de alimentos. Podría estudiarse como
una extensión separada sobre costos energéticos, pero no debe mezclarse
con precios alimentarios sin una justificación analítica.

------------------------------------------------------------------------

# 4. Estrategia de adquisición y validación de datos

## 4.1. Principio de trazabilidad

Cada registro debe conservar información sobre su procedencia. Como
mínimo:

-   Nombre de la fuente.
-   URL o identificador del recurso.
-   Nombre del archivo original.
-   Fecha de descarga o captura.
-   Fecha de observación del precio.
-   Versión o periodo del recurso, si existe.
-   Método de adquisición.
-   Estado de validación.

Los datos originales deben conservarse sin sobrescribirlos. Las
transformaciones deben generar conjuntos derivados.

## 4.2. Matriz de validación de fuentes

  Criterio        Pregunta
  --------------- ------------------------------------------
  Accesibilidad   ¿Se puede descargar el recurso?
  Licencia        ¿Se permite reutilizarlo?
  Formato         ¿CSV, JSON, XLSX, PDF o API?
  Granularidad    ¿Índice agregado o precio por producto?
  Periodo         ¿Qué fechas cubre?
  Frecuencia      ¿Diaria, semanal, mensual o irregular?
  Geografía       ¿Qué mercados, ciudades o tiendas cubre?
  Unidad          ¿Se identifica kg, litro, unidad u otra?
  Calidad         ¿Hay nulos, duplicados o errores?
  Actualización   ¿Se publica periódicamente?

## 4.3. Procedimiento de validación

Para cada archivo:

1.  Descargar el archivo original.
2.  Guardar su URL y fecha de descarga.
3.  Registrar el nombre y tamaño del archivo.
4.  Identificar codificación y delimitador.
5.  Inspeccionar encabezados y tipos de datos.
6.  Contar registros y columnas.
7.  Calcular valores nulos por columna.
8.  Identificar duplicados.
9.  Revisar rangos y valores inválidos.
10. Comprobar unidades y moneda.
11. Documentar el periodo cubierto.
12. Decidir si el recurso es apto para comparación, analítica contextual
    o solamente referencia.

## 4.4. Volumen de datos

  Indicador                                  Valor
  -------------------------------- ----------------------------
  Histórico SISAP (verificado)       62 599 filas
  Productos SISAP                    59
  Periodo SISAP                      01-01-2024 a 26-11-2026
                                     (1 061 días)
  Productos por boletín PDF          72 (verificado)
  Datos nuevos por boletín           unos 72 eventos por día hábil
  Muestra de supermercados           30--50 productos (meta)
  Serie Qhawarina                    124 días

Dimensionamiento del histórico:

`59 productos × 1 061 días = 62 599 observaciones`

Si se incorporan otros mercados de SISAP o las colecciones del Mercado
de Frutas N.º 2 y del Mercado de Productores, el volumen aumenta en
proporción.

**Justificación del uso de Spark:** el volumen real cabe en memoria con
pandas. Spark se justifica por tres razones: (1) Structured Streaming
para incorporar cada boletín nuevo, (2) un pipeline uniforme para el
histórico y los datos nuevos, y (3) la demostración de escalabilidad con
datos sintéticos etiquetados. El informe debe reconocerlo así.

Los datos sintéticos pueden utilizarse para probar carga y
escalabilidad, pero deben etiquetarse explícitamente y mantenerse
separados de los datos reales.

------------------------------------------------------------------------

# 5. Arquitectura técnica propuesta

## 5.1. Flujo general

1.  **Fuentes:** SISAP (histórico GMML), boletines PDF del GMML (datos
    nuevos), Qhawarina, INEI y una muestra manual de supermercados.
2.  **Ingestión:** `SisapReader` (jsoup) convierte las exportaciones de
    SISAP a formato largo y `GmmlPdfExtractor` (PDFBox) extrae los PDF.
    Todo está en el módulo Java `smartprice-jobs`.
3.  **Zona Raw:** conservación de datos originales y metadatos.
4.  **MongoDB Atlas:** almacenamiento de registros y resultados.
5.  **Apache Spark Batch:** limpieza, normalización, agregaciones y
    análisis histórico.
6.  **Apache Spark Structured Streaming:** procesamiento de nuevos
    eventos de precios.
7.  **Backend Java Spring Boot:** exposición de consultas y resultados.
8.  **Frontend Thymeleaf:** vistas HTML renderizadas por Spring Boot, con CSS y JavaScript para interacción y visualizaciones.

## 5.2. Diagrama conceptual

``` text
SISAP (histórico) / PDF GMML (diario) / QHAWARINA / INEI / SUPERMERCADOS
                       |
                       v
             ADQUISICIÓN DE DATOS
             Exportación manual SISAP / descarga PDF / JSON / CSV
                       |
                       v
                ZONA RAW
             Archivos originales (.xls SISAP, PDF, JSON...)
                       |
                       v
             LECTURA Y EXTRACCIÓN
             SisapReader / GmmlPdfExtractor -> CSV
                       |
                       v
                MONGODB ATLAS
                       |
             +---------+---------+
             |                   |
             v                   v
       SPARK BATCH       SPARK STREAMING
       Históricos        Nuevos eventos
             |                   |
             +---------+---------+
                       |
                       v
             DATOS NORMALIZADOS
             E INDICADORES
                       |
                       v
          JAVA SPRING BOOT + MVC
          Controladores y casos de uso
                       |
                       v
        THYMELEAF + HTML / CSS / JS
```

## 5.3. Tecnologías

  -----------------------------------------------------------------------
  Componente              Tecnología propuesta    Función
  ----------------------- ----------------------- -----------------------
  Lenguaje                Java 17                 Jobs Spark y
                                                  aplicación web

  Extracción de PDF       Apache PDFBox 3         Tablas de los
                                                  boletines GMML

  Lectura SISAP           jsoup                   Tablas HTML (.xls)

  Build                   Maven (Wrapper)         Compilación y
                                                  pruebas

  Motor                   Apache Spark 3.5.6      Procesamiento
                                                  distribuido

  Procesamiento           Spark API Java          Jobs Batch y
                                                  Streaming

  Base de datos           MongoDB Atlas           Persistencia NoSQL

  Conector                MongoDB Spark Connector Integración
                          10.7.0                  Spark--MongoDB

  Backend y MVC           Java Spring Boot         Casos de uso, API REST
                                                  y renderizado de vistas

  Motor de plantillas     Thymeleaf                Vistas HTML del servidor

  Interfaz                HTML, CSS y JavaScript   Presentación e interacción

  Visualización           Apache ECharts          Gráficos

  Control de versiones    Git                     Historial de cambios

  Entorno opcional        Docker Desktop          Entorno reproducible
  -----------------------------------------------------------------------

Las versiones indicadas son una línea base de propuesta. Deben probarse
juntas en el equipo antes de congelar el entorno.

------------------------------------------------------------------------

# 6. Diseño de MongoDB Atlas

## 6.1. Base de datos

Nombre propuesto: `smartprice_db`

## 6.2. Colecciones (implementadas)

  Colección              Contenido                                             Escribe
  ---------------------- ----------------------------------------------------- ---------------------------
  `wholesale_prices`     Observaciones diarias (precio por kg y volumen)       `load-sisap`, `stream-prices`
  `source_files`         Archivos de origen con hash SHA-256 y periodo         `load-sisap`
  `price_analytics`      `PRICE_MONTHLY`, `PRODUCT_SUMMARY`, `ALERT_BASELINE`  `batch-analytics`
  `price_alerts`         Variaciones diarias atípicas (IQR)                    `batch-analytics`, `stream-prices`
  `processing_logs`      Ejecuciones del Streaming y validación cruzada        `stream-prices`

El esquema completo de cada colección está en `documentation/diccionario_datos.md`.
Las colecciones del diseño inicial para supermercados (`products`, `stores`,
`raw_supermarket_prices`, etc.) no se crearon, porque esa fuente quedó fuera del
alcance (sección 3.0).

## 6.3. Ejemplo de observación de supermercado

``` json
{
  "source": "SUPERMARKET",
  "store": "Plaza Vea",
  "product": {
    "name": "Arroz Superior",
    "brand": "Costeño",
    "sku": "ARZ-001",
    "category": "Abarrotes"
  },
  "presentation": {
    "quantity": 5,
    "unit": "kg"
  },
  "price": {
    "regular": 24.90,
    "promotional": 21.90,
    "currency": "PEN"
  },
  "captured_at": "2026-11-26T10:00:00Z",
  "source_metadata": {
    "source_file": "catalog_sample.json",
    "method": "authorized_catalog"
  }
}
```

Este documento es un ejemplo de diseño, no un registro real obtenido de
un supermercado.

## 6.4. Documento de `wholesale_prices` (esquema real)

Documento real de `smartprice_db.wholesale_prices`, consultado el
26-11-2026:

``` json
{
  "_id": "2026-11-06|GMML|Aji Rocoto",
  "observed_at": { "$date": "2026-11-06T00:00:00Z" },
  "market_code": "GMML",
  "market_name": "Gran Mercado Mayorista de Lima",
  "product_name": "Aji Rocoto",
  "price": { "avg": 3.06, "currency": "PEN", "unit_basis": "kg|l|unit" },
  "volume_t": 187.0,
  "source": {
    "system": "SISAP",
    "file": "sisap_2026-07_2026-11.xls",
    "reader_version": "sisap_v1"
  },
  "ingested_at": { "$date": "..." }
}
```

-   `price.avg` está en soles por kg (o por litro o unidad, según el
    producto), tal como lo publica SISAP.
-   El boletín PDF del mismo día publica S/ 55.00 por cajón de 18 kg:
    55.00 / 18 = 3.06, el mismo valor.
-   Los datos nuevos que lleguen por los PDF usarán el mismo esquema, con
    `source.system = "GMML_PDF"`.

## 6.5. Normalización de unidades

No se deben comparar directamente presentaciones distintas. El precio
unitario se calcula así:

`Precio unitario = Precio de la presentación / Cantidad de la presentación`

Ejemplo:

-   Bolsa de 5 kg a S/ 24.90.
-   Precio unitario: S/ 4.98 por kg.

En el GMML, la cantidad de la presentación es la columna "Equiv. en kg"
del boletín. Por ejemplo: Tomate, S/ 85.00 el cajón chico de 27 kg,
equivale a S/ 3.15 por kg.

La comparación también debe considerar variedad, marca, calidad,
presentación y otras características relevantes. No basta con que dos
productos compartan una categoría.

## 6.6. Índices sugeridos

Los índices deben definirse después de conocer las consultas reales.
Candidatos:

-   `product.sku`
-   `product.category`
-   `store`
-   `market`
-   `captured_at`
-   Índice compuesto de producto, establecimiento y fecha.
-   Índice compuesto de producto, mercado y fecha.

------------------------------------------------------------------------

# 7. Entorno de desarrollo local en Windows 11

Todo el proyecto está en **Java 17**: los jobs de Apache Spark (API Java)
y la aplicación Spring Boot. No se usa Python.

## 7.1. Configuración base (instalada y verificada el 26-11-2026)

  Componente                      Versión / ubicación
  ------------------------------- ----------------------------------
  Sistema operativo               Windows 11
  JDK para el proyecto            Temurin 17.0.21 en `C:\tools\jdk-17`
  Build                           Maven 3.9.16 mediante Maven Wrapper
                                  (`mvnw.cmd`, sin instalación)
  Apache Spark                    3.5.6 (`spark-sql_2.12`, API Java)
  MongoDB Spark Connector         10.7.0 (`_2.12`)
  Driver MongoDB                  `mongodb-driver-sync` 5.1.4
  Hadoop para Windows             `winutils.exe` y `hadoop.dll`
                                  (Hadoop 3.3.5) en `C:\tools\hadoop\bin`
  Lectura SISAP                   jsoup 1.22.2
  Lectura PDF                     Apache PDFBox 3.0.8
  Configuración                   dotenv-java 3.2.0 (archivo `.env`)
  Pruebas                         JUnit 5.14.4

> **Prueba de compatibilidad de Java (26-11-2026):** el equipo tiene
> Java 26 como predeterminado. Con Java 26, Spark 3.5 no arranca
> (`UnsupportedOperationException: getSubject is not supported`). Spark
> 3.5 admite Java 8, 11 y 17. **Decisión:** el proyecto usa el JDK 17
> de `C:\tools\jdk-17` sin cambiar el Java predeterminado. El script
> `jobs.ps1` fija `JAVA_HOME` solo para el proceso que ejecuta los jobs.

## 7.1.1. Requisito de Hadoop en Windows (`winutils.exe`)

En Windows, Spark necesita los binarios nativos de Hadoop para escribir
archivos y para los *checkpoints* de Structured Streaming. Sin ellos,
los errores típicos son `Could not locate executable null\bin\winutils.exe`
o `UnsatisfiedLinkError ... NativeIO$Windows.access0`.

Instalación realizada:

1.  `winutils.exe` y `hadoop.dll` para Hadoop 3.3.5, descargados del
    repositorio comunitario `cdarlint/winutils`. SHA-256 registrados:
    -   `winutils.exe`: `a0ca6e358357c41ef56ebdb02c38e4a4d55da7ca7a13001678bb2ef7d644adea`
    -   `hadoop.dll`: `d3dd64afdc85f2a7eb5345abf2ecaa744b0a157de40859313337d47f81ee1c7b`
2.  Ubicación: `C:\tools\hadoop\bin\`.
3.  `HADOOP_HOME` y `PATH` se fijan solo para el proceso en `jobs.ps1`;
    no se modifican las variables globales del sistema.

## 7.1.2. Opciones de la JVM para Spark en Java 17

Spark 3.5 accede a clases internas del JDK. Al ejecutarlo con
`java -jar`, sin `spark-submit`, hay que abrir esos módulos con opciones
`--add-opens` (`java.base/java.lang`, `java.nio`, `sun.nio.ch`,
`sun.util.calendar`, etc.). El script `jobs.ps1` las incluye.

## 7.2. Estructura del proyecto Maven

``` text
SmartPrice/
├── pom.xml                  (padre: versiones y módulos)
├── mvnw, mvnw.cmd, .mvn/    (Maven Wrapper)
├── jobs.ps1                 (ejecuta los jobs con JDK 17, Hadoop y --add-opens)
├── .env                     (MONGODB_URI; no se versiona)
├── smartprice-jobs/         (Apache Spark, API Java)
└── smartprice-app/          (Spring Boot; se añadirá como segundo módulo)
```

## 7.3. Compilar y ejecutar

``` powershell
# Compilar y ejecutar las pruebas
.\jobs.ps1 build

# Ver los comandos disponibles
.\jobs.ps1
```

Comandos del módulo `smartprice-jobs`:

  Comando                         Función
  ------------------------------- ------------------------------------------
  `sisap-to-csv`                  Une las exportaciones SISAP en un CSV largo
  `load-sisap`                    Carga el CSV en MongoDB Atlas (Spark)
  `explore-sisap`                 Exploración inicial con Spark
  `extract-pdf <archivo.pdf>`     Extrae la tabla de un boletín GMML
  `check-mongo`                   Prueba la conexión a MongoDB Atlas
  `setup-mongo`                   Crea índices y muestra un resumen

Si PowerShell bloquea el script:

``` powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

## 7.4. Variables de entorno

Archivo `.env` en la raíz (no se versiona):

``` env
MONGODB_URI=mongodb+srv://smartprice_app:CONTRASENA@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0
MONGODB_DATABASE=smartprice_db
```

La URI no debe publicarse en GitHub ni incluirse en capturas o
documentación pública. Los jobs la muestran enmascarada
(`smartprice_app:****@...`).

## 7.5. MongoDB Atlas (configurado el 26-11-2026)

-   Cluster: `Cluster0` (M0 gratuito, AWS São Paulo `sa-east-1`,
    MongoDB 8.0), compartido con otro proyecto del mismo propietario.
-   Base de datos: `smartprice_db`.
-   Usuario: `smartprice_app`, con el privilegio específico
    **`readWrite@smartprice_db`** y sin roles globales. Así un error en
    SmartPrice no puede afectar a otras bases del cluster.
-   Acceso de red: solo la IP de desarrollo en *IP Access List*.

`.gitignore` del proyecto:

``` gitignore
.env
target/
.mvn/wrapper/maven-wrapper.jar
spark-warehouse/
data/streaming/checkpoint/
data/interim/
data/processed/
.idea/
.vscode/
```

------------------------------------------------------------------------

# 8. Integración Apache Spark--MongoDB

## 8.1. Conector

``` xml
<dependency>
    <groupId>org.mongodb.spark</groupId>
    <artifactId>mongo-spark-connector_2.12</artifactId>
    <version>10.7.0</version>
</dependency>
```

Documentación: https://www.mongodb.com/docs/spark-connector/v10.x/

El conector declara `mongodb-driver-sync` solo en ámbito *runtime*. Por
eso el módulo añade `mongodb-driver-sync` 5.1.4 de forma explícita para
los comandos de administración.

## 8.2. Escritura idempotente

Clase `MongoWriter` (`smartprice-jobs`):

``` java
df.write().format("mongodb").mode("append")
        .option("connection.uri", uri)
        .option("database", database)
        .option("collection", collection)
        .option("operationType", "replace")
        .option("idFieldList", "_id")
        .option("upsertDocument", "true")
        .save();
```

-   Cada documento tiene un `_id` determinista (por ejemplo
    `2026-11-06|GMML|Aji Rocoto`), así que **repetir la carga reemplaza
    los documentos en vez de duplicarlos**. Verificado: tres cargas
    seguidas dejan exactamente 62 599 documentos.
-   La URI se pasa como opción de escritura y no como configuración de
    la sesión. (Con PySpark, la configuración de sesión se pasaba a
    `spark-submit.cmd`, y los `&` de la URI rompían el comando en
    Windows.)
-   La sesión usa `spark.sql.session.timeZone=UTC`, para que las fechas
    sin hora se guarden como `00:00 UTC` y no se desplacen 5 horas por
    la zona horaria de Lima.

------------------------------------------------------------------------

# 9. Primer pipeline: histórico SISAP

## 9.1. Exportación

Procedimiento descrito en la sección 3.2. Los archivos se guardan en
`data/raw/sisap/sisap_AAAA-MM_AAAA-MM.xls`.

## 9.2. Lectura (`SisapReader`)

Clase `pe.smartprice.jobs.sisap.SisapReader`:

1.  Lee los bytes del archivo y calcula su SHA-256.
2.  Decodifica en **ISO-8859-1** y parsea la tabla HTML con jsoup.
3.  Comprueba que el título sea una exportación "Precio Promedio,
    Volumen" y toma el periodo del segundo `<h1>`.
4.  Primera fila: nombres de producto. Filas de datos: fecha + pares
    (precio, volumen).
5.  Convierte a formato largo: una fila por fecha y producto. `...` se
    convierte en nulo.

Detalle de implementación: las celdas se leen con `td.text()` una por
una. `Elements.eachText()` omite las celdas vacías y desalinearía las
columnas.

``` powershell
.\jobs.ps1 sisap-to-csv      # data/raw/sisap -> data/interim/sisap_prices.csv
```

Resultado (26-11-2026): 7 archivos, **62 599 filas**, 59 productos, del
01-01-2024 al 26-11-2026, 0 duplicados y 71 precios vacíos.

## 9.3. Extracción de los boletines PDF (`GmmlPdfExtractor`)

Clase `pe.smartprice.jobs.gmml.GmmlPdfExtractor` (Apache PDFBox):

1.  Extrae el texto de cada página, ordenado por posición.
2.  Lee la fecha del encabezado ("viernes, 6 de Noviembre de 2026").
3.  Interpreta cada línea **de derecha a izquierda**: 4 números finales
    (equivalencia en kg, precio de ayer, de hoy y promedio de 7 días),
    la unidad (una o más palabras), 4 volúmenes (número o `:`) y el
    resto como nombre del producto.
4.  Separa las notas al pie (`Papa Unica 2/` → `Papa Unica`, nota `2`).
5.  Calcula `price_per_kg = price / kg_equivalent`.

``` powershell
.\jobs.ps1 extract-pdf data\raw\midagri\gmml_pdf\<archivo>.pdf
```

Resultado sobre el boletín del 06-11-2026: **72 productos**.

## 9.4. Pruebas unitarias

  Prueba                    Verifica
  ------------------------- ---------------------------------------------
  `SisapReaderTest`         156 filas del archivo de prueba, decodificación
                            Latin-1 ("Aji Montaña"), `...` → nulo, y
                            precio del Aji Rocoto igual al del PDF (3.06)
  `GmmlPdfExtractorTest`    Unidades de una y dos palabras, volúmenes `:`,
                            notas al pie, descarte de encabezados y
                            lectura de la fecha

## 9.5. Verificación de la migración desde Python

Los primeros prototipos se escribieron en Python y PySpark. Se migraron
a Java porque el requisito es Apache Spark, no PySpark, y porque así el
proyecto queda en un solo lenguaje. Las salidas de Java se compararon
con las de Python:

  Componente               Resultado de la comparación
  ------------------------ ----------------------------------------------
  Lector SISAP             62 599 filas idénticas (valores, hashes,
                           periodos y nombres con tildes)
  Extractor PDF            72 productos idénticos en los 13 campos
  Exploración con Spark    Mismas variaciones anuales por producto
  Carga en MongoDB         Mismos 62 599 documentos y mismo esquema

## 9.6. Validaciones recomendadas para nuevos datos

-   Comparar el número de filas por boletín. Una caída brusca indica un
    cambio de formato.
-   Comprobar que la fecha del encabezado coincide con la del nombre del
    archivo.
-   Detectar productos que cambian de unidad o de equivalencia en kg.
-   Detectar precios cero, negativos o fuera de rango.
-   Comparar con SISAP los días que estén en las dos fuentes.

------------------------------------------------------------------------

# 10. Carga en MongoDB Atlas con Apache Spark

Clase `pe.smartprice.jobs.mongo.LoadSisapJob`:

``` powershell
.\jobs.ps1 load-sisap     # data/interim/sisap_prices.csv -> smartprice_db
.\jobs.ps1 setup-mongo    # índices y resumen
```

Colecciones escritas:

-   **`wholesale_prices`**: una observación por fecha, mercado y
    producto (esquema en la sección 6.4).
-   **`source_files`**: un documento por archivo original, con hash
    SHA-256, periodo, primera y última fecha, filas, versión del lector
    y método de adquisición (`manual_export`). El hash se guarda aquí
    una sola vez y no se repite en cada observación.

Índices de `wholesale_prices`: `product_date` (producto + fecha
descendente), `date` y `market_date`.

Estado verificado (26-11-2026): 62 599 documentos, 59 productos, del
01-01-2024 al 26-11-2026, 71 precios nulos; unos 23 MB de datos (9 MB
comprimidos) y unos 20 MB de índices.

## 10.1. Idempotencia y duplicados (implementado)

1.  **Clave natural:** `(observed_at, market_code, product_name)`,
    convertida en un `_id` determinista.
2.  **Escritura:** `operationType=replace` con `upsertDocument=true`. Una
    recarga reemplaza los documentos existentes.
3.  **Archivos:** `source_files` registra el SHA-256 de cada archivo, lo
    que permite detectar si un archivo ya se procesó o cambió.
4.  **Lectura:** el lector SISAP descarta los pares (fecha, producto)
    repetidos entre archivos solapados.

------------------------------------------------------------------------

# 11. Procesamiento Batch (implementado)

Clase `pe.smartprice.jobs.analytics.BatchAnalyticsJob`:

``` powershell
.\jobs.ps1 batch-analytics            # k = 3, variación mínima = 15 %
.\jobs.ps1 batch-analytics 3 20       # parámetros alternativos
```

El job **lee `wholesale_prices` desde MongoDB Atlas** con el conector de
Spark, calcula los indicadores y escribe los resultados en
`price_analytics` y `price_alerts`. Cada ejecución borra antes sus
resultados anteriores (campo `job = batch_analytics`) y los vuelve a
escribir: verificado con dos ejecuciones seguidas, que dan los mismos
conteos.

## 11.1. Resultados que genera

  Colección / tipo                     Contenido                                     Documentos
  ------------------------------------ --------------------------------------------- ----------
  `price_analytics` `PRICE_MONTHLY`    Precio promedio, mínimo y máximo por producto      2 065
                                       y mes; volumen promedio y total; variación
                                       frente al mes anterior y al mismo mes del año
                                       anterior; días con dato
  `price_analytics` `PRODUCT_SUMMARY`  Último precio, promedios de 30 y 365 días,            59
                                       variación de 30 días y anual, mínimo y máximo
                                       históricos, correlación precio-volumen
  `price_alerts`                       Variaciones diarias atípicas (IQR), con               1 666
                                       severidad y volumen de contexto

-   Un mes con menos de 15 días de datos se marca como
    `data_quality_status = PARTIAL_MONTH`; el resto, como `VALIDATED`.
-   Las variaciones frente al mes y al año anteriores se calculan con un
    auto-join por mes (`add_months`), que funciona aunque falte algún mes.
-   La correlación precio-volumen usa `corr()` de Spark sobre los datos
    diarios de cada producto.

## 11.2. Variación porcentual

`Variación (%) = ((Precio actual - Precio anterior) / Precio anterior) × 100`

Si la referencia es nula o cero, la variación queda nula (no se divide
por cero).

## 11.3. Detección de anomalías (IQR)

Para cada producto:

1.  Variación diaria: cambio porcentual frente al día anterior con dato
    (`lag` sobre una ventana ordenada por fecha).
2.  Q1 y Q3 de todas sus variaciones diarias (`percentile_approx`).
    IQR = Q3 - Q1.
3.  Alerta si la variación está fuera de **[Q1 - k·IQR, Q3 + k·IQR]** y
    además su valor absoluto es al menos la **variación mínima**.
4.  Severidad según cuántos IQR se aleja del límite: `HIGH` (3 o más),
    `MEDIUM` (de 1 a 3) y `LOW` (menos de 1).
5.  Contexto: volumen del día y volumen promedio de los 30 días
    anteriores.

**Calibración de los parámetros** (evaluada sobre el histórico de 59
productos y 35 meses):

  k     Variación mínima   Alertas   Alertas por mes
  ----- ------------------ --------- ---------------
  3     10 %                 2 361   ~67
  **3** **15 %**           **1 666** **~47**
  3     20 %                 1 186   ~34
  5     15 %                   760   ~22

Se eligió **k = 3 y 15 %**. La variación mínima hace falta porque 7 de
los 59 productos tienen IQR = 0: su precio no cambia la mayoría de los
días, así que sin ese umbral cualquier cambio mínimo sería una alerta.

Ejemplo real (`price_alerts`):

``` json
{
  "_id": "IQR_DAILY_CHANGE|GMML|Zanahoria|2026-11-23",
  "alert_type": "PRICE_SPIKE",
  "price": { "current": 4.23, "previous": 2.73, "currency": "PEN" },
  "change_pct": 54.95,
  "bounds": { "q1": -2.92, "q3": 3.333, "lower": -21.679, "upper": 22.092, "k": 3.0, "min_abs_change_pct": 15.0 },
  "volume": { "current_t": 45.0, "avg_30d_t": 275.47 },
  "severity": "HIGH"
}
```

Una anomalía estadística no implica un error. En el ejemplo, la subida
del 55 % coincide con un ingreso de 45 t frente a un promedio de 275 t:
es un caso típico de relación volumen-precio.

## 11.4. Primeros hallazgos (26-11-2026)

-   **Zanahoria:** de S/ 0.75 por kg (julio de 2026) a S/ 3.05 por kg
    (noviembre de 2026), +68 % frente a octubre y +114 % frente a
    noviembre de 2025, mientras el volumen promedio diario cae de 359 t
    a 260 t. Correlación precio-volumen: -0.32.
-   **Mayores subidas de 30 días:** haba verde serrana (+106 %), lechuga
    americana (+80 %), beterraga (+66 %) y zanahoria (+64 %).
-   **Mayores bajadas de 2024 a 2026 (promedio anual):** ajo criollo
    (-54 %), ajo morado (-47 %) y kion (-38 %).

## 11.5. Hallazgo de calidad de datos: Papa Blanca y Papa Yungay

En SISAP, "Papa Blanca" tiene **el mismo precio que "Papa Yungay" en
1 059 de 1 061 días**, pero un volumen distinto. Probablemente "Papa
Blanca" es una categoría agregada cuyo precio de referencia es el de la
Papa Yungay; en el boletín PDF, "Papa Yungay" lleva la nota al pie `1/`.
Hay que confirmarlo con la nota del boletín. Mientras tanto, la
aplicación no debe presentar estas dos series como evidencias
independientes.

## 11.6. Canasta inteligente (extensión opcional)

Primera versión:

-   El usuario selecciona productos.
-   El sistema consulta precios comparables.
-   Calcula el total por establecimiento.
-   Muestra diferencias de costo.

Versión posterior:

-   Permitir que la canasta se distribuya entre establecimientos.
-   Considerar costos de traslado, disponibilidad y compra mínima.
-   Comparar ahorro potencial con el costo de visitar más de una tienda.

------------------------------------------------------------------------

# 12. Procesamiento Streaming (implementado)

## 12.1. Objetivo

MIDAGRI publica un boletín del GMML cada día hábil. El Streaming procesa
cada boletín **en cuanto llega**, sin volver a ejecutar el Batch sobre
todo el histórico: solo trata las filas nuevas y genera sus alertas en
segundos.

## 12.2. Flujo implementado

``` text
Boletín PDF ──► .\jobs.ps1 ingest-pdf <pdf>     (productor)
                 extrae la tabla (PDFBox) -> data/streaming/staging/
                 movimiento atómico      -> data/streaming/incoming/
                                                   │  Spark vigila la carpeta
                                                   ▼
               .\jobs.ps1 stream-prices          (Spark Structured Streaming)
                 fuente de archivos CSV, un boletín por micro-lote,
                 checkpoint en data/streaming/checkpoint/, foreachBatch:
                 1. valida filas y toma precio/kg (redondeado como SISAP)
                 2. filtra al catálogo (productos con rango normal)
                 3. separa nuevas / ya existentes  -> no duplica (SISAP manda)
                    y compara precios con lo guardado (validación cruzada)
                 4. guarda las nuevas en wholesale_prices (source.system = GMML_PDF)
                 5. alertas con las MISMAS reglas IQR del Batch (clase IqrAlertRules),
                    usando el rango normal guardado (ALERT_BASELINE) y 45 días de
                    historial leídos de MongoDB
                 6. registra la ejecución en processing_logs
                                                   ▼
               La web muestra los precios, las alertas y la ejecución (página Fuentes)
```

-   `stream-prices` se queda escuchando (disparo cada 15 s).
-   `stream-prices --once` procesa lo pendiente y termina
    (`Trigger.AvailableNow`), útil para pruebas y demostraciones.
-   El checkpoint hace que cada archivo se procese **una sola vez**,
    aunque el job se reinicie.
-   El Batch guarda en `price_analytics` un documento `ALERT_BASELINE`
    por producto (Q1, Q3, k y variación mínima), que el Streaming
    reutiliza. Así una alerta diaria es idéntica la calcule quien la
    calcule.
-   Los productos del boletín que no están en el catálogo del proyecto
    (18 en el boletín del 06-11-2026, por ejemplo Acelga) no se guardan:
    se registran en `products_outside_catalog`.

## 12.3. Pruebas con datos reales (26-11-2026)

  Prueba                         Resultado
  ------------------------------ ---------------------------------------------
  A. Boletín del 06-11 con los   72 leídas, 54 del catálogo, **0 nuevas,
  datos de SISAP presentes       54 ya existían y las 54 coinciden** en precio
                                 (validación cruzada automática)
  B. Mismo boletín tras quitar   54 precios reconstruidos desde el PDF y
  temporalmente el 06-11         **la misma alerta que el Batch** (Zanahoria
                                 +33.7 %, MEDIUM). Después se restauraron los
                                 datos de SISAP (`load-sisap` + `batch-analytics`)
  C. Modo continuo               El boletín dejado en `incoming/` mientras el
                                 job corría se procesó en unos 9 segundos

## 12.4. Limitación actual

El histórico de SISAP llega hasta el 26-11-2026, así que hoy no hay
boletines con fechas nuevas. El Streaming tendrá datos nuevos reales a
partir del boletín del 27-11-2026. No se generan eventos sintéticos.

## 12.5. Evolución opcional con Kafka

`Productores → Kafka → Spark Structured Streaming → MongoDB Atlas`

No es necesaria para el MVP: la fuente de archivos cubre una frecuencia
diaria. El caso de uso y las reglas de alerta no cambiarían.

------------------------------------------------------------------------

# 13. Backend y frontend (implementado: módulo `smartprice-app`)

**Estado (26-11-2026):** aplicación Spring Boot 4.1.3 + Thymeleaf
funcionando contra MongoDB Atlas en `http://localhost:8085` (el 8080
estaba ocupado por otra aplicación; el puerto se puede cambiar con la
variable `SERVER_PORT`). Se arranca con `.pp.ps1`.

-   **Arquitectura hexagonal:** módulos `price`, `analytics`, `alert` y
    `source`, cada uno con `domain`, `application` (puertos `in`/`out` y
    servicios) e `infrastructure` (adaptadores web y MongoDB). Los
    servicios no llevan anotaciones de Spring: se ensamblan en
    `UseCaseConfig`.
-   **Pruebas del núcleo sin infraestructura:** 10 pruebas con puertos
    falsos (sin MongoDB ni Spring), que cumplen el criterio de aceptación
    de la sección 22.
-   **Verificación:** las 4 vistas y las páginas de detalle de los 59
    productos responden 200 sin errores; la API devuelve 400 con un
    mensaje claro ante un rango de fechas inválido.
-   **Gráficos:** Apache ECharts 5.6.0 servido localmente (funciona sin
    internet). Precio y volumen van en **dos gráficos de una serie** que
    comparten el eje de fechas, el cursor y el zoom, en lugar de un
    gráfico con dos ejes Y. Paleta validada para daltonismo en modo claro
    y oscuro.
-   **Configuración:** la app lee `MONGODB_URI` del mismo `.env` que los
    jobs (`spring.config.import`).

Detalles técnicos encontrados:

-   Spring 6+ requiere compilar con `-parameters` para los
    `@RequestParam` sin nombre explícito (configurado en el POM padre).
-   Thymeleaf 3.1 prohíbe llamadas a beans (`@fmt`) en atributos
    genéricos como `th:aria-label`; se usan en `th:text`.
-   En el inlining de JavaScript de Thymeleaf, cada valor inlined debe ir
    solo en su línea.

## 13.1. Backend Java Spring Boot con arquitectura hexagonal

Responsabilidades:

-   Consultar productos y mercados.
-   Exponer precios normalizados.
-   Consultar indicadores calculados por Spark.
-   Consultar históricos y alertas.
-   Administrar filtros por fecha y categoría.

Spring Boot **solo lee** los resultados que Spark escribe en MongoDB.
En el MVP, los jobs Spark se ejecutan desde la línea de comandos. El
adaptador que lanza jobs desde la web (sección 21.5) queda como
extensión.

Endpoints candidatos:

  Método   Ruta                          Función
  -------- ----------------------------- -------------------------
  GET      `/api/products`               Listar productos
  GET      `/api/products/{id}/prices`   Consultar precios
  GET      `/api/markets`                Listar mercados
  GET      `/api/analytics/trends`       Consultar tendencias
  GET      `/api/analytics/alerts`       Consultar alertas
  GET      `/api/context/indexes`        Qhawarina e IPC
  POST     `/api/baskets/compare`        Comparar una canasta
                                         (extensión)

## 13.2. Frontend Thymeleaf

La interfaz se implementará dentro de la misma aplicación Spring Boot mediante Spring MVC y Thymeleaf. Thymeleaf renderizará plantillas HTML en el servidor. CSS y JavaScript complementarán la presentación y las interacciones del usuario. No se utilizará Angular ni React como SPA independiente.

Vistas del MVP:

1.  Dashboard de inteligencia de precios (GMML más contexto de índices).
2.  Historial y tendencias por producto (precio por kg y volumen).
3.  Alertas de precios.
4.  Fuentes y trazabilidad: qué boletines se procesaron, con su fecha y
    estado.

Vistas de extensión: comparador de supermercados y canasta simple.

## 13.3. Recursos de interfaz y visualizaciones

-   Gráficos de líneas para evolución temporal.
-   Gráfico combinado de precio y volumen de ingreso.
-   Barras para comparación entre establecimientos (extensión).
-   Tablas para productos y precios.
-   Indicadores para variaciones porcentuales.
-   Mapas, únicamente si la fuente incluye ubicaciones suficientemente
    precisas y consistentes.

------------------------------------------------------------------------

# 14. Estructura de carpetas del proyecto

``` text
SmartPrice/
├── pom.xml                         (padre Maven)
├── mvnw, mvnw.cmd, .mvn/           (Maven Wrapper)
├── jobs.ps1                        (ejecuta los jobs: JDK 17 + Hadoop + --add-opens)
├── .env                            (MONGODB_URI, no versionado)
│
├── data/
│   ├── raw/
│   │   ├── sisap/                  (exportaciones SISAP .xls)
│   │   ├── midagri/gmml_pdf/       (boletines PDF diarios)
│   │   ├── qhawarina/
│   │   ├── inei/
│   │   └── supermarkets/
│   ├── interim/                    (sisap_prices.csv, gmml_csv/)
│   └── streaming/
│       ├── incoming/
│       └── checkpoint/
│
├── smartprice-jobs/                (Apache Spark, API Java)
│   ├── pom.xml
│   └── src/
│       ├── main/java/pe/smartprice/jobs/
│       │   ├── JobsMain.java               (comandos)
│       │   ├── common/                     (AppConfig, Sparks, CsvWriter, Hashes)
│       │   ├── sisap/                      (SisapReader, SisapToCsvJob)
│       │   ├── gmml/                       (GmmlPdfExtractor, ExtractGmmlPdfJob)
│       │   ├── mongo/                      (LoadSisapJob, MongoWriter, MongoSetupJob)
│       │   ├── analytics/                  (ExploreSisapJob; Batch: tendencias,
│       │   │                                variaciones, anomalías)
│       │   └── streaming/                  (PriceStreamingJob: boletines nuevos)
│       ├── main/resources/log4j2.properties
│       └── test/java/...                   (SisapReaderTest, GmmlPdfExtractorTest)
│
├── smartprice-app/                 (Spring Boot + Thymeleaf, hexagonal)
│   ├── pom.xml
│   └── src/main/
│       ├── java/pe/smartprice/
│       │   ├── SmartPriceApplication.java
│       │   ├── price/      {domain, application, infrastructure}
│       │   ├── analytics/  {domain, application, infrastructure}
│       │   └── alert/      {domain, application, infrastructure}
│       └── resources/
│           ├── templates/  {fragments, dashboard, products, alerts, sources}
│           └── static/     {css, js, img}
│
└── documentation/
```

Estado: `smartprice-jobs` está implementado y probado (ingestión SISAP,
extractor PDF, carga en MongoDB y exploración). Los paquetes
`analytics` (Batch) y `streaming` y el módulo `smartprice-app` son las
siguientes etapas.

------------------------------------------------------------------------

# 15. Plan de trabajo individual

Cronograma estimado para un MVP académico. Los tiempos dependen de la
disponibilidad y calidad de las fuentes.

  Etapa   Actividad                                    Duración estimada
  ------- ------------------------------------------ -------------------
  0       Verificación de fuentes                       Hecho (26-11-2026)
  1       Entorno: JDK 17, Maven, Spark (Java),         Hecho (26-11-2026)
          winutils
  2       Histórico SISAP: descarga, lector y           Hecho (26-11-2026)
          primera exploración con Spark
  3       MongoDB Atlas: usuario, carga del         Hecho (26-11-2026)
          histórico e índices
  4       Spark Batch: tendencias mensuales,        Hecho (26-11-2026)
          resumen por producto, alertas IQR
  5       Qhawarina e INEI como contexto                        1 día
  6       Extractor PDF y Structured Streaming      Hecho (26-11-2026)
          con boletines nuevos y alertas
  7       Spring Boot (4 módulos hexagonales) y     Hecho (26-11-2026)
          Thymeleaf (4 vistas + API para gráficos)
  8       Pruebas, informe y demostración                      2--3 días
  --      Extensiones opcionales: muestra de                   3--4 días
          supermercados y canasta

Duración restante del núcleo: **3 a 5 días** (Qhawarina/INEI, pruebas e
informe). Con las extensiones, de 6 a 9 días.

El riesgo principal ya no es la obtención del histórico, porque ya está
descargado. Ahora es el extractor de PDF para los boletines nuevos: si el
formato cambia, el Streaming puede seguir funcionando con eventos de
prueba etiquetados mientras se ajusta el extractor.

## 15.1. Entregables

-   Documento de investigación de fuentes, con el resultado de la
    verificación.
-   Archivos originales (exportaciones SISAP, PDF y JSON) y registro de
    procedencia.
-   Lector de SISAP y extractor de PDF probados, con su validación
    cruzada.
-   Diccionario de datos.
-   Base de datos MongoDB Atlas.
-   Pipeline Batch.
-   Pipeline Streaming.
-   Resultados analíticos.
-   Aplicación web de consulta.
-   Informe de pruebas y limitaciones.

------------------------------------------------------------------------

# 16. Plan de pruebas y evaluación

## 16.1. Pruebas de ingestión

-   El archivo se puede leer.
-   El extractor devuelve el número esperado de filas por boletín.
-   Una muestra de filas coincide con el PDF original.
-   El esquema se identifica correctamente.
-   La cantidad de registros se registra.
-   Los metadatos de fuente se conservan.
-   Los datos se almacenan en MongoDB.

## 16.2. Pruebas de calidad

-   Porcentaje de valores nulos.
-   Cantidad de duplicados.
-   Fechas inválidas.
-   Precios negativos o no numéricos.
-   Unidades no reconocidas.
-   Productos sin atributos suficientes para compararlos.

## 16.3. Pruebas de procesamiento

-   Comparar resultados Spark con una muestra calculada manualmente.
-   Verificar la fórmula de variación.
-   Verificar el cálculo de precio por kg.
-   Comprobar agrupaciones por producto y periodo.
-   Revisar resultados de detección de anomalías.
-   Confirmar que Streaming procesa eventos nuevos.

## 16.4. Métricas de evaluación

  Métrica                   Propósito
  ------------------------- --------------------------
  Registros leídos          Medir volumen de entrada
  Registros válidos         Medir calidad
  Registros rechazados      Identificar problemas
  Tiempo de procesamiento   Evaluar rendimiento
  Tiempo de escritura       Evaluar persistencia
  Duplicados                Evaluar idempotencia
  Eventos procesados        Evaluar Streaming
  Exactitud de cálculos     Verificar resultados

No se deben presentar resultados de rendimiento hasta ejecutar las
pruebas en el equipo y documentar hardware, configuración, tamaño de
archivos y condiciones de ejecución.

------------------------------------------------------------------------

# 17. Riesgos y consideraciones

  -----------------------------------------------------------------------
  Riesgo                              Mitigación
  ----------------------------------- -----------------------------------
  Cambio de formato de los PDF        Versionar el extractor
  entre años                          (`extractor_version`), validar
                                      filas por boletín y limitar el
                                      histórico al formato estable

  Errores de extracción del PDF       Revisar a mano una muestra contra
                                      el original y registrar la tasa de
                                      error

  Fuente sin descarga granular        Utilizar otra fuente o solicitar
  (Qhawarina, supermercados)          acceso

  Qhawarina sin actualizar desde      Usarla solo como contexto
  junio de 2026                       histórico y declarar el periodo

  SISAP con certificado vencido       Exportación manual documentada;
                                      no automatizar la descarga

  Límite de consulta de SISAP         Descargar en bloques de unos
  (unas 9 000 combinaciones)          5 meses con la misma selección
                                      de productos

  Cambios en el portal                Guardar archivos originales y
                                      registrar fecha

  Spark falla en Windows              Instalar `winutils.exe` y
                                      `HADOOP_HOME` antes de empezar
                                      (sección 7.1.1)

  Datos incompletos                   Medir cobertura y documentar
                                      limitaciones

  Diferencias de unidades             Normalizar y conservar unidad
                                      original

  Productos no equivalentes           Crear reglas de correspondencia y
                                      revisión

  Restricciones de extracción         Utilizar fuentes y métodos
                                      autorizados

  Duplicados                          Implementar identificadores
                                      estables y upsert

  Falta de datos en tiempo real       Utilizar captura periódica y
                                      describir la frecuencia real

  Poco volumen                        Ampliar fuentes o usar datos
                                      sintéticos claramente etiquetados
                                      para pruebas
  -----------------------------------------------------------------------

------------------------------------------------------------------------

# 18. Primeras tareas de implementación

Estado al 26-11-2026: las tareas 18.1 a 18.4 están **completadas**.

## 18.1. Entorno (hecho)

-   JDK 17 en `C:\tools\jdk-17` y `winutils` en `C:\tools\hadoop`.
-   Proyecto Maven con Maven Wrapper; `.\jobs.ps1 build` compila y pasa
    las 8 pruebas unitarias.

## 18.2. Histórico SISAP (hecho)

``` powershell
.\jobs.ps1 sisap-to-csv
.\jobs.ps1 explore-sisap
```

## 18.3. Extracción de un boletín PDF (hecho con un boletín)

``` powershell
.\jobs.ps1 extract-pdf data\raw\midagri\gmml_pdf\<archivo>.pdf
```

Pendiente: probar el extractor con boletines de 2024 y 2025 para
detectar cambios de formato antes de usarlo en Streaming.

## 18.4. MongoDB Atlas (hecho)

``` powershell
.\jobs.ps1 check-mongo
.\jobs.ps1 load-sisap
.\jobs.ps1 setup-mongo
```

## 18.5. Criterio para pasar a la siguiente etapa

No avanzar al procesamiento analítico definitivo hasta que:

-   La fuente tenga procedencia documentada.
-   El archivo se haya inspeccionado.
-   Las columnas y unidades estén identificadas.
-   La conexión con Atlas funcione.
-   Los registros se almacenen correctamente.
-   Se conozcan los posibles duplicados y limitaciones.

------------------------------------------------------------------------

# 19. Referencias técnicas y fuentes

## Fuentes de datos

1.  Qhawarina -- Datos: https://qhawarina.pe/datos
2.  Qhawarina -- Documentación API: https://qhawarina.pe/api/docs
3.  Qhawarina -- Índice JSON:
    https://qhawarina.pe/assets/data/daily_price_index.json
4.  Qhawarina -- Índice CSV:
    https://qhawarina.pe/assets/data/csv/precios_diarios.csv
5.  MIDAGRI -- Precios de alimentos:
    https://www.gob.pe/institucion/midagri/informes-publicaciones/1211-precios-de-alimentos
6.  MIDAGRI -- Colección de reportes GMML (fuente principal):
    https://www.gob.pe/institucion/midagri/colecciones/335-reporte-de-ingreso-y-precios-en-el-gran-mercado-mayorista-de-lima
7.  SISAP -- Mayorista, fuente histórica (certificado vencido el 26-11-2026):
    https://sistemas.midagri.gob.pe/sisap/portal2/mayorista/resumenes/consultar/
8.  PNDA -- Datero Agrario (descartada, no contiene precios):
    https://www.datosabiertos.gob.pe/dataset/midagri-02-datero-agrario-ministerio-de-desarrollo-agrario-y-riego
9.  INEI -- Índices de precios:
    https://www.inei.gob.pe/estadisticas/indice-tematico/price-indexes/
10. INEI -- Sistema de Series Nacionales:
    https://www.gob.pe/institucion/inei/pages/32329-consultar-el-sistema-de-series-nacionales

## Documentación tecnológica

1.  Apache Spark 3.5.6: https://spark.apache.org/docs/3.5.6/
2.  Spark Java API: https://spark.apache.org/docs/3.5.6/api/java/
3.  Spark Structured Streaming:
    https://spark.apache.org/docs/3.5.6/structured-streaming-programming-guide.html
4.  Spark MLlib: https://spark.apache.org/docs/3.5.6/ml-guide.html
5.  MongoDB Spark Connector:
    https://www.mongodb.com/docs/spark-connector/v10.x/
6.  MongoDB Atlas: https://www.mongodb.com/docs/atlas/
7.  Apache PDFBox: https://pdfbox.apache.org/
10. jsoup: https://jsoup.org/
8.  Eclipse Temurin JDK 17:
    https://adoptium.net/temurin/releases/?version=17

------------------------------------------------------------------------

# 20. Conclusión

SmartPrice propone una solución de análisis de precios de alimentos
centrada en el Gran Mercado Mayorista de Lima, con índices de
supermercados e inflación como contexto. La combinación de MongoDB Atlas
y Apache Spark permitirá almacenar datos originales, procesar históricos
y analizar nuevos boletines a medida que se publican.

La verificación de fuentes confirmó que el principal desafío no es
implementar Spark, sino obtener datos con suficiente granularidad,
calidad, cobertura temporal y trazabilidad. La fuente que se había
supuesto principal no contenía precios. La fuente adoptada (boletines
diarios del GMML) está al día y es extraíble, pero exige un paso de
extracción desde PDF que debe validarse con cuidado.

La primera demostración deberá probar el siguiente recorrido:

**Boletín PDF real → archivo descargado y registrado → extracción de la
tabla → carga con Apache Spark en MongoDB Atlas → procesamiento analítico →
resultado verificable contra el PDF.**

Una vez completado ese flujo, se podrá ampliar el proyecto con
Structured Streaming, visualizaciones y, si hay tiempo, el comparador de
supermercados y la canasta.

> **Principio de investigación:** todo resultado presentado como real
> debe poder rastrearse hasta una fuente, archivo, fecha de observación
> y transformación documentada. Los datos de prueba o sintéticos deben
> identificarse expresamente.


---

# 21. Decisión arquitectónica: Spring Boot y arquitectura hexagonal

## 21.1. Decisión

SmartPrice se desarrollará como una aplicación web MVC con **Java Spring Boot**, aplicando **arquitectura hexagonal (Ports and Adapters)**. Las vistas se renderizarán en el servidor mediante **Thymeleaf**. HTML, CSS y JavaScript completarán la interfaz.

Se mantienen MongoDB Atlas y Apache Spark, este último con su API Java. No se utilizarán Python, FastAPI, Angular ni React en la arquitectura seleccionada.

La separación de responsabilidades queda definida así:

| Tecnología | Responsabilidad |
|---|---|
| Thymeleaf | Plantillas HTML renderizadas por Spring MVC |
| HTML, CSS y JavaScript | Estructura, estilos e interacciones del navegador |
| Java Spring Boot | API REST, casos de uso y reglas de negocio |
| MongoDB Atlas | Persistencia operacional, datos originales y resultados analíticos |
| Apache Spark (API Java) | Ingestión analítica, ETL, Batch y Structured Streaming (módulo `smartprice-jobs`) |

### Funcionamiento de Spring MVC + Thymeleaf

El navegador solicita una ruta; un controlador MVC invoca un caso de uso, prepara un modelo de presentación y devuelve el nombre de una plantilla Thymeleaf. Thymeleaf genera el HTML que recibe el navegador. JavaScript podrá complementar la experiencia y consumir endpoints REST para operaciones asíncronas cuando sea necesario.

## 21.2. ¿Por qué arquitectura hexagonal?

La arquitectura hexagonal busca que el dominio y los casos de uso no dependan directamente de frameworks, bases de datos o servicios externos.

El núcleo de SmartPrice contiene reglas como:

- Comparar precios de productos equivalentes.
- Calcular el costo de una canasta.
- Consultar el historial de precios.
- Determinar si una variación amerita una alerta.
- Consultar indicadores analíticos.

Los detalles externos —HTTP, MongoDB, ejecución de jobs Spark— se conectan al núcleo mediante puertos y adaptadores.

### Capas conceptuales

1. **Dominio:** entidades, objetos de valor, reglas e invariantes del negocio. No debe depender de Spring ni MongoDB.
2. **Aplicación:** casos de uso, servicios de aplicación y puertos de entrada y salida.
3. **Adaptadores de entrada:** controladores REST y, posteriormente, consumidores de eventos.
4. **Adaptadores de salida:** repositorios MongoDB, clientes externos y mecanismos de ejecución de jobs Spark.
5. **Configuración:** ensamblaje de dependencias e infraestructura.

## 21.3. Organización por módulos funcionales

Se recomienda combinar arquitectura hexagonal con organización por funcionalidades. No crear únicamente carpetas globales `controllers`, `services` y `repositories`. Los controladores web MVC y las vistas Thymeleaf serán adaptadores de entrada; las plantillas no contendrán reglas de negocio.

Estructura de referencia:

```text
smartprice-app/
└── src/
    ├── main/java/pe/smartprice/
    │   ├── SmartPriceApplication.java
    │   ├── product/
    │   │   ├── domain/
    │   │   ├── application/
    │   │   │   ├── port/in/
    │   │   │   ├── port/out/
    │   │   │   └── service/
    │   │   └── infrastructure/
    │   │       ├── adapter/in/web/
    │   │       ├── adapter/out/persistence/
    │   │       └── config/
    │   ├── price/
    │   ├── comparison/
    │   ├── basket/
    │   └── analytics/
    └── main/resources/
        ├── templates/
        │   ├── fragments/
        │   ├── dashboard/
        │   ├── products/
        │   ├── comparison/
        │   └── analytics/
        └── static/
            ├── css/
            ├── js/
            └── img/
```

Los módulos iniciales son una propuesta. Se pueden incorporar `store`, `market`, `alert` e `ingestion` cuando los casos de uso estén definidos. En una implementación más estricta, separar los controladores MVC que devuelven vistas de los controladores REST que devuelven DTO.

## 21.4. Ejemplo de puertos y adaptadores: comparación de precios

### Puerto de entrada

```java
public interface ComparePricesUseCase {
    ComparisonResult execute(String productId);
}
```

### Puerto de salida

```java
public interface PriceRepositoryPort {
    List<Price> findComparablePrices(String productId);
}
```

### Servicio de aplicación

```java
@Service
@RequiredArgsConstructor
public class ComparePricesService
        implements ComparePricesUseCase {

    private final PriceRepositoryPort priceRepository;

    @Override
    public ComparisonResult execute(String productId) {
        List<Price> prices =
            priceRepository.findComparablePrices(productId);

        return ComparisonResult.from(prices);
    }
}
```

### Adaptador MongoDB

```java
@Repository
@RequiredArgsConstructor
public class MongoPriceRepositoryAdapter
        implements PriceRepositoryPort {

    private final SpringDataPriceRepository repository;

    @Override
    public List<Price> findComparablePrices(String productId) {
        return repository.findByProductId(productId)
            .stream()
            .map(PriceMapper::toDomain)
            .toList();
    }
}
```

Los fragmentos son ilustrativos. Se deberán implementar las clases de dominio, DTO, mappers, repositorios Spring Data y validaciones. La entidad de dominio no debe ser una clase que dependa de `@Document` de Spring Data. La anotación `@RequiredArgsConstructor` requiere añadir **Lombok** como dependencia; sin Lombok, hay que escribir el constructor a mano.

## 21.5. Integración entre Spring Boot, Spark y MongoDB

Spark y Spring Boot serán procesos independientes.

**Spring Boot no debe ejecutar transformaciones Spark pesadas dentro del hilo de una solicitud HTTP.** Tampoco se recomienda ejecutar Spark dentro del proceso de Spring Boot: Spark 3.5 usa `javax.servlet` (Jetty 9) y Spring Boot 3 usa `jakarta.servlet`, y además chocan las versiones de Jackson y los sistemas de logging. Por eso los jobs viven en un módulo Maven separado y se ejecutan como otro proceso.

### Flujo recomendado

```text
Navegador (HTML renderizado con Thymeleaf)
   |
   v
Spring Boot / Spring MVC
   |
   v
Caso de uso: Solicitar análisis
   |
   v
Puerto: SparkJobPort
   |
   v
Adaptador de ejecución de jobs
   |
   v
Proceso independiente (smartprice-jobs.jar)
   |
   v
MongoDB Atlas
   |
   v
Spring Boot consulta resultados
   |
   v
Controlador MVC prepara el modelo
   |
   v
Thymeleaf renderiza la vista
```

### Primera etapa: ejecución local

> **Alcance del MVP:** en el MVP, los jobs Spark se ejecutan desde la línea de comandos y Spring Boot solo lee sus resultados en MongoDB. El adaptador descrito a continuación es una extensión que se implementa solo si sobra tiempo.

Como extensión, un adaptador de infraestructura puede iniciar un proceso Java separado (`smartprice-jobs.jar`) que ejecute un job de Spark. El adaptador debe:

- Validar el nombre del job permitido.
- Pasar parámetros de forma segura.
- No concatenar entradas del usuario en comandos de shell.
- Registrar identificador, hora de inicio y fin.
- Capturar salida y errores.
- Gestionar timeout y estado.
- Evitar bloquear solicitudes HTTP.
- Restringir ejecuciones simultáneas según la capacidad del equipo.

Esta estrategia es adecuada para experimentar localmente, pero no es la arquitectura ideal para producción.

### Evolución

Cuando se necesite mayor robustez, reemplazar el adaptador local por un servicio de ejecución de jobs u orquestador. El caso de uso seguirá dependiendo del puerto `SparkJobPort`, no de una implementación concreta.

### Estados sugeridos de ejecución

- `PENDING`
- `RUNNING`
- `SUCCEEDED`
- `FAILED`
- `CANCELLED`

Spring Boot puede exponer una solicitud de inicio y una consulta de estado. El job Spark escribirá sus resultados y metadatos en MongoDB. La API consultará los resultados cuando estén disponibles.

## 21.6. Dos integraciones distintas con MongoDB

No confundir los conectores:

- **Spring Data MongoDB:** utilizado por Spring Boot para consultas y operaciones de la aplicación.
- **MongoDB Spark Connector:** utilizado por Spark para leer y escribir conjuntos de datos.

Ambos acceden a MongoDB Atlas, pero sus responsabilidades son diferentes. Deben compartir convenciones de nombres, esquemas y reglas de compatibilidad de documentos.

## 21.7. Contrato de datos entre Spark y Spring Boot

Los resultados analíticos deben tener un contrato estable. Ejemplo:

```json
{
  "analysis_id": "analysis-2026-0001",
  "analysis_type": "PRICE_TREND",
  "product_id": "product-001",
  "period": {
    "from": "2026-10-01",
    "to": "2026-10-31"
  },
  "currency": "PEN",
  "unit": "kg",
  "average_price": 4.85,
  "minimum_price": 4.20,
  "maximum_price": 5.40,
  "source_count": 3,
  "generated_at": "2026-11-26T20:00:00Z",
  "data_quality_status": "VALIDATED"
}
```

Es un contrato ilustrativo, no un resultado real. Debe incluir versión de esquema y trazabilidad del job cuando se implemente.

## 21.8. Reglas de dependencia

- El dominio no importa Spring Framework, MongoDB ni Spark.
- La aplicación define puertos y coordina casos de uso.
- Los adaptadores implementan los puertos.
- Los controladores MVC convierten solicitudes HTTP en comandos o consultas de aplicación y preparan el modelo para las vistas Thymeleaf.
- Las plantillas Thymeleaf pertenecen a los adaptadores de entrada/presentación; no contienen reglas de negocio.
- Si se requieren actualizaciones parciales o gráficos dinámicos, JavaScript puede consumir endpoints REST de la misma aplicación.
- Los documentos MongoDB se mapean a modelos de dominio; no se exponen directamente al frontend.
- Spark no modifica directamente entidades de dominio Java: intercambia documentos analíticos mediante un contrato de datos.
- Las páginas Thymeleaf reciben datos preparados por los controladores; el navegador nunca accede directamente a MongoDB.

## 21.9. Pruebas recomendadas

| Nivel | Prueba |
|---|---|
| Dominio | Reglas de comparación y cálculo de precios |
| Aplicación | Casos de uso con puertos simulados |
| Adaptador MongoDB | Persistencia y consultas de integración |
| API | Controladores y códigos HTTP |
| Spark | Transformaciones con muestras controladas |
| Integración | Job Spark escribe resultados y Spring Boot los consulta |
| E2E | Navegador recorre las vistas Thymeleaf y verifica los flujos de usuario |

---

# 22. Arquitectura objetivo y decisiones pendientes

## Decisiones adoptadas

- Backend: Java Spring Boot.
- Estilo arquitectónico: hexagonal.
- Frontend: Thymeleaf con Spring MVC, HTML, CSS y JavaScript.
- Base de datos: MongoDB Atlas.
- Procesamiento: Apache Spark 3.5.6 con su API Java (módulo `smartprice-jobs`).
- Entorno inicial: local en Windows 11.
- Procesamiento: Batch y Structured Streaming.
- Modalidad: individual.
- Enfoque: investigación y MVP verificable.
- Fuente principal: SISAP para el histórico (exportación manual, 2024-2026) y boletines PDF diarios del GMML para los datos nuevos (Apache PDFBox).
- Entorno: JDK 17 en `C:\tools\jdk-17`, Hadoop `winutils` en `C:\tools\hadoop` y Maven Wrapper. Los jobs se ejecutan con `jobs.ps1`, que fija las variables solo para el proceso.
- Lenguaje: Java 17 en todo el proyecto, sin Python.
- Módulos backend del MVP: `price`, `analytics` y `alert`.
- Ejecución de jobs Spark en el MVP: por línea de comandos.
- Autenticación de usuarios: fuera del MVP.

## Decisiones que deben cerrarse durante el diseño detallado

1. Versión de Spring Boot compatible con Java 17 (Spring Boot 3.x requiere Java 17 o superior).
2. Maven o Gradle para la construcción del backend.
3. Estructura definitiva de módulos y límites de dominio.
4. Contrato versionado entre jobs Spark y backend.
5. Estrategia de idempotencia y reprocesamiento.
6. Índices, retención y tamaño de documentos en MongoDB.
7. Periodo histórico de boletines GMML que se procesará, según la estabilidad de su formato.
8. Autorización de acceso a fuentes externas (supermercados, feed granular de Qhawarina).
9. Estrategia de despliegue cuando se supere el entorno local.

## Criterio de aceptación arquitectónica

La arquitectura se considerará correctamente implementada cuando:

- Los casos de uso de negocio puedan probarse sin levantar MongoDB ni Spark.
- Los adaptadores MongoDB implementen puertos definidos por la aplicación.
- Los jobs Spark se ejecuten de forma independiente al backend.
- Spring Boot pueda consultar el estado y los resultados de un análisis. Solicitar análisis desde la web es una extensión.
- Los resultados de Spark puedan consultarse desde Spring Boot.
- Las vistas Thymeleaf presenten información mediante modelos preparados por la capa de aplicación/controladores.
- Los endpoints REST, si se habilitan para AJAX, expongan DTO y no documentos internos de MongoDB.
- Se mantenga trazabilidad entre fuente, procesamiento y resultado.

---

# 23. Conclusión arquitectónica actualizada

SmartPrice se implementará con una separación clara entre el backend transaccional y el procesamiento analítico.

Java Spring Boot, mediante arquitectura hexagonal y Spring MVC, será responsable de los casos de uso, la lógica de negocio y la capa web. Thymeleaf renderizará las vistas HTML del lado del servidor, complementadas con CSS y JavaScript. MongoDB Atlas almacenará la información operacional y analítica. Apache Spark, mediante su API Java en un módulo separado, ejecutará los procesos de ingestión, transformación y análisis de datos.

Esta decisión evita acoplar las reglas de negocio a una base de datos o motor de procesamiento y permite evolucionar cada componente de manera independiente.

El primer hito técnico es: descargar boletines GMML reales, extraer sus tablas, cargarlas con Apache Spark en MongoDB Atlas, ejecutar una transformación y consultar el resultado desde Spring Boot.
