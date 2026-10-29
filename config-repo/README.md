# config-repo

Centralized configuration served by the **Spring Cloud Config Server** (added in Phase 2)
using the **native / filesystem backend** pointed at this folder.

## Layout

```
config-repo/
├── application.yml                 # shared defaults for ALL services
├── <service>.yml                   # per-service config (e.g. auth-service.yml)
└── <service>-<profile>.yml         # per-profile overrides (e.g. auth-service-dev.yml)
```

Resolution order (lowest → highest precedence): `application.yml` →
`application-<profile>.yml` → `<service>.yml` → `<service>-<profile>.yml`.

Services fetch config at boot via `spring.config.import=configserver:...`.

## Secrets

This repo holds **configuration, not secrets**. Secret values are injected at runtime from
environment variables or a secrets manager and referenced here as `${PLACEHOLDER}`. Never
inline passwords, API keys, or signing keys. Encrypted values (`{cipher}...`) or Vault are
the production approach — documented in Phase 2.

## Splitting into a separate repo later

For a real deployment you would typically move this folder into its **own Git repository**
and point the Config Server at it with `spring.cloud.config.server.git.uri`. Benefits:
config changes get their own review/audit trail and don't trigger application CI; access
can be restricted separately. The native/filesystem backend used here keeps everything in
one monorepo for easy local learning — the migration is just moving the folder and swapping
the backend from `native` to `git`.
