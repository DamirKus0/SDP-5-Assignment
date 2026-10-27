package kz.sdp.delivery;

import static kz.sdp.delivery.TestRoutes.route;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import kz.sdp.delivery.domain.DeliveryPriority;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.domain.Location;
import kz.sdp.delivery.domain.Route;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DomainValidationTest {
    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void routeRejectsInvalidDistanceCostAndEmissions(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> route("R", invalid, 10, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> route("R", 1, 10, invalid, 1));
        assertThrows(IllegalArgumentException.class, () -> route("R", 1, 10, 0, invalid));
        assertThrows(IllegalArgumentException.class,
                () -> route("R", 1, 10, 0, 1, 0, 0, invalid, true, Set.of()));
    }

    @Test
    void routeRejectsBlankIdAndInvalidTimeOrDelay() {
        assertThrows(IllegalArgumentException.class, () -> route(" ", 1, 10, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> route("R", 1, 0, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> route("R", 1, 10, 0, 1, -1, 0, 0, true, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> route("R", 1, 10, 0, 1, 0, -1, 0, true, Set.of()));
    }

    @Test
    void zeroCostDistanceAndEmissionsAreValidBoundaryValues() {
        Route route = route("walking", 0, 1, 0, 0);

        assertEquals(0.0, route.distanceKm());
        assertEquals(0.0, route.baseCost());
        assertEquals(0.0, route.carbonEmission());
    }

    @Test
    void locationRejectsInvalidCoordinatesAndBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Location("", 43, 76));
        assertThrows(IllegalArgumentException.class, () -> new Location("A", 91, 76));
        assertThrows(IllegalArgumentException.class, () -> new Location("A", 43, -181));
        assertThrows(IllegalArgumentException.class, () -> new Location("A", Double.NaN, 76));
    }

    @Test
    void requestRejectsMissingPriorityAndInvalidLimits() {
        assertThrows(NullPointerException.class, () -> new DeliveryRequest(null, 100, 10, false));
        assertThrows(IllegalArgumentException.class,
                () -> new DeliveryRequest(DeliveryPriority.STANDARD, -1, 10, false));
        assertThrows(IllegalArgumentException.class,
                () -> new DeliveryRequest(DeliveryPriority.STANDARD, Double.NaN, 10, false));
        assertThrows(IllegalArgumentException.class,
                () -> new DeliveryRequest(DeliveryPriority.STANDARD, 100, 0, false));
        assertThrows(IllegalArgumentException.class,
                () -> new DeliveryRequest(DeliveryPriority.STANDARD, 100, 10, false, -1, Set.of()));
    }

    @Test
    void domainDefensivelyCopiesRestrictionSets() {
        Set<String> requestRestrictions = new HashSet<>(Set.of("blocked"));
        DeliveryRequest request = new DeliveryRequest(DeliveryPriority.STANDARD, 100, 10,
                false, 5, requestRestrictions);
        requestRestrictions.clear();
        assertEquals(Set.of("blocked"), request.prohibitedRouteIds());
        assertThrows(UnsupportedOperationException.class, () -> request.prohibitedRouteIds().clear());

        Set<DeliveryPriority> routeRestrictions = new HashSet<>(Set.of(DeliveryPriority.URGENT));
        Route route = route("R", 1, 10, 0, 1, 0, 0, 0, true, routeRestrictions);
        routeRestrictions.clear();
        assertEquals(Set.of(DeliveryPriority.URGENT), route.prohibitedPriorities());
        assertThrows(UnsupportedOperationException.class, () -> route.prohibitedPriorities().clear());
    }
}
