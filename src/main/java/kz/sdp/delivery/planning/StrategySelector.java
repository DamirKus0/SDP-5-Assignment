package kz.sdp.delivery.planning;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import kz.sdp.delivery.domain.DeliveryPriority;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.strategy.BalancedRouteStrategy;
import kz.sdp.delivery.strategy.CheapestRouteStrategy;
import kz.sdp.delivery.strategy.EcoFriendlyRouteStrategy;
import kz.sdp.delivery.strategy.FastestRouteStrategy;
import kz.sdp.delivery.strategy.RouteStrategy;

public final class StrategySelector {
    private final Map<DeliveryPriority, RouteStrategy> strategies;

    public StrategySelector() {
        this(Map.of(
                DeliveryPriority.URGENT, new FastestRouteStrategy(),
                DeliveryPriority.ECONOMY, new CheapestRouteStrategy(),
                DeliveryPriority.ECO, new EcoFriendlyRouteStrategy(),
                DeliveryPriority.STANDARD, new BalancedRouteStrategy()));
    }

    public StrategySelector(Map<DeliveryPriority, RouteStrategy> strategies) {
        this.strategies = Map.copyOf(Objects.requireNonNull(strategies));
        if (!this.strategies.keySet().containsAll(EnumSet.allOf(DeliveryPriority.class))) {
            throw new IllegalArgumentException("A strategy mapping is required for every delivery priority");
        }
    }

    public RouteStrategy select(DeliveryRequest request) {
        Objects.requireNonNull(request, "Delivery request is required");
        DeliveryPriority key = request.priority() == DeliveryPriority.URGENT
                ? DeliveryPriority.URGENT
                : request.ecoPreferred() ? DeliveryPriority.ECO : request.priority();
        return strategies.get(key);
    }
}
