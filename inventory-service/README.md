# inventory-service

Authoritative for **stock availability and reservations**. Prevents overselling, holds
reservations with a TTL, expires stale ones, and makes releases idempotent.

## No overselling — how

The reserve path locks the stock row with `SELECT ... FOR UPDATE` (pessimistic), checks
availability, and holds — all inside one transaction. Because the row is locked, two
concurrent buyers are serialized, so they can't both pass the availability check. The
`@Version` column adds optimistic protection on other paths. The DB also enforces
`available >= 0` and `reserved >= 0` as a last-resort guard.

## Reservation lifecycle

`HELD` → `COMMITTED` (payment captured) or `RELEASED` (failure/timeout/expiry).

- **TTL**: `HELD` reservations expire after `reservation-ttl-seconds` (default **1200 = 20m**,
  intentionally longer than the order payment deadline of 15m so a reservation never expires
  mid-payment).
- **Expiry job**: a `@Scheduled` sweep wrapped in **ShedLock** (runs on one instance only)
  releases expired `HELD` reservations.
- **Idempotent reserve**: keyed on `orderId:sku` — a duplicate reserve returns the existing
  reservation, never double-holds.
- **Idempotent release**: status-guarded — only `HELD → RELEASED` returns stock; releasing an
  already released/committed reservation is a safe no-op.

## Run (dev)

Needs PostgreSQL (`gesili_inventory`).
```powershell
.\mvnw.cmd -pl inventory-service spring-boot:run
```
Port **9003**. Swagger: http://localhost:9003/swagger-ui.html

## Verify

```bash
# set stock (ADMIN token)
curl -X PUT http://localhost:9003/api/v1/inventory/stock -H "Authorization: Bearer <ADMIN>" \
  -H "Content-Type: application/json" \
  -d '{"productId":"<PID>","sku":"TSHIRT-RED-XL-001","available":10}'

# reserve (any authenticated token)
curl -X POST http://localhost:9003/api/v1/inventory/reservations -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"<OID>","productId":"<PID>","sku":"TSHIRT-RED-XL-001","quantity":2}'

# check stock
curl http://localhost:9003/api/v1/inventory/stock/TSHIRT-RED-XL-001 -H "Authorization: Bearer <TOKEN>"
```

## Tests

```powershell
.\mvnw.cmd -pl inventory-service verify   # InventoryServiceIT: no-oversell under concurrency,
                                          # idempotent reserve + release (Postgres Testcontainers)
```
The IT needs Docker; it runs in CI. The concurrency test spins 20 threads against 10 units and
asserts stock never goes negative and successes never exceed stock.
