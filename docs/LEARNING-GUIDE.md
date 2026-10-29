# Learning Guide — how a request flows through Gesili

This guide traces real requests end-to-end so you can read the code in the order things
actually happen. Keep it open next to the source.

## The 30-second mental model

```
Browser (Next.js :3000)
   │  every call carries a JWT (except login/register/webhook)
   ▼
API Gateway :8080   ── validates JWT, rate-limits, adds X-Correlation-Id ──┐
   │  routes by lb://<service> via Eureka                                   │
   ▼                                                                        │
Business services (auth, product, inventory, cart, order, payment, notify)  │
   │  each validates the JWT again (defence in depth)                       │
   ▼                                                                        │
PostgreSQL (one DB per service) · Redis (cart) · Elasticsearch (search)     │
   │                                                                        │
   └──── order/payment/inventory talk to each other ONLY via Kafka events ──┘
         (the "saga" — no direct calls between them)
```

Two supporting services start first and everything leans on them:
- **config-server :8888** — hands every service its configuration at boot.
- **service-registry (Eureka) :8761** — every service registers here; the gateway and Feign
  clients look services up by name instead of hardcoding host:port.

---

## Flow 1 — Login (synchronous, simplest)

Read these files in order:

1. `frontend/src/app/login/page.tsx` — the form calls `api.login()`.
2. `frontend/src/lib/api.ts` → `login()` — POSTs to `/api/v1/auth/login` **through the gateway**.
3. `api-gateway` → `GatewaySecurityConfig` — `/api/v1/auth/login` is PUBLIC (no token needed),
   so the request passes straight through; the route sends it to `lb://auth-service`.
4. `auth-service` → `AuthController.login()` — entry point.
5. → `AuthService.login()` — checks the password (BCrypt), then:
   - `JwtService.issueAccessToken()` signs a short-lived **RS256** JWT with the private key.
   - `RefreshTokenService.issueNewFamily()` creates a rotating refresh token (only its hash
     is stored).
6. The token comes back to the browser; `tokenStore.set()` saves it.

**How other services later trust that token:** they fetch auth-service's **public** key from
`GET /.well-known/jwks.json` (`JwksController`) and verify the signature locally — no shared
secret. See any service's `SecurityConfig` (`jwk-set-uri`).

---

## Flow 2 — Browse & add to cart (synchronous, crosses two services)

1. `frontend` home `page.tsx` → `api.search()` → gateway → `product-service`
   `ProductQueryController.search()`.
2. `product-service` reads from **Elasticsearch** (the CQRS *read* model), not Postgres —
   `CatalogQueryService.search()`. (Writes go to Postgres via `CatalogCommandService` and are
   projected into ES by `ProductProjectionService` — that's the CQRS split.)
3. "Add to cart" → gateway → `cart-service` `CartController.addItem()` →
   `CartService.addItem()` → stored in **Redis** under `cart:{userId}`.

**Resilience demo lives here:** `cart-service` can also call `product-service` over OpenFeign
(`ProductClient`) wrapped in Resilience4j (`ProductLookupService`) — circuit breaker + retry +
bulkhead + timeout, with a graceful fallback. Endpoint: `GET /api/v1/cart/product-info/{id}`.

---

## Flow 3 — Checkout: the order SAGA (asynchronous, the heart of the system)

This is the important one. No service calls another directly — they communicate by **events on
Kafka**. Follow the arrows:

```
order-service                 Kafka topics              other services
─────────────                 ───────────               ──────────────
POST /orders
 OrderController.place()
  → OrderService.placeOrder()
     saves Order(PENDING_INVENTORY)
     + writes OutboxEvent        ── order.events ──▶   inventory-service
     (SAME DB transaction)         (OrderCreated)       InventorySagaListener
                                                         → reserve stock (lock, no oversell)
     OutboxRelay polls the                               → emit InventoryReserved
     outbox table and publishes  ◀── inventory.events ──  (or ...Failed)
     to Kafka (single writer)      (InventoryReserved)
  SagaEventListener
   → onInventoryReserved()
     Order → PAYMENT_PENDING
     (+ 15-min payment deadline)

                                 ── inventory.events ─▶  payment-service
                                   (InventoryReserved)    PaymentSagaListener
                                                          → create provider order
                                                          → payment INITIATED
                                   (customer pays at provider; provider calls the webhook)
                                 ◀── payment.events ────  WebhookController (raw body,
                                   (PaymentCompleted)      signature-verified, idempotent)
  SagaEventListener                                       → PaymentService emits
   → onPaymentCompleted()                                   PaymentCompleted
     Order → CONFIRMED
     + emit OrderConfirmed       ── order.events ──────▶  notification-service
                                   (OrderConfirmed)        NotificationEventListener
                                                           → email "order confirmed"
```

**Compensation (when something fails):**
- Out of stock → `InventoryReservationFailed` → order `CANCELLED`.
- Payment fails → `PaymentFailed` → inventory **releases** the hold → order `CANCELLED`.
- Payment never arrives → `PaymentTimeoutJob` cancels the order → inventory releases.
- Late payment on an already-cancelled order → `payment-service` auto-**refunds**
  (`PaymentRefunded`).

**Why the outbox?** `OrderService.placeOrder()` writes the order row AND the event row in one
DB transaction, so they can never disagree. `OutboxRelay` then publishes to Kafka. Read
`order-service/.../outbox/OutboxRelay.java` — the key line is
`WHERE published_at IS NULL ORDER BY id`.

**Why is everything idempotent?** Kafka delivers "at least once" — the same event can arrive
twice. Every consumer dedupes on the event's `eventId` (`EventDeduplicator` /
`processed_events` table), and inventory reserve/release are guarded so a repeat does no harm.

**What the frontend sees:** `POST /orders` returns `202` immediately with status
`PENDING_INVENTORY`. The order page (`frontend/src/app/orders/[id]/page.tsx`) **polls**
`GET /orders/{id}` every 2s and shows the status changing as the saga progresses.

---

## Suggested reading order (start here)

1. `docs/event-catalogue.md` — the events and topics (the vocabulary of the saga).
2. `common-lib` — `EventEnvelope`, the `event/payload/*` records, the enums. These are the
   shared contracts every service agrees on.
3. Flow 1 files (auth) — simplest end-to-end request.
4. Flow 2 files (product + cart) — CQRS and a cross-service call.
5. Flow 3 files (order → inventory → payment → notification) — the saga + outbox. Read
   `OrderService`, then `OutboxRelay`, then each `*SagaListener`/`*EventListener`.
6. `docs/observability-and-resilience.md` — how to watch all this at runtime.

## Glossary (the patterns, in one line each)

| Term | What it means here |
|---|---|
| Saga (choreography) | a multi-service transaction coordinated by events, no central boss |
| Outbox | write the event to the DB with the data, publish to Kafka later — never lose an event |
| Idempotency | processing the same event twice has the same effect as once |
| CQRS | separate write model (Postgres) from read model (Elasticsearch) |
| Compensation | an "undo" action (release stock, refund) when a later step fails |
| JWKS | the public keys other services use to verify auth-service's JWTs |
| Circuit breaker | stop calling a failing dependency for a while, fail fast instead |
```
