package kz.sdp.delivery.planning;

import java.util.List;
import java.util.Objects;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.domain.Route;
import kz.sdp.delivery.domain.RouteDecision;
import kz.sdp.delivery.strategy.RouteStrategy;

/** Runs an injected registry of strategies against exactly the same input. */
public final class StrategyComparison {
    private final List<RouteStrategy> strategies;

    public StrategyComparison(List<RouteStrategy> strategies) {
        this.strategies = List.copyOf(Objects.requireNonNull(strategies));
        if (this.strategies.isEmpty()) {
            throw new IllegalArgumentException("Comparison requires at least one strategy");
        }
    }

    public List<RouteDecision> compare(List<Route> routes, DeliveryRequest request) {
        List<Route> snapshot = List.copyOf(Objects.requireNonNull(routes));
        Objects.requireNonNull(request, "Delivery request is required");
        return strategies.stream().map(strategy -> strategy.selectRoute(snapshot, request)).toList();
    }
}
