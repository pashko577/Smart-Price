package pe.smartprice.analytics.infrastructure.adapter.out.persistence;

import static org.springframework.data.mongodb.core.query.Criteria.where;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import pe.smartprice.analytics.application.port.out.AnalyticsPort;
import pe.smartprice.analytics.domain.MonthlyPrice;
import pe.smartprice.analytics.domain.ProductSummary;
import pe.smartprice.shared.persistence.Docs;

/**
 * Adaptador MongoDB de {@link AnalyticsPort}: colección price_analytics.
 * El contrato de datos lo define el job BatchAnalyticsJob de smartprice-jobs.
 */
@Component
public class MongoAnalyticsAdapter implements AnalyticsPort {

    static final String COLLECTION = "price_analytics";

    private final MongoTemplate mongo;

    public MongoAnalyticsAdapter(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Override
    public List<ProductSummary> findSummaries(String marketCode) {
        Query q = new Query(where("analysis_type").is("PRODUCT_SUMMARY").and("market_code").is(marketCode));
        return mongo.find(q, Document.class, COLLECTION).stream().map(MongoAnalyticsAdapter::toSummary).toList();
    }

    @Override
    public Optional<ProductSummary> findSummary(String marketCode, String productName) {
        Query q = new Query(where("analysis_type").is("PRODUCT_SUMMARY").and("market_code").is(marketCode)
                .and("product_name").is(productName));
        return Optional.ofNullable(mongo.findOne(q, Document.class, COLLECTION)).map(MongoAnalyticsAdapter::toSummary);
    }

    @Override
    public List<MonthlyPrice> findMonthly(String marketCode, String productName) {
        Query q = new Query(where("analysis_type").is("PRICE_MONTHLY").and("market_code").is(marketCode)
                .and("product_name").is(productName));
        return mongo.find(q, Document.class, COLLECTION).stream().map(MongoAnalyticsAdapter::toMonthly).toList();
    }

    static ProductSummary toSummary(Document d) {
        return new ProductSummary(
                Docs.str(d, "product_name"),
                Docs.str(d, "market_code"),
                Docs.date(d, "period.from"),
                Docs.date(d, "period.to"),
                Docs.dbl(d, "price.last"),
                Docs.dbl(d, "price.avg_30d"),
                Docs.dbl(d, "price.avg_365d"),
                Docs.dbl(d, "price.min_all"),
                Docs.dbl(d, "price.max_all"),
                Docs.dbl(d, "last_volume_t"),
                Docs.dbl(d, "var_pct_30d"),
                Docs.dbl(d, "var_pct_yoy"),
                Docs.dbl(d, "corr_price_volume"),
                Docs.lng(d, "observations"));
    }

    static MonthlyPrice toMonthly(Document d) {
        return new MonthlyPrice(
                Docs.str(d, "product_name"),
                YearMonth.parse(Docs.str(d, "period.label")),
                Docs.dbl(d, "price.avg"),
                Docs.dbl(d, "price.min"),
                Docs.dbl(d, "price.max"),
                Docs.dbl(d, "volume.avg_t"),
                Docs.dbl(d, "volume.total_t"),
                Docs.dbl(d, "var_pct_vs_prev_month"),
                Docs.dbl(d, "var_pct_vs_prev_year"),
                (int) Docs.lng(d, "days_observed"),
                Docs.str(d, "data_quality_status"));
    }
}
