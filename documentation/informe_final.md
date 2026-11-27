# SmartPrice — Informe final del proyecto

**Plataforma de inteligencia de precios de alimentos del Gran Mercado Mayorista de Lima**

| | |
|---|---|
| Curso | Inteligencia de Negocio |
| Modalidad | Proyecto individual |
| Fecha | 26 de noviembre de 2026 |
| Tecnologías centrales | Apache Spark 3.5 (API Java), MongoDB Atlas, Java 17, Spring Boot 4.1, Thymeleaf |
| Datos | MIDAGRI: SISAP y boletines diarios del GMML (01-01-2024 a 26-11-2026) |

---

## Resumen

SmartPrice es una plataforma que reúne los precios mayoristas diarios de 59 alimentos del
Gran Mercado Mayorista de Lima, los procesa con Apache Spark y los presenta en una aplicación
web con tendencias, comparaciones y alertas de variaciones atípicas.

El proyecto empezó como un comparador de precios de supermercados. La **verificación de fuentes**
mostró que ese planteamiento no era viable con datos públicos: la fuente que se había supuesto
principal (el *Datero Agrario* de la Plataforma Nacional de Datos Abiertos) no contenía precios, y
el índice de supermercados disponible llevaba más de cinco meses sin actualizarse. El proyecto se
reorientó hacia una fuente oficial, diaria y verificable: **MIDAGRI**. Su sistema SISAP aporta el
histórico y sus boletines PDF diarios aportan los datos nuevos.

Resultado:

- **62 599 observaciones** (59 productos × 1 061 días continuos), cargadas sin duplicados en MongoDB Atlas.
- **Procesamiento Batch** con Spark: 2 065 indicadores mensuales, 59 resúmenes por producto y
  1 666 alertas detectadas con el método del rango intercuartílico (IQR).
- **Procesamiento Streaming** con Spark Structured Streaming: cada boletín diario se procesa al
  llegar, sin duplicar datos, con validación cruzada automática y alertas calculadas con las mismas
  reglas que el Batch.
- **Aplicación web** con arquitectura hexagonal, cuatro vistas y gráficos interactivos.
- **20 pruebas automatizadas** y varias validaciones con datos reales. Destaca una: los precios de
  SISAP y del boletín PDF coinciden en los 54 productos comunes.

El hallazgo principal ilustra el valor del sistema. Entre julio y noviembre de 2026 el precio de la
zanahoria pasó de S/ 0.75 a S/ 3.05 por kg, mientras el volumen que entraba al mercado caía de
359 a 260 toneladas diarias.

---

## 1. Problema

Los precios de los alimentos cambian a diario y la información para seguirlos existe, pero está
dispersa, en formatos difíciles de procesar (PDF, exportaciones web) y sin un historial consolidado.
Un comprador, un comerciante o un analista no tiene una forma simple de responder preguntas como:

- ¿Este producto está caro o barato frente a su historial?
- ¿Cuánto subió en el último mes y frente al año pasado?
- ¿La subida de hoy es normal para este producto o es atípica?
- ¿La subida se explica por una menor entrada de producto al mercado?

## 2. Objetivos

**Objetivo general.** Desarrollar una plataforma de análisis de precios de alimentos con Apache Spark
y MongoDB Atlas, que integre, almacene, procese y presente los precios diarios del Gran Mercado
Mayorista de Lima, para generar indicadores que apoyen decisiones de compra.

| Objetivo específico | Estado |
|---|---|
| Identificar y evaluar fuentes públicas de precios | Cumplido: 9 enlaces verificados y muestras inspeccionadas (sección 4) |
| Descargar y documentar los datos con su procedencia | Cumplido: 7 exportaciones SISAP y boletines PDF, con hash SHA-256 |
| Diseñar una base NoSQL para datos originales y procesados | Cumplido: 6 colecciones en MongoDB Atlas |
| Ingestión y transformación con Spark | Cumplido: lectores de SISAP y PDF, y carga con Spark |
| Pipeline Batch para el histórico | Cumplido: indicadores mensuales, resúmenes y alertas IQR |
| Pipeline Structured Streaming para datos nuevos | Cumplido y probado con un boletín real |
| Calcular indicadores, variaciones y tendencias | Cumplido |
| Demostración funcional de consulta y visualización | Cumplido: aplicación web con 4 vistas y API |
| Evaluar calidad de datos, trazabilidad y rendimiento | Cumplido: validación cruzada, registro de ejecuciones y pruebas |

