# ADR 0002 — common-lib shares contracts, never entities

- Status: accepted
- Date: 2026-10-29

## Context

Services need to agree on the shapes that cross their boundaries (events, DTOs, enums,
error model). The tempting shortcut is to also share JPA entities.

## Decision

`common-lib` contains only shared **contracts**: DTOs, Kafka event payloads, the versioned
`EventEnvelope`, enums, the RFC 7807 error model, exceptions, constants, and validation
annotations. It contains **no JPA entities, repositories, or business logic**. Each service
owns its own entities and database (database per service).

## Consequences

- **Pro:** DRY contracts with compile-time safety; services stay decoupled at the
  persistence layer and can evolve their schemas independently.
- **Con:** the shared jar must be versioned carefully; a breaking contract change ripples to
  consumers (mitigated by the envelope `version` field and additive changes).
- Sharing entities would have created a **distributed monolith**: a column rename in one
  service would force recompiles/redeploys of others, and every consumer would inherit
  Hibernate and a DB dialect it doesn't need.
- Alternatives considered: per-service DTO copies (more decoupling, more drift) and a schema
  registry with Avro/Protobuf (language-agnostic, enforced compatibility, but extra infra +
  codegen). The envelope versioning is, by convention, the discipline a registry would
  enforce; migration to a registry is a documented future option.
