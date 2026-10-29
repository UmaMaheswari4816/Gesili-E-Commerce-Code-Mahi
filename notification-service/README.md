# notification-service

Consumes saga events and sends **email/SMS** via a channel **Strategy**. Idempotent,
templated, and logs every notification. Owns no business data — only an audit log.

## What it does

Listens on `order.events`, `payment.events`, `inventory.events` and notifies the customer on:
`OrderConfirmed`, `OrderCancelled`, `PaymentCompleted`, `PaymentFailed`, `PaymentRefunded`,
`InventoryReservationFailed`. Everything else is ignored.

## Design

- **Channel Strategy**: `NotificationChannel` with `EmailChannel` + `SmsChannel`. Adding a
  channel = one class. The channels are **dev stubs that log** the message (no SMTP/Twilio
  dependency); swapping in a real client is localized to `send()`.
- **Idempotent**: dedupe on the envelope `eventId` (`processed_events`), so a redelivered
  Kafka event never double-notifies.
- **Templated**: `NotificationTemplates` maps event type → subject + body (a real system uses
  Thymeleaf/FreeMarker + localized bundles).
- **Logged**: every send is written to `notifications` for an audit trail; view via
  `GET /api/v1/notifications/order/{orderId}`.

> Recipient resolution is simplified to a placeholder derived from the order id. A real impl
> looks up the user's email/phone (auth-service or a cached profile) — a documented extension.

## Run (dev)

Needs PostgreSQL (`gesili_notification`) and Kafka.
```powershell
.\mvnw.cmd -pl notification-service spring-boot:run
```
Port **9007**. With the saga running, place an order and watch `[EMAIL] ...` lines appear in
this service's logs as events flow; then:
```bash
curl http://localhost:9007/api/v1/notifications/order/<ORDER_ID>
```

## Tests

```powershell
.\mvnw.cmd -pl notification-service test   # templating + dispatch + logging + channel fallback
```
