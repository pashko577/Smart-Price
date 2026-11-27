# SMARTPRICE

## Documento de propuesta, diseño técnico y plan de implementación

**Versión:** 1.1\
**Fecha:** 26 de noviembre de 2026\
**Tipo de documento:** Documento de investigación y diseño técnico\
**Modalidad:** Desarrollo individual\
**Entorno de procesamiento:** Local (Windows 11)\
**Tecnologías centrales:** MongoDB Atlas, Apache Spark y PySpark

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

  Mercados considerados               Supermercados y mercados
                                      tradicionales

  Procesamiento                       Batch y Streaming

  Prioridad funcional                 Comparación comercial y analítica
                                      de precios

  Modalidad de trabajo                Individual

  Tipo de entrega                     Investigación tecnológica con
                                      demostración funcional
  -----------------------------------------------------------------------

> **Estado de madurez:** este documento define una propuesta y un plan
> técnico. Los enlaces de fuentes se identificaron como candidatos
> durante la investigación inicial, pero todavía deben descargarse y
> validarse los archivos originales. Los volúmenes, campos y formatos
> que no se hayan inspeccionado directamente se presentan como objetivos
> o hipótesis de trabajo, no como datos confirmados.

------------------------------------------------------------------------

# 1. Resumen ejecutivo

SmartPrice es una propuesta de plataforma de inteligencia de precios que
integra información procedente de supermercados y mercados tradicionales
del Perú. Su propósito es organizar y analizar datos de productos,
establecimientos, mercados, precios y fechas para facilitar la
comparación de precios, observar tendencias y apoyar decisiones de
compra.

El proyecto utilizará **MongoDB Atlas** como base de datos NoSQL y
**Apache Spark**, mediante PySpark, como motor de procesamiento de
datos. El sistema contempla dos modalidades de procesamiento:

-   **Batch:** análisis de archivos históricos y conjuntos de datos
    acumulados.
-   **Streaming:** procesamiento de nuevos eventos de precios conforme
    ingresan al sistema. En el prototipo inicial, los eventos pueden
    simularse o generarse desde archivos JSON; esto no significa que los
    supermercados publiquen datos en tiempo real.

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

1.  Consultar precios por producto, categoría, establecimiento y fecha.
2.  Comparar precios de productos equivalentes.
3.  Calcular variaciones y tendencias históricas.
4.  Identificar valores atípicos que requieran revisión.
5.  Analizar por separado indicadores de supermercados y mercados
    tradicionales.
6.  Explorar una canasta de compra de menor costo, cuando exista
    información suficiente y comparable.

## 1.3. Objetivo general

Desarrollar una plataforma de comparación y análisis de precios mediante
MongoDB Atlas y Apache Spark que permita integrar, almacenar, procesar y
analizar información comercial de supermercados y mercados tradicionales
del Perú, generando indicadores que apoyen las decisiones de compra.

## 1.4. Objetivos específicos

-   Identificar y evaluar fuentes de datos públicas y comerciales sobre
    precios.
-   Descargar y documentar conjuntos de datos en formatos como CSV,
    JSON, Excel y PDF.
-   Diseñar una base de datos NoSQL para conservar datos originales y
    procesados.
-   Implementar procesos de ingestión y transformación mediante PySpark.
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

La solución contempla los siguientes módulos:

1.  **Comparador de precios:** búsqueda de productos y comparación entre
    establecimientos, cuando los datos sean equivalentes.
2.  **Inteligencia de precios:** indicadores, promedios, mínimos,
    máximos y variaciones.
3.  **Canasta inteligente:** estimación del costo de una lista de
    productos y, en una etapa posterior, búsqueda de combinaciones de
    establecimientos de menor costo.
4.  **Alertas:** identificación de variaciones relevantes y valores
    atípicos.
5.  **Mercados tradicionales:** consulta y análisis de precios
    mayoristas o minoristas disponibles en fuentes oficiales.
6.  **Administración de datos:** seguimiento de fuentes, archivos,
    fechas de captura, registros procesados y errores.

## 2.2. Alcance técnico

-   Procesamiento local en una computadora con Windows 11.
-   MongoDB Atlas como almacenamiento remoto.
-   PySpark para procesamiento Batch y Structured Streaming.
-   Python para adquisición y utilidades de datos.
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

------------------------------------------------------------------------

# 3. Fuentes de datos identificadas

La disponibilidad de una página web no garantiza que exista una API
pública ni que se permita la extracción automatizada. Cada fuente debe
validarse antes de integrarla.

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

