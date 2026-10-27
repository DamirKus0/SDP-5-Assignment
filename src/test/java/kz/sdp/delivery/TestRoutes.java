package kz.sdp.delivery;

import java.util.Set;
import kz.sdp.delivery.domain.DeliveryPriority;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.domain.Location;
import kz.sdp.delivery.domain.Route;

final class TestRoutes {
    static final Location ORIGIN = new Location("Warehouse", 43.25, 76.95);
    static final Location DESTINATION = new Location("Hospital", 43.27, 76.98);
    static final DeliveryRequest STANDARD =
            new DeliveryRequest(DeliveryPriority.STANDARD, 100_000, 240, false);

    private TestRoutes() { }

    static Route route(String id, double distance, int minutes, double baseCost, double carbon) {
        return route(id, distance, minutes, baseCost, carbon, 0, 0, 0, true, Set.of());
    }

    static Route route(String id, double distance, int minutes, double baseCost, double carbon,
                       int traffic, int weather, double fees, boolean available,
                       Set<DeliveryPriority> prohibited) {
        return new Route(id, ORIGIN, DESTINATION, distance, minutes, baseCost, carbon,
                traffic, weather, fees, available, prohibited);
    }
}
