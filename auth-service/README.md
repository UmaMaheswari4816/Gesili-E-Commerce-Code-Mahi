# auth-service

Custom authentication service: registration, login, roles, **RS256 JWT** access tokens,
**rotating refresh tokens** with reuse detection, and a **JWKS** endpoint for verification.

## Why custom (and the production alternative)

We build auth ourselves to learn OAuth2/JWT internals and to have visible Java code for a
portfolio. Real companies usually delegate this to **Keycloak / Auth0 / Cognito** — wired in
optionally at the end of the project. The token *contract* (RS256 + JWKS) is identical, so
swapping the issuer later changes little for the resource servers.

## Key security decisions

- **RS256 (asymmetric)**: auth-service signs with a private key; everyone else verifies with
  the public key from JWKS. No shared secret — a compromised resource server can't mint tokens.
- **JWKS** at `/.well-known/jwks.json`: public keys with a `kid`. **Rotation with overlap** —
  publish both old and new keys during a window so live tokens keep validating.
- **Refresh-token rotation + reuse detection**: each refresh issues a new token and marks the
  old one used; presenting an already-used token revokes the whole token *family* (theft
  response). Only SHA-256 hashes are stored, never raw tokens.
- **Defence in depth**: the gateway validates tokens AND this service validates again.

## Run (dev)

Needs PostgreSQL (`gecili_auth` DB), and ideally config-server + registry running.

```powershell
# generate a stable signing key (optional; otherwise an ephemeral dev key is used):
.\auth-service\scripts\gen-jwt-keys.ps1
$env:JWT_PRIVATE_KEY_LOCATION = "file:./auth-service/keys/jwt-private.pem"
$env:JWT_PUBLIC_KEY_LOCATION  = "file:./auth-service/keys/jwt-public.pem"
$env:JWT_ACTIVE_KID           = "key-1"

.\mvnw.cmd -pl auth-service spring-boot:run
```

Port **9001**. Swagger UI: http://localhost:9001/swagger-ui.html (click **Authorize**, paste
an access token from `/login`). JWKS: http://localhost:9001/.well-known/jwks.json

## Verify (curl)

```bash
# register
curl -X POST http://localhost:9001/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"S3curePass!","displayName":"Jane"}'

# login -> returns {accessToken, refreshToken, ...}
curl -X POST http://localhost:9001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@example.com","password":"S3curePass!"}'

# authenticated profile
curl http://localhost:9001/api/v1/auth/me -H "Authorization: Bearer <ACCESS_TOKEN>"

# refresh (rotates the refresh token; the old one becomes invalid)
curl -X POST http://localhost:9001/api/v1/auth/refresh \
  -H "Content-Type: application/json" -d '{"refreshToken":"<REFRESH_TOKEN>"}'
```

## Keys & secrets

`auth-service/keys/` is **gitignored** — the private key never gets committed. For local dev
with no key configured, the service generates an **ephemeral** keypair at startup (tokens
die on restart; a warning is logged). Production injects the key via a file/secret or Vault.

## Tests

```powershell
.\mvnw.cmd -pl auth-service test                 # unit: refresh rotation + reuse detection
.\mvnw.cmd -pl auth-service verify               # + AuthFlowIT (Testcontainers Postgres)
```
`AuthFlowIT` needs Docker running (Testcontainers). The unit tests do not.
