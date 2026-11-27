package pe.smartprice.alert.infrastructure.adapter.out.persistence;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import pe.smartprice.alert.application.port.out.AlertPort;
import pe.smartprice.alert.domain.AlertFilter;
import pe.smartprice.alert.domain.AlertType;
import pe.smartprice.alert.domain.PriceAlert;
import pe.smartprice.alert.domain.Severity;
import pe.smartprice.shared.persistence.Docs;

/** Adaptador MongoDB de {@link AlertPort}: colección price_alerts. */
@Component
public class MongoAlertAdapter implements AlertPort {

    static final String COLLECTION = "price_alerts";

    private final MongoTemplate mongo;

    public MongoAlertAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public List<PriceAlert> find(String marketCode, AlertFilter f) {
        Query q = new Query(criteria(marketCode, f))
                .with(Sort.by(Sort.Order.desc("observed_at"), Sort.Order.desc("change_pct")))
                .limit(f.limit());
        return mongo.find(q, Document.class, COLLECTION).stream().map(MongoAlertAdapter::toAlert).toList();
    }

    @Override
    public List<PriceAlert> findMatching(String marketCode, AlertFilter f) {
        Query q = new Query(criteria(marketCode, f))
                .with(Sort.by(Sort.Order.desc("observed_at"), Sort.Order.desc("change_pct")));
        return mongo.find(q, Document.class, COLLECTION).stream().map(MongoAlertAdapter::toAlert).toList();
    }

    /** Criterios de la consulta; "menor abastecimiento" es una regla de dominio y se aplica en el servicio. */
    static Criteria criteria(String marketCode, AlertFilter f) {
        Criteria c = where("market_code").is(marketCode);
        if (f.minSeverity() != null) {
            List<String> allowed = Arrays.stream(Severity.values())
                    .filter(s -> s.isAtLeast(f.minSeverity())).map(Enum::name).toList();
            c = c.and("severity").in(allowed);
        }
        if (f.productName() != null) {
            c = c.and("product_name").is(f.productName());
        }
        if (f.type() != null) {
            c = c.and("alert_type").is(f.type().name());
        }
        if (f.from() != null && f.to() != null) {
            c = c.and("observed_at").gte(Docs.utc(f.from())).lte(Docs.utc(f.to()));
        } else if (f.from() != null) {
            c = c.and("observed_at").gte(Docs.utc(f.from()));
        } else if (f.to() != null) {
            c = c.and("observed_at").lte(Docs.utc(f.to()));
        }
        if (!f.products().isEmpty() && f.productName() == null) { // un producto exacto manda sobre la lista
            c = c.and("product_name").in(f.products());
        }
        return c;
    }

    @Override
    public List<PriceAlert> findSince(String marketCode, LocalDate since) {
        Query q = new Query(where("market_code").is(marketCode).and("observed_at").gte(Docs.utc(since)));
        return mongo.find(q, Document.class, COLLECTION).stream().map(MongoAlertAdapter::toAlert).toList();
    }

    @Override
    public Optional<LocalDate> lastAlertDate(String marketCode) {
        Query q = new Query(where("market_code").is(marketCode))
                .with(Sort.by(Sort.Direction.DESC, "observed_at")).limit(1);
        return Optional.ofNullable(mongo.findOne(q, Document.class, COLLECTION))
                .map(d -> Docs.date(d, "observed_at"));
    }

    static PriceAlert toAlert(Document d) {
        return new PriceAlert(
                Docs.str(d, "_id"),
                Docs.str(d, "product_name"),
                Docs.str(d, "market_code"),
                Docs.date(d, "observed_at"),
                AlertType.parseOrNull(Docs.str(d, "alert_type")),
                Severity.parseOrNull(Docs.str(d, "severity")),
                Docs.dbl(d, "price.previous"),
                Docs.dbl(d, "price.current"),
                Docs.dbl(d, "change_pct"),
                Docs.dbl(d, "bounds.lower"),
                Docs.dbl(d, "bounds.upper"),
                Docs.dbl(d, "volume.current_t"),
                Docs.dbl(d, "volume.avg_30d_t"),
                Docs.dbl(d, "bounds.k"),
                Docs.dbl(d, "bounds.min_abs_change_pct"));
    }
}
