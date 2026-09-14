#!/usr/bin/env bash
# =====================================================================
# Dump the Render-managed Postgres database into a local SQL file.
# Run on your laptop BEFORE the cutover so you can import on the VPS.
# =====================================================================
set -euo pipefail

# Find these on the Render dashboard for your PostgreSQL instance.
# Connections -> Internal Database URL -> "PSQL Command".
RENDER_HOST="${RENDER_PGHOST:?Set RENDER_PGHOST (e.g. dpg-xxxx-a.oregon-postgres.render.com)}"
RENDER_PORT="${RENDER_PGPORT:-5432}"
RENDER_DB="${RENDER_PGDATABASE:-cou_bus_tracker}"
RENDER_USER="${RENDER_PGUSER:?Set RENDER_PGUSER}"
RENDER_PASS="${RENDER_PGPASSWORD:?Set RENDER_PGPASSWORD}"

OUT="${1:-render_dump_$(date +%Y%m%d_%H%M%S).sql}"

PGPASSWORD="$RENDER_PASS" pg_dump \
  --host="$RENDER_HOST" \
  --port="$RENDER_PORT" \
  --username="$RENDER_USER" \
  --dbname="$RENDER_DB" \
  --no-owner \
  --no-acl \
  --clean \
  --if-exists \
  --format=plain \
  "$OUT"

echo "Wrote $OUT ($(du -h "$OUT" | cut -f1))"