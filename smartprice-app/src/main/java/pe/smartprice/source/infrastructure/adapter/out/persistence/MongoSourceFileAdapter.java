package pe.smartprice.source.infrastructure.adapter.out.persistence;

import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import pe.smartprice.shared.persistence.Docs;
import pe.smartprice.source.application.port.out.SourceFilePort;
import pe.smartprice.source.domain.SourceFile;

/** Adaptador MongoDB de {@link SourceFilePort}: colección source_files. */
@Component
public class MongoSourceFileAdapter implements SourceFilePort {

    private final MongoTemplate mongo;

    public MongoSourceFileAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public List<SourceFile> findAll() {
        return mongo.find(new Query(), Document.class, "source_files").stream()
                .map(d -> new SourceFile(
                        Docs.str(d, "_id"),
                        Docs.str(d, "system"),
                        Docs.str(d, "market_code"),
                        Docs.str(d, "sha256"),
                        Docs.str(d, "period_label"),
                        Docs.date(d, "first_date"),
                        Docs.date(d, "last_date"),
                        Docs.lng(d, "rows"),
                        Docs.str(d, "reader_version"),
                        Docs.str(d, "acquisition_method"),
                        Docs.instant(d, "loaded_at")))
                .toList();
    }
}
