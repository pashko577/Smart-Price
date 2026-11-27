# SmartPrice — Diccionario de datos

Base de datos: **`smartprice_db`** (MongoDB Atlas). Usuario de la aplicación: `smartprice_app`,
con `readWrite` solo sobre esta base.

| Colección | Escribe | Lee | Documentos (26-11-2026) |
|---|---|---|---|
| `wholesale_prices` | `load-sisap`, `stream-prices` | Batch, Streaming, web | 62 599 |
| `source_files` | `load-sisap` | web | 7 |
| `price_analytics` | `batch-analytics` | Streaming, web | 2 065 `PRICE_MONTHLY` + 59 `PRODUCT_SUMMARY` + 59 `ALERT_BASELINE` |
| `price_alerts` | `batch-analytics`, `stream-prices` | web | 1 666 |
| `processing_logs` | `stream-prices` | web | 1 por micro-lote |
| `price_indexes` | `load-context` | web (vista Contexto) | 889 (8 series de índices) |

Convenciones:

- **Fechas de observación** (`observed_at`, `period.from`…): tipo `Date` a las **00:00 UTC** del día
  observado. Spark trabaja en UTC para que la zona horaria de Lima no desplace las fechas.
- **Precios**: soles (PEN) por kg, por litro o por unidad, según el producto (`unit_basis = "kg|l|unit"`).
  Así los publica SISAP.
- **Variaciones** (`*_pct`): porcentaje con 2 decimales. Son nulas si no hay referencia.
- **`_id` deterministas**: repetir un proceso reemplaza los documentos en vez de duplicarlos.
- **`market_code`**: `GMML` (Gran Mercado Mayorista de Lima).

---

## `wholesale_prices` — observaciones diarias

`_id`: `"<AAAA-MM-DD>|<market_code>|<product_name>"`, por ejemplo `"2026-11-06|GMML|Aji Rocoto"`.

| Campo | Tipo | Descripción |
|---|---|---|
| `observed_at` | Date | Día observado (00:00 UTC) |
| `market_code` | string | `GMML` |
| `market_name` | string | `Gran Mercado Mayorista de Lima` |
| `product_name` | string | Nombre del producto según SISAP (59 productos) |
| `price.avg` | double \| null | Precio promedio del día (S/ por kg, litro o unidad) |
| `price.currency` | string | `PEN` |
| `price.unit_basis` | string | `kg\|l\|unit` |
| `volume_t` | double \| null | Toneladas ingresadas al mercado ese día |
| `source.system` | string | `SISAP` (histórico) o `GMML_PDF` (boletín procesado por Streaming) |
| `source.file` | string | Archivo de origen (por ejemplo `sisap_2026-07_2026-11.xls`) |
| `source.reader_version` | string | Versión del lector o extractor (`sisap_v1`, `gmml_v1`) |
| `ingested_at` | Date | Momento de la carga |

Índices: `product_date` (`product_name` ↑, `observed_at` ↓), `date` (`observed_at` ↓) y
`market_date` (`market_code` ↑, `observed_at` ↓).

## `source_files` — archivos de origen (trazabilidad)

`_id`: nombre del archivo (por ejemplo `sisap_2024-01_2024-05.xls`).

| Campo | Tipo | Descripción |
|---|---|---|
| `system` | string | `SISAP` |
| `market_code` | string | `GMML` |
| `sha256` | string | Hash SHA-256 del archivo original |
| `period_label` | string | Periodo que figura en el archivo (`01/01/2024 - 31/05/2024`) |
| `first_date`, `last_date` | string (AAAA-MM-DD) | Primera y última fecha con datos |
| `rows` | long | Observaciones aportadas |
| `reader_version` | string | `sisap_v1` |
| `acquisition_method` | string | `manual_export` |
| `loaded_at` | Date | Momento de la carga |

## `price_analytics` — resultados del Batch

Un mismo esquema base, distinguido por `analysis_type`. Todos llevan `market_code`,
`product_name`, `job` (`batch_analytics`), `job_version` y `generated_at`.

### `PRICE_MONTHLY` — un documento por producto y mes

`_id`: `"PRICE_MONTHLY|GMML|<producto>|<AAAA-MM>"`.

| Campo | Tipo | Descripción |
|---|---|---|
| `period.label` | string | `AAAA-MM` |
| `period.from`, `period.to` | Date | Primer y último día del mes |
| `price.avg`, `price.min`, `price.max` | double | Promedio, mínimo y máximo de los precios diarios del mes |
| `volume.avg_t`, `volume.total_t` | double | Volumen diario promedio y total del mes (t) |
| `var_pct_vs_prev_month` | double \| null | Variación del promedio frente al mes anterior |
| `var_pct_vs_prev_year` | double \| null | Variación frente al mismo mes del año anterior |
| `days_observed` | long | Días con precio en el mes |
| `data_quality_status` | string | `VALIDATED` (15 días o más con dato) o `PARTIAL_MONTH` |

### `PRODUCT_SUMMARY` — un documento por producto

`_id`: `"PRODUCT_SUMMARY|GMML|<producto>"`.

| Campo | Tipo | Descripción |
|---|---|---|
| `period.from`, `period.to` | Date | Primera y última observación |
| `price.last` | double | Último precio informado |
| `price.avg_30d`, `price.avg_365d` | double | Promedio de los últimos 30 y 365 días |
| `price.min_all`, `price.max_all` | double | Mínimo y máximo históricos |
| `last_volume_t` | double \| null | Volumen del último día |
| `var_pct_30d` | double \| null | Promedio de 30 días frente a los 30 días anteriores |
| `var_pct_yoy` | double \| null | Promedio de 30 días frente al mismo periodo del año anterior |
| `corr_price_volume` | double \| null | Correlación de Pearson entre precio y volumen diarios |
| `observations` | long | Días con precio |
| `data_quality_status` | string | `VALIDATED` |