## 3. Metodología

El trabajo siguió una regla establecida desde el principio: **validar los datos antes de construir
pantallas**. Ningún resultado se presenta como real si no se puede rastrear hasta un archivo de
origen, una fecha y una transformación documentada.

1. **Verificación de fuentes.** Se comprobó cada enlace y se descargaron e inspeccionaron muestras.
2. **Adquisición del histórico.** Exportación manual de SISAP en bloques compatibles con su límite de consulta.
3. **Ingestión y carga.** Lectores probados con pruebas unitarias y carga idempotente en MongoDB.
4. **Procesamiento Batch.** Indicadores y alertas sobre todo el histórico.
5. **Aplicación web.** Arquitectura hexagonal que solo lee los resultados.
6. **Procesamiento Streaming.** Datos nuevos con las mismas reglas que el Batch.
7. **Verificación.** Pruebas automatizadas, comparación entre fuentes y revisión visual de cada vista.

## 4. Fuentes de datos

### 4.1. Verificación (26-11-2026)

| Fuente | Hallazgo | Decisión |
|---|---|---|
| PNDA — *Datero Agrario* (MIDAGRI) | **No contiene precios**: registra el uso del servicio de consultas por celular. Último archivo: mayo de 2020 | Descartada |
| Qhawarina (índice de supermercados) | Solo índices agregados; licencia CC BY 4.0; **sin actualizar desde el 13-06-2026** | Contexto histórico (pendiente de integrar) |
| INEI (IPC) | Portales activos | Contexto macro (pendiente de integrar) |
| Catálogos de supermercados | Sin API pública autorizada | Fuera del alcance |
| **SISAP (MIDAGRI)** | Exporta precio por kg y volumen de todos los días. Certificado TLS vencido: descarga manual | **Fuente histórica principal** |
| **Boletín diario del GMML (MIDAGRI, PDF)** | Publicación diaria al día; PDF con texto extraíble | **Fuente de datos nuevos y de validación** |

Esta verificación cambió el rumbo del proyecto. El documento inicial dependía de una fuente que,
en realidad, no tenía precios.

### 4.2. Histórico SISAP

- SISAP rechaza las consultas de más de unas 9 000 combinaciones de producto y día. Por eso el
  histórico se exportó en **7 bloques de unos 5 meses**, siempre con los mismos 59 productos.
- Resultado: del 01-01-2024 al 26-11-2026, **1 061 días continuos, sin huecos ni solapamientos**,
  con 62 599 observaciones y solo 71 precios vacíos (0,1 %).
- Formato real: aunque la extensión es `.xls`, cada archivo es una tabla HTML en Latin-1.

### 4.3. Boletín diario en PDF

- 72 productos por boletín, con volumen, unidad comercial (cajón, saco, atado…), equivalencia en
  kg y precios de ayer, de hoy y promedio de 7 días.
- El precio está por unidad comercial. Por ejemplo, el ají rocoto a S/ 55.00 el cajón de 18 kg
  equivale a S/ 3.06 por kg.

### 4.4. Validación cruzada entre fuentes

Para el 06-11-2026, **los 54 productos presentes en las dos fuentes coinciden** en precio por kg y en
volumen. El sistema de Streaming repite esta comparación de forma automática con cada boletín.

## 5. Arquitectura

