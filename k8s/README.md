# Kubernetes & CI/CD

Phase 10 deliverables: a **Helm chart** for the platform and a **GitHub Actions** pipeline
that builds, tests, builds images, and (optionally) deploys.

> These are authored to spec but **not applied to a live cluster** in this project — you can
> render/lint them locally and deploy to kind/minikube or a managed cluster when ready.

## Layout

```
k8s/
├── helm/gesili/            # the platform chart
│   ├── Chart.yaml
│   ├── values.yaml         # services map + defaults + global config
│   ├── secrets.example.yaml
│   └── templates/
│       ├── _helpers.tpl
│       ├── configmap.yaml  # non-secret shared env
│       ├── secret.yaml     # DB/payment secrets (values supplied at deploy)
│       ├── deployments.yaml # Deployment + Service per service (looped)
│       ├── hpa.yaml        # HPA for opted-in services
│       └── ingress.yaml    # all traffic → api-gateway
└── backing-services/       # DEV-ONLY Postgres/Redis/Kafka/ES/Zipkin for a local cluster
```

## Why a templated chart (not 10 manifest sets)

`values.yaml` has a `services:` map; `templates/deployments.yaml` loops over it to render a
Deployment + Service for each, pulling probes/resources from `defaults`. Changing replicas,
image tag, or a probe is a one-line values edit — no copy-paste drift across 10 services.
This is how real teams keep a fleet of similar services maintainable.

## Key production practices baked in

- **Config vs secrets split**: non-secret env in a ConfigMap, credentials in a Secret whose
  values are injected at deploy time (never committed). Real clusters use sealed-secrets /
  external-secrets feeding from a cloud secret manager.
- **Liveness & readiness probes** on Spring Boot's `/actuator/health/{liveness,readiness}`
  (enabled by `management.endpoint.health.probes.enabled`, set in shared config).
- **Startup ordering** via an initContainer that waits for the config server before other
  services boot — mirrors the local startup order.
- **HPA** on the gateway and order-service (CPU-based), demonstrating autoscaling.
- **Single ingress** to the API gateway — the one public entry point, same as the design.
- **Resource requests/limits** on every pod.

## Deploy to a local cluster (kind/minikube)

```bash
# 1. create the secret-backed backing services (dev only)
kubectl apply -f k8s/backing-services/backing-services.yaml   # needs app-secrets first:
helm template gesili k8s/helm/gesili -s templates/secret.yaml -f k8s/helm/gesili/secrets.prod.yaml | kubectl apply -f -

# 2. install the platform (real secrets from your gitignored values file)
helm upgrade --install gesili k8s/helm/gesili -f k8s/helm/gesili/secrets.prod.yaml

# 3. point gesili.local at the ingress and browse the aggregated Swagger
#    (add '127.0.0.1 gesili.local' to your hosts file for a local ingress controller)
```

Per-service databases (`gesili_auth`, …) must exist in Postgres — run an init Job or create
them manually (the dev Postgres here starts with just the default DB). For a real deployment
use CloudNativePG / a managed Postgres with the databases provisioned.

## Lint / render without a cluster

```bash
helm lint k8s/helm/gesili
helm template gesili k8s/helm/gesili -f k8s/helm/gesili/secrets.example.yaml | less
```

## CI/CD (GitHub Actions)

- **`ci.yml`** (Phase 1): on PR/push to main — `./mvnw clean verify`, spotless check, uploads
  OpenAPI specs + test reports. The quality gate.
- **`release.yml`** (this phase): on a `v*` tag — builds a container image per service
  (matrix) and pushes to **GHCR** with cache. A **deploy** job runs `helm upgrade --install`
  **only if** a `KUBE_CONFIG` secret is present, so the pipeline is safe with no cluster.

Required GitHub secrets for deploy (optional): `KUBE_CONFIG` (base64 kubeconfig),
`POSTGRES_PASSWORD`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`. Image push uses the
built-in `GITHUB_TOKEN`.
