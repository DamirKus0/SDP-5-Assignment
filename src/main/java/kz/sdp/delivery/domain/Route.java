package kz.sdp.delivery.domain;

import java.util.Objects;
import java.util.Set;

/** All candidate routes for a planning call must describe the same delivery endpoints. */
public record Route(
        String id,
        Location origin,
        Location destination,
        double distanceKm,
        int estimatedMinutes,
        double baseCost,
        double carbonEmission,
        int trafficDelayMinutes,
        int weatherDelayMinutes,
        double additionalFees,
        boolean available,
        Set<DeliveryPriority> prohibitedPriorities
) {
    public Route {
        Checks.nonBlank(id, "Route ID");
        Objects.requireNonNull(origin, "Route origin is required");
        Objects.requireNonNull(destination, "Route destination is required");
        Checks.nonNegativeFinite(distanceKm, "Route distance");
        Checks.nonNegativeFinite(baseCost, "Base cost");
        Checks.nonNegativeFinite(carbonEmission, "Carbon emission");
        Checks.nonNegativeFinite(additionalFees, "Additional fees");
        if (estimatedMinutes <= 0 || trafficDelayMinutes < 0 || weatherDelayMinutes < 0) {
            throw new IllegalArgumentException("Travel time must be positive and delays nonnegative");
        }
        prohibitedPriorities = Set.copyOf(Objects.requireNonNull(prohibitedPriorities));
    }
}