```
 FUENTES                    PROCESAMIENTO (smartprice-jobs)           ALMACENAMIENTO        PRESENTACIÓN (smartprice-app)
 ───────                    ───────────────────────────────           ──────────────        ─────────────────────────────
 SISAP (.xls) ──► SisapReader ──► CSV ──► Spark: load-sisap ─┐
                                          Spark: batch-analytics ─┼──► MongoDB Atlas ──► Spring Boot (solo lectura)
 Boletín PDF ──► GmmlPdfExtractor ──► carpeta vigilada ──►    │     smartprice_db       · arquitectura hexagonal
                                   Spark Structured Streaming ┘                          · Thymeleaf + ECharts
                                                                                         · API REST para los gráficos
```

### 5.1. Decisiones principales

| Decisión | Motivo |
|---|---|
| **Apache Spark con su API Java** (no PySpark) | El requisito es Apache Spark. Usar Java en todo el proyecto (jobs y web) simplifica el mantenimiento. Los prototipos en Python se migraron y se verificó que dan resultados idénticos |
| **Jobs y web como procesos separados** | Spark 3.5 (Jetty 9, `javax.servlet`) y Spring Boot 4 (`jakarta.servlet`) chocan si comparten proceso. Además, un job pesado no debe afectar a la web |
| **MongoDB como punto de encuentro** | Spark escribe y Spring lee. Si los jobs están detenidos, la web sigue funcionando con los últimos resultados |
| **Cálculos pesados en Spark, cálculos ligeros en Spring** | Spark recorre el histórico una vez y guarda los resultados. Spring solo aplica filtros y reglas de presentación |
| **Arquitectura hexagonal en la web** | El núcleo (dominio y casos de uso) no depende de Spring ni de MongoDB y se prueba con puertos falsos |
| **Escritura idempotente** | `_id` determinista (`fecha|mercado|producto`) y escritura *replace* con *upsert*: repetir una carga no duplica datos |

### 5.2. Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 17 (JDK Temurin) |
| Procesamiento | Apache Spark 3.5.6 (Batch y Structured Streaming) |
| Base de datos | MongoDB Atlas (M0, MongoDB 8.0) + MongoDB Spark Connector 10.7.0 |
| Lectura de fuentes | jsoup (SISAP) y Apache PDFBox 3 (boletines) |
| Aplicación web | Spring Boot 4.1.3, Spring Data MongoDB, Thymeleaf |
| Gráficos | Apache ECharts 5.6 (servido localmente) |
| Construcción y pruebas | Maven (Wrapper) y JUnit 5 |

### 5.3. Seguridad

- El usuario de base de datos `smartprice_app` solo tiene **`readWrite` sobre `smartprice_db`**.
  El cluster lo comparte otro proyecto, y este usuario no puede afectarlo.
- La cadena de conexión vive en `.env`, excluido del control de versiones, y los programas la
  muestran enmascarada (`smartprice_app:****@…`).
- El acceso de red de Atlas se limita a la IP de desarrollo.

## 6. Implementación

El módulo **`smartprice-jobs`** tiene 19 clases y unas 1 700 líneas. El módulo **`smartprice-app`**
tiene 41 clases, unas 1 500 líneas y 6 plantillas.

### 6.1. Ingestión

- **`SisapReader`** decodifica cada archivo en Latin-1, lee la tabla HTML y la convierte a formato
  largo: una fila por fecha y producto.
- **`GmmlPdfExtractor`** extrae el texto del PDF y lo interpreta de derecha a izquierda: 4 números
  finales, unidad, 4 volúmenes y nombre. También separa las notas al pie (`Papa Unica 2/`) y calcula
  el precio por kg.

### 6.2. Carga en MongoDB (`load-sisap`)

Spark lee el CSV con un esquema explícito y escribe en `wholesale_prices`. En `source_files` registra
cada archivo de origen con su hash, periodo y número de filas. Se probaron tres cargas seguidas y
siempre quedaron 62 599 documentos.

### 6.3. Procesamiento Batch (`batch-analytics`)

Spark lee el histórico **desde MongoDB** y calcula:

