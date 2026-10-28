package kz.sdp.delivery.routing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.domain.Route;
import kz.sdp.delivery.domain.RouteMetrics;
import kz.sdp.delivery.domain.RouteRejection;

public final class RouteConstraints {
    public record EligibleRoute(Route route, RouteMetrics metrics) { }

    public record Evaluation(List<EligibleRoute> eligible, List<RouteRejection> rejections) {
        public Evaluation {
            eligible = List.copyOf(eligible);
            rejections = List.copyOf(rejections);
        }
    }

    public Evaluation evaluate(List<Route> routes, DeliveryRequest request) {
        Objects.requireNonNull(request, "Delivery request is required");
        Objects.requireNonNull(routes, "Candidate routes are required");
        if (routes.isEmpty()) {
            throw new NoSuitableRouteException("Candidate route list is empty", List.of());
        }
        List<Route> candidates = List.copyOf(routes);
        validateCandidates(candidates);
        var eligible = new ArrayList<EligibleRoute>();
        var rejections = new ArrayList<RouteRejection>();
        for (Route route : candidates) {
            var metrics = RouteMetricsCalculator.calculate(route, request);
            var reasons = new ArrayList<String>();
            if (!route.available()) reasons.add("Route is unavailable");
            if (metrics.totalCost() > request.maxBudget()) reasons.add("Budget exceeded");
            if (metrics.deliveryMinutes() > request.maxDeliveryMinutes()) reasons.add("Delivery deadline exceeded");
            if (route.prohibitedPriorities().contains(request.priority())) reasons.add("Delivery priority is prohibited");
            if (request.prohibitedRouteIds().contains(route.id())) reasons.add("Route is prohibited for this request");
            if (metrics.carbonEmission() > request.maxCarbonEmission()) reasons.add("Emission limit exceeded");
            if (reasons.isEmpty()) eligible.add(new EligibleRoute(route, metrics));
            else rejections.add(new RouteRejection(route.id(), reasons));
        }
        if (eligible.isEmpty()) {
            throw new NoSuitableRouteException("No route satisfies the delivery constraints", rejections);
        }
        return new Evaluation(eligible, rejections);
    }

    private void validateCandidates(List<Route> candidates) {
        var ids = new HashSet<String>();
        Route first = candidates.get(0);
        for (Route route : candidates) {
            if (!ids.add(route.id())) {
                throw new IllegalArgumentException("Duplicate route ID: " + route.id());
            }
            if (!route.origin().equals(first.origin()) || !route.destination().equals(first.destination())) {
                throw new IllegalArgumentException("Candidate routes must share origin and destination");
            }
        }
    }
}
