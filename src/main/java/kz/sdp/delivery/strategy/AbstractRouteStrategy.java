package kz.sdp.delivery.strategy;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import kz.sdp.delivery.domain.DeliveryRequest;
import kz.sdp.delivery.domain.Route;
import kz.sdp.delivery.domain.RouteDecision;
import kz.sdp.delivery.routing.RouteConstraints;
import kz.sdp.delivery.routing.RouteConstraints.EligibleRoute;

/** Reuses filtering and deterministic selection; each strategy supplies its own scoring rule. */
public abstract class AbstractRouteStrategy implements RouteStrategy {
    private final RouteConstraints constraints = new RouteConstraints();

    protected record ScoringRule(ToDoubleFunction<EligibleRoute> score,
                                 Function<EligibleRoute, List<String>> explanation) { }

    protected abstract ScoringRule scoringFor(List<EligibleRoute> candidates);

    @Override
    public final RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        var evaluation = constraints.evaluate(routes, request);
        var rule = scoringFor(evaluation.eligible());
        // Compute each score once and reject arithmetic overflow explicitly.
        record Scored(EligibleRoute candidate, double value) { }
        var selected = evaluation.eligible().stream().map(candidate -> {
            double value = rule.score().applyAsDouble(candidate);
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Calculated route score must be finite");
            }
            return new Scored(candidate, value);
        }).min(Comparator.comparingDouble(Scored::value)
                .thenComparing(scored -> scored.candidate().route().id())).orElseThrow();
        var best = selected.candidate();
        return new RouteDecision(getStrategyName(), best.route(), best.metrics(), selected.value(),
                rule.explanation().apply(best), evaluation.rejections());
    }
}