### Campos del índice que deben comprobarse

  Campo identificado o esperado   Significado
  ------------------------------- -----------------------------------
  `date`                          Fecha de observación
  `index_all`                     Índice general
  `index_food`                    Índice de alimentos
  `index_nonfood`                 Índice no alimentario
  `var_all`                       Variación general
  `cum_pct`                       Variación acumulada
  `n_matched`                     Cantidad de productos emparejados
  `interpolated`                  Indicador de interpolación

Los nombres anteriores deben contrastarse con la versión actual del
archivo descargado.

### Uso en SmartPrice

-   Incorporar indicadores agregados como contexto de evolución de
    precios.
-   Comparar tendencias generales por categoría, si la fuente lo
    permite.
-   No usar el índice como sustituto de precios individuales por SKU.
-   Solicitar al proveedor información sobre acceso académico al feed
    granular, licencia, frecuencia, campos y condiciones de uso.

### Validaciones pendientes

-   Confirmar que los enlaces siguen activos.
-   Descargar el JSON y CSV.
-   Registrar fecha de descarga y periodo cubierto.
-   Inspeccionar campos, tipos y cantidad de observaciones.
-   Revisar licencia y condiciones de reutilización.
-   Consultar disponibilidad y costo del feed granular.

## 3.2. MIDAGRI -- SISAP y reportes de precios

**Portal de reportes:**
https://www.gob.pe/institucion/midagri/informes-publicaciones/1211-precios-de-alimentos\
**SISAP mayorista:**
https://sistemas.minagri.gob.pe/sisap/portal2/mayorista/

El Ministerio de Desarrollo Agrario y Riego publica información
relacionada con precios y abastecimiento de productos agropecuarios. Los
reportes pueden incluir información de mercados mayoristas de Lima y
productos agrícolas.

### Formatos a investigar

-   PDF de reportes diarios.
-   Consultas y posibles exportaciones desde SISAP.
-   Archivos descargables, si están disponibles para la consulta
    elegida.

### Campos objetivo para el modelo de SmartPrice

  Campo de destino   Descripción
  ------------------ ----------------------------------
  `product_name`     Nombre del producto
  `market_name`      Nombre del mercado
  `observed_at`      Fecha de observación
  `price_min`        Precio mínimo, si se publica
  `price_max`        Precio máximo, si se publica
  `price_average`    Precio promedio, si se publica
  `unit`             Unidad comercial
  `volume`           Volumen ingresado, si se publica
  `source_name`      Fuente de procedencia

Estos son campos objetivo; no debe suponerse que todos estén presentes
en cada archivo. Se debe inspeccionar el reporte o exportación original.

### Uso en SmartPrice

-   Analizar evolución de precios agrícolas.
-   Comparar mercados mayoristas cuando el producto, unidad y fecha sean
    equivalentes.
-   Examinar relaciones entre abastecimiento y precios, si ambas
    variables están disponibles.
-   Mantener los datos mayoristas diferenciados de precios minoristas de
    supermercados.

## 3.3. PNDA -- MIDAGRI, Datero Agrario

**Ficha identificada:**\
https://www.datosabiertos.gob.pe/dataset/midagri-02-datero-agrario-ministerio-de-desarrollo-agrario-y-riego

La ficha de datos abiertos se identificó como una posible fuente de
recursos CSV históricos sobre precios de productos agropecuarios en
mercados mayoristas de Lima y otras ciudades. Se mencionaron recursos
desde 2015; el periodo exacto, cantidad de archivos, filas y esquema
deben verificarse directamente en la ficha y en los archivos
descargados.

### Formato

-   CSV, sujeto a confirmar por recurso.

### Campos que deben buscarse

-   Fecha o periodo.
-   Producto.
-   Mercado, ciudad o ubicación.
-   Precio promedio o precio publicado.
-   Unidad de comercialización.
-   Otros atributos que incluya el diccionario de datos.

### Uso en SmartPrice

Puede constituir una de las fuentes principales para el análisis
histórico de mercados tradicionales, siempre que los archivos tengan
cobertura y granularidad suficientes.

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

## 4.4. Volumen de datos: metas, no hechos confirmados

Se propone como meta inicial:

  Indicador                                      Meta de diseño
  -------------------------- ----------------------------------
  Observaciones de precios                              100 000
  Productos de catálogo                              500--1 000
  Supermercados                                            5--6
  Mercados tradicionales                                   3--5
  Historial objetivo           90 días, sujeto a disponibilidad

Ejemplo de dimensionamiento:

`500 productos × 5 establecimientos × 40 días = 100 000 observaciones`

Este cálculo supone que todos los productos tienen observación en cada
establecimiento y fecha. En la práctica, disponibilidad, promociones y
cobertura pueden producir un volumen menor.

Los datos sintéticos pueden utilizarse para probar carga y
escalabilidad, pero deben etiquetarse explícitamente y mantenerse
separados de los datos reales.

