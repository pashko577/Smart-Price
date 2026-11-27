# SmartPrice — Guía de presentación

Duración prevista: **15 minutos** de presentación + preguntas.
Si solo tienes **10 minutos**, sigue las indicaciones "Versión corta" de cada bloque.

| Bloque | Tiempo | Diapositivas |
|---|---|---|
| 1. Problema y giro del proyecto | 3 min | 1–4 |
| 2. Arquitectura y tecnologías | 3 min | 5–7 |
| 3. Demostración en vivo | 6 min | 8 (pantalla compartida) |
| 4. Resultados, validación y cierre | 3 min | 9–12 |

---

## Antes de empezar (checklist, 15 minutos antes)

1. Abre PowerShell en la carpeta del proyecto.
2. Comprueba la conexión a Atlas:
   ```powershell
   .\jobs.ps1 check-mongo
   ```
   Debe decir "Ping OK" y "Escritura OK". Si falla por la IP, entra en Atlas → *Database & Network
   Access* → *IP Access List* → *Add Current IP Address*. Es lo más probable si presentas desde otra
   red, como la de la universidad.
3. Arranca la web en una **segunda** ventana de PowerShell y déjala abierta:
   ```powershell
   .\app.ps1
   ```
4. Abre en el navegador, en pestañas y en este orden:
   - http://localhost:8085/ (Dashboard)
   - http://localhost:8085/product?name=Zanahoria
   - http://localhost:8085/alerts
   - http://localhost:8085/context
   - http://localhost:8085/sources
5. Prepara una **tercera** ventana de PowerShell para el Streaming (bloque 3, paso 5).
6. Pon el navegador al 100–110 % de zoom y cierra todo lo demás.
7. **Plan B:** ten a mano capturas de las cuatro páginas por si falla internet o Atlas.

---

## Bloque 1 — Problema y giro del proyecto (3 min)

### Diapositiva 1 · Título
**SmartPrice: inteligencia de precios de alimentos del Gran Mercado Mayorista de Lima**
Apache Spark · MongoDB Atlas · Java Spring Boot

### Diapositiva 2 · El problema
- Los precios de los alimentos cambian a diario.
- La información existe, pero está dispersa, en PDF o exportaciones web y sin historial consolidado.
- Preguntas sin respuesta fácil: ¿está caro frente a su historial? ¿La subida de hoy es normal?
  ¿Se explica porque entró menos producto?

> **Qué decir:** "Quería una herramienta que respondiera esas preguntas con datos oficiales y
> verificables, no con estimaciones."

### Diapositiva 3 · El plan original no era viable *(la diapositiva más importante del bloque)*

| Fuente supuesta | Lo que encontré al verificarla |
|---|---|
| Datero Agrario (datos abiertos) | No tiene precios: registra el uso de un servicio por celular |
| Índice de supermercados (Qhawarina) | Solo índices agregados, sin actualizar desde junio de 2026 |
| Catálogos de supermercados | Sin API pública autorizada |

> **Qué decir:** "Antes de escribir código descargué y abrí cada fuente. La fuente principal del
> plan original no contenía precios. Si no lo hubiera comprobado, habría construido todo sobre un
> dataset vacío."

### Diapositiva 4 · La fuente correcta: MIDAGRI
- **SISAP** para el histórico: 62 599 precios diarios, 59 productos, del 01-01-2024 al 26-11-2026, sin huecos.
- **Boletín PDF diario** para los datos nuevos.
- **Las dos fuentes coinciden**: 54 de 54 productos comunes con el mismo precio el 06-11-2026.

> **Versión corta:** junta las diapositivas 3 y 4 en una sola: "plan original → verificación → MIDAGRI".

---

## Bloque 2 — Arquitectura y tecnologías (3 min)

### Diapositiva 5 · Arquitectura

```
SISAP (.xls) ─┐                                   ┌─► MongoDB Atlas ─► Spring Boot + Thymeleaf
              ├─► Apache Spark (Java) ────────────┤      (escribe)      (lee y presenta)
Boletín PDF ──┘   Batch + Structured Streaming
```

