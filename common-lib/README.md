# common-lib

Shared **contracts** for the Gesili e-commerce platform. Every service depends on this
library; it is the single source of truth for the data shapes that cross service
boundaries.

## What belongs here

| Package | Contents |
|---|---|
| `enums` | `OrderStatus`, `PaymentStatus`, `ReservationStatus`, `Role` |
| `event` | `EventEnvelope<T>` (versioned), `EventTypes` (topics + type constants) |
| `event.payload` | Kafka event payload records (OrderCreated, InventoryReserved, PaymentCompleted, …) |
| `error` | RFC 7807 `ProblemDetails` factory + `ErrorType` catalogue |
| `exception` | `BusinessException` + `ResourceNotFoundException`, `ConflictException`, `InsufficientStockException` |
| `constant` | `HttpHeaders`, `ApiConstants` |
| `validation` | shared constraints (`@Sku`) |
| `util` | `CorrelationIds` |

## What must NEVER go here

- **JPA entities** (`@Entity`), repositories, `@Service` beans, controllers
- Business logic / state-transition rules
- Database drivers, web servers, or heavy framework dependencies

### Why not share entities?

A JPA `@Entity` is a **persistence** concern bound to one service's schema. If two services
shared a `Product` entity, a column rename in one service's database would force a recompile
and redeploy of the other — coupling their release cycles. That is a **distributed
monolith**: the costs of microservices without the independence. Entities also drag in
Hibernate, a DB dialect and lazy-loading proxies that consumers should never inherit.

Sharing **contracts** (DTOs/events) is safe because a contract is designed to be stable and
versioned; sharing **implementation** (entities) leaks internal structure.

### Trade-offs vs. alternatives

| Approach | Pros | Cons |
|---|---|---|
| **Shared contract lib (this)** | DRY, compile-time safety, fast | Must version carefully; JVM-only |
| Per-service copies of DTOs | Total decoupling | Duplication, drift risk |
| Schema registry (Avro/Protobuf) | Language-agnostic, enforced compat | Extra infra + codegen |

The `EventEnvelope` versioning here is, by convention, the discipline a schema registry
would otherwise enforce — see `version` on the envelope and the per-payload version passed
by producers. Migration path to a registry is noted in `docs/adr/`.

## How services consume it

```xml
<dependency>
    <groupId>com.gesili.ecommerce</groupId>
    <artifactId>common-lib</artifactId>
    <!-- version managed by the parent POM's dependencyManagement -->
</dependency>
```

Build and install locally so other modules can resolve it:

```bash
./mvnw -pl common-lib -am clean install
```

## The versioned event envelope

Every Kafka message is an `EventEnvelope<T>`:

```jsonc
{
  "eventId": "…",        // dedup / idempotency key (Kafka is at-least-once)
  "eventType": "OrderCreated",
  "version": 1,           // schema version of payload
  "occurredAt": "…",
  "correlationId": "…",  // flows gateway → services → traces
  "source": "order-service",
  "payload": { … }
}
```

Consumers deduplicate on `eventId`, route on `eventType`, and interpret `payload` according
to `version`.

## Tests

```bash
./mvnw -pl common-lib test
```
