package kz.sdp.delivery.domain;

public record RouteMetrics(long deliveryMinutes, double totalCost,
                           double distanceKm, double carbonEmission) { }
