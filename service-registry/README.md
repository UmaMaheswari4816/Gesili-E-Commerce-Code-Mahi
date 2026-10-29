# service-registry (Eureka)

Netflix **Eureka Server** — the service registry. Services register on startup and send
heartbeats; clients resolve logical names (e.g. `lb://product-service`) to live instances
and load-balance across them. No hardcoded hosts/ports.

## Run (standalone, dev)

Start **after** the Config Server (it imports its config) and **before** the business
services.

```bash
.\mvnw.cmd -pl service-registry spring-boot:run    # Windows
./mvnw -pl service-registry spring-boot:run        # Linux/macOS
```

Dashboard: **http://localhost:8761** — shows registered instances, their status and the
registry's own health.

## Verify

```bash
curl http://localhost:8761/actuator/health          # UP
curl http://localhost:8761/eureka/apps               # registered apps (XML; empty at first)
```

Once a service registers (from Phase 2+), it appears under "Instances currently registered
with Eureka" on the dashboard.

## Standalone vs. clustered

- **Standalone (default dev)**: one node, `register-with-eureka=false`,
  `fetch-registry=false` — the server doesn't register with itself.
- **Clustered (HA, prod)**: two or more peers that register **with each other** (peer
  awareness) and replicate the registry, so the registry is not a single point of failure.
  Run the bundled profiles:

  ```bash
  # add to your hosts file:  127.0.0.1 peer1 peer2
  .\mvnw.cmd -pl service-registry spring-boot:run -Dspring-boot.run.profiles=peer1
  .\mvnw.cmd -pl service-registry spring-boot:run -Dspring-boot.run.profiles=peer2
  ```

  Clients list all peers in `eureka.client.service-url.defaultZone`.

## Self-preservation (important interview topic)

Eureka's **self-preservation mode** protects the registry during network trouble: if the
rate of received heartbeats drops below a threshold (e.g. a partition makes many instances
*look* dead), Eureka assumes the problem is the network, not the instances, and **stops
evicting** registrations rather than wrongly deregistering healthy services.

- **Production**: keep it ON — a brief network blip shouldn't wipe the registry.
- **Local dev**: we turn it OFF (`enable-self-preservation=false`) and shorten the eviction
  timer, because you start/stop services constantly and want stale entries cleared quickly.
  The dashboard's red "self-preservation" banner in dev is expected and harmless.

## Failure behavior

- **Registry down when a service starts**: the client retries registration with backoff; it
  can still start and will register once Eureka returns.
- **Registry down after services registered**: clients use their **locally cached** registry
  snapshot, so existing service-to-service calls keep resolving. New instances can't be
  discovered until the registry returns. A clustered setup avoids this single point of
  failure.
