package kz.sdp.delivery.strategy;

import java.util.List;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.domain.Route;
import kz.sdp.delivery.domain.RouteDecision;

public interface RouteStrategy {
    RouteDecision selectRoute(List<Route> routes, DeliveryRequest request);
    String getStrategyName();
}
