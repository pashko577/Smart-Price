package pe.smartprice.context.infrastructure.adapter.out.persistence;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.util.List;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import pe.smartprice.context.application.port.out.IndexSeriesPort;
import pe.smartprice.context.domain.IndexPoint;
import pe.smartprice.context.domain.IndexSeries;
import pe.smartprice.shared.persistence.Docs;

/** Adaptador MongoDB de {@link IndexSeriesPort}: colección price_indexes (escrita por load-context). */
@Component
public class MongoIndexSeriesAdapter implements IndexSeriesPort {

    private final MongoTemplate mongo;

    public MongoIndexSeriesAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public IndexSeries find(String source, String indicator) {
        Query q = new Query(where("source").is(source).and("indicator").is(indicator))
                .with(Sort.by(Sort.Direction.ASC, "date"));
        List<Document> docs = mongo.find(q, Document.class, "price_indexes");
        if (docs.isEmpty()) {
            return new IndexSeries(source, indicator, indicator, "", "", List.of());
        }
        Document first = docs.get(0);
        return new IndexSeries(source, indicator, Docs.str(first, "label"), Docs.str(first, "frequency"),
                Docs.str(first, "base_label"),
                docs.stream()
                        .filter(d -> Docs.dbl(d, "value") != null && Docs.dbl(d, "value") > 0)
                        .map(d -> new IndexPoint(Docs.date(d, "date"), Docs.dbl(d, "value")))
                        .toList());
    }
}
