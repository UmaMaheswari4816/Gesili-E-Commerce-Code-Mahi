# order-service

Owns the **order lifecycle**, the **choreography saga**, and the **transactional outbox**.

## The saga (choreography)

```
place order ──persist order + outbox (1 TX)──▶ OrderCreated
  → inventory reserves → InventoryReserved → order: PAYMENT_PENDING (+ deadline)
      → payment captured → PaymentCompleted → order: CONFIRMED → OrderConfirmed
  compensation:
      out of stock  → InventoryReservationFailed → order: CANCELLED
      payment fails → PaymentFailed → inventory releases → order: CANCELLED
      payment late  → payment-timeout job → OrderCancelled(PAYMENT_TIMEOUT) → inventory releases
```

No central orchestrator — each service reacts to events and emits the next. (Orchestration is
the documented alternative: a single saga coordinator issuing commands; prefer it when flows
get complex or you need one place to see saga state.)

## Transactional outbox (Phase 0 Note 1)

`placeOrder` writes the `orders` row **and** an `outbox` row in one DB transaction. The
`OutboxRelay` then:
- selects `WHERE published_at IS NULL ORDER BY id` — oldest-first, **no high-water-mark cursor**;
- publishes each to Kafka **keyed by orderId** (per-order ordering end to end);
- stamps `published_at`;
- runs under **ShedLock** as a **single writer** so two instances never double-publish or
  interleave ordering.

At-least-once publication is safe because consumers dedupe on the envelope `eventId`
(`processed_events` table).

## Kafka policy

- Producer: `enable.idempotence=true`, `acks=all`.
- Consumer: manual ack after processing; consumer group `order-service`.
- Topics: `order.events`, `inventory.events`, `payment.events` — 3 partitions, keyed by orderId.
- Errors: exponential backoff retries, then route to `<topic>.DLT`.

## Run (dev)

Needs PostgreSQL (`gesili_order`) and Kafka (:9092).
```powershell
.\mvnw.cmd -pl order-service spring-boot:run
```
Port **9005**. Swagger: http://localhost:9005/swagger-ui.html

## Verify

```bash
# place an order (any authenticated token). Returns 202 + PENDING_INVENTORY.
curl -X POST http://localhost:9005/api/v1/orders -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"currency":"INR","items":[{"productId":"<PID>","sku":"TSHIRT-RED-XL-001","quantity":2,"unitPrice":499.00}]}'

# poll until status becomes PAYMENT_PENDING (inventory reserved) or CANCELLED (out of stock)
curl http://localhost:9005/api/v1/orders/<ORDER_ID> -H "Authorization: Bearer <TOKEN>"
```
(With inventory-service running and stock set, the status should advance to PAYMENT_PENDING.)

## Tests

```powershell
.\mvnw.cmd -pl order-service test     # OrderServiceTest: creation + saga transitions (mocks)
.\mvnw.cmd -pl order-service verify   # + outbox/saga ITs (Postgres + Kafka Testcontainers)
```
ITs need Docker; they run in CI.
