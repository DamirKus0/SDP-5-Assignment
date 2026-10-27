package kz.sdp.delivery.domain;

/** Comparable metrics use minutes, KZT, kilometres and kilograms of CO2. */
public record RouteMetrics(long deliveryMinutes, double totalCost,
                           double distanceKm, double carbonEmission) { }