| Resultado | Documentos | Contenido |
|---|---|---|
| `PRICE_MONTHLY` | 2 065 | Promedio, mínimo y máximo mensual; volumen; variación frente al mes anterior y al mismo mes del año anterior |
| `PRODUCT_SUMMARY` | 59 | Último precio; promedios de 30 y 365 días; variaciones; rango histórico; correlación precio-volumen |
| `ALERT_BASELINE` | 59 | Rango normal de variación diaria de cada producto (Q1, Q3), reutilizado por el Streaming |
| Alertas IQR | 1 666 | Variaciones diarias atípicas con severidad y contexto de volumen |

**Método de alertas (IQR):**

1. Variación diaria de cada producto frente al día anterior con dato.
2. Q1 y Q3 de esas variaciones e IQR = Q3 − Q1.
3. Hay alerta si la variación sale de **[Q1 − 3·IQR, Q3 + 3·IQR]** y además supera el **15 %**.
4. La severidad depende de cuántos IQR se aleja del límite: alta (3 o más), media (de 1 a 3) o baja.

**Calibración.** Se evaluaron varios umbrales sobre el histórico completo:

| k | Variación mínima | Alertas | Por mes |
|---|---|---|---|
| 3 | 10 % | 2 361 | ~67 |
| **3** | **15 %** | **1 666** | **~47** |
| 3 | 20 % | 1 186 | ~34 |
| 5 | 15 % | 760 | ~22 |

Se eligió k = 3 con un mínimo del 15 %. El umbral mínimo hace falta porque 7 productos tienen
IQR = 0: su precio casi nunca cambia y, sin ese umbral, cualquier movimiento sería una alerta.

### 6.4. Procesamiento Streaming (`ingest-pdf` + `stream-prices`)

`ingest-pdf` extrae el boletín y lo mueve de forma atómica a la carpeta que vigila Spark. Por cada
micro-lote, Spark:

1. Valida las filas y las limita al catálogo de 59 productos.
2. Separa las observaciones nuevas de las ya existentes. No duplica, y en las existentes compara el
   precio con el guardado (**validación cruzada**).
3. Guarda las nuevas con la fuente `GMML_PDF`.
4. Calcula sus alertas con **la misma clase de reglas que el Batch** (`IqrAlertRules`) y el rango
   normal guardado.
5. Registra la ejecución en `processing_logs`, que la web muestra.

El *checkpoint* de Spark garantiza que cada archivo se procese una sola vez, aunque el proceso se reinicie.

### 6.5. Aplicación web (`smartprice-app`)

| Vista | Contenido |
|---|---|
| **Dashboard** | Indicadores generales, mayores subidas y bajadas de 30 días, alertas recientes con la etiqueta "Menor abastecimiento" |
| **Productos** | Catálogo de 59 productos con buscador |
| **Detalle de producto** | Precio y volumen diarios en gráficos enlazados, rango histórico, tabla mensual y alertas del producto |
| **Alertas** | Filtros por severidad, tipo, producto y periodo |
| **Fuentes** | Archivos de origen con su hash, ejecuciones del Streaming y notas de calidad de datos |

**Arquitectura hexagonal.** Cada funcionalidad (`price`, `analytics`, `alert`, `source`) tiene
`domain`, `application` (puertos de entrada y salida y servicios) e `infrastructure` (controladores
y adaptadores de MongoDB). Los servicios no llevan anotaciones de Spring: se ensamblan en una clase
de configuración.

Ejemplo de **regla de dominio**: una subida de precio acompañada de una caída del volumen de al menos
el 30 % frente a su promedio de 30 días se marca como explicada por menor abastecimiento.

**Visualización.** El precio y el volumen se muestran en **dos gráficos que comparten el eje de
fechas, el cursor y el zoom**, en lugar de un gráfico con dos ejes Y, que suele llevar a lecturas
erróneas. La paleta se validó para personas con daltonismo en modo claro y oscuro.

## 7. Resultados y hallazgos

### 7.1. Caso destacado: la zanahoria