> **Qué decir:** "Son dos programas independientes. Spark hace el trabajo pesado y escribe en
> MongoDB. La aplicación web solo lee los resultados ya calculados. Si Spark está apagado, la web
> sigue funcionando."

### Diapositiva 6 · Decisiones técnicas

| Decisión | Por qué |
|---|---|
| Spark con API Java, no PySpark | El requisito es Apache Spark y así todo el proyecto queda en Java. Migré el prototipo de Python y verifiqué que da resultados idénticos |
| Spark separado de Spring Boot | Sus librerías chocan (`javax` frente a `jakarta`) y un job pesado no debe tumbar la web |
| Arquitectura hexagonal | El núcleo del negocio no depende de Spring ni de MongoDB: lo pruebo con puertos falsos |
| Cargas sin duplicados | Cada precio tiene un `_id` fijo (`fecha\|mercado\|producto`) |

### Diapositiva 7 · Batch y Streaming
- **Batch:** recorre todo el histórico y calcula 2 065 indicadores mensuales, 59 resúmenes y 1 666 alertas.
- **Streaming:** procesa cada boletín diario al llegar, sin recalcular todo, en segundos.
- **Las dos usan la misma clase de reglas de alerta**, así que una alerta sale igual la calcule quien la calcule.

> **Versión corta:** muestra solo la diapositiva 5 y di las decisiones en voz alta.

---

## Bloque 3 — Demostración en vivo (6 min)

### Paso 1 · Dashboard (1 min) — pestaña 1
- Señala los indicadores: **59 productos, 1 061 días y 62 599 observaciones**.
- Señala "Mayores subidas": haba verde +106 %, lechuga +80 %, zanahoria +64 %.
- Baja a "Alertas recientes" y muestra la fila de la **Zanahoria del 23/11** con la etiqueta
  **"Menor abastecimiento"**.

> **Qué decir:** "Esta alerta no es solo un número. El sistema detectó que el precio subió un 55 %
> el mismo día que entró un 84 % menos de zanahoria que de costumbre."

### Paso 2 · Detalle de la zanahoria (2 min) — pestaña 2
- Indicadores de arriba: último precio S/ 3.04 y **+96 % frente a hace un año**.
- Gráfico de precio: la subida desde septiembre.
- **Pasa el cursor** sobre el gráfico: la línea vertical se mueve a la vez en los dos gráficos.
- Gráfico de volumen: la entrada de producto baja mientras el precio sube.
- Pulsa **"Todo"** para ver los casi 3 años.
- Señala la correlación precio-volumen (−0.32) y la tabla mensual: de S/ 0.75 en julio a S/ 3.05 en noviembre.

> **Qué decir:** "Precio y volumen están en dos gráficos que comparten las fechas, y no en uno con
> dos ejes, para no inducir a error. La tabla de abajo tiene los mismos datos en números."

### Paso 3 · Alertas (1 min) — pestaña 3
- Cambia "Severidad mínima" a **Alta**, "Periodo" a **Último año** y pulsa **Aplicar**.
- Señala la columna "Rango normal": cada producto tiene su propio rango.

> **Qué decir:** "No uso un umbral fijo. Un 20 % puede ser normal para un producto volátil y
> anormal para uno estable. Por eso el rango se calcula por producto con el método IQR."

### Paso 4 · Contexto (1 min) — pestaña 4
- Primer gráfico: desde enero de 2024, el mayorista subió **+39.8 %** frente al **+8.5 %** del IPC de Lima.
- Señala que el IPC total y el IPC sin alimentos van casi pegados: en la canasta completa, los
  alimentos no fueron los que empujaron la inflación.
- Segundo gráfico: entre febrero y junio de 2026, el mayorista subió **+11.5 %** y los
  supermercados estuvieron estables (**−1.3 %**).

> **Qué decir:** "El mercado mayorista adelanta y amplifica lo que el consumidor ve después.
> Los supermercados amortiguan esa volatilidad. Todas las series tienen la misma base = 100 y se
> comparan en el mismo periodo."

### Paso 5 · Fuentes y trazabilidad (1 min) — pestaña 5
- Los 7 archivos de origen con su hash: "cada precio se puede rastrear hasta su archivo".
- La tabla "Boletines procesados por Streaming", con **"54 de 54 coinciden"**.
- Las notas de calidad de datos (Papa Blanca = Papa Yungay).

### Paso 6 · Streaming en vivo (1 min, opcional) — tercera ventana
```powershell
.\jobs.ps1 stream-prices
```
Espera el mensaje "Streaming activo". En otra ventana:
```powershell
.\jobs.ps1 ingest-pdf data\raw\midagri\gmml_pdf\<boletin_del_dia>.pdf
```
- En unos 10 segundos aparece "[lote N] … nuevas …, ya existían …".
- Recarga la pestaña **Fuentes**: aparece la nueva ejecución.

> **Importante:** usa un **boletín nuevo**, de una fecha posterior al 26-11-2026, descargado de
> gob.pe → MIDAGRI → "Reporte de ingreso y precios en el Gran Mercado Mayorista de Lima". Spark
> procesa cada nombre de archivo una sola vez. Si repites el del 06-11, cópialo antes con otro
> nombre: saldrá "nuevas 0, ya existían 54", lo cual también demuestra que no duplica.
> Detén el Streaming con **Ctrl+C**.

> **Versión corta:** omite los pasos 4 y 6. Menciona el contexto en la diapositiva 9 y las pruebas del Streaming en la diapositiva 10.

---

## Bloque 4 — Resultados, validación y cierre (3 min)

### Diapositiva 9 · Hallazgo: la zanahoria

| | Julio 2026 | Noviembre 2026 |
|---|---|---|
| Precio promedio | S/ 0.75 por kg | S/ 3.05 por kg (×4) |
| Volumen promedio diario | 359 t | 260 t (−28 %) |

> **Qué decir:** "Es un patrón con sentido económico: menos oferta, precio más alto. El sistema
> lo detecta solo y lo explica con el dato de volumen."

Añade una línea de contexto: "En el conjunto, el mayorista subió un 39.8 % desde 2024, frente al
8.5 % del IPC de Lima."

### Diapositiva 10 · Cómo sé que funciona

| Verificación | Resultado |
|---|---|
| 24 pruebas automatizadas | Todas correctas |
| Índice mayorista recalculado de forma independiente | 8 de 8 valores coinciden |
| SISAP frente al PDF | 54 de 54 coinciden |
| Migración Python → Java | Resultados idénticos (62 599 filas) |
| Streaming frente a Batch | Misma alerta reproducida desde el PDF |
| Cargas repetidas | 0 duplicados |

### Diapositiva 11 · Limitaciones (dilas tú antes de que te pregunten)
- Precios **mayoristas**, no de supermercado.
- Un solo mercado y 59 productos.
- SISAP se descarga a mano porque su certificado está vencido.
- Con 62 599 filas, Spark está sobredimensionado. Se justifica por el Streaming, por usar un mismo
  pipeline y porque escala sin cambiar el código.

### Diapositiva 12 · Conclusiones
1. Verificar las fuentes cambió el proyecto y lo hizo viable.
2. Los datos oficiales de MIDAGRI son suficientes, continuos y coinciden entre sí.
3. Spark cubre el Batch y el Streaming con la misma lógica.
4. Separar el procesamiento de la presentación da robustez.
5. El sistema detecta patrones útiles para decidir compras.

---

## Preguntas probables y respuestas

**¿Por qué Spark si son solo 62 mil filas?**
Para este volumen, pandas o SQL bastarían, y lo digo en el informe. Spark se justifica por tres
motivos: (1) Structured Streaming para los boletines diarios, (2) el mismo pipeline sirve para el
histórico y para los datos nuevos, y (3) si se suman los demás mercados del país o más años, el
código no cambia.

**¿Por qué MongoDB y no una base relacional?**
Los documentos de origen tienen estructuras distintas (SISAP, PDF, indicadores, alertas, registros
de ejecución) y anidadas (precio, volumen, fuente, límites del IQR). MongoDB los guarda tal como se
consultan, y el conector de Spark lee y escribe directamente.

**¿Cómo evitas datos duplicados?**
Cada observación tiene un `_id` fijo (`2026-11-06|GMML|Zanahoria`), y la escritura reemplaza en
lugar de insertar. Lo probé con tres cargas seguidas: siempre 62 599 documentos. En el Streaming,
además, el *checkpoint* evita reprocesar archivos.

**¿Qué es el IQR y por qué k = 3 y 15 %?**
IQR es el rango intercuartílico (Q3 − Q1) de las variaciones diarias de cada producto. Si la
variación de un día sale de [Q1 − 3·IQR, Q3 + 3·IQR] y supera el 15 %, hay alerta. Probé varios
umbrales sobre el histórico. Con un 10 % salían unas 67 alertas al mes, demasiadas. Con un 15 %
salen unas 47. El mínimo del 15 % hace falta porque 7 productos casi nunca cambian de precio y su IQR es 0.

**¿Los datos son reales?**
Sí. No hay datos sintéticos en el sistema. Cada precio está ligado a su archivo de origen con su
hash SHA-256 (página Fuentes), y las dos fuentes de MIDAGRI coinciden entre sí.

**¿Qué pasa si MIDAGRI cambia el formato del PDF?**
El extractor dejaría de reconocer filas y el Streaming registraría "0 válidas", sin datos corruptos.
Habría que ajustar el extractor. Por eso el histórico viene de SISAP y no de los PDF.

**¿Qué es la arquitectura hexagonal en tu proyecto?**
Cada funcionalidad tiene un núcleo (dominio y casos de uso) que no conoce Spring ni MongoDB. Se
comunica mediante puertos (interfaces), y los adaptadores los implementan: los controladores web de
entrada y los repositorios de MongoDB de salida. Por eso pruebo los casos de uso con puertos falsos,
sin base de datos.

**¿Por qué Papa Blanca y Papa Yungay tienen el mismo precio?**
Lo detecté analizando los datos: coinciden en 1 059 de 1 061 días, pero con volumen distinto.
Probablemente "Papa Blanca" es una categoría agregada que usa la Yungay como referencia. Lo dejé
documentado como hallazgo de calidad de datos.

**¿Cómo comparas precios en soles con el IPC, que es un índice?**
Convierto los precios mayoristas en un índice con la fórmula de Jevons (la media geométrica de los
cambios de precio de los 59 productos), la misma que usa Qhawarina. Luego llevo todas las series a
una base común = 100 y las comparo en el mismo periodo. Comparo tendencias, no niveles: no son la
misma canasta.

**¿Por qué no usas el IPC de alimentos?**
La API del BCRP no publica "alimentos y bebidas" como serie aparte con la base actual (dic. 2021).
Uso el IPC total y el IPC sin alimentos: la diferencia entre ambos muestra el peso de los alimentos.
No reconstruí esa serie con pesos supuestos, porque sería un dato inventado.

**¿Por qué no incluiste supermercados?**
Están como contexto, con el índice de Qhawarina, pero no producto por producto: no hay una fuente
pública autorizada con esos precios, y el índice de Qhawarina no se actualiza desde junio de 2026.
Preferí un sistema verificable con datos oficiales antes que uno con datos dudosos.

---

## Si algo falla durante la presentación

| Problema | Solución rápida |
|---|---|
| La web no abre | Mira la ventana de `.\app.ps1`. Si dice "Port … already in use", ejecuta `$env:SERVER_PORT=8090; .\app.ps1` y usa el puerto 8090 |
| Error de conexión a MongoDB | La IP cambió: agrégala en Atlas (*IP Access List*). Mientras tanto, usa las capturas |
| El Streaming no detecta el archivo | Ese nombre ya se procesó antes: cópialo con otro nombre y vuelve a hacer `ingest-pdf` |
| Spark dice algo de `getSubject` | Se ejecutó con Java 26: usa siempre `.\jobs.ps1`, que fija el JDK 17 |
| Sin internet | Sin internet no hay Atlas: presenta con las capturas y el informe |
