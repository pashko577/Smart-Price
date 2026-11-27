package pe.smartprice.price.infrastructure.adapter.out.persistence;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import pe.smartprice.price.application.port.out.PriceHistoryPort;
import pe.smartprice.price.domain.PricePoint;
import pe.smartprice.shared.persistence.Docs;

/** Adaptador MongoDB de {@link PriceHistoryPort}: colección wholesale_prices. */
@Component
public class MongoPriceHistoryAdapter implements PriceHistoryPort {

    static final String COLLECTION = "wholesale_prices";

    private final MongoTemplate mongo;

    public MongoPriceHistoryAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public List<PricePoint> findByProduct(String marketCode, String productName, LocalDate from, LocalDate to) {
        Query q = new Query(where("market_code").is(marketCode)
                .and("product_name").is(productName)
                .and("observed_at").gte(Docs.utc(from)).lte(Docs.utc(to)))
                .with(Sort.by(Sort.Direction.ASC, "observed_at"));
        q.fields().include("observed_at", "price.avg", "volume_t");
        return mongo.find(q, Document.class, COLLECTION).stream()
                .map(d -> new PricePoint(Docs.date(d, "observed_at"), Docs.dbl(d, "price.avg"),
                        Docs.dbl(d, "volume_t")))
                .toList();
    }

    @Override
    public Optional<LocalDate> lastObservedDate(String marketCode) {
        Query q = new Query(where("market_code").is(marketCode))
                .with(Sort.by(Sort.Direction.DESC, "observed_at")).limit(1);
        q.fields().include("observed_at");
        return Optional.ofNullable(mongo.findOne(q, Document.class, COLLECTION))
                .map(d -> Docs.date(d, "observed_at"));
    }

    @Override
    public List<String> productNames(String marketCode) {
        return mongo.findDistinct(new Query(where("market_code").is(marketCode)), "product_name",
                COLLECTION, String.class).stream().sorted().toList();
    }
}
