package kz.sdp.delivery.strategy;

import java.util.List;
import kz.sdp.delivery.routing.RouteConstraints.EligibleRoute;

public final class DistanceFirstStrategy extends AbstractRouteStrategy {
    @Override
    public String getStrategyName() {
        return "Distance-first";
    }

    @Override
    protected ScoringRule scoringFor(List<EligibleRoute> candidates) {
        return new ScoringRule(candidate -> candidate.metrics().distanceKm(), candidate -> List.of(
                "Minimum distance among feasible routes; ties use route ID.",
                "Selected distance: " + candidate.metrics().distanceKm() + " km"));
    }
}