------------------------------------------------------------------------

# 5. Arquitectura técnica propuesta

## 5.1. Flujo general

1.  **Fuentes:** PNDA, MIDAGRI, Qhawarina, INEI y catálogos autorizados.
2.  **Ingestión:** scripts Python para descargar o leer CSV, JSON, Excel
    y reportes.
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
PNDA / MIDAGRI / QHAWARINA / INEI / SUPERMERCADOS
                       |
                       v
             ADQUISICIÓN DE DATOS
             Python / CSV / JSON / API
                       |
                       v
                ZONA RAW
             Datos originales
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
  Lenguaje                Python 3.11             Ingestión y
                                                  procesamiento

  Motor                   Apache Spark 3.5.6      Procesamiento
                                                  distribuido

  Procesamiento           PySpark                 Desarrollo y ejecución
                                                  de jobs Spark

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

## 6.2. Colecciones

  Colección                  Contenido
  -------------------------- ----------------------------------------
  `products`                 Catálogo de productos y presentaciones
  `stores`                   Supermercados y establecimientos
  `markets`                  Mercados tradicionales y mayoristas
  `raw_midagri_prices`       Registros originales de MIDAGRI
  `raw_qhawarina_index`      Índices agregados de Qhawarina
  `raw_supermarket_prices`   Observaciones originales de catálogos
  `normalized_prices`        Precios estandarizados
  `price_analytics`          Resultados de Spark
  `price_alerts`             Variaciones y anomalías detectadas
  `processing_logs`          Historial de ejecuciones

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

## 6.4. Ejemplo de observación de mercado tradicional

``` json
{
  "source": "TRADITIONAL_MARKET",
  "market": "Gran Mercado Mayorista de Lima",
  "product": {
    "name": "Papa blanca",
    "category": "Tubérculos"
  },
  "price": {
    "minimum": 1.80,
    "maximum": 2.40,
    "average": 2.10,
    "currency": "PEN"
  },
  "unit": "kg",
  "observed_at": "2026-11-26",
  "source_metadata": {
    "source_name": "MIDAGRI"
  }
}
```

Este documento también es ilustrativo. Los campos deben adaptarse al
esquema real de la fuente.

## 6.5. Normalización de unidades

No se deben comparar directamente presentaciones distintas. El precio
unitario se calcula así:

`Precio unitario = Precio de la presentación / Cantidad de la presentación`

Ejemplo:

-   Bolsa de 5 kg a S/ 24.90.
-   Precio unitario: S/ 4.98 por kg.

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

## 7.1. Configuración base

  Componente                      Versión propuesta
  ------------------------------- ----------------------------
  Sistema operativo               Windows 11
  Python                          3.11.x
  Java JDK                        17
  Apache Spark                    3.5.6
  Scala del artefacto Connector   2.12
  MongoDB Spark Connector         10.7.0
  Git                             Versión estable disponible

Apache Spark 3.5.6 admite Java 8, 11 y 17 y Python 3.8 o superior. La
combinación debe probarse en el equipo.

## 7.2. Instalación de herramientas

-   Python: https://www.python.org/downloads/release/python-3119/
-   Eclipse Temurin JDK 17:
    https://adoptium.net/temurin/releases/?version=17
-   Git: https://git-scm.com/downloads
-   MongoDB Atlas: https://cloud.mongodb.com/
-   Documentación Spark 3.5.6: https://spark.apache.org/docs/3.5.6/

Verificar desde PowerShell:

``` powershell
python --version
java -version
git --version
```

## 7.3. Crear el proyecto

``` powershell
mkdir SmartPrice
cd SmartPrice

mkdir data
mkdir data\raw
mkdir data\raw\midagri
mkdir data\raw\qhawarina
mkdir data\processed

mkdir ingestion
mkdir spark
mkdir spark\batch
mkdir spark\streaming
mkdir documentation

git init
```

## 7.4. Crear entorno virtual

``` powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

Si PowerShell bloquea la activación:

``` powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

Luego activar nuevamente el entorno.

## 7.5. Dependencias Python

Archivo `requirements.txt` propuesto:

``` text
pyspark==3.5.6
pymongo
python-dotenv
pandas
requests
openpyxl
```

Instalación:

``` powershell
python -m pip install --upgrade pip
pip install -r requirements.txt
```

Verificación:

``` powershell
python -c "import pyspark; print(pyspark.__version__)"
```

## 7.6. Variables de entorno

Crear `.env` en la raíz:

``` env
MONGODB_URI=mongodb+srv://USUARIO:CONTRASENA@cluster.mongodb.net/?retryWrites=true&w=majority
```

La URI debe reemplazarse por la cadena real de Atlas. No debe publicarse
en GitHub ni incluirse en capturas o documentación pública.

