package kz.sdp.delivery;

import static kz.sdp.delivery.TestRoutes.STANDARD;
import static kz.sdp.delivery.TestRoutes.route;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import kz.sdp.delivery.domain.DeliveryPriority;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.domain.Route;
import kz.sdp.delivery.domain.RouteDecision;
import kz.sdp.delivery.planning.RoutePlanner;
import kz.sdp.delivery.planning.StrategyComparison;
import kz.sdp.delivery.planning.StrategySelector;
import kz.sdp.delivery.strategy.BalancedRouteStrategy;
import kz.sdp.delivery.strategy.CheapestRouteStrategy;
import kz.sdp.delivery.strategy.DistanceFirstStrategy;
import kz.sdp.delivery.strategy.EcoFriendlyRouteStrategy;
import kz.sdp.delivery.strategy.FastestRouteStrategy;
import kz.sdp.delivery.strategy.RouteStrategy;
import org.junit.jupiter.api.Test;

class PlanningIntegrationTest {
    @Test
    void onePlannerSwitchesAlgorithmsAtRuntimeThreeTimes() {
        List<Route> routes = comparisonCandidates();
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        assertEquals("fast", planner.plan(routes, STANDARD).route().id());

        planner.setStrategy(new CheapestRouteStrategy());
        assertEquals("cheap", planner.plan(routes, STANDARD).route().id());
        planner.setStrategy(new EcoFriendlyRouteStrategy());
        assertEquals("eco", planner.plan(routes, STANDARD).route().id());
        planner.setStrategy(new BalancedRouteStrategy(1, 0, 0, 0));
        assertEquals("fast", planner.plan(routes, STANDARD).route().id());
    }

    @Test
    void plannerAcceptsFifthStrategyWithoutKnowingItsType() {
        Route chosen = route("custom", 1, 10, 10, 1);
        RouteStrategy shortest = new DistanceFirstStrategy();
        RoutePlanner planner = new RoutePlanner(shortest);

        RouteDecision decision = planner.plan(List.of(chosen), STANDARD);

        assertEquals(chosen, decision.route());
        assertEquals(shortest.getStrategyName(), decision.strategyName());
    }

    @Test
    void plannerRejectsNullStrategyAndKeepsPreviousStrategyAfterFailedSwitch() {
        assertThrows(NullPointerException.class, () -> new RoutePlanner(null));
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        assertThrows(NullPointerException.class, () -> planner.setStrategy(null));
        assertEquals("fast", planner.plan(comparisonCandidates(), STANDARD).route().id());
    }

    @Test
    void automaticSelectorMapsAllBusinessPriorities() {
        StrategySelector selector = new StrategySelector();

        assertInstanceOf(FastestRouteStrategy.class, selector.select(request(DeliveryPriority.URGENT, false)));
        assertInstanceOf(CheapestRouteStrategy.class, selector.select(request(DeliveryPriority.ECONOMY, false)));
        assertInstanceOf(EcoFriendlyRouteStrategy.class, selector.select(request(DeliveryPriority.ECO, false)));
        assertInstanceOf(BalancedRouteStrategy.class, selector.select(request(DeliveryPriority.STANDARD, false)));
    }

    @Test
    void urgentOverridesEcoPreferenceButOtherPrioritiesHonorIt() {
        StrategySelector selector = new StrategySelector();

        assertInstanceOf(FastestRouteStrategy.class, selector.select(request(DeliveryPriority.URGENT, true)));
        assertInstanceOf(EcoFriendlyRouteStrategy.class, selector.select(request(DeliveryPriority.ECONOMY, true)));
        assertInstanceOf(EcoFriendlyRouteStrategy.class, selector.select(request(DeliveryPriority.STANDARD, true)));
    }

    @Test
    void selectorAllowsReplacingPolicyThroughMappingAndCopiesIt() {
        RouteStrategy distance = new DistanceFirstStrategy();
        Map<DeliveryPriority, RouteStrategy> mapping = new EnumMap<>(DeliveryPriority.class);
        mapping.put(DeliveryPriority.URGENT, new FastestRouteStrategy());
        mapping.put(DeliveryPriority.ECONOMY, new CheapestRouteStrategy());
        mapping.put(DeliveryPriority.ECO, new EcoFriendlyRouteStrategy());
        mapping.put(DeliveryPriority.STANDARD, distance);
        StrategySelector selector = new StrategySelector(mapping);
        mapping.put(DeliveryPriority.STANDARD, new BalancedRouteStrategy());

        assertSame(distance, selector.select(request(DeliveryPriority.STANDARD, false)));
    }

    @Test
    void selectorRejectsIncompleteMappingsAndMissingRequests() {
        assertThrows(IllegalArgumentException.class,
                () -> new StrategySelector(Map.of(DeliveryPriority.STANDARD, new BalancedRouteStrategy())));
        assertThrows(NullPointerException.class, () -> new StrategySelector().select(null));
    }

    @Test
    void automaticSelectionAndPlanningProduceDifferentRequestOutcomes() {
        StrategySelector selector = new StrategySelector();
        DeliveryRequest urgent = request(DeliveryPriority.URGENT, false);
        DeliveryRequest economy = request(DeliveryPriority.ECONOMY, false);
        DeliveryRequest eco = request(DeliveryPriority.ECO, false);
        RoutePlanner planner = new RoutePlanner(selector.select(urgent));
        assertEquals("fast", planner.plan(comparisonCandidates(), urgent).route().id());
        planner.setStrategy(selector.select(economy));
        assertEquals("cheap", planner.plan(comparisonCandidates(), economy).route().id());
        planner.setStrategy(selector.select(eco));
        assertEquals("eco", planner.plan(comparisonCandidates(), eco).route().id());
    }

    @Test
    void comparisonRunsEveryStrategyForExactlyTheSameRequest() {
        List<RouteStrategy> strategies = List.of(new FastestRouteStrategy(), new CheapestRouteStrategy(),
                new EcoFriendlyRouteStrategy(), new BalancedRouteStrategy(), new DistanceFirstStrategy());
        List<RouteDecision> results = new StrategyComparison(strategies).compare(comparisonCandidates(), STANDARD);

        assertEquals(5, results.size());
        assertEquals(List.of("fast", "cheap", "eco", "eco", "cheap"),
                results.stream().map(result -> result.route().id()).toList());
        assertEquals(strategies.stream().map(RouteStrategy::getStrategyName).toList(),
                results.stream().map(RouteDecision::strategyName).toList());
        assertThrows(UnsupportedOperationException.class, () -> results.clear());
    }

    @Test
    void comparisonDefensivelyCopiesRegisteredStrategies() {
        List<RouteStrategy> registrations = new ArrayList<>();
        registrations.add(new FastestRouteStrategy());
        registrations.add(new CheapestRouteStrategy());
        StrategyComparison comparison = new StrategyComparison(registrations);
        registrations.clear();

        List<RouteDecision> decisions = comparison.compare(comparisonCandidates(), STANDARD);

        assertEquals(2, decisions.size());
        assertEquals("fast", decisions.get(0).route().id());
        assertEquals("cheap", decisions.get(1).route().id());
    }

    private static DeliveryRequest request(DeliveryPriority priority, boolean ecoPreferred) {
        return new DeliveryRequest(priority, 100_000, 120, ecoPreferred);
    }

    private static List<Route> comparisonCandidates() {
        return List.of(route("fast", 10, 10, 1000, 3),
                route("cheap", 1, 40, 100, 2), route("eco", 5, 30, 700, .1));
    }
}