| Mes | Precio promedio (S/ por kg) | vs mes anterior | vs mismo mes 2025 | Volumen promedio (t/día) |
|---|---|---|---|---|
| Jul-2026 | 0.75 | −1.5 % | −44.7 % | 359 |
| Ago-2026 | 0.87 | +14.9 % | −27.9 % | 343 |
| Sep-2026 | 1.32 | +52.7 % | +5.0 % | 318 |
| Oct-2026 | 1.81 | +36.9 % | +62.8 % | 297 |
| Nov-2026 | 3.05 | +68.2 % | +113.6 % | 260 |

- El precio se multiplicó por cuatro en cuatro meses, mientras el volumen ingresado caía un 28 %.
- La correlación diaria entre precio y volumen es de −0.32: cuando entra menos producto, el precio
  tiende a subir.
- El 23-11-2026 el sistema generó una alerta alta. El precio subió un 55 % (de S/ 2.73 a S/ 4.23)
  el mismo día que entraron **45 toneladas, frente a un promedio de 275**.

### 7.2. Movimientos de los últimos 30 días (al 26-11-2026)

| Mayores subidas | Variación | Mayores bajadas | Variación |
|---|---|---|---|
| Haba verde serrana | +105.8 % | Ají rocoto | −31.9 % |
| Lechuga americana | +80.3 % | Pimiento morrón | −19.6 % |
| Beterraga | +66.4 % | Culantro | −16.1 % |
| Zanahoria | +64.2 % | Col china | −11.8 % |

37 de los 59 productos están al menos un 5 % más caros que en los 30 días anteriores, y 9 están más
baratos. En ese periodo hubo 37 alertas, 12 de ellas de severidad alta.

### 7.3. Tendencia 2024 → 2026 (promedio anual)

- Mayores subidas: apio (+113 %), yuca amarilla (+100 %) y zanahoria (+86 %).
- Mayores bajadas: ajo criollo (−54 %), ajo morado (−47 %) y kion (−38 %).

### 7.4. Calidad de datos

- En SISAP, **"Papa Blanca" tiene el mismo precio que "Papa Yungay" en 1 059 de 1 061 días**, pero
  un volumen distinto. Probablemente es una categoría agregada que toma la Yungay como referencia.
  No deben presentarse como dos evidencias independientes.
- Solo hay 71 precios vacíos en el histórico (0,1 %), casi todos en 2024.

## 8. Validación y pruebas

| Verificación | Resultado |
|---|---|
| Pruebas unitarias | **20 pruebas**, todas correctas: lectores SISAP y PDF (8) y núcleo de la aplicación con puertos falsos, sin MongoDB ni Spring (12) |
| SISAP vs boletín PDF (06-11-2026) | 54 de 54 productos comunes coinciden |
| Migración de Python a Java | Lector SISAP: 62 599 filas idénticas. Extractor PDF: 72 productos idénticos en sus 13 campos. Mismas variaciones anuales con Spark |
| Idempotencia | 3 cargas seguidas → 62 599 documentos; 2 ejecuciones del Batch → mismos conteos |
| Calidad de las alertas | Las alertas de Spark coinciden con una réplica independiente del cálculo (2 361 frente a 2 360; la diferencia se debe al percentil aproximado) |
| Streaming A: boletín con datos ya cargados | 0 duplicados; 54 de 54 precios coinciden |
| Streaming B: día quitado temporalmente y reconstruido desde el PDF | Misma alerta que el Batch (Zanahoria +33.7 %, media). Después se restauraron los datos |
| Streaming C: modo continuo | Boletín detectado y procesado en unos 9 segundos |
| Aplicación web | Las 4 vistas y los 59 detalles de producto responden sin errores. Las páginas tardan entre 0.1 y 0.6 s |
| Revisión visual | Capturas en modo claro y oscuro. Sirvieron para corregir cuatro defectos que las pruebas no detectaban |

## 9. Limitaciones