### `ALERT_BASELINE` — rango normal por producto

`_id`: `"ALERT_BASELINE|GMML|<producto>"`. El Streaming lo usa para calcular las alertas de los días nuevos.

| Campo | Tipo | Descripción |
|---|---|---|
| `q1`, `q3` | double | Cuartiles 1 y 3 de la variación diaria (%) del producto |
| `k` | double | Multiplicador del IQR (3) |
| `min_abs_change_pct` | double | Variación mínima para generar una alerta (15) |
| `method` | string | `IQR_DAILY_CHANGE` |

## `price_alerts` — variaciones diarias atípicas

`_id`: `"IQR_DAILY_CHANGE|GMML|<producto>|<AAAA-MM-DD>"`. Es el mismo en Batch y Streaming.

| Campo | Tipo | Descripción |
|---|---|---|
| `alert_type` | string | `PRICE_SPIKE` (subida) o `PRICE_DROP` (bajada) |
| `method` | string | `IQR_DAILY_CHANGE` |
| `observed_at` | Date | Día de la alerta |
| `previous_observed_at` | Date | Día anterior con dato |
| `price.current`, `price.previous` | double | Precio del día y del día anterior |
| `change_pct` | double | Variación (%) |
| `bounds.q1`, `bounds.q3` | double | Cuartiles de la variación diaria del producto |
| `bounds.lower`, `bounds.upper` | double | Rango normal: Q1 − k·IQR y Q3 + k·IQR |
| `bounds.k`, `bounds.min_abs_change_pct` | double | Parámetros usados |
| `volume.current_t` | double \| null | Volumen del día |
| `volume.avg_30d_t` | double \| null | Volumen promedio de las 30 observaciones anteriores |
| `severity` | string | `HIGH` (3 IQR o más fuera del límite), `MEDIUM` (de 1 a 3) o `LOW` |
| `status` | string | `OPEN` |
| `job` | string | `batch_analytics` o `streaming_prices` |

Índices: `date` (`observed_at` ↓) y `product_date` (`product_name` ↑, `observed_at` ↓).

Regla derivada en la aplicación (no se guarda): una alerta `PRICE_SPIKE` con un volumen al menos un
30 % inferior a su promedio de 30 días se muestra como **"Menor abastecimiento"**.

## `processing_logs` — ejecuciones del Streaming

`_id`: `"streaming_prices|<epoch_ms>|<batch_id>"`.

| Campo | Tipo | Descripción |
|---|---|---|
| `job`, `job_version` | string | `streaming_prices`, `stream_v1` |
| `batch_id` | long | Número de micro-lote de Spark |
| `files` | array | Boletines procesados en el micro-lote |
| `rows_read` | long | Filas leídas del CSV |
| `rows_valid` | long | Filas válidas del catálogo |
| `products_outside_catalog` | array | Productos del boletín que no forman parte del catálogo |
| `rows_new` | long | Observaciones nuevas guardadas |
| `rows_existing` | long | Observaciones que ya existían (no se sobrescriben) |
| `existing_price_matches` | long | Existentes cuyo precio coincide con el guardado (validación cruzada) |
| `existing_price_mismatches` | long | Existentes con precio distinto |
| `mismatch_examples` | array | Hasta 10 ejemplos de diferencias |
| `alerts_generated` | long | Alertas generadas |
| `started_at`, `finished_at` | Date | Inicio y fin |
| `duration_ms` | long | Duración |
| `status` | string | `SUCCEEDED` |

---

## `price_indexes` — índices de contexto

`_id`: `"<source>|<indicator>|<AAAA-MM-DD>"`, por ejemplo `"BCRP_INEI|PN38705PM|2026-10-01"`.

| Campo | Tipo | Descripción |
|---|---|---|
| `source` | string | `SMARTPRICE` (calculado aquí), `BCRP_INEI` o `QHAWARINA` |
| `indicator` | string | `gmml_jevons_monthly`, `gmml_jevons_daily`, `PN38705PM` (IPC Lima), `PN38706PM` (IPC sin alimentos y bebidas), `index_all`, `index_food`, `index_verduras`, `index_frutas` |
| `label` | string | Nombre para mostrar |
| `frequency` | string | `DAILY` o `MONTHLY` (fecha = primer día del mes) |
| `date` | Date | Fecha (00:00 UTC) |
| `value` | double | Valor del índice en su base original |
| `base_label` | string | Base original (`ene. 2024 = 100`, `dic. 2021 = 100`, `10-02-2026 = 100`) |
| `interpolated` | boolean | Qhawarina: el día fue interpolado por la fuente |
| `source_file` | string | Archivo de origen (o `wholesale_prices` para los índices calculados) |

El índice `SMARTPRICE` es un **índice de Jevons**: 100 × la media geométrica de (precio / precio base)
de los productos con precio en la fecha base. La aplicación cambia la base de todas las series a
una fecha común antes de compararlas.

`source_files` también registra los archivos de contexto (`market_code = CONTEXT`, sistema
`QHAWARINA` o `BCRP_INEI`), con su SHA-256.

## Archivos intermedios (no versionados)

| Archivo | Columnas |
|---|---|
| `data/interim/sisap_prices.csv` | `observed_at, product_name, price_avg_per_unit, volume_t, market, source_file, source_sha256, period_label, reader_version` |
| `data/interim/gmml_csv/*.csv` y `data/streaming/incoming/*.csv` | `observed_at, product_name, footnote, volume_t_prev, volume_t, volume_t_avg7, volume_t_avg4_wd, unit, kg_equivalent, price_prev, price, price_avg7, price_per_kg, source_file, source_sha256, page, extractor_version` |
