# SmartPrice

Plataforma de inteligencia de precios de alimentos del **Gran Mercado Mayorista de Lima (GMML)**.
Integra datos oficiales de MIDAGRI, los procesa con **Apache Spark** (Batch y Structured Streaming),
los guarda en **MongoDB Atlas** y los presenta en una aplicación web **Spring Boot + Thymeleaf**.
Además, compara el mercado mayorista con el IPC de Lima (INEI) y con un índice de supermercados (Qhawarina).

Todo el proyecto está en **Java 17**.

| Documento | Contenido |
|---|---|
| [documentation/informe_final.md](documentation/informe_final.md) | Informe del proyecto: problema, fuentes, arquitectura, resultados y conclusiones |
| [documentation/guia_presentacion.md](documentation/guia_presentacion.md) | Guion de la presentación y de la demostración en vivo |
| [documentation/diccionario_datos.md](documentation/diccionario_datos.md) | Colecciones de MongoDB y sus campos |
| [SmartPrice_Documentacion_Tecnica_v4.md](SmartPrice_Documentacion_Tecnica_v4.md) | Documento técnico detallado (diseño, decisiones, verificación de fuentes) |

---

## Arquitectura

```
 SISAP (.xls, histórico) ─┐
                          ├─► smartprice-jobs (Apache Spark, API Java) ──► MongoDB Atlas ──► smartprice-app (Spring Boot)
 Boletín PDF diario ──────┘     · Batch: carga, tendencias, alertas IQR     smartprice_db      · hexagonal: price, analytics,
                                · Streaming: boletines nuevos                                    alert, source, context
 Qhawarina + IPC (BCRP) ─────►  · Contexto: índice mayorista (Jevons)                          · Thymeleaf + ECharts
```

- **smartprice-jobs**: programa de consola que escribe en MongoDB.
- **smartprice-app**: servidor web que solo lee de MongoDB.
- Los dos módulos son procesos independientes y se comunican a través de la base de datos.

---

## Requisitos (Windows 11)

| Componente | Ubicación |
|---|---|
| JDK 17 (Temurin) | `C:\tools\jdk-17` |
| Hadoop `winutils.exe` + `hadoop.dll` (3.3.5) | `C:\tools\hadoop\bin` |
| Maven | No hace falta instalarlo: se usa el Maven Wrapper (`mvnw.cmd`) |
| MongoDB Atlas | Usuario `smartprice_app` con `readWrite@smartprice_db` |

Los scripts fijan `JAVA_HOME` y `HADOOP_HOME` **solo para su propio proceso**, así que el Java
predeterminado del equipo no cambia.

Spark 3.5 no funciona con Java 26: usa el JDK 17.

### Archivo `.env` (no se versiona)

```
MONGODB_URI=mongodb+srv://smartprice_app:CONTRASENA@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0
MONGODB_DATABASE=smartprice_db
```

Si PowerShell bloquea los scripts, ejecuta una vez:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

---

## Uso rápido

```powershell
# 1. Compilar y ejecutar las pruebas (37 pruebas)
.\jobs.ps1 build

# 2. Comprobar la conexión a MongoDB
.\jobs.ps1 check-mongo

# 3. Pipeline Batch (solo hace falta la primera vez o cuando cambian los datos)
.\jobs.ps1 sisap-to-csv        # une las exportaciones SISAP en un CSV
.\jobs.ps1 load-sisap          # carga en MongoDB con Spark (no duplica datos)
.\jobs.ps1 setup-mongo         # índices y resumen
.\jobs.ps1 batch-analytics     # tendencias, resúmenes y alertas -> MongoDB
.\jobs.ps1 load-context        # Qhawarina, IPC (INEI vía BCRP) e índice mayorista -> vista Contexto

# 4. Aplicación web -> http://localhost:8085
.\app.ps1
```

### Streaming de boletines diarios

```powershell
# Terminal 1: Spark se queda escuchando la carpeta data/streaming/incoming
.\jobs.ps1 stream-prices

# Terminal 2: cuando llega un boletín nuevo
.\jobs.ps1 ingest-pdf data\raw\midagri\gmml_pdf\<boletin>.pdf
```

Para procesar lo pendiente y terminar (útil en demostraciones): `.\jobs.ps1 stream-prices --once`.

### Todos los comandos de `jobs.ps1`

| Comando | Qué hace |
|---|---|
| `build` | Compila los dos módulos y ejecuta las pruebas |
| `sisap-to-csv [carpeta] [csv]` | Lee `data/raw/sisap/sisap_*.xls` y escribe `data/interim/sisap_prices.csv` |
| `load-sisap [csv]` | Carga el CSV en `wholesale_prices` y `source_files` (Spark) |
| `explore-sisap [csv]` | Exploración inicial: variación del precio promedio anual |
| `batch-analytics [k] [min%]` | Indicadores mensuales, resumen por producto y alertas IQR (por defecto k = 3 y 15 %) |
| `load-context` | Carga Qhawarina (`data/raw/qhawarina`) y el IPC (`data/raw/bcrp`) y calcula el índice mayorista de Jevons |
| `extract-pdf <pdf> [carpeta]` | Extrae la tabla de un boletín PDF a CSV |
| `ingest-pdf <pdf> [...]` | Extrae boletines y los deja en la carpeta del Streaming |
| `stream-prices [--once]` | Spark Structured Streaming de boletines nuevos |
| `check-mongo` | Ping y prueba de escritura en MongoDB |
| `setup-mongo` | Crea índices y muestra un resumen de la base |

---

## Estructura

```
├── pom.xml, mvnw.cmd, .mvn/     Proyecto Maven (padre) y wrapper
├── jobs.ps1, app.ps1            Scripts de ejecución
├── data/raw/sisap/              Exportaciones originales de SISAP (7 archivos)
├── data/raw/midagri/gmml_pdf/   Boletines PDF originales
├── data/raw/qhawarina, bcrp/    Índices de contexto (supermercados e IPC)
├── smartprice-jobs/             Apache Spark (Java): sisap, gmml, mongo, analytics, streaming
├── smartprice-app/              Spring Boot: price, analytics, alert, source, context, insight, compare (hexagonal)
└── documentation/               Informe, guía de presentación y diccionario de datos
```

## Fuente de los datos

MIDAGRI: Sistema de Abastecimiento y Precios (SISAP) y boletines diarios del Gran Mercado Mayorista
de Lima. La fuente primaria es EMMSA. Son **precios mayoristas promedio**, no precios al consumidor.

Contexto: índice de supermercados de Qhawarina (CC BY 4.0) e IPC de Lima Metropolitana (INEI),
obtenido de la API pública del BCRP.
