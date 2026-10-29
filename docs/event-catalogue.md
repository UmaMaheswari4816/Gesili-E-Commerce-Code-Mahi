# Event Catalogue (AsyncAPI-style markdown)

All events are published to Kafka wrapped in a versioned `EventEnvelope<T>` (see
`common-lib`). Consumers deduplicate on `eventId` (at-least-once delivery), route on
`eventType`, and interpret `payload` per `version`.

## Envelope

| Field | Type | Notes |
|---|---|---|
| `eventId` | UUID | dedup / idempotency key |
| `eventType` | string | e.g. `OrderCreated` |
| `version` | int | schema version of `payload` |
| `occurredAt` | instant (UTC) | producer clock |
| `correlationId` | string | gateway → services → traces |
| `source` | string | producing service |
| `payload` | object | typed body (below) |

## Topics, keys, partitions, consumer groups

| Topic | Partitions (dev) | Key | Consumer groups |
|---|---|---|---|
| `order.events` | 3 | `orderId` | inventory-service, payment-service, notification-service |
| `inventory.events` | 3 | `orderId` | order-service, payment-service, notification-service |
| `payment.events` | 3 | `orderId` | order-service, inventory-service, notification-service |
| `*.DLT` | 1 | inherited | ops/alerting |

Keying by `orderId` keeps all of a saga's events on one partition → per-order ordering.

## Retry & dead-letter policy

- **Non-retryable** (deserialization, validation, business-rule violations) → straight to
  `<topic>.DLT`, no retry.
- **Retryable** (transient DB/connection errors) → exponential backoff (1s → 2s → 4s, ~3–5
  attempts) → then `<topic>.DLT`.
- Producer: `enable.idempotence=true`, `acks=all`. Consumers: manual ack after processing.

## Events

### OrderCreated — `order.events` (v1)
Producer: order-service · Consumers: inventory-service, notification-service
```jsonc
{ "orderId": "uuid", "userId": "uuid",
  "items": [{ "productId": "uuid", "sku": "TSHIRT-RED-XL-001", "quantity": 2, "unitPrice": 499.00 }],
  "totalAmount": 998.00, "currency": "INR" }
```

### InventoryReserved — `inventory.events` (v1)
Producer: inventory-service · Consumers: order-service, payment-service
```jsonc
{ "orderId": "uuid",
  "reservations": [{ "reservationId": "uuid", "productId": "uuid", "sku": "…", "quantity": 2 }],
  "expiresAt": "2026-10-29T10:35:00Z" }   // TTL 20m > payment deadline 15m
```

### InventoryReservationFailed — `inventory.events` (v1)
Producer: inventory-service · Consumers: order-service, notification-service
```jsonc
{ "orderId": "uuid", "reason": "OUT_OF_STOCK", "unavailable": ["SKU-1"] }
```

### InventoryReleased — `inventory.events` (v1)
Producer: inventory-service · Consumers: order-service, notification-service
```jsonc
{ "orderId": "uuid", "reservationIds": ["uuid"], "reason": "TTL_EXPIRED|PAYMENT_FAILED|ORDER_CANCELLED" }
```
Idempotent: re-processing must never re-add stock for an already-released reservation.

### PaymentCompleted — `payment.events` (v1)
Producer: payment-service · Consumers: order-service, inventory-service, notification-service
```jsonc
{ "orderId": "uuid", "paymentId": "uuid", "providerRef": "pay_…", "amount": 998.00, "currency": "INR" }
```

### PaymentFailed — `payment.events` (v1)
Producer: payment-service · Consumers: order-service, inventory-service
```jsonc
{ "orderId": "uuid", "paymentId": "uuid", "reason": "DECLINED" }
```

### PaymentRefunded — `payment.events` (v1)
Producer: payment-service · Consumers: order-service, notification-service
```jsonc
{ "orderId": "uuid", "paymentId": "uuid", "providerRef": "rfnd_…", "amount": 998.00,
  "reason": "LATE_PAYMENT_ON_CANCELLED" }
```

### OrderConfirmed — `order.events` (v1)
Producer: order-service · Consumers: notification-service
```jsonc
{ "orderId": "uuid" }
```

### OrderCancelled — `order.events` (v1)
Producer: order-service · Consumers: notification-service, inventory-service
```jsonc
{ "orderId": "uuid", "reason": "OUT_OF_STOCK|PAYMENT_FAILED|PAYMENT_TIMEOUT" }
```