Crear `.gitignore`:

``` gitignore
.venv/
.env
__pycache__/
*.pyc
.idea/
.vscode/
data/raw/
data/processed/
.sparkStaging/
```

En MongoDB Atlas se debe crear un usuario de base de datos y autorizar
la IP de desarrollo mediante Network Access. Para una configuración
segura, no se debe dejar acceso abierto innecesariamente.

------------------------------------------------------------------------

# 8. Integración Apache Spark--MongoDB

## 8.1. Conector

Artefacto propuesto:

``` text
org.mongodb.spark:mongo-spark-connector_2.12:10.7.0
```

Documentación:

https://www.mongodb.com/docs/spark-connector/v10.x/getting-started/

La versión 10.7.0 del conector se plantea para Spark 3.2 a 3.5 con Scala
2.12. Debe verificarse que la versión de Spark y el artefacto utilizado
coincidan.

## 8.2. Configuración de escritura

En la sesión Spark se puede configurar:

``` python
.config(
    "spark.jars.packages",
    "org.mongodb.spark:mongo-spark-connector_2.12:10.7.0"
)
.config(
    "spark.mongodb.write.connection.uri",
    mongo_uri
)
```

Para escribir, se especifican la base de datos y la colección:

``` python
(
    df.write
    .format("mongodb")
    .mode("append")
    .option("database", "smartprice_db")
    .option("collection", "raw_midagri_prices")
    .save()
)
```

La primera ejecución puede descargar dependencias desde Maven, por lo
que requiere conexión a Internet.

------------------------------------------------------------------------

# 9. Primer pipeline: inspección del CSV de MIDAGRI

## 9.1. Obtener el archivo

Ficha PNDA identificada:

https://www.datosabiertos.gob.pe/dataset/midagri-02-datero-agrario-ministerio-de-desarrollo-agrario-y-riego

Descargar un recurso CSV y guardarlo como:

``` text
data/raw/midagri/precios_midagri.csv
```

El nombre es una convención del proyecto; puede cambiar según el archivo
real.

## 9.2. Script de inspección

Archivo: `spark/batch/inspect_midagri.py`

``` python
from pyspark.sql import SparkSession

spark = (
    SparkSession.builder
    .appName("SmartPrice-MIDAGRI-Inspection")
    .master("local[*]")
    .config("spark.sql.shuffle.partitions", "4")
    .getOrCreate()
)

df = (
    spark.read
    .option("header", True)
    .option("inferSchema", True)
    .option("encoding", "UTF-8")
    .csv("data/raw/midagri/precios_midagri.csv")
)

print("========== ESQUEMA ==========")
df.printSchema()

print("========== PRIMEROS REGISTROS ==========")
df.show(10, truncate=False)

print("Total de registros:", df.count())
print("Total de columnas:", len(df.columns))

spark.stop()
```

Ejecutar desde la raíz:

``` powershell
python spark\batch\inspect_midagri.py
```

El esquema y la cantidad de registros que aparezcan serán los datos
reales de la inspección. No se deben copiar resultados hipotéticos como
si fueran resultados de ejecución.

## 9.3. Validaciones adicionales recomendadas

Después de inspeccionar:

-   Contar nulos por columna.
-   Revisar duplicados.
-   Identificar valores negativos o precios fuera de rango.
-   Identificar formatos de fecha.
-   Verificar moneda y unidad.
-   Revisar valores distintos de producto y mercado.
-   Determinar si el precio es promedio, mínimo, máximo o transacción.

------------------------------------------------------------------------

# 10. Primer pipeline: carga de MIDAGRI en MongoDB Atlas

Archivo: `ingestion/load_midagri_to_mongo.py`

``` python
import os

from dotenv import load_dotenv
from pyspark.sql import SparkSession
from pyspark.sql.functions import (
    lit,
    current_timestamp,
    input_file_name
)

load_dotenv()

mongo_uri = os.getenv("MONGODB_URI")

if not mongo_uri:
    raise ValueError(
        "No se encontró MONGODB_URI en el archivo .env"
    )

spark = (
    SparkSession.builder
    .appName("SmartPrice-MIDAGRI-Ingestion")
    .master("local[*]")
    .config("spark.sql.shuffle.partitions", "4")
    .config(
        "spark.jars.packages",
        "org.mongodb.spark:mongo-spark-connector_2.12:10.7.0"
    )
    .config(
        "spark.mongodb.write.connection.uri",
        mongo_uri
    )
    .getOrCreate()
)

df = (
    spark.read
    .option("header", True)
    .option("inferSchema", True)
    .option("encoding", "UTF-8")
    .csv("data/raw/midagri/precios_midagri.csv")
)

df = (
    df
    .withColumn("_source", lit("MIDAGRI_PNDA"))
    .withColumn("_ingested_at", current_timestamp())
    .withColumn("_source_file", input_file_name())
)

print("Registros encontrados:", df.count())
df.printSchema()

(
    df.write
    .format("mongodb")
    .mode("append")
    .option("database", "smartprice_db")
    .option("collection", "raw_midagri_prices")
    .save()
)

print("Ingesta completada.")

spark.stop()
```

