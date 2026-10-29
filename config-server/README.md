# config-server

Spring Cloud **Config Server** — one place for every service's configuration, served from
the [`config-repo/`](../config-repo) folder via the **native (filesystem) backend**.

## Why centralized config

- **12-factor**: config lives outside the code; the same artifact runs in dev/stage/prod
  with different config.
- **No duplication**: shared defaults (`application.yml`) are written once; services add only
  their deltas.
- **Dynamic refresh**: change a value and refresh running services without redeploying.

## Run

Start this **before** the registry and business services (they import config from it).

```bash
# from the repo root (so file:./config-repo resolves)
./mvnw -pl config-server spring-boot:run        # Linux/macOS
.\mvnw.cmd -pl config-server spring-boot:run    # Windows
```

Default port **8888**. The native backend reads `../config-repo` / `./config-repo`
(see `application.yml` `search-locations`).

## Verify

```bash
# Merged config for a service + profile (JSON):
curl http://localhost:8888/auth-service/dev
# As a rendered YAML document:
curl http://localhost:8888/auth-service-dev.yml
# Health:
curl http://localhost:8888/actuator/health
```

A service's config is the merge of (lowest → highest precedence):
`application.yml` → `application-{profile}.yml` → `{service}.yml` → `{service}-{profile}.yml`.

## How services consume it

Each service declares, in its own `application.yml`:

```yaml
spring:
  config:
    import: "optional:configserver:http://localhost:8888"
```

`optional:` means the service still starts if the config server is briefly down (dev-friendly).
Drop `optional:` in production to fail fast when config is unavailable.

## Dynamic refresh

Two options:

1. **Per instance** — `POST /actuator/refresh` on a service re-reads config and re-binds
   `@ConfigurationProperties` / `@RefreshScope` beans.
2. **Broadcast (Spring Cloud Bus over Kafka)** — start this server and services with the
   `bus` profile, then a single `POST /actuator/busrefresh` fans a refresh to everyone:

   ```bash
   .\mvnw.cmd -pl config-server spring-boot:run -Dspring-boot.run.profiles=native,bus
   curl -X POST http://localhost:8888/actuator/busrefresh
   ```

## Secrets / encryption

This server serves **config, not secrets**. Secret values are injected at runtime via env
vars and referenced as `${PLACEHOLDER}` in `config-repo`. For encrypted values the server
supports `{cipher}...` (symmetric or RSA key); Vault is the production-grade option. Covered
in depth where the first real secret appears (auth-service, Phase 3).

## Failure behavior

- **Config server down at service boot**: with `optional:configserver:` the service starts
  using any locally-bundled defaults; without it, the service fails fast. Choose per
  environment.
- **Config server down after boot**: running services keep their last-loaded config; only
  refresh is unavailable until it returns.
