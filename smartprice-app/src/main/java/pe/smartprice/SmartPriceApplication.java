package pe.smartprice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicación web de SmartPrice.
 *
 * <p>Solo lee de MongoDB: los datos y los indicadores los escriben los jobs de Apache Spark
 * (módulo smartprice-jobs). Cada funcionalidad (price, analytics, alert, source) sigue la
 * arquitectura hexagonal: domain / application (puertos y servicios) / infrastructure (adaptadores).
 */
@SpringBootApplication
public class SmartPriceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartPriceApplication.class, args);
    }
}
