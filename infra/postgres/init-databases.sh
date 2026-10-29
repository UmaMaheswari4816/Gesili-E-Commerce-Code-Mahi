#!/bin/bash
# =============================================================================
# Creates one database per service on first Postgres boot (database-per-service).
# Reads a comma-separated list from GESILI_DATABASES (set in docker-compose.yml).
# Each service owns its schema and migrations (Flyway); there are NO shared tables.
# =============================================================================
set -euo pipefail

IFS=',' read -ra DBS <<< "${GESILI_DATABASES:-}"

for db in "${DBS[@]}"; do
  db_trimmed="$(echo "$db" | xargs)"   # trim whitespace
  [ -z "$db_trimmed" ] && continue
  echo "Creating database: gesili_${db_trimmed}"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE DATABASE gesili_${db_trimmed}'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'gesili_${db_trimmed}')\gexec
EOSQL
done

echo "Database-per-service initialization complete."
