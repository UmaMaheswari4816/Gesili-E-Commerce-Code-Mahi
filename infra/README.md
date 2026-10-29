# infra — local platform (Docker Compose)

A single `docker-compose.yml` driven by **Compose profiles**. Run everything from this
`infra/` directory; values come from `../.env` (copy `../.env.example`).

## Quick start

```bash
cp ../.env.example ../.env          # fill in local values (never commit ../.env)

# Phase 1 — backing services needed to build & test:
docker compose --profile phase1 up -d

# Backing services only:
docker compose --profile infra up -d

# Observability stack (Prometheus, Grafana, Loki, Promtail):
docker compose --profile observability up -d

# Everything:
docker compose --profile full up -d

# Combine profiles:
COMPOSE_PROFILES=infra,observability docker compose up -d

# Tear down (keep volumes):
docker compose --profile full down
# Tear down and wipe data:
docker compose --profile full down -v
```

## Why profiles instead of multiple files

Each `phaseN` profile is **self-contained** — it tags its own backing-service
dependencies, so `docker compose --profile phase6 up` starts Kafka + Postgres (and later
the registry/config-server/order-service) without you remembering to start infra first.
Profiles compose additively, and a single file means no duplicated service definitions to
drift out of sync. On a limited-RAM machine, start only the current phase's profile and,
if you like, run the one service you're editing from your IDE instead of a container.

## Profile → service map

| Profile | Brings up |
|---|---|
| `infra` | postgres, redis, kafka, elasticsearch, zipkin |
| `observability` | prometheus, grafana, loki, promtail |
| `phase1` | postgres, redis, kafka, elasticsearch, zipkin (build/test infra) |
| `phase2` | postgres, config-server, service-registry |
| `phase3` | postgres, redis, config-server, service-registry, auth-service, api-gateway |
| `phase4` | postgres, redis, elasticsearch, config-server, service-registry, auth-service, api-gateway, product-service |
| `phase5` | postgres, redis, config-server, service-registry, auth-service, api-gateway, inventory-service, cart-service |
| `phase6` | postgres, kafka, config-server, service-registry, auth-service, api-gateway, inventory-service, order-service |
| `phase7` | postgres, kafka, config-server, service-registry, auth-service, api-gateway, inventory-service, order-service, payment-service |
| `phase8` | postgres, kafka, config-server, service-registry, auth-service, api-gateway, inventory-service, order-service, payment-service, notification-service |
| `phase9` | zipkin + full observability |
| `full` | everything |

## Service endpoints (local)

| Service | URL |
|---|---|
| PostgreSQL | `localhost:5432` (user/pass from `.env`) |
| Redis | `localhost:6379` |
| Kafka | `localhost:9092` |
| Elasticsearch | http://localhost:9200 |
| Zipkin | http://localhost:9411 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin / `.env` password) |
| Loki | http://localhost:3100 |

PostgreSQL creates one database per service on first boot
(`gecili_auth`, `gecili_product`, `gecili_inventory`, `gecili_order`, `gecili_payment`)
via `postgres/init-databases.sh` — **database per service, no shared tables**.

---

## Design notes carried from Phase 0

These two decisions were locked in Phase 0 and are implemented in the phases noted. They
are recorded here so the infra and the services that depend on it stay consistent.

> **Note 1 — Outbox relay query (implemented in Phase 6, order-service).**
> The outbox relay selects unpublished rows with
> `SELECT ... WHERE published_at IS NULL ORDER BY id` — a simple "unpublished, oldest
> first" scan with **no high-water-mark cursor**. Rows are published to Kafka keyed by
> `aggregate_id` (orderId) so per-order ordering holds end to end, then stamped
> `published_at`. A single-writer relay (ShedLock) prevents two instances from
> publishing the same rows or interleaving order. At-least-once publishing is safe
> because all consumers deduplicate on `eventId`.

> **Note 2 — Webhook route has no body-reading gateway filters (implemented in Phase 3
> gateway + Phase 7 payment-service).**
> The payment provider webhook route (`/api/v1/payments/webhook/**`) is public at the
> gateway (providers can't carry a JWT) and must pass the request body through
> **untouched**. No gateway filter may read, buffer, or rewrite the body on this route,
> because the payment-service verifies the provider signature (Razorpay
> `X-Razorpay-Signature` / Stripe `Stripe-Signature`) over the **raw bytes**. Any
> body-reading filter would alter the bytes and break signature verification. The route
> applies forward-only state transitions and idempotency on the provider event id.
