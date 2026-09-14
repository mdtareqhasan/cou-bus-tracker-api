#!/usr/bin/env bash
# =====================================================================
# One-shot VPS provisioning for CoU Bus Tracker.
# Run as root (or with sudo) the first time you log into MetroVPS.
# =====================================================================
set -euo pipefail

APP_DIR="/opt/cou-bus-tracker"

echo "==> Installing base packages"
apt-get update
apt-get install -y ca-certificates curl gnupg ufw nginx rsync git

echo "==> Installing Docker (official repo)"
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
   https://download.docker.com/linux/ubuntu \
   $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list

apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

echo "==> Configuring firewall"
ufw default deny incoming
ufw default allow outgoing
ufw allow OpenSSH
ufw allow "Nginx Full"
ufw --force enable

echo "==> Creating deploy layout"
mkdir -p "$APP_DIR"/{Backend/backups,Backend/uploads}
mkdir -p /var/www/{admin-panel,super-admin,flutter-web}

echo ""
echo "============================================================"
echo "  Next steps:"
echo ""
echo "  1. Clone your repo:"
echo "     cd /opt && git clone <your-git-url> cou-bus-tracker"
echo ""
echo "  2. Edit Backend/.env.production with your secrets"
echo ""
echo "  3. Start Postgres:"
echo "     cd $APP_DIR/Backend"
echo "     docker compose -f docker-compose.postgres.yml up -d"
echo ""
echo "  4. Import database dump (if migrating from Render):"
echo "     bash $APP_DIR/deploy/db-import-vps.sh <dump-file.sql>"
echo ""
echo "  5. Deploy everything:"
echo "     bash $APP_DIR/deploy/deploy.sh"
echo ""
echo "  6. Setup TLS with Let's Encrypt:"
echo "     apt install -y certbot python3-certbot-nginx"
echo "     certbot --nginx -d kubijatra.com -d www.kubijatra.com \\"
echo "              -d api.kubijatra.com -d admin.kubijatra.com \\"
echo "              -d super.kubijatra.com -d app.kubijatra.com \\"
echo "             --agree-tos -m your@email.com --redirect"
echo "============================================================"
