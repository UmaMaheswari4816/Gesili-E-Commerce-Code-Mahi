# Phase 9 — Resilience & Observability

## Resilience4j (demonstrated: cart-service → product-service)

`cart-service` calls `product-service` over OpenFeign (`lb://product-service`), wrapped in
`ProductLookupService` with four patterns stacked:

| Pattern | Instance config | What it protects against |
|---|---|---|
| **Retry** | 3 attempts, 500ms exp backoff | transient blips (one dropped packet) |
| **CircuitBreaker** | open at 50% of last 10 calls, 10s open, 3 half-open probes | hammering a sick dependency; fail fast |
| **Bulkhead** | max 10 concurrent | a slow dependency exhausting all threads |
| **TimeLimiter** | 2s | a hung call blocking forever |

On exhaustion / open circuit / timeout the **fallback** returns empty and the cart degrades
gracefully (works without live product enrichment). Demo endpoint:
`GET /api/v1/cart/product-info/{productId}`. Circuit state is exposed at
`/actuator/health` and `/actuator/circuitbreakers`, and as the
`resilience4j_circuitbreaker_state` metric (graphed in Grafana).

The **API gateway** independently applies CircuitBreaker + RequestRateLimiter (Redis) +
timeouts per route (Phase 3), so resilience exists at both the edge and service-to-service
layers.

## Observability stack (lightweight, RAM-friendly)

```
services ──metrics──▶ Prometheus ─┐
services ──logs─────▶ Loki ◀─ Promtail (scrapes container logs)   ├─▶ Grafana (one UI)
services ──traces───▶ Zipkin ─────┘
```

- **Metrics**: Micrometer → `/actuator/prometheus`; Prometheus scrapes; Grafana dashboard
  `Gecili — Overview` (request rate, p95 latency, 5xx, circuit-breaker state, JVM heap, logs).
- **Logs**: Promtail ships container logs to Loki; the log pattern embeds
  `traceId`, `spanId`, and `correlationId` so a request's logs are filterable and line up with
  its trace.
- **Traces**: Micrometer Tracing (Brave) → Zipkin; sample rate 1.0 in dev.

Start it: `docker compose --profile observability up -d` (or it's included in `full`).
Grafana at http://localhost:3000, Zipkin at http://localhost:9411, Prometheus at :9090.

## Correlation-id ↔ trace story (end to end)

1. The gateway's `CorrelationIdFilter` sets `X-Correlation-Id` (generated if absent) and
   echoes it on the response.
2. It propagates downstream on every hop; each service puts it in the MDC, so it appears in
   every log line (`%X{correlationId}`).
3. Micrometer Tracing stamps `traceId`/`spanId` across the same hops and reports spans to
   Zipkin.
4. In Grafana/Loki, filter logs by `correlationId` to see one request across all services;
   open the matching `traceId` in Zipkin for the latency breakdown.

## Load testing

See [`../load-tests/README.md`](../load-tests/README.md) — k6 `smoke.js` (sanity) and
`checkout-load.js` (ramping 20→50 VUs) drive the flow through the gateway with pass/fail
thresholds, while Grafana shows the system under load.

## Failure scenarios exercised here

| Scenario | Behavior |
|---|---|
| product-service down | cart circuit opens → fallback → cart still serves (degraded) |
| product-service slow | TimeLimiter fires at 2s → fallback; Bulkhead caps concurrency |
| transient blip | Retry recovers within 3 attempts, no user impact |
| traffic spike | gateway rate limiter sheds excess (429); dashboards show it |
