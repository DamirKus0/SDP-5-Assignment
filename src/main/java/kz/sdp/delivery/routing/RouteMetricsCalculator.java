package kz.sdp.delivery.routing;

import kz.sdp.delivery.domain.Checks;
import kz.sdp.delivery.domain.DeliveryPriority;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.domain.Route;
import kz.sdp.delivery.domain.RouteMetrics;

/** One pricing/time policy keeps feasibility checks and all algorithms consistent. */
public final class RouteMetricsCalculator {
    public static final double DISTANCE_RATE_KZT = 40;
    public static final double URGENT_SURCHARGE_KZT = 500;

    private RouteMetricsCalculator() { }

    public static RouteMetrics calculate(Route route, DeliveryRequest request) {
        long minutes = (long) route.estimatedMinutes()
                + route.trafficDelayMinutes() + route.weatherDelayMinutes();
        double surcharge = request.priority() == DeliveryPriority.URGENT ? URGENT_SURCHARGE_KZT : 0;
        double totalCost = route.baseCost() + route.distanceKm() * DISTANCE_RATE_KZT
                + surcharge + route.additionalFees();
        Checks.nonNegativeFinite(totalCost, "Calculated total cost");
        return new RouteMetrics(minutes, totalCost, route.distanceKm(), route.carbonEmission());
    }
}