Ejecutar:

``` powershell
python ingestion\load_midagri_to_mongo.py
```

## 10.1. Consideración sobre duplicados

El modo `append` inserta documentos nuevos cada vez que se ejecuta el
script. Por tanto, ejecutar el proceso repetidamente puede duplicar
registros.

Para una versión más robusta:

1.  Identificar una clave natural o un identificador de origen.
2.  Crear un hash estable cuando no exista identificador.
3.  Definir una estrategia de upsert o `foreachBatch`.
4.  Registrar el archivo y la ejecución de ingestión.
5.  Evitar reingresar archivos ya procesados, salvo que se solicite una
    recarga controlada.

------------------------------------------------------------------------

# 11. Diseño de procesamiento Batch

El procesamiento Batch analizará datos históricos almacenados en MongoDB
o archivos.

## 11.1. Procesos propuestos

  Proceso         Resultado
  --------------- -----------------------------------
  Limpieza ETL    Registros válidos y tipados
  Normalización   Unidades y moneda estandarizadas
  Comparación     Precios equivalentes por producto
  Agregación      Promedios, mínimos y máximos
  Tendencias      Evolución por periodo
  Anomalías       Observaciones para revisión
  Canasta         Cálculo de costo total

## 11.2. Variación porcentual

Fórmula:

`Variación (%) = ((Precio actual - Precio anterior) / Precio anterior) × 100`

Ejemplo ilustrativo:

-   Precio anterior: S/ 20.00.
-   Precio actual: S/ 24.00.
-   Variación: +20 %.

Se debe controlar el caso de precio anterior igual a cero y los cambios
de unidad o presentación.

## 11.3. Detección de anomalías

Se puede iniciar con reglas estadísticas, por ejemplo el rango
intercuartílico (IQR):

-   Calcular Q1 y Q3 por grupo comparable.
-   Calcular IQR = Q3 - Q1.
-   Marcar valores fuera de los límites definidos.
-   Enviar los casos a revisión.

Una anomalía estadística no implica necesariamente un error: puede ser
una promoción, un cambio de presentación o una observación excepcional.

## 11.4. Canasta inteligente

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

# 12. Diseño de procesamiento Streaming

## 12.1. Objetivo

Procesar nuevos eventos de precios a medida que llegan al sistema.

La frecuencia de captura depende de las fuentes. Si un catálogo se
consulta diariamente, el flujo de eventos será diario, aunque Spark
procese los eventos mediante microbatches.

## 12.2. Primera implementación

Para evitar añadir infraestructura innecesaria desde el inicio, se
propone comenzar con archivos JSON en una carpeta de entrada.

Estructura:

``` text
data/
└── streaming/
    ├── incoming/
    ├── checkpoint/
    └── processed/
```

Evento de ejemplo:

``` json
{
  "product": "Arroz Superior",
  "store": "Plaza Vea",
  "price": 22.90,
  "unit": "kg",
  "captured_at": "2026-11-26T15:00:00Z"
}
```

Este evento es ilustrativo y debe identificarse como dato de prueba
hasta que provenga de una captura real autorizada.

## 12.3. Flujo

1.  El extractor o generador produce un evento.
2.  El evento llega a la carpeta de entrada.
3.  Spark Structured Streaming detecta el archivo nuevo.
4.  Se valida el esquema.
5.  Se calcula la variación respecto de una referencia histórica.
6.  Se persiste el evento y su resultado.
7.  Se registra el estado de procesamiento.

## 12.4. Evolución opcional con Kafka

Kafka puede añadirse si se necesita demostrar una arquitectura de
eventos más completa:

`Productores → Kafka → Spark Structured Streaming → MongoDB Atlas`

No es obligatorio para el primer prototipo. La prioridad es demostrar
correctamente la lectura continua, el procesamiento y la persistencia.

------------------------------------------------------------------------

# 13. Backend y frontend propuestos

Esta etapa se implementará después de validar la adquisición y el
procesamiento.

## 13.1. Backend Java Spring Boot con arquitectura hexagonal

Responsabilidades:

-   Consultar productos y establecimientos.
-   Exponer precios normalizados.
-   Consultar indicadores calculados por Spark.
-   Consultar históricos y alertas.
-   Exponer resultados de canastas.
-   Administrar filtros por fecha, categoría y ubicación.

