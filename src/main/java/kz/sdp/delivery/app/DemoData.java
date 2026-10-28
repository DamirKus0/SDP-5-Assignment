package kz.sdp.delivery.app;

import java.util.List;
import java.util.Set;
import kz.sdp.delivery.domain.DeliveryPriority;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.domain.Location;
import kz.sdp.delivery.domain.Route;

public final class DemoData {
    private DemoData() { }

    public static List<Route> routes() {
        var depot = new Location("Almaty depot", 43.2389, 76.8897);
        var hospital = new Location("Almaty hospital", 43.2560, 76.9286);
        return List.of(
                new Route("R1-Express", depot, hospital, 18, 25, 3400, 4.8, 2, 1, 150, true, Set.of()),
                new Route("R2-City", depot, hospital, 12, 22, 1800, 3.8, 18, 2, 100, true, Set.of()),
                new Route("R3-Electric", depot, hospital, 15, 38, 2100, 0.6, 4, 0, 0, true, Set.of()),
                new Route("R4-Economy", depot, hospital, 24, 46, 700, 3.2, 3, 1, 50, true, Set.of()),
                new Route("R5-Avenue", depot, hospital, 16, 30, 1800, 1.4, 3, 1, 0, true, Set.of()),
                new Route("R6-Local", depot, hospital, 9, 27, 1900, 2.5, 7, 2, 250, true, Set.of()),
                new Route("R7-Tunnel", depot, hospital, 8, 18, 500, 0.4, 0, 0, 0, false, Set.of()),
                new Route("R8-Freight", depot, hospital, 20, 33, 1000, 4.0, 3, 1, 0, true,
                        Set.of(DeliveryPriority.URGENT)));
    }

    public static DeliveryRequest standardRequest() {
        return new DeliveryRequest(DeliveryPriority.STANDARD, 6000, 90, false);
    }
}
