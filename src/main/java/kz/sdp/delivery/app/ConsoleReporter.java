package kz.sdp.delivery.app;

import java.util.Locale;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.domain.RouteDecision;
import kz.sdp.delivery.routing.NoSuitableRouteException;

public final class ConsoleReporter {
    private ConsoleReporter() { }

    public static void printRequest(DeliveryRequest request) {
        System.out.printf(Locale.ROOT,
                "Request: %s | budget %.2f KZT | deadline %d min | eco preferred %s%n",
                request.priority(), request.maxBudget(), request.maxDeliveryMinutes(), request.ecoPreferred());
        if (request.maxCarbonEmission() != Double.MAX_VALUE) {
            System.out.printf(Locale.ROOT, "Emission cap: %.2f kg CO2%n", request.maxCarbonEmission());
        }
        if (!request.prohibitedRouteIds().isEmpty()) {
            System.out.println("Prohibited routes: " + request.prohibitedRouteIds());
        }
    }

    public static void printDecision(RouteDecision decision) {
        var metrics = decision.metrics();
        System.out.printf(Locale.ROOT,
                "%s -> %s | time %d min | cost %.2f KZT | distance %.2f km | CO2 %.2f kg | score %.4f%n",
                decision.strategyName(), decision.route().id(), metrics.deliveryMinutes(),
                metrics.totalCost(), metrics.distanceKm(), metrics.carbonEmission(), decision.score());
        decision.explanation().forEach(reason -> System.out.println("  Reason: " + reason));
        decision.rejections().forEach(rejection -> System.out.println(
                "  Rejected " + rejection.routeId() + ": " + String.join("; ", rejection.reasons())));
    }

    public static void printFailure(String label, Runnable action) {
        try {
            action.run();
            throw new IllegalStateException("Expected failure did not occur: " + label);
        } catch (NoSuitableRouteException exception) {
            System.out.println(label + ": " + exception.getMessage());
            exception.rejections().forEach(rejection -> System.out.println(
                    "  Rejected " + rejection.routeId() + ": " + String.join("; ", rejection.reasons())));
        } catch (IllegalArgumentException exception) {
            System.out.println(label + ": " + exception.getMessage());
        }
    }
}
