package kz.sdp.delivery.strategy;

import java.util.List;
import java.util.Locale;
import java.util.function.ToDoubleFunction;
import kz.sdp.delivery.domain.Checks;
import kz.sdp.delivery.routing.RouteConstraints.EligibleRoute;

public final class BalancedRouteStrategy extends AbstractRouteStrategy {
    private static final double WEIGHT_TOLERANCE = 1e-9;
    private final double timeWeight;
    private final double costWeight;
    private final double distanceWeight;
    private final double emissionWeight;

    public BalancedRouteStrategy() {
        this(0.40, 0.30, 0.10, 0.20);
    }

    public BalancedRouteStrategy(double timeWeight, double costWeight,
                                 double distanceWeight, double emissionWeight) {
        Checks.nonNegativeFinite(timeWeight, "Time weight");
        Checks.nonNegativeFinite(costWeight, "Cost weight");
        Checks.nonNegativeFinite(distanceWeight, "Distance weight");
        Checks.nonNegativeFinite(emissionWeight, "Emission weight");
        double sum = timeWeight + costWeight + distanceWeight + emissionWeight;
        if (!Double.isFinite(sum) || Math.abs(sum - 1.0) > WEIGHT_TOLERANCE) {
            throw new IllegalArgumentException("Balanced weights must sum to 1.0 (tolerance 1e-9)");
        }
        // Canonicalize a valid floating-point sum so scores remain on the same scale.
        this.timeWeight = timeWeight / sum;
        this.costWeight = costWeight / sum;
        this.distanceWeight = distanceWeight / sum;
        this.emissionWeight = emissionWeight / sum;
    }

    @Override
    public String getStrategyName() {
        return "Balanced";
    }

    @Override
    protected ScoringRule scoringFor(List<EligibleRoute> candidates) {
        var time = range(candidates, c -> c.metrics().deliveryMinutes());
        var cost = range(candidates, c -> c.metrics().totalCost());
        var distance = range(candidates, c -> c.metrics().distanceKm());
        var emission = range(candidates, c -> c.metrics().carbonEmission());
        return new ScoringRule(candidate ->
                time.normalize(candidate.metrics().deliveryMinutes()) * timeWeight
                        + cost.normalize(candidate.metrics().totalCost()) * costWeight
                        + distance.normalize(candidate.metrics().distanceKm()) * distanceWeight
                        + emission.normalize(candidate.metrics().carbonEmission()) * emissionWeight,
                candidate -> List.of(
                        "Minimum normalized weighted score over feasible routes; ties use route ID.",
                        "Normalization: (value - min) / (max - min); equal ranges contribute zero.",
                        String.format(Locale.ROOT,
                                "Time %.4f * %.4f + cost %.4f * %.4f + distance %.4f * %.4f + CO2 %.4f * %.4f",
                                time.normalize(candidate.metrics().deliveryMinutes()), timeWeight,
                                cost.normalize(candidate.metrics().totalCost()), costWeight,
                                distance.normalize(candidate.metrics().distanceKm()), distanceWeight,
                                emission.normalize(candidate.metrics().carbonEmission()), emissionWeight)));
    }

    private static Range range(List<EligibleRoute> candidates, ToDoubleFunction<EligibleRoute> metric) {
        var summary = candidates.stream().mapToDouble(metric).summaryStatistics();
        return new Range(summary.getMin(), summary.getMax());
    }

    private record Range(double min, double max) {
        double normalize(double value) {
            return max == min ? 0.0 : (value - min) / (max - min);
        }
    }
}
