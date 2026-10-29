# product-service

Product catalog with **CQRS**: PostgreSQL is the authoritative **write model** (products,
categories); Elasticsearch is the **read model** for search and filtering.

## Why CQRS here

Catalog reads (search, filter, browse) vastly outnumber writes and have very different
shapes — full-text relevance, faceting — that a relational DB serves poorly. Splitting the
models lets each scale and be optimized independently: writes stay transactional in
PostgreSQL; reads hit a denormalized Elasticsearch index. The cost is **eventual
consistency** — a just-created product appears in search a moment later, after the async
projection runs.

## Write → read projection

On each successful write, `CatalogCommandService` calls `ProductProjectionService` (`@Async`)
which upserts a denormalized `ProductDocument` (category name/slug inlined) into ES. If ES is
down the write still succeeds and the failure is logged for a later reindex. In a larger
system this projection would ride the outbox/events instead of an in-process async call —
noted as the scale-up path.

## Endpoints

Public (no auth):
- `GET /api/v1/products/{id}` — point lookup (write model)
- `GET /api/v1/products/search?q=...` — full-text search (read model)
- `GET /api/v1/products/category/{slug}` — by category (read model)

Admin (`ROLE_ADMIN`, JWT):
- `POST /api/v1/admin/catalog/categories`
- `POST /api/v1/admin/catalog/products`
- `PATCH /api/v1/admin/catalog/products/{id}`
- `DELETE /api/v1/admin/catalog/products/{id}`

Swagger UI at :9002/swagger-ui.html with **public** and **admin** groups.

## Run (dev)

```powershell
cd infra; docker compose --profile phase4 up -d   # postgres, elasticsearch, config, registry (+auth,gateway)
.\mvnw.cmd -pl product-service spring-boot:run
```
Port **9002**. Needs the `gesili_product` DB and Elasticsearch on :9200.

## Verify

```bash
# (get an ADMIN token from auth-service first; promote a user to ADMIN in gesili_auth)
curl -X POST http://localhost:9002/api/v1/admin/catalog/categories \
  -H "Authorization: Bearer <ADMIN_TOKEN>" -H "Content-Type: application/json" \
  -d '{"name":"T-Shirts","slug":"t-shirts","description":"Cotton tees"}'

curl -X POST http://localhost:9002/api/v1/admin/catalog/products \
  -H "Authorization: Bearer <ADMIN_TOKEN>" -H "Content-Type: application/json" \
  -d '{"sku":"TSHIRT-RED-XL-001","name":"Red Tee","description":"cotton","price":499.00,"currency":"INR","categoryId":"<CATEGORY_ID>"}'

# search (public) — appears after the async projection:
curl "http://localhost:9002/api/v1/products/search?q=red"
```

## Tests

```powershell
.\mvnw.cmd -pl product-service test     # unit (command side with mocks)
.\mvnw.cmd -pl product-service verify   # + CatalogCqrsIT (Postgres + Elasticsearch Testcontainers)
```
The IT needs Docker running.