Endpoints candidatos:

  Método   Ruta                          Función
  -------- ----------------------------- -------------------------
  GET      `/api/products`               Listar productos
  GET      `/api/products/{id}/prices`   Consultar precios
  GET      `/api/stores`                 Listar establecimientos
  GET      `/api/markets`                Listar mercados
  GET      `/api/analytics/trends`       Consultar tendencias
  GET      `/api/analytics/alerts`       Consultar alertas
  POST     `/api/baskets/compare`        Comparar una canasta

## 13.2. Frontend Thymeleaf

La interfaz se implementará dentro de la misma aplicación Spring Boot mediante Spring MVC y Thymeleaf. Thymeleaf renderizará plantillas HTML en el servidor. CSS y JavaScript complementarán la presentación y las interacciones del usuario. No se utilizará Angular ni React como SPA independiente.

Vistas propuestas:

1.  Dashboard de inteligencia de precios.
2.  Comparador de productos y establecimientos.
3.  Canasta inteligente.
4.  Historial y tendencias de precios.
5.  Alertas de precios.
6.  Análisis de mercados tradicionales.
7.  Panel administrativo de fuentes y procesos.

## 13.3. Recursos de interfaz y visualizaciones

-   Gráficos de líneas para evolución temporal.
-   Barras para comparación entre establecimientos.
-   Tablas para productos y precios.
-   Indicadores para variaciones porcentuales.
-   Mapas, únicamente si la fuente incluye ubicaciones suficientemente
    precisas y consistentes.

------------------------------------------------------------------------

# 14. Estructura de carpetas del proyecto

``` text
SmartPrice/
│
├── data/
│   ├── raw/
│   │   ├── midagri/
│   │   ├── pnda/
│   │   ├── qhawarina/
│   │   ├── inei/
│   │   └── supermarkets/
│   │
│   ├── processed/
│   │
│   └── streaming/
│       ├── incoming/
│       ├── checkpoint/
│       └── processed/
│
├── ingestion/
│   ├── midagri_extractor.py
│   ├── pnda_extractor.py
│   ├── qhawarina_extractor.py
│   └── supermarket_extractor.py
│
├── spark/
│   ├── batch/
│   │   ├── inspect_midagri.py
│   │   ├── etl.py
│   │   ├── price_comparison.py
│   │   ├── trends.py
│   │   └── anomaly_detection.py
│   │
│   └── streaming/
│       ├── price_stream.py
│       └── alerts.py
│
├── smartprice-app/
│   └── src/
│       ├── main/java/pe/smartprice/
│       │   ├── SmartPriceApplication.java
│       │   ├── product/
│       │   │   ├── domain/
│       │   │   ├── application/
│       │   │   └── infrastructure/
│       │   ├── price/
│       │   │   ├── domain/
│       │   │   ├── application/
│       │   │   └── infrastructure/
│       │   ├── comparison/
│       │   │   ├── domain/
│       │   │   ├── application/
│       │   │   └── infrastructure/
│       │   ├── basket/
│       │   │   ├── domain/
│       │   │   ├── application/
│       │   │   └── infrastructure/
│       │   └── analytics/
│       │       ├── domain/
│       │       ├── application/
│       │       └── infrastructure/
│       │
│       └── main/resources/
│       ├── templates/
│       │   ├── fragments/
│       │   ├── products/
│       │   ├── comparison/
│       │   ├── analytics/
│       │   └── dashboard/
│       └── static/
│           ├── css/
│           ├── js/
│           └── img/
│
│
├── documentation/
│   ├── architecture.md
│   ├── data_dictionary.md
│   ├── datasets.md
│   └── validation_report.md
│
├── .env
├── .gitignore
├── requirements.txt
└── README.md
```

------------------------------------------------------------------------

# 15. Plan de trabajo individual

Cronograma estimado para un MVP académico. Los tiempos dependen de la
disponibilidad y calidad de las fuentes.

  Etapa   Actividad                                    Duración estimada
  ------- ------------------------------------------ -------------------
  1       Investigar, descargar y validar datasets                2 días
  2       Explorar, limpiar y documentar datos                    2 días
  3       Configurar MongoDB Atlas                             1--2 días
  4       Implementar Spark Batch                              2--3 días
  5       Implementar Structured Streaming                     2--3 días
  6       Construir API y visualización básica                 3--4 días
  7       Ejecutar pruebas y documentar resultados                2 días

Duración total orientativa: 14--18 días.

## 15.1. Entregables

