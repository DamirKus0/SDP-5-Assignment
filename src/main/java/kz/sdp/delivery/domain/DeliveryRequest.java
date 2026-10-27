package kz.sdp.delivery.domain;

import java.util.Objects;
import java.util.Set;

public record DeliveryRequest(
        DeliveryPriority priority,
        double maxBudget,
        int maxDeliveryMinutes,
        boolean ecoPreferred,
        double maxCarbonEmission,
        Set<String> prohibitedRouteIds
) {
    public DeliveryRequest {
        Objects.requireNonNull(priority, "Delivery priority is required");
        Checks.nonNegativeFinite(maxBudget, "Maximum budget");
        Checks.nonNegativeFinite(maxCarbonEmission, "Maximum carbon emission");
        if (maxDeliveryMinutes <= 0) {
            throw new IllegalArgumentException("Delivery deadline must be positive");
        }
        prohibitedRouteIds = Set.copyOf(Objects.requireNonNull(prohibitedRouteIds));
        prohibitedRouteIds.forEach(id -> Checks.nonBlank(id, "Prohibited route ID"));
    }

    public DeliveryRequest(DeliveryPriority priority, double maxBudget,
                           int maxDeliveryMinutes, boolean ecoPreferred) {
        this(priority, maxBudget, maxDeliveryMinutes, ecoPreferred, Double.MAX_VALUE, Set.of());
    }
}
