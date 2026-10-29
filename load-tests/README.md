# load-tests

[k6](https://k6.io/) load/smoke tests that drive the platform through the **API gateway**
(`:8080`), so they exercise routing, JWT validation, and rate limiting too.

## Install k6 (Windows)

```powershell
winget install k6.k6
# or: choco install k6
```

## Run

Start the platform first (Phases 2–8 services + infra). Then:

```powershell
# quick end-to-end sanity check (1 VU, 1 iteration)
k6 run load-tests/k6/smoke.js

# ramping load: 20 → 50 virtual users
k6 run load-tests/k6/checkout-load.js

# point at a different base URL
k6 run -e BASE_URL=http://localhost:8080 load-tests/k6/smoke.js
```

## What they check (thresholds)

- `http_req_failed < 1–2%` — error budget
- `http_req_duration p95 < 800ms` (smoke) / `< 1500ms` (load)
- `login_duration p95 < 1000ms`

k6 exits non-zero if a threshold is breached, so these double as CI gates.

## Watching the system under load

Open Grafana (http://localhost:3000, from the `observability` compose profile) and the
**Gecili — Overview** dashboard to watch request rate, p95 latency, 5xx rate, the
`productLookup` circuit-breaker state, JVM heap, and live logs while k6 runs.

## JMeter alternative

The same flows can be modeled in JMeter (Thread Group → HTTP Samplers for
register/login/search/cart, with a JSON Extractor pulling `accessToken` into a variable used
as the `Authorization` header). k6 is scripted and CI-friendly; JMeter is GUI-first. Either
works — k6 is the default here.
