package kz.sdp.delivery.strategy;

import java.util.List;
import java.util.Locale;
import kz.sdp.delivery.domain.Checks;
import kz.sdp.delivery.routing.RouteConstraints.EligibleRoute;

public final class EcoFriendlyRouteStrategy extends AbstractRouteStrategy {
    private final double emissionWeight;
    private final double distanceWeight;

    public EcoFriendlyRouteStrategy() {
        this(1.0, 0.03);
    }

    public EcoFriendlyRouteStrategy(double emissionWeight, double distanceWeight) {
        this.emissionWeight = Checks.nonNegativeFinite(emissionWeight, "Emission weight");
        this.distanceWeight = Checks.nonNegativeFinite(distanceWeight, "Distance weight");
        if (emissionWeight == 0 && distanceWeight == 0) {
            throw new IllegalArgumentException("At least one eco weight must be positive");
        }
    }

    @Override
    public String getStrategyName() {
        return "Eco-friendly";
    }

    @Override
    protected ScoringRule scoringFor(List<EligibleRoute> candidates) {
        return new ScoringRule(candidate -> candidate.metrics().carbonEmission() * emissionWeight
                + candidate.metrics().distanceKm() * distanceWeight, candidate -> List.of(
                "Minimum environmental proxy score among feasible routes; ties use route ID.",
                String.format(Locale.ROOT, "CO2 %.3f kg * %.3f + distance %.3f km * %.3f",
                        candidate.metrics().carbonEmission(), emissionWeight,
                        candidate.metrics().distanceKm(), distanceWeight)));
    }
}