-   Documento de investigación de fuentes.
-   Archivos originales y registro de procedencia.
-   Diccionario de datos.
-   Base de datos MongoDB Atlas.
-   Pipeline de ingestión.
-   Pipeline Batch.
-   Pipeline Streaming.
-   Resultados analíticos.
-   Demostración de consulta.
-   Informe de pruebas y limitaciones.

------------------------------------------------------------------------

# 16. Plan de pruebas y evaluación

## 16.1. Pruebas de ingestión

-   El archivo se puede leer.
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
-   Comprobar agrupaciones por producto y establecimiento.
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
  Fuente sin descarga granular        Utilizar otra fuente o solicitar
                                      acceso

  Cambios en el portal                Guardar archivos originales y
                                      registrar fecha

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

El trabajo debe comenzar por la validación del entorno y de los
archivos, no por el desarrollo de pantallas.

## 18.1. Verificar el equipo

Ejecutar en PowerShell:

``` powershell
Write-Host "===== PYTHON ====="
python --version

Write-Host "`n===== JAVA ====="
java -version

Write-Host "`n===== GIT ====="
git --version

Write-Host "`n===== DOCKER ====="
docker --version

Write-Host "`n===== RAM ====="
Get-CimInstance Win32_ComputerSystem |
Select-Object @{Name="RAM_GB";Expression={
    [math]::Round($_.TotalPhysicalMemory/1GB,2)
}}

