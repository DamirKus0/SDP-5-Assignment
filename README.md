# Smart Delivery and Route Optimization Engine

Assignment 5: a Java 17 application demonstrating the **Strategy pattern**. It selects the fastest, cheapest, greenest, balanced, or shortest route from predefined candidates, checking hard constraints before scoring.

## Build and run

Requires JDK 17 or newer. The included Maven Wrapper downloads Maven 3.9.11 and dependencies on the first run.

```bash
./mvnw clean verify
java -jar target/smart-delivery.jar demo
```

On Windows, use `.\mvnw.cmd clean verify`. Run tests separately with `./mvnw test`.

| Mode | Purpose |
| --- | --- |
| `demo` (default) | Runtime switching, automatic selection, comparison, and failures |
| `compare` | Compare all five strategies on identical input |
| `failures` | Show empty candidates, infeasible requests, and invalid weights |
| `help` or `--help` | Print usage |

## Architecture

`RoutePlanner` is the context and delegates selection to the `RouteStrategy` interface. `setStrategy()` replaces the algorithm at runtime. `AbstractRouteStrategy` shares filtering, scoring, and deterministic selection; each concrete strategy supplies its scoring rule.

| Role | Implementation |
| --- | --- |
| Context | [`RoutePlanner`](src/main/java/kz/sdp/delivery/planning/RoutePlanner.java) |
| Strategy | [`RouteStrategy`](src/main/java/kz/sdp/delivery/strategy/RouteStrategy.java) |
| Concrete strategies | `FastestRouteStrategy`, `CheapestRouteStrategy`, `EcoFriendlyRouteStrategy`, `BalancedRouteStrategy`, `DistanceFirstStrategy` |
| Shared validation and selection | `AbstractRouteStrategy` |
| Algorithm selection by business rules | `StrategySelector` |
| Constraints and shared metrics | `RouteConstraints`, `RouteMetricsCalculator` |
| Algorithm comparison | `StrategyComparison` |
| Explainable result | `RouteDecision`, `RouteMetrics`, `RouteRejection` |

| Package | Responsibility |
| --- | --- |
| `domain` | Routes, requests, locations, and immutable results |
| `routing` | Hard constraints and shared metric calculation |
| `strategy` | Strategy interface and five algorithms |
| `planning` | Planner, automatic selector, and comparison |
| `app` | CLI, sample routes, and reporting |

`RouteDecision` includes the selected route, metrics, score, explanation, and rejected candidates with reasons.

## Algorithms and metrics

| Strategy | Minimizes |
| --- | --- |
| Fastest | Delivery time, including traffic and weather |
| Cheapest | Total delivery cost |
| Eco-friendly | `CO2 * emissionWeight + distance * distanceWeight` |
| Balanced | Weighted normalized time, cost, distance, and CO2 |
| Distance-first | Distance |

```text
deliveryMinutes = estimatedMinutes + trafficDelayMinutes + weatherDelayMinutes
totalCost = baseCost + distanceKm * 40 + prioritySurcharge + additionalFees
prioritySurcharge = 500 KZT for URGENT, otherwise 0
```

Eco defaults: emission weight `1.0`, distance weight `0.03`. Balanced defaults: time `0.40`, cost `0.30`, distance `0.10`, emissions `0.20`. Balanced normalizes each criterion using `(x - min) / (max - min)` across feasible candidates; constant criteria contribute zero. Weights must be finite and nonnegative; Balanced weights must sum to one within `1e-9`.

Lower scores win, with route ID breaking ties. Scores from different strategies are not directly comparable.

## Constraints and selection

Candidates must be available, within budget, deadline, and emission limits, and allowed by both request and priority restrictions. Limits are inclusive. Route IDs must be unique, and all routes must share endpoints. Invalid input is rejected; empty or entirely infeasible candidates raise `NoSuitableRouteException`.

`StrategySelector` uses the following default mapping:

| Priority | Default strategy |
| --- | --- |
| URGENT | Fastest |
| ECONOMY | Cheapest |
| ECO | Eco-friendly |
| STANDARD | Balanced |

An eco preference overrides nonurgent priorities; urgent deliveries retain Fastest.

Add an algorithm by implementing `RouteStrategy` or extending `AbstractRouteStrategy`, then register it for comparison or automatic selection. `RoutePlanner` requires no changes. This supports SRP, OCP, and DIP through separate responsibilities and interface-based delegation.

## Verification and documentation

**62 JUnit tests pass**, covering validation, formulas, constraints, normalization, ties, runtime switching, and automatic selection.

| Test suite | Tests | Main checks |
| --- | ---: | --- |
| `DomainValidationTest` | 9 | Invalid numbers, coordinates, required data, defensive copies |
| `StrategyAlgorithmsTest` | 23 | Five algorithms, formulas, custom weights, normalization, ties, invalid weights |
| `HardConstraintsTest` | 20 | Constraints across strategies, budget, deadline, infeasible routes, boundaries, time overflow |
| `PlanningIntegrationTest` | 10 | Three runtime switches, request-based selection, rule precedence, custom mappings, comparison |

- [Class diagram](docs/class-diagram.mmd)
- [Sequence diagram](docs/sequence-diagram.mmd)
- [Demo output](docs/console-output.txt)
- [Saved test results](docs/test-results.txt)

