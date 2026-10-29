# cart-service

A **Redis-backed** shopping cart, keyed by user id, with a TTL so abandoned carts expire.

## Why Redis (not a relational DB)

A cart is high-churn, short-lived, session-like state — a perfect fit for an in-memory store
with native TTL. Storing it in Postgres would add write load and need a cleanup job; Redis
expires abandoned carts for free.

## Design notes

- The cart belongs to the **authenticated user** — the user id comes from the JWT subject,
  never the client, so no one can read/mutate another user's cart.
- The cart is a **draft**: `unitPrice` is a snapshot at add-time. Order-service re-validates
  price and stock at checkout (prices drift; stock is owned by inventory-service).
- Stored as JSON under `cart:{userId}`; every write refreshes the TTL (default 7 days).

## Run (dev)

Needs Redis (Memurai on Windows) on :6379.
```powershell
.\mvnw.cmd -pl cart-service spring-boot:run
```
Port **9004**. Swagger: http://localhost:9004/swagger-ui.html

## Verify

```bash
curl -X POST http://localhost:9004/api/v1/cart/items -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"productId":"<PID>","sku":"TSHIRT-RED-XL-001","name":"Red Tee","quantity":2,"unitPrice":499.00}'

curl http://localhost:9004/api/v1/cart -H "Authorization: Bearer <TOKEN>"
```

## Tests

```powershell
.\mvnw.cmd -pl cart-service test   # CartTest: merge/update/remove/total — no Redis needed
```
