package kz.sdp.delivery.domain;

import java.util.List;

public record RouteRejection(String routeId, List<String> reasons) {
    public RouteRejection {
        reasons = List.copyOf(reasons);
    }
}
