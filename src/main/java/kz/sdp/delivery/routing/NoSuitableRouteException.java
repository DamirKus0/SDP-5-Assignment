package kz.sdp.delivery.routing;

import java.util.List;
import kz.sdp.delivery.domain.RouteRejection;

public final class NoSuitableRouteException extends RuntimeException {
    private final List<RouteRejection> rejections;

    public NoSuitableRouteException(String message, List<RouteRejection> rejections) {
        super(message);
        this.rejections = List.copyOf(rejections);
    }

    public List<RouteRejection> rejections() {
        return rejections;
    }
}
