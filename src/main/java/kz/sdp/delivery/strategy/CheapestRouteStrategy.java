package kz.sdp.delivery.strategy;

import java.util.List;
import java.util.Locale;
import kz.sdp.delivery.routing.RouteConstraints.EligibleRoute;
import kz.sdp.delivery.routing.RouteMetricsCalculator;

public final class CheapestRouteStrategy extends AbstractRouteStrategy {
    @Override
    public String getStrategyName() {
        return "Cheapest";
    }

    @Override
    protected ScoringRule scoringFor(List<EligibleRoute> candidates) {
        return new ScoringRule(candidate -> candidate.metrics().totalCost(), candidate -> {
            var route = candidate.route();
            double distanceCost = route.distanceKm() * RouteMetricsCalculator.DISTANCE_RATE_KZT;
            double surcharge = candidate.metrics().totalCost() - route.baseCost()
                    - distanceCost - route.additionalFees();
            return List.of("Minimum total delivery cost among feasible routes; ties use route ID.",
                    String.format(Locale.ROOT,
                            "Base %.2f + distance %.2f km * %.2f + priority surcharge %.2f + fees %.2f = %.2f KZT",
                            route.baseCost(), route.distanceKm(), RouteMetricsCalculator.DISTANCE_RATE_KZT,
                            surcharge, route.additionalFees(), candidate.metrics().totalCost()));
        });
    }
}
