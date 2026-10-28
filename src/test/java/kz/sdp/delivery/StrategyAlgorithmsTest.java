package kz.sdp.delivery;

import static kz.sdp.delivery.TestRoutes.STANDARD;
import static kz.sdp.delivery.TestRoutes.route;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import kz.sdp.delivery.domain.DeliveryPriority;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.domain.Route;
import kz.sdp.delivery.domain.RouteDecision;
import kz.sdp.delivery.routing.RouteMetricsCalculator;
import kz.sdp.delivery.strategy.BalancedRouteStrategy;
import kz.sdp.delivery.strategy.CheapestRouteStrategy;
import kz.sdp.delivery.strategy.DistanceFirstStrategy;
import kz.sdp.delivery.strategy.EcoFriendlyRouteStrategy;
import kz.sdp.delivery.strategy.FastestRouteStrategy;
import kz.sdp.delivery.strategy.RouteStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StrategyAlgorithmsTest {
    @Test
    void fastestConsidersTrafficAndWeatherRatherThanRawTravelTime() {
        Route rawFastest = route("A", 10, 20, 500, 2, 15, 10, 0, true, Set.of());
        Route actuallyFastest = route("B", 12, 30, 600, 3, 2, 3, 0, true, Set.of());
        RouteDecision decision = new FastestRouteStrategy()
                .selectRoute(List.of(rawFastest, actuallyFastest), STANDARD);

        assertAll(
                () -> assertEquals("B", decision.route().id()),
                () -> assertEquals(35, decision.metrics().deliveryMinutes()),
                () -> assertEquals(35.0, decision.score(), 1e-12),
                () -> assertFalse(decision.explanation().isEmpty()));
    }

    @Test
    void cheapestMinimizesTotalCostInsteadOfBaseCost() {
        Route lowBase = route("A", 20, 30, 0, 2);
        Route lowTotal = route("B", 1, 35, 600, 3, 0, 0, 20, true, Set.of());
        RouteDecision decision = new CheapestRouteStrategy().selectRoute(List.of(lowBase, lowTotal), STANDARD);

        assertAll(
                () -> assertEquals("B", decision.route().id()),
                () -> assertEquals(660.0, decision.metrics().totalCost(), 1e-12),
                () -> assertEquals(660.0, decision.score(), 1e-12));
    }

    @Test
    void totalPriceIncludesDistanceUrgentSurchargeAndAdditionalFees() {
        Route candidate = route("R", 2, 20, 1000, 1, 3, 4, 75, true, Set.of());
        DeliveryRequest urgent = new DeliveryRequest(DeliveryPriority.URGENT, 5000, 60, false);

        assertAll(
                () -> assertEquals(1655.0,
                        RouteMetricsCalculator.calculate(candidate, urgent).totalCost(), 1e-12),
                () -> assertEquals(1155.0,
                        RouteMetricsCalculator.calculate(candidate, STANDARD).totalCost(), 1e-12),
                () -> assertEquals(27, RouteMetricsCalculator.calculate(candidate, urgent).deliveryMinutes()));
    }

    @Test
    void ecoBalancesEmissionsAndDistanceInsteadOfEmissionsAlone() {
        Route lowestCarbon = route("A", 100, 30, 100, 1);
        Route bestComposite = route("B", 10, 35, 100, 2);
        RouteDecision decision = new EcoFriendlyRouteStrategy()
                .selectRoute(List.of(lowestCarbon, bestComposite), STANDARD);

        assertEquals("B", decision.route().id());
        assertEquals(2.3, decision.score(), 1e-12);
    }

    @Test
    void ecoWeightsCanChangeTheSelectedRoute() {
        List<Route> routes = List.of(route("A", 100, 30, 100, 1), route("B", 10, 35, 100, 2));

        assertEquals("A", new EcoFriendlyRouteStrategy(1, 0).selectRoute(routes, STANDARD).route().id());
        assertEquals("B", new EcoFriendlyRouteStrategy(0, 1).selectRoute(routes, STANDARD).route().id());
    }

    @Test
    void ecoRejectsZeroNegativeAndNonFiniteWeights() {
        assertThrows(IllegalArgumentException.class, () -> new EcoFriendlyRouteStrategy(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new EcoFriendlyRouteStrategy(-1, .03));
        assertThrows(IllegalArgumentException.class, () -> new EcoFriendlyRouteStrategy(Double.NaN, .03));
        assertThrows(IllegalArgumentException.class,
                () -> new EcoFriendlyRouteStrategy(1, Double.POSITIVE_INFINITY));
    }

    @Test
    void balancedProducesAnIndependentlyCalculatedNormalizedScore() {
        RouteDecision decision = new BalancedRouteStrategy(.4, .3, .1, .2)
                .selectRoute(balancedCandidates(), STANDARD);

        assertEquals("A", decision.route().id());
        assertEquals(.2, decision.score(), 1e-12);
    }

    @Test
    void balancedChangingWeightsChangesTheWinner() {
        RouteDecision decision = new BalancedRouteStrategy(.1, .05, .05, .8)
                .selectRoute(balancedCandidates(), STANDARD);

        assertEquals("C", decision.route().id());
        assertEquals(.2, decision.score(), 1e-12);
    }

    @Test
    void balancedSingleFeasibleRouteHasZeroNormalizedScore() {
        Route candidate = route("only", 10, 20, 500, 2);
        RouteDecision decision = new BalancedRouteStrategy().selectRoute(List.of(candidate), STANDARD);

        assertEquals(candidate, decision.route());
        assertEquals(0.0, decision.score(), 1e-12);
    }

    @Test
    void balancedEqualCriteriaDoNotDivideByZeroAndTiesAreStable() {
        Route z = route("Z", 10, 20, 500, 2);
        Route a = route("A", 10, 20, 500, 2);
        RouteDecision decision = new BalancedRouteStrategy().selectRoute(List.of(z, a), STANDARD);

        assertEquals("A", decision.route().id());
        assertEquals(0.0, decision.score(), 1e-12);
        assertTrue(Double.isFinite(decision.score()));
    }

    @Test
    void rejectedExtremeRouteCannotDistortBalancedNormalization() {
        List<Route> eligible = balancedCandidates();
        Route unavailable = route("invalid", 10_000, 1, 10_000, 0, 0, 0, 0, false, Set.of());
        BalancedRouteStrategy strategy = new BalancedRouteStrategy(.4, .3, .1, .2);
        RouteDecision before = strategy.selectRoute(eligible, STANDARD);
        RouteDecision after = strategy.selectRoute(
                List.of(eligible.get(0), eligible.get(1), eligible.get(2), unavailable), STANDARD);

        assertEquals(before.route(), after.route());
        assertEquals(before.score(), after.score(), 1e-12);
        assertEquals("invalid", after.rejections().get(0).routeId());
    }

    @Test
    void fifthStrategySelectsShortestDistanceEvenWhenSlowerAndCostlier() {
        Route shortest = route("shortest", 1, 60, 2000, 3);
        Route alternative = route("other", 10, 10, 100, 1);
        RouteDecision decision = new DistanceFirstStrategy().selectRoute(List.of(alternative, shortest), STANDARD);

        assertEquals(shortest, decision.route());
        assertEquals(1.0, decision.score(), 1e-12);
    }

    @ParameterizedTest(name = "Invalid Balanced weights: {0}, {1}, {2}, {3}")
    @MethodSource("invalidBalancedWeights")
    void balancedRejectsInvalidWeights(double time, double cost, double distance, double emission) {
        assertThrows(IllegalArgumentException.class,
                () -> new BalancedRouteStrategy(time, cost, distance, emission));
    }

    static Stream<Arguments> invalidBalancedWeights() {
        return Stream.of(
                Arguments.of(-.1, .5, .3, .3),
                Arguments.of(.4, .3, .1, .3),
                Arguments.of(0, 0, 0, 0),
                Arguments.of(Double.NaN, .3, .1, .2),
                Arguments.of(.4, Double.POSITIVE_INFINITY, .1, .2));
    }

    @Test
    void balancedAcceptsFloatingPointSumAndSingleCriterionWeights() {
        assertEquals("A", new BalancedRouteStrategy(.1 + .2, .3, .2, .2)
                .selectRoute(balancedCandidates(), STANDARD).route().id());
        assertEquals("C", new BalancedRouteStrategy(0, 0, 0, 1)
                .selectRoute(balancedCandidates(), STANDARD).route().id());
    }

    @ParameterizedTest
    @MethodSource("allStrategies")
    void everyStrategyBreaksTiesByRouteIdAndNamesItsDecision(RouteStrategy strategy) {
        RouteDecision decision = strategy.selectRoute(
                List.of(route("Z", 10, 20, 500, 2), route("A", 10, 20, 500, 2)), STANDARD);

        assertEquals("A", decision.route().id());
        assertEquals(strategy.getStrategyName(), decision.strategyName());
        assertFalse(decision.explanation().isEmpty());
    }

    static Stream<RouteStrategy> allStrategies() {
        return Stream.of(new FastestRouteStrategy(), new CheapestRouteStrategy(),
                new EcoFriendlyRouteStrategy(), new BalancedRouteStrategy(), new DistanceFirstStrategy());
    }

    private static List<Route> balancedCandidates() {
        return List.of(route("A", 10, 10, 600, 3),
                route("B", 20, 20, 1200, 2), route("C", 30, 30, 1800, 1));
    }
}