1. **Precios mayoristas, no minoristas.** No representan el precio que paga el consumidor final.
2. **Un solo mercado.** Solo el Gran Mercado Mayorista de Lima, con 59 productos.
3. **Descarga manual de SISAP.** El certificado TLS vencido impide automatizar la descarga de forma segura.
4. **Sin datos de supermercados.** No existe una fuente pública autorizada con precios por producto.
5. **Streaming sin fechas nuevas todavía.** El histórico llega hasta el día del informe; los datos
   realmente nuevos empiezan con el boletín del 27-11-2026.
6. **Spark está sobredimensionado para este volumen.** 62 599 filas caben en memoria. Spark se
   justifica por el Streaming, por usar un mismo pipeline para el histórico y los datos nuevos, y
   porque escala sin cambiar el código.
7. **Formato del PDF.** El extractor se probó con un formato de boletín. Si MIDAGRI cambia el
   diseño, habrá que ajustarlo.

## 10. Trabajo futuro

- Integrar como contexto el índice de supermercados de Qhawarina y el IPC del INEI.
- Añadir otros mercados de SISAP (Mercado de Frutas N.º 2, Mercado de Productores, otras ciudades).
- Descargar automáticamente los boletines diarios desde gob.pe.
- Canasta de compra y comparación con una muestra de precios minoristas.
- Notificaciones (correo o mensajería) para las alertas de severidad alta.

## 11. Conclusiones

1. **La validación de las fuentes fue la decisión más importante del proyecto.** Sin ella, el
   sistema se habría construido sobre un dataset que no contenía precios.
2. **Los datos públicos de MIDAGRI son suficientes para un análisis útil y verificable.** Son
   continuos, diarios y coinciden entre sus dos canales de publicación.
3. **Apache Spark cubre los dos modos de procesamiento con una sola lógica.** El Batch y el
   Streaming comparten las mismas reglas de alerta, y el Streaming reprodujo exactamente la alerta
   del Batch.
4. **Separar el procesamiento de la presentación da robustez.** Spark escribe, Spring Boot lee, y
   cada parte se prueba y se despliega por separado.
5. **El sistema detecta patrones con sentido económico**, como la relación inversa entre
   abastecimiento y precio. Esa información sirve para anticipar decisiones de compra.

---

## Anexo A. Comandos

```powershell
.\jobs.ps1 build                 # compilar y probar
.\jobs.ps1 sisap-to-csv          # SISAP -> CSV
.\jobs.ps1 load-sisap            # CSV -> MongoDB (Spark)
.\jobs.ps1 batch-analytics       # indicadores y alertas (Spark)
.\jobs.ps1 ingest-pdf <pdf>      # boletín -> carpeta del Streaming
.\jobs.ps1 stream-prices         # Spark Structured Streaming
.\app.ps1                        # web en http://localhost:8085
```

## Anexo B. Referencias

1. MIDAGRI — Precios de alimentos: https://www.gob.pe/institucion/midagri/informes-publicaciones/1211-precios-de-alimentos
2. MIDAGRI — Reporte de ingreso y precios en el Gran Mercado Mayorista de Lima: https://www.gob.pe/institucion/midagri/colecciones/335-reporte-de-ingreso-y-precios-en-el-gran-mercado-mayorista-de-lima
3. SISAP — Sistema de Abastecimiento y Precios: https://sistemas.midagri.gob.pe/sisap/portal2/mayorista/
4. Qhawarina — Datos: https://qhawarina.pe/datos
5. INEI — Índices de precios: https://www.inei.gob.pe/estadisticas/indice-tematico/price-indexes/
6. Apache Spark 3.5.6: https://spark.apache.org/docs/3.5.6/
7. Spark Structured Streaming: https://spark.apache.org/docs/3.5.6/structured-streaming-programming-guide.html
8. MongoDB Spark Connector: https://www.mongodb.com/docs/spark-connector/v10.x/
9. Spring Boot: https://spring.io/projects/spring-boot
10. Apache ECharts: https://echarts.apache.org/

El detalle técnico completo (verificación de fuentes, entorno, decisiones y pruebas) está en
`SmartPrice_Documentacion_Tecnica_v4.md`.
