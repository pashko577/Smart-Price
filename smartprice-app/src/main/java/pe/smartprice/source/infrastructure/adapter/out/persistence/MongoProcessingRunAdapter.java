package pe.smartprice.source.infrastructure.adapter.out.persistence;

import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import pe.smartprice.shared.persistence.Docs;
import pe.smartprice.source.application.port.out.ProcessingRunPort;
import pe.smartprice.source.domain.ProcessingRun;

/** Adaptador MongoDB de {@link ProcessingRunPort}: colección processing_logs (escrita por PriceStreamingJob). */
@Component
public class MongoProcessingRunAdapter implements ProcessingRunPort {

    private final MongoTemplate mongo;

    public MongoProcessingRunAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public List<ProcessingRun> findRecent(int limit) {
        Query q = new Query().with(Sort.by(Sort.Direction.DESC, "started_at")).limit(limit);
        return mongo.find(q, Document.class, "processing_logs").stream()
                .map(d -> new ProcessingRun(
                        Docs.str(d, "_id"),
                        Docs.str(d, "job"),
                        Docs.instant(d, "started_at"),
                        Docs.lng(d, "duration_ms"),
                        d.getList("files", String.class),
                        Docs.lng(d, "rows_read"),
                        Docs.lng(d, "rows_valid"),
                        d.getList("products_outside_catalog", String.class) == null ? 0
                                : d.getList("products_outside_catalog", String.class).size(),
                        Docs.lng(d, "rows_new"),
                        Docs.lng(d, "rows_existing"),
                        Docs.lng(d, "existing_price_matches"),
                        Docs.lng(d, "existing_price_mismatches"),
                        Docs.lng(d, "alerts_generated"),
                        Docs.str(d, "status")))
                .toList();
    }
}