Write-Host "`n===== PROCESADOR ====="
Get-CimInstance Win32_Processor |
Select-Object -ExpandProperty Name
```

Docker es opcional en la primera etapa. El primer prototipo se plantea
con PySpark local nativo.

## 18.2. Descargar un dataset inicial

Comenzar con un recurso CSV del Datero Agrario de MIDAGRI:

https://www.datosabiertos.gob.pe/dataset/midagri-02-datero-agrario-ministerio-de-desarrollo-agrario-y-riego

Guardar el archivo en:

`data/raw/midagri/`

## 18.3. Ejecutar la inspección

Ejecutar:

``` powershell
python spark\batch\inspect_midagri.py
```

Registrar:

-   Nombre del archivo.
-   Fecha de descarga.
-   Tamaño.
-   Cantidad de registros.
-   Cantidad de columnas.
-   Esquema.
-   Valores nulos.
-   Duplicados.
-   Periodo cubierto.

## 18.4. Conectar MongoDB

Configurar la URI en `.env`, ejecutar el pipeline de ingestión y
comprobar los documentos en Atlas.

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
6.  SISAP -- Mayorista:
    https://sistemas.minagri.gob.pe/sisap/portal2/mayorista/
7.  PNDA -- Datero Agrario:
    https://www.datosabiertos.gob.pe/dataset/midagri-02-datero-agrario-ministerio-de-desarrollo-agrario-y-riego
8.  INEI -- Índices de precios:
    https://www.inei.gob.pe/estadisticas/indice-tematico/price-indexes/
9.  INEI -- Sistema de Series Nacionales:
    https://www.gob.pe/institucion/inei/pages/32329-consultar-el-sistema-de-series-nacionales

## Documentación tecnológica

1.  Apache Spark 3.5.6: https://spark.apache.org/docs/3.5.6/
2.  PySpark API: https://spark.apache.org/docs/3.5.6/api/python/
3.  Spark Structured Streaming:
    https://spark.apache.org/docs/3.5.6/structured-streaming-programming-guide.html
4.  Spark MLlib: https://spark.apache.org/docs/3.5.6/ml-guide.html
5.  MongoDB Spark Connector:
    https://www.mongodb.com/docs/spark-connector/v10.x/
6.  MongoDB Atlas: https://www.mongodb.com/docs/atlas/
7.  Python 3.11: https://www.python.org/downloads/release/python-3119/
8.  Eclipse Temurin JDK 17:
    https://adoptium.net/temurin/releases/?version=17

------------------------------------------------------------------------

# 20. Conclusión

SmartPrice propone una solución de análisis de precios que integra
fuentes heterogéneas de supermercados y mercados tradicionales. La
combinación de MongoDB Atlas y Apache Spark permitirá almacenar datos
originales, procesar históricos y analizar nuevos eventos de precios.

El principal desafío técnico no es únicamente implementar Spark, sino
obtener datos con suficiente granularidad, calidad, cobertura temporal y
trazabilidad. Por eso, el proyecto prioriza la validación de fuentes y
la construcción de un pipeline reproducible antes de desarrollar la
interfaz final.

La primera demostración deberá probar el siguiente recorrido:

**Fuente real → archivo descargado → inspección con PySpark →
almacenamiento en MongoDB Atlas → procesamiento analítico → resultado
verificable.**

Una vez completado ese flujo, se podrá ampliar el proyecto con
Structured Streaming, comparación de productos, canastas y
visualizaciones.

> **Principio de investigación:** todo resultado presentado como real
> debe poder rastrearse hasta una fuente, archivo, fecha de observación
> y transformación documentada. Los datos de prueba o sintéticos deben
> identificarse expresamente.


---

# 21. Decisión arquitectónica: Spring Boot y arquitectura hexagonal

## 21.1. Decisión

SmartPrice se desarrollará como una aplicación web MVC con **Java Spring Boot**, aplicando **arquitectura hexagonal (Ports and Adapters)**. Las vistas se renderizarán en el servidor mediante **Thymeleaf**. HTML, CSS y JavaScript completarán la interfaz.

Se mantienen MongoDB Atlas y Apache Spark/PySpark. No se utilizarán FastAPI, Angular ni React en la arquitectura seleccionada.

La separación de responsabilidades queda definida así:

| Tecnología | Responsabilidad |
|---|---|
| Thymeleaf | Plantillas HTML renderizadas por Spring MVC |
| HTML, CSS y JavaScript | Estructura, estilos e interacciones del navegador |
| Java Spring Boot | API REST, casos de uso y reglas de negocio |
| MongoDB Atlas | Persistencia operacional, datos originales y resultados analíticos |
| Apache Spark / PySpark | Ingestión analítica, ETL, Batch y Structured Streaming |
| Python | Scripts de adquisición y jobs Spark |

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

Los fragmentos son ilustrativos. Se deberán implementar las clases de dominio, DTO, mappers, repositorios Spring Data y validaciones. La entidad de dominio no debe ser una clase que dependa de `@Document` de Spring Data.

## 21.5. Integración entre Spring Boot, Spark y MongoDB

Spark y Spring Boot serán procesos independientes.

**Spring Boot no debe ejecutar transformaciones Spark pesadas dentro del hilo de una solicitud HTTP.** Tampoco se recomienda incorporar el runtime PySpark dentro de la aplicación Java.

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
Proceso independiente PySpark
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

Para el prototipo individual, un adaptador de infraestructura puede iniciar un proceso Python separado que ejecute un job PySpark. El adaptador debe:

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

- El dominio no importa Spring Framework, MongoDB ni PySpark.
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
- Procesamiento: Apache Spark con PySpark.
- Entorno inicial: local en Windows 11.
- Procesamiento: Batch y Structured Streaming.
- Modalidad: individual.
- Enfoque: investigación y MVP verificable.

## Decisiones que deben cerrarse durante el diseño detallado

1. Versión de Java y Spring Boot compatible con las dependencias seleccionadas.
2. Estrategia de autenticación, autorización y gestión de sesiones web.
3. Maven o Gradle para la construcción del backend.
4. Estructura definitiva de módulos y límites de dominio.
5. Mecanismo de ejecución y seguimiento de jobs Spark.
6. Contrato versionado entre jobs Spark y backend.
7. Estrategia de idempotencia y reprocesamiento.
8. Índices, retención y tamaño de documentos en MongoDB.
9. Autorización de acceso a fuentes externas.
10. Estrategia de despliegue cuando se supere el entorno local.

## Criterio de aceptación arquitectónica

La arquitectura se considerará correctamente implementada cuando:

- Los casos de uso de negocio puedan probarse sin levantar MongoDB ni Spark.
- Los adaptadores MongoDB implementen puertos definidos por la aplicación.
- Los jobs Spark se ejecuten de forma independiente al backend.
- Spring Boot pueda solicitar y consultar el estado de un análisis.
- Los resultados de Spark puedan consultarse desde Spring Boot.
- Las vistas Thymeleaf presenten información mediante modelos preparados por la capa de aplicación/controladores.
- Los endpoints REST, si se habilitan para AJAX, expongan DTO y no documentos internos de MongoDB.
- Se mantenga trazabilidad entre fuente, procesamiento y resultado.

---

# 23. Conclusión arquitectónica actualizada

SmartPrice se implementará con una separación clara entre el backend transaccional y el procesamiento analítico.

Java Spring Boot, mediante arquitectura hexagonal y Spring MVC, será responsable de los casos de uso, la lógica de negocio y la capa web. Thymeleaf renderizará las vistas HTML del lado del servidor, complementadas con CSS y JavaScript. MongoDB Atlas almacenará la información operacional y analítica. Apache Spark, mediante PySpark, ejecutará los procesos de ingestión, transformación y análisis de datos.

Esta decisión evita acoplar las reglas de negocio a una base de datos o motor de procesamiento y permite evolucionar cada componente de manera independiente.

El primer hito técnico sigue siendo el mismo: descargar una fuente real, inspeccionarla con PySpark, almacenarla en MongoDB Atlas, ejecutar una transformación y consultar el resultado desde Spring Boot.
