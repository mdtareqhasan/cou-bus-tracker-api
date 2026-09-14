#!/usr/bin/env bash
# =====================================================================
# Import a Render SQL dump into the VPS Postgres container.
# =====================================================================
set -euo pipefail

DUMP="${1:?Path to the .sql file from db-export-render.sh}"
cd "$(dirname "$0")/../Backend"

# Pull creds from .env.production
set -a
# shellcheck disable=SC1091
source .env.production
set +a

docker exec -i cou-bus-tracker-postgres \
  psql -U "$DB_USERNAME" -d "$DB_NAME" \
  < "$DUMP"

echo "Imported $DUMP into $DB_NAME"