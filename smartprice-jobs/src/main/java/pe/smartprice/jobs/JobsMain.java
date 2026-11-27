package pe.smartprice.jobs;

import java.util.Arrays;

import pe.smartprice.jobs.analytics.BatchAnalyticsJob;
import pe.smartprice.jobs.analytics.ExploreSisapJob;
import pe.smartprice.jobs.gmml.ExtractGmmlPdfJob;
import pe.smartprice.jobs.mongo.LoadSisapJob;
import pe.smartprice.jobs.mongo.MongoSetupJob;
import pe.smartprice.jobs.sisap.SisapToCsvJob;
import pe.smartprice.jobs.streaming.IngestPdfJob;
import pe.smartprice.jobs.streaming.PriceStreamingJob;

/**
 * Punto de entrada de los jobs de SmartPrice.
 *
 * <pre>
 *   .\jobs.ps1 sisap-to-csv   [carpeta_xls] [csv_salida]
 *   .\jobs.ps1 load-sisap     [csv_entrada]
 *   .\jobs.ps1 explore-sisap  [csv_entrada]
 *   .\jobs.ps1 batch-analytics
 *   .\jobs.ps1 extract-pdf    archivo.pdf [carpeta_salida]
 *   .\jobs.ps1 ingest-pdf     boletin.pdf [...]
 *   .\jobs.ps1 stream-prices  [--once]
 *   .\jobs.ps1 check-mongo
 *   .\jobs.ps1 setup-mongo
 * </pre>
 */
public final class JobsMain {

    private JobsMain() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0]) {
            case "sisap-to-csv" -> SisapToCsvJob.run(rest);
            case "load-sisap" -> LoadSisapJob.run(rest);
            case "explore-sisap" -> ExploreSisapJob.run(rest);
            case "batch-analytics" -> BatchAnalyticsJob.run(rest);
            case "extract-pdf" -> ExtractGmmlPdfJob.run(rest);
            case "ingest-pdf" -> IngestPdfJob.run(rest);
            case "stream-prices" -> PriceStreamingJob.run(rest);
            case "check-mongo" -> MongoSetupJob.check();
            case "setup-mongo" -> MongoSetupJob.setup();
            default -> {
                System.err.println("Comando desconocido: " + args[0]);
                usage();
                System.exit(1);
            }
        }
    }

    private static void usage() {
        System.err.println("""
                Uso: jobs.ps1 <comando> [argumentos]
                  sisap-to-csv   [carpeta_xls] [csv_salida]   Une las exportaciones SISAP en un CSV largo
                  load-sisap     [csv_entrada]                Carga el CSV en MongoDB (Spark)
                  explore-sisap  [csv_entrada]                Exploración inicial con Spark
                  batch-analytics                             Tendencias, resúmenes y alertas IQR -> MongoDB
                  extract-pdf    archivo.pdf [carpeta]        Extrae la tabla de un boletín GMML
                  ingest-pdf     boletin.pdf [...]            Extrae boletines y los deja para el Streaming
                  stream-prices  [--once]                     Spark Structured Streaming de boletines nuevos
                  check-mongo                                 Prueba la conexión a MongoDB Atlas
                  setup-mongo                                 Crea índices y muestra un resumen""");
    }
}
