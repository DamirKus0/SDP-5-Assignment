package kz.sdp.delivery.domain;

import java.util.List;

public record RouteDecision(String strategyName, Route route, RouteMetrics metrics,
                            double score, List<String> explanation,
                            List<RouteRejection> rejections) {
    public RouteDecision {
        explanation = List.copyOf(explanation);
        rejections = List.copyOf(rejections);
    }
}
