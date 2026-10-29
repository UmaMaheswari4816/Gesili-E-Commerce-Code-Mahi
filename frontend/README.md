# frontend — Gecili Shop (Next.js)

A React/Next.js (App Router, TypeScript) frontend that consumes the platform **through the
API gateway** (`:8080`). Demonstrates the full customer flow: register/login with JWT +
transparent refresh, browse/search products, cart, and checkout with **order-status
polling**.

## Prerequisites

- Node.js 18+ and npm (you have Node 24 / npm 11).
- The backend reachable at the gateway URL (default `http://localhost:8080`). You can run the
  frontend without the backend — pages render and show friendly errors until the APIs are up.

## Setup & run

```powershell
cd frontend
copy .env.local.example .env.local      # set NEXT_PUBLIC_API_BASE_URL if not 8080
npm install
npm run dev                             # http://localhost:3000
```

Other scripts: `npm run build`, `npm start`, `npm run typecheck`.

## How it maps to the backend

| Page | Calls (via gateway) |
|---|---|
| `/` products | `GET /api/v1/products/search`, `POST /api/v1/cart/items` |
| `/login` | `POST /api/v1/auth/register`, `/login`, `GET /api/v1/auth/me` |
| `/cart` | `GET /api/v1/cart`, `POST /api/v1/orders`, `DELETE /api/v1/cart` |
| `/orders/[id]` | `GET /api/v1/orders/{id}` (polled every 2s) |

## Auth / token handling

`src/lib/api.ts` attaches the access token and, on a `401`, **transparently rotates** the
refresh token once and retries (mirroring the backend's refresh-rotation). Tokens are in
`localStorage` for simplicity — a production app should use **httpOnly cookies** to resist
XSS (noted in code).

## Order-status polling

`POST /orders` returns `202` with `PENDING_INVENTORY`. `/orders/[id]` polls every 2s until the
status becomes `PAYMENT_PENDING` (checkout params appear → "Pay" button) or a terminal state
(`CONFIRMED` / `CANCELLED` / `PAYMENT_TIMEOUT`) — exactly the flow designed in Phase 0 (B3).

## Generating a typed client from OpenAPI (optional, recommended)

`src/lib/types.ts` is hand-written to match the DTOs. To generate it from the live spec
instead (so it never drifts):

```powershell
# aggregated spec is proxied by the gateway per service, e.g. auth:
npx openapi-typescript http://localhost:8080/auth-service/v3/api-docs -o src/lib/openapi.auth.ts
npx openapi-typescript http://localhost:8080/product-service/v3/api-docs -o src/lib/openapi.product.ts
# …repeat per service, then import those types instead of the hand-written ones.
```

CI already uploads each service's OpenAPI JSON as a build artifact (Phase 1 `ci.yml`), so you
can generate the client in a pipeline step too.
