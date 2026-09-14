#!/usr/bin/env bash
# =====================================================================
# MetroVPS deploy script for the CoU Bus Tracker stack.
# ---------------------------------------------------------------------
# Run from the repo root on the VPS after `git pull`:
#   bash deploy/deploy.sh
#
# What it does:
#   1. (Re)builds the Spring Boot image.
#   2. Restarts the app container (Postgres is in a separate stack).
#   3. Rebuilds the admin-panel and super-admin React bundles with
#      VITE_API_BASE_URL pointing at https://api.kubijatra.com/api.
#   4. Syncs the dist folders into /var/www.
#   5. (Re)loads Nginx if any config changed.
# =====================================================================
set -euo pipefail

# ---- Editable --------------------------------------------------------
APP_DIR="/opt/cou-bus-tracker"
WEB_API="https://api.kubijatra.com/api"
DOMAIN="kubijatra.com"
ADMIN_DIST="/var/www/admin-panel"
SUPER_DIST="/var/www/super-admin"
# ---------------------------------------------------------------------

cd "$APP_DIR"

echo "==> Pulling latest code"
git pull --rebase --autostash

echo "==> [1/5] Building + restarting backend"
cd Backend
docker compose -f docker-compose.prod.yml build app
docker compose -f docker-compose.prod.yml up -d --no-deps app

echo "==> Waiting for /api/buses to respond"
for i in {1..30}; do
  if curl -fsS http://127.0.0.1:8080/api/buses >/dev/null; then
    echo "    API is up"
    break
  fi
  sleep 2
done

echo "==> [2/5] Building admin panel"
cd ../admin/admin-panel
npm ci
VITE_API_BASE_URL="$WEB_API" npm run build

echo "==> [3/5] Building super-admin"
cd ../../super_admin
npm ci
VITE_API_BASE_URL="$WEB_API" npm run build

echo "==> [4/5] Syncing dist -> /var/www"
sudo mkdir -p "$ADMIN_DIST" "$SUPER_DIST"
sudo rsync -a --delete ../admin/admin-panel/dist/ "$ADMIN_DIST/"
sudo rsync -a --delete ./dist/                    "$SUPER_DIST/"

echo "==> [5/5] Reloading Nginx"
sudo nginx -t
sudo systemctl reload nginx

echo "==> Done. Sanity checks:"
echo "    curl -fsS https://api.$DOMAIN/api/buses | head"
echo "    curl -fsS -o /dev/null -w '%{http_code}\\n' https://admin.$DOMAIN/"
echo "    curl -fsS -o /dev/null -w '%{http_code}\\n' https://super.$DOMAIN/"