The demo uses eight synthetic routes between an Almaty depot and hospital. The application selects existing routes; it does not calculate road paths or allocate vehicles. Prices and delays are fixed inputs. Financial production use would require decimal arithmetic and explicit rounding rules.

## UML class diagram

```mermaid
classDiagram
    class RouteStrategy {
        <<interface>>
        +selectRoute(List~Route~, DeliveryRequest) RouteDecision
        +getStrategyName() String
    }
    class RoutePlanner {
        -RouteStrategy strategy
        +RoutePlanner(RouteStrategy)
        +setStrategy(RouteStrategy) void
        +plan(List~Route~, DeliveryRequest) RouteDecision
    }
    class AbstractRouteStrategy {
        <<abstract>>
        -RouteConstraints constraints
        +selectRoute(List~Route~, DeliveryRequest) RouteDecision
        #scoringFor(List~EligibleRoute~) ScoringRule
    }
    class FastestRouteStrategy
    class CheapestRouteStrategy
    class EcoFriendlyRouteStrategy
    class BalancedRouteStrategy
    class DistanceFirstStrategy
    class StrategySelector {
        -Map strategies
        +select(DeliveryRequest) RouteStrategy
    }
    class StrategyComparison {
        -List~RouteStrategy~ strategies
        +compare(List~Route~, DeliveryRequest) List~RouteDecision~
    }
    class RouteConstraints {
        +evaluate(List~Route~, DeliveryRequest) Evaluation
    }
    class RouteMetricsCalculator {
        +calculate(Route, DeliveryRequest) RouteMetrics
    }
    class Route {
        <<record>>
        +String id
        +Location origin
        +Location destination
        +double distanceKm
        +int estimatedMinutes
        +double baseCost
        +double carbonEmission
        +int trafficDelayMinutes
        +int weatherDelayMinutes
        +double additionalFees
        +boolean available
        +Set prohibitedPriorities
    }
    class DeliveryRequest {
        <<record>>
        +DeliveryPriority priority
        +double maxBudget
        +int maxDeliveryMinutes
        +boolean ecoPreferred
        +double maxCarbonEmission
        +Set prohibitedRouteIds
    }
    class Location {
        <<record>>
    }
    class RouteDecision {
        <<record>>
        +String strategyName
        +Route route
        +RouteMetrics metrics
        +double score
        +List explanation
        +List~RouteRejection~ rejections
    }
    class RouteMetrics {
        <<record>>
    }
    class RouteRejection {
        <<record>>
    }
    class NoSuitableRouteException {
        +rejections() List~RouteRejection~
    }

    RoutePlanner o-- RouteStrategy : strategy
    AbstractRouteStrategy ..|> RouteStrategy
    FastestRouteStrategy --|> AbstractRouteStrategy
    CheapestRouteStrategy --|> AbstractRouteStrategy
    EcoFriendlyRouteStrategy --|> AbstractRouteStrategy
    BalancedRouteStrategy --|> AbstractRouteStrategy
    DistanceFirstStrategy --|> AbstractRouteStrategy
    StrategySelector --> RouteStrategy : selects from mapping
    StrategySelector ..> DeliveryRequest
    StrategyComparison o-- RouteStrategy : executes registry
    AbstractRouteStrategy *-- RouteConstraints
    AbstractRouteStrategy ..> RouteDecision : returns
    RouteConstraints ..> RouteMetricsCalculator : calculates metrics
    RouteConstraints ..> Route : filters
    RouteConstraints ..> DeliveryRequest : checks limits
    RouteConstraints ..> NoSuitableRouteException : throws
    RouteMetricsCalculator ..> RouteMetrics : creates
    Route --> Location : origin and destination
    RouteDecision --> Route
    RouteDecision --> RouteMetrics
    RouteDecision o-- RouteRejection
    NoSuitableRouteException o-- RouteRejection
```

## UML sequence diagram

```mermaid
sequenceDiagram
    actor Client
    participant S as StrategySelector
    participant P as RoutePlanner
    participant F as FastestRouteStrategy
    participant C as RouteConstraints
    participant M as RouteMetricsCalculator
    participant D as RouteDecision

    Client->>S: select(urgentRequest)
    S-->>Client: FastestRouteStrategy
    Client->>P: setStrategy(selectedStrategy)
    Client->>P: plan(routes, urgentRequest)
    P->>F: selectRoute(routes, urgentRequest)
    Note over F: selectRoute() inherited<br/>from<br/>AbstractRouteStrategy
    F->>C: evaluate(routes, urgentRequest)
    alt Candidate list is empty
        C--xF: NoSuitableRouteException
        F--xP: propagate exception
        P--xClient: propagate exception
    else Candidate list is nonempty
        loop Every candidate route
            C->>M: calculate(route, urgentRequest)
            M-->>C: RouteMetrics
            C->>C: collect eligible route or rejection reasons
        end
        alt No feasible route
            C--xF: NoSuitableRouteException(rejections)
            F--xP: propagate exception
            P--xClient: propagate exception
        else Feasible routes exist
            C-->>F: Evaluation(eligible, rejections)
            F->>F: scoringFor(eligible)
            F->>F: score each route, minimize score then route ID
            F->>D: new RouteDecision(strategyName, winner, metrics, score, explanation, rejections)
            D-->>F: decision
            F-->>P: decision
            P-->>Client: decision
        end
    end
```
