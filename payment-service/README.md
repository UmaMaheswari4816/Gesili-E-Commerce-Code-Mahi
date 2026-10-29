# payment-service

Payments via a provider **Strategy** (Razorpay default, Stripe second), an **async
webhook** flow with **raw-body signature verification**, idempotent webhooks,
**refund-on-late-payment**, and a **reconciliation** job.

## Flow

```
InventoryReserved → create provider order (Razorpay/Stripe) → payment INITIATED
                  → client completes checkout at the provider
provider → POST /api/v1/payments/webhook/{provider}  (public, raw body)
         → verify signature over RAW bytes
         → dedupe on provider event id (webhook_events)
         → forward-only transition:
             captured (active order)    → CAPTURED → PaymentCompleted
             captured (cancelled order) → REFUND_PENDING → refund → REFUNDED → PaymentRefunded
             failed                     → FAILED → PaymentFailed
reconciliation job (ShedLock) retries stuck refunds — the missed-webhook safety net
```

## Design decisions

- **Strategy + Factory**: `PaymentProvider` interface; `RazorpayProvider` (default) and
  `StripeProvider`. Adding a provider = one class. Chosen by config or `X-Payment-Provider`.
- **Raw-body signature verification (Phase 0 Note 2)**: the webhook controller takes
  `@RequestBody String` (unparsed bytes) and verifies the provider HMAC over exactly those
  bytes. The gateway route for `/api/v1/payments/webhook/**` is public with **no body-reading
  filters**, so the bytes reach us untouched.
- **Idempotent webhooks**: dedupe on the provider event id (`webhook_events` PK). Replays
  return 200 with no side effects.
- **Forward-only transitions**: a stale/out-of-order webhook can't move a payment backward.
- **Late-payment refund**: a capture for an already cancelled/timed-out order is auto-refunded
  (`PaymentRefunded`), order stays CANCELLED.

> Note: the providers are sandbox-shaped — signature verification and webhook parsing mirror
> the real contracts, but `createOrder`/`refund` synthesize references instead of calling the
> live APIs, so the service builds and tests without live keys or SDKs. Swapping in the real
> SDK is localized to `createOrder`/`refund`.

## Run (dev)

Needs PostgreSQL (`gesili_payment`) and Kafka. Sandbox keys via env (`.env`, gitignored).
```powershell
.\mvnw.cmd -pl payment-service spring-boot:run
```
Port **9006**. Swagger: http://localhost:9006/swagger-ui.html

## Simulating a webhook locally

Compute the HMAC-SHA256 of the raw JSON with your `RAZORPAY_WEBHOOK_SECRET`, then:
```bash
curl -X POST http://localhost:9006/api/v1/payments/webhook/razorpay \
  -H "X-Razorpay-Signature: <hex-hmac-of-body>" \
  -H "Content-Type: application/json" \
  --data '{"id":"evt_1","event":"payment.captured","notes":{"orderId":"<ORDER_ID>"},"payload":{"payment":{"entity":{"order_id":"order_abc"}}}}'
```

## Tests

```powershell
.\mvnw.cmd -pl payment-service test   # PaymentServiceTest: idempotency, capture, late-refund
```
