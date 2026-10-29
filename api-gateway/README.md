# api-gateway

Spring Cloud **Gateway** (reactive/WebFlux) — the single public entry point.

## Responsibilities

- **Discovery routing**: routes to services via `lb://service-name` resolved through Eureka.
- **JWT validation at the edge**: validates RS256 tokens against the auth-service JWKS
  (`jwk-set-uri`). Resource servers validate again (defence in depth).
- **Rate limiting**: Redis-backed, per authenticated user (JWT subject) or per IP.
- **CORS**: configured globally (allow the frontend origin).
- **Correlation id**: generates/propagates `X-Correlation-Id` (headers only — body untouched).
- **Circuit-breaker fallbacks**: Resilience4j; open circuit → clean RFC 7807 503 from `/fallback/*`.
- **Aggregated Swagger UI**: one UI at `/swagger-ui.html` with a per-service dropdown.

## Run (dev)

Needs Eureka + at least one downstream service (auth-service), and Redis for rate limiting.

```powershell
cd infra; docker compose --profile phase3 up -d   # postgres, redis, config, registry
.\mvnw.cmd -pl auth-service spring-boot:run        # (separate terminal)
.\mvnw.cmd -pl api-gateway spring-boot:run
```

Port **8080**.

## Verify

```bash
# Through the gateway (note the port 8080):
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"S3curePass!"}'

# Protected call via the gateway (edge JWT validation):
curl http://localhost:8080/api/v1/auth/me -H "Authorization: Bearer <ACCESS_TOKEN>"

# Rate limit: hammer an endpoint and watch for HTTP 429 after the burst.
# Aggregated Swagger UI:
start http://localhost:8080/swagger-ui.html
```

## Payment webhook route (Phase 0 Note 2)

`/api/v1/payments/webhook/**` is **public** (providers can't carry a JWT) and has **no
body-reading filters** — no rate-limiter body inspection, no body rewrite, no body caching.
The payment-service (Phase 7) verifies the provider signature over the **raw request bytes**,
so the gateway must forward the body untouched. Any filter that reads/buffers/rewrites the
body on this route would break signature verification. Keep this route filter-free.

## Failure behavior

- **Downstream service down**: its circuit opens → gateway returns the `/fallback/*` 503
  instead of hanging. Other routes are unaffected (bulkhead by route).
- **Redis down**: rate limiting fails closed/open depending on config; in dev it logs and
  allows. Monitor in prod.
- **Auth JWKS unreachable**: edge validation fails; resource servers still validate with
  cached keys. Prefer a reachable, cached JWKS.
