package kz.sdp.delivery.strategy;

import java.util.List;
import kz.sdp.delivery.routing.RouteConstraints.EligibleRoute;

public final class FastestRouteStrategy extends AbstractRouteStrategy {
    @Override
    public String getStrategyName() {
        return "Fastest";
    }

    @Override
    protected ScoringRule scoringFor(List<EligibleRoute> candidates) {
        return new ScoringRule(candidate -> candidate.metrics().deliveryMinutes(), candidate -> List.of(
                "Minimum effective delivery time among feasible routes; ties use route ID.",
                "Base time " + candidate.route().estimatedMinutes()
                        + " + traffic " + candidate.route().trafficDelayMinutes()
                        + " + weather " + candidate.route().weatherDelayMinutes()
                        + " = " + candidate.metrics().deliveryMinutes() + " min"));
    }
}
