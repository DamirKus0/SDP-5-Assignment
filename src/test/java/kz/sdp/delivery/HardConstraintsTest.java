package kz.sdp.delivery;

import static kz.sdp.delivery.TestRoutes.STANDARD;
import static kz.sdp.delivery.TestRoutes.route;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import kz.sdp.delivery.domain.DeliveryPriority;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.domain.Location;
import kz.sdp.delivery.domain.Route;
import kz.sdp.delivery.domain.RouteDecision;
import kz.sdp.delivery.domain.RouteRejection;
import kz.sdp.delivery.routing.NoSuitableRouteException;
import kz.sdp.delivery.strategy.BalancedRouteStrategy;
import kz.sdp.delivery.strategy.CheapestRouteStrategy;
import kz.sdp.delivery.strategy.DistanceFirstStrategy;
import kz.sdp.delivery.strategy.EcoFriendlyRouteStrategy;
import kz.sdp.delivery.strategy.FastestRouteStrategy;
import kz.sdp.delivery.strategy.RouteStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class HardConstraintsTest {
    @ParameterizedTest
    @MethodSource("strategies")
    void everyStrategyFiltersEveryHardConstraintBeforeRanking(RouteStrategy strategy) {
        DeliveryRequest request = new DeliveryRequest(DeliveryPriority.STANDARD, 1000, 60,
                false, 5, Set.of("blocked"));
        Route valid = route("valid", 5, 40, 500, 2);
        List<Route> candidates = List.of(
                valid,
                route("unavailable", 1, 1, 0, 0, 0, 0, 0, false, Set.of()),
                route("expensive", 1, 1, 2000, 0),
                route("late", 1, 61, 0, 0),
                route("polluting", 1, 1, 0, 6),
                route("prohibited", 1, 1, 0, 0, 0, 0, 0, true, Set.of(DeliveryPriority.STANDARD)),
                route("blocked", 1, 1, 0, 0));

        RouteDecision result = strategy.selectRoute(candidates, request);

        assertEquals(valid, result.route());
        assertEquals(Set.of("unavailable", "expensive", "late", "polluting", "prohibited", "blocked"),
                result.rejections().stream().map(RouteRejection::routeId).collect(Collectors.toSet()));
        assertTrue(result.rejections().stream().allMatch(r -> !r.reasons().isEmpty()));
    }

    @Test
    void budgetChecksDistanceFeesAndPrioritySurcharge() {
        Route deceptivelyCheap = route("over-budget", 5, 10, 100, 1,
                0, 0, 50, true, Set.of());
        Route affordable = route("affordable", 1, 20, 100, 1);
        DeliveryRequest request = new DeliveryRequest(DeliveryPriority.URGENT, 700, 60, false);

        RouteDecision decision = new CheapestRouteStrategy()
                .selectRoute(List.of(deceptivelyCheap, affordable), request);

        assertEquals("affordable", decision.route().id());
        assertEquals(640.0, decision.metrics().totalCost(), 1e-12);
        assertEquals("over-budget", decision.rejections().get(0).routeId());
    }

    @Test
    void deadlineChecksTrafficAndWeatherDelays() {
        Route delayed = route("late", 2, 10, 100, 1, 10, 15, 0, true, Set.of());
        Route feasible = route("valid", 3, 25, 100, 1);
        DeliveryRequest request = new DeliveryRequest(DeliveryPriority.STANDARD, 1000, 30, false);

        RouteDecision decision = new FastestRouteStrategy().selectRoute(List.of(delayed, feasible), request);

        assertEquals(feasible, decision.route());
        assertEquals("late", decision.rejections().get(0).routeId());
    }

    @Test
    void equalityAtBudgetDeadlineAndEmissionLimitsIsAllowed() {
        Route boundary = route("boundary", 5, 20, 100, 1.5, 3, 2, 50, true, Set.of());
        DeliveryRequest request = new DeliveryRequest(DeliveryPriority.STANDARD, 350, 25,
                false, 1.5, Set.of());

        RouteDecision decision = new BalancedRouteStrategy().selectRoute(List.of(boundary), request);

        assertEquals(boundary, decision.route());
        assertTrue(decision.rejections().isEmpty());
    }

    @Test
    void changingRequestBudgetCanChangeTheSameStrategysWinner() {
        List<Route> routes = List.of(route("fast", 10, 10, 1000, 1), route("cheap", 1, 30, 100, 2));
        FastestRouteStrategy strategy = new FastestRouteStrategy();

        assertEquals("fast", strategy.selectRoute(routes,
                new DeliveryRequest(DeliveryPriority.STANDARD, 2000, 60, false)).route().id());
        assertEquals("cheap", strategy.selectRoute(routes,
                new DeliveryRequest(DeliveryPriority.STANDARD, 200, 60, false)).route().id());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void noFeasibleRouteRaisesExplicitExceptionWithRejectionDetails(RouteStrategy strategy) {
        Route unavailable = route("closed", 1, 10, 0, 0, 0, 0, 0, false, Set.of());

        NoSuitableRouteException exception = assertThrows(NoSuitableRouteException.class,
                () -> strategy.selectRoute(List.of(unavailable), STANDARD));

        assertEquals("closed", exception.rejections().get(0).routeId());
        assertFalse(exception.rejections().get(0).reasons().isEmpty());
        assertFalse(exception.getMessage().isBlank());
    }

    @Test
    void emptyCandidatesAreHandledExplicitly() {
        assertThrows(NoSuitableRouteException.class,
                () -> new FastestRouteStrategy().selectRoute(List.of(), STANDARD));
    }

    @Test
    void rejectionExplainsMultipleViolationsOnTheSameRoute() {
        Route impossible = route("impossible", 1, 90, 2000, 10,
                0, 0, 0, false, Set.of(DeliveryPriority.STANDARD));
        Route valid = route("valid", 1, 10, 0, 1);
        DeliveryRequest request = new DeliveryRequest(DeliveryPriority.STANDARD, 100, 30,
                false, 2, Set.of("impossible"));

        RouteDecision decision = new FastestRouteStrategy().selectRoute(List.of(impossible, valid), request);

        assertEquals(6, decision.rejections().get(0).reasons().size());
    }

    @Test
    void nullRequestAndNullCandidatesFailImmediately() {
        FastestRouteStrategy strategy = new FastestRouteStrategy();

        assertThrows(NullPointerException.class, () -> strategy.selectRoute(List.of(route("A", 1, 10, 0, 1)), null));
        assertThrows(NullPointerException.class, () -> strategy.selectRoute(null, STANDARD));
        List<Route> containsNull = new ArrayList<>();
        containsNull.add(null);
        assertThrows(NullPointerException.class, () -> strategy.selectRoute(containsNull, STANDARD));
    }

    @Test
    void duplicateIdsAndMixedDeliveryEndpointsAreRejected() {
        Route first = route("duplicate", 1, 10, 0, 1);
        Route duplicate = route("duplicate", 2, 20, 0, 1);
        FastestRouteStrategy strategy = new FastestRouteStrategy();
        assertThrows(IllegalArgumentException.class,
                () -> strategy.selectRoute(List.of(first, duplicate), STANDARD));

        Route otherDestination = new Route("other", TestRoutes.ORIGIN,
                new Location("Another destination", 43.3, 77), 1, 10, 0, 1,
                0, 0, 0, true, Set.of());
        assertThrows(IllegalArgumentException.class,
                () -> strategy.selectRoute(List.of(first, otherDestination), STANDARD));
    }

    @Test
    void largeTimeValuesCannotOverflowAndBecomeFalselyFeasible() {
        Route tooLate = route("overflow-risk", 1, Integer.MAX_VALUE, 0, 0,
                Integer.MAX_VALUE, Integer.MAX_VALUE, 0, true, Set.of());
        Route valid = route("valid", 2, 10, 0, 1);
        DeliveryRequest request = new DeliveryRequest(DeliveryPriority.STANDARD,
                1000, Integer.MAX_VALUE, false);

        RouteDecision decision = new FastestRouteStrategy().selectRoute(List.of(tooLate, valid), request);

        assertEquals(valid, decision.route());
        assertEquals("overflow-risk", decision.rejections().get(0).routeId());
    }

    @Test
    void resultCollectionsCannotBeModified() {
        Route valid = route("valid", 1, 10, 0, 1);
        Route unavailable = route("closed", 1, 10, 0, 0, 0, 0, 0, false, Set.of());
        RouteDecision decision = new FastestRouteStrategy().selectRoute(List.of(valid, unavailable), STANDARD);

        assertThrows(UnsupportedOperationException.class, () -> decision.explanation().add("changed"));
        assertThrows(UnsupportedOperationException.class, () -> decision.rejections().clear());
        assertThrows(UnsupportedOperationException.class, () -> decision.rejections().get(0).reasons().clear());
    }

    static Stream<RouteStrategy> strategies() {
        return Stream.of(new FastestRouteStrategy(), new CheapestRouteStrategy(),
                new EcoFriendlyRouteStrategy(), new BalancedRouteStrategy(), new DistanceFirstStrategy());
    }
}
