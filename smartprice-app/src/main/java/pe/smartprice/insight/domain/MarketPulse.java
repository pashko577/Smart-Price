package pe.smartprice.insight.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoublePredicate;

import pe.smartprice.analytics.domain.ProductSummary;

/**
 * Pulso del mercado: cómo se reparten las variaciones de 30 días entre los productos.
 *
 * @param basketChangePct variación de la canasta completa: media geométrica de (1 + variación) de cada
 *                        producto, la misma idea que el índice de Jevons
 * @param medianChangePct variación mediana por producto
 * @param distribution    cantidad de productos en cada tramo de variación, de la mayor bajada a la mayor subida
 */
public record MarketPulse(
        LocalDate asOf,
        int products,
        int strongRise,
        int rise,
        int stable,
        int fall,
        int strongFall,
        int unknown,
        Double basketChangePct,
        Double medianChangePct,
        List<Bucket> distribution) {

    /**
     * Un tramo de variación.
     *
     * @param tone     up / down / neutral (subir = más caro)
     * @param products productos del tramo, ordenados de mayor a menor variación
     */
    public record Bucket(String label, String tone, List<String> products) {

        public Bucket {
            products = List.copyOf(products);
        }

        public int count() {
            return products.size();
        }
    }

    /** Fase del mercado, según la variación de la canasta completa. */
    public enum Phase {
        RISING("En alza", "en fase de alza"),
        FALLING("A la baja", "en fase de baja"),
        STABLE("Estable", "relativamente estable");

        private final String label;
        private final String phrase;

        Phase(String label, String phrase) {
            this.label = label;
            this.phrase = phrase;
        }

        public String label() {
            return label;
        }

        public String phrase() {
            return phrase;
        }
    }

    public MarketPulse {
        distribution = List.copyOf(distribution);
    }

    public int rising() {
        return strongRise + rise;
    }

    public int falling() {
        return strongFall + fall;
    }

    public Phase phase() {
        if (basketChangePct == null) {
            return Phase.STABLE;
        }
        if (basketChangePct >= 5) {
            return Phase.RISING;
        }
        if (basketChangePct <= -5) {
            return Phase.FALLING;
        }
        return Phase.STABLE;
    }

    /** El tramo con más productos (para escalar el gráfico). */
    public int maxBucket() {
        return distribution.stream().mapToInt(Bucket::count).max().orElse(0);
    }

    public static MarketPulse of(LocalDate asOf, List<ProductSummary> summaries) {
        int[] counts = new int[Situation.values().length];
        summaries.forEach(s -> counts[Situation.of(s.varPct30d()).ordinal()]++);

        List<Double> changes = summaries.stream().map(ProductSummary::varPct30d)
                .filter(v -> v != null && v > -100).sorted().toList();
        Double basket = null;
        Double median = null;
        if (!changes.isEmpty()) {
            double logSum = changes.stream().mapToDouble(v -> Math.log(1 + v / 100)).sum();
            basket = (Math.exp(logSum / changes.size()) - 1) * 100;
            int n = changes.size();
            median = n % 2 == 1 ? changes.get(n / 2) : (changes.get(n / 2 - 1) + changes.get(n / 2)) / 2;
        }

        List<Bucket> buckets = new ArrayList<>();
        String pc = Text.NBSP + "%";
        buckets.add(bucket(summaries, "−20" + pc + " o más", "down", v -> v <= -20));
        buckets.add(bucket(summaries, "−20 a −5" + pc, "down", v -> v > -20 && v <= -5));
        buckets.add(bucket(summaries, "−5 a +5" + pc, "neutral", v -> v > -5 && v < 5));
        buckets.add(bucket(summaries, "+5 a +20" + pc, "up", v -> v >= 5 && v < 20));
        buckets.add(bucket(summaries, "+20 a +50" + pc, "up", v -> v >= 20 && v < 50));
        buckets.add(bucket(summaries, "+50" + pc + " o más", "up", v -> v >= 50));

        return new MarketPulse(asOf, summaries.size(),
                counts[Situation.STRONG_RISE.ordinal()], counts[Situation.RISE.ordinal()],
                counts[Situation.STABLE.ordinal()], counts[Situation.FALL.ordinal()],
                counts[Situation.STRONG_FALL.ordinal()], counts[Situation.UNKNOWN.ordinal()],
                basket, median, buckets);
    }

    private static Bucket bucket(List<ProductSummary> summaries, String label, String tone, DoublePredicate in) {
        return new Bucket(label, tone, summaries.stream()
                .filter(s -> s.varPct30d() != null && in.test(s.varPct30d()))
                .sorted(Comparator.comparing(ProductSummary::varPct30d).reversed())
                .map(ProductSummary::productName).toList());
    }
}
