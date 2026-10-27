package kz.sdp.delivery.app;

import java.util.List;
import java.util.Set;
import kz.sdp.delivery.domain.DeliveryPriority;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.planning.RoutePlanner;
import kz.sdp.delivery.planning.StrategyComparison;
import kz.sdp.delivery.planning.StrategySelector;
import kz.sdp.delivery.strategy.BalancedRouteStrategy;
import kz.sdp.delivery.strategy.CheapestRouteStrategy;
import kz.sdp.delivery.strategy.DistanceFirstStrategy;
import kz.sdp.delivery.strategy.EcoFriendlyRouteStrategy;
import kz.sdp.delivery.strategy.FastestRouteStrategy;
import kz.sdp.delivery.strategy.RouteStrategy;

public final class DeliveryApplication {
    private DeliveryApplication() { }

    public static void main(String[] args) {
        String mode = args.length == 0 ? "demo" : args[0];
        if (args.length > 1 || !List.of("demo", "compare", "failures", "help", "--help").contains(mode)) {
            System.err.println("Usage: java -jar target/smart-delivery.jar [demo|compare|failures|help]");
            System.exit(2);
            return;
        }
        if (mode.equals("help") || mode.equals("--help")) {
            System.out.println("Usage: java -jar target/smart-delivery.jar [demo|compare|failures|help]");
            return;
        }
        if (mode.equals("compare")) {
            comparisonMode();
            return;
        }
        if (mode.equals("failures")) {
            failureMode();
            return;
        }
        System.out.println("SMART DELIVERY AND ROUTE OPTIMIZATION ENGINE");
        System.out.println("Candidate routes: " + DemoData.routes().size());
        runtimeSwitching();
        automaticSelection();
        dynamicConstraints();
        comparisonMode();
        failureMode();
    }

    public static List<RouteStrategy> availableStrategies() {
        return List.of(new FastestRouteStrategy(), new CheapestRouteStrategy(),
                new EcoFriendlyRouteStrategy(), new BalancedRouteStrategy(), new DistanceFirstStrategy());
    }

    private static void runtimeSwitching() {
        System.out.println("\nRUNTIME SWITCHING: one planner, three strategy replacements");
        var routes = DemoData.routes();
        var request = DemoData.standardRequest();
        var planner = new RoutePlanner(new FastestRouteStrategy());
        ConsoleReporter.printRequest(request);
        ConsoleReporter.printDecision(planner.plan(routes, request));
        planner.setStrategy(new CheapestRouteStrategy());
        ConsoleReporter.printDecision(planner.plan(routes, request));
        planner.setStrategy(new EcoFriendlyRouteStrategy());
        ConsoleReporter.printDecision(planner.plan(routes, request));
        planner.setStrategy(new BalancedRouteStrategy(0.40, 0.30, 0.10, 0.20));
        ConsoleReporter.printDecision(planner.plan(routes, request));
    }

    private static void automaticSelection() {
        System.out.println("\nAUTOMATIC SELECTION: request rules select an algorithm");
        var selector = new StrategySelector();
        var planner = new RoutePlanner(new BalancedRouteStrategy());
        var requests = List.of(
                new DeliveryRequest(DeliveryPriority.URGENT, 10000, 35, true),
                new DeliveryRequest(DeliveryPriority.ECONOMY, 2000, 90, false),
                new DeliveryRequest(DeliveryPriority.ECO, 4000, 60, true, 1.0, Set.of()),
                new DeliveryRequest(DeliveryPriority.STANDARD, 3000, 45, false));
        for (var request : requests) {
            planner.setStrategy(selector.select(request));
            ConsoleReporter.printRequest(request);
            ConsoleReporter.printDecision(planner.plan(DemoData.routes(), request));
        }
    }

    private static void dynamicConstraints() {
        System.out.println("\nDYNAMIC CONSTRAINTS: same Fastest algorithm, different budgets");
        var planner = new RoutePlanner(new FastestRouteStrategy());
        for (double budget : new double[] {6000, 3000}) {
            var request = new DeliveryRequest(DeliveryPriority.STANDARD, budget, 90, false);
            ConsoleReporter.printRequest(request);
            ConsoleReporter.printDecision(planner.plan(DemoData.routes(), request));
        }
    }

    private static void comparisonMode() {
        System.out.println("\nSTRATEGY COMPARISON: identical request and candidates");
        var request = DemoData.standardRequest();
        ConsoleReporter.printRequest(request);
        new StrategyComparison(availableStrategies()).compare(DemoData.routes(), request)
                .forEach(ConsoleReporter::printDecision);
    }

    private static void failureMode() {
        System.out.println("\nEXPECTED FAILURES: explicit handling");
        var planner = new RoutePlanner(new BalancedRouteStrategy());
        ConsoleReporter.printFailure("Empty candidates", () -> planner.plan(List.of(), DemoData.standardRequest()));
        ConsoleReporter.printFailure("No feasible route", () -> planner.plan(DemoData.routes(),
                new DeliveryRequest(DeliveryPriority.STANDARD, 1, 1, false)));
        ConsoleReporter.printFailure("Invalid Balanced weights", () -> new BalancedRouteStrategy(.4, .4, .4, .4));
    }
}
