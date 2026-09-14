# CoU Bus Tracker — MetroVPS deployment guide

> Goal: move the **backend (Spring Boot + Flyway)**, **admin panel (React)**, **super admin (React)**, and **Flutter web** off Render/Vercel and onto your MetroVPS Ubuntu server (kubijatra.com), behind Nginx + Let's Encrypt.

## 0 · Architecture on the VPS

```
                      Internet
                          │
                ┌─────────┴──────────┐
                │   Nginx :443       │   /etc/nginx/sites-available/kubijatra.com.conf
                │  (TLS + SPA + API) │
                └─────────┬──────────┘
                          │ http://127.0.0.1:8080
              ┌───────────┴────────────┐
              │  cou-bus-tracker-app   │   docker-compose.prod.yml
              │  Spring Boot 3 / Java  │   listens on container :8080
              └─────┬──────────────────┘
                    │ jdbc:postgresql://host.docker.internal:5432
              ┌─────┴──────────────┐
              │ host.docker.internal│  = host loopback
              │ port 5432           │
              │  cou-bus-tracker-    │   docker-compose.postgres.yml
              │  postgres (PG 16)    │   bound to 127.0.0.1:5432 only
              └─────────────────────┘
```

`/var/www/admin-panel/`, `/var/www/super-admin/`, `/var/www/flutter-web/` hold the built SPA bundles that Nginx serves as the document root.

## 1 · DNS (do this first)

On your DNS provider (Cloudflare / Namecheap / MetroVPS DNS panel), point these A records at the VPS IP `103.174.50.68`:

| Host                       | Type | Value             |
| -------------------------- | ---- | ----------------- |
| `kubijatra.com`            | A    | `103.174.50.68`   |
| `www.kubijatra.com`        | A    | `103.174.50.68`   |
| `api.kubijatra.com`        | A    | `103.174.50.68`   |
| `admin.kubijatra.com`      | A    | `103.174.50.68`   |
| `super.kubijatra.com`      | A    | `103.174.50.68`   |
| `app.kubijatra.com`        | A    | `103.174.50.68`   |

If you use Cloudflare, leave the orange-cloud proxy **OFF** until TLS is issued (letsencrypt needs to hit port 80 unproxied). Then you can re-enable the proxy for CDN.

## 2 · First boot

SSH into the VPS as the user MetroVPS gave you:

```bash
ssh root@103.174.50.68
bash /opt/cou-bus-tracker/deploy/first-boot.sh   # after you've cloned the repo there
```

If you prefer to run the manual flow:

```bash
apt update && apt install -y ca-certificates curl gnupg nginx ufw rsync

# Docker
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  > /etc/apt/sources.list.d/docker.list
apt update && apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Firewall - lock everything except SSH and Nginx
ufw default deny incoming
ufw allow OpenSSH
ufw allow "Nginx Full"
ufw --force enable

# Layout
mkdir -p /opt/cou-bus-tracker/{Backend/backups,Backend/uploads} \
         /var/www/{admin-panel,super-admin,flutter-web}

cd /opt && git clone <your-git-url> cou-bus-tracker
```

## 3 · Fill in secrets

Edit `Backend/.env.production` on the VPS. Replace every `CHANGE_ME_*`:

```bash
cd /opt/cou-bus-tracker/Backend
nano .env.production
```

Generate a real JWT secret:

```bash
openssl rand -base64 48
```

Generate a real DB password:

```bash
openssl rand -base64 32 | tr -d '/+=' | head -c 32 ; echo
```

## 4 · Bring up Postgres (alone, in its own stack)

```bash
cd /opt/cou-bus-tracker/Backend
docker compose -f docker-compose.postgres.yml up -d
docker compose -f docker-compose.postgres.yml ps          # should be 'healthy'
docker exec -it cou-bus-tracker-postgres psql -U cou_bus_tracker -d cou_bus_tracker -c "SELECT 1;"
```

This stack only contains Postgres. It will keep running across app redeploys. Bound to `127.0.0.1:5432` so the public can never reach it.

## 5 · TLS (Let's Encrypt)

```bash
apt install -y certbot python3-certbot-nginx
certbot --nginx -d kubijatra.com -d www.kubijatra.com \
                 -d api.kubijatra.com -d admin.kubijatra.com \
                 -d super.kubijatra.com -d app.kubijatra.com \
        --agree-tos -m you@example.com --redirect
```

Certbot will create the SSL lines and the `listen 80 -> 443` redirect for you. Then **drop in your site config** so the right roots are used:

```bash
sudo ln -s /opt/cou-bus-tracker/deploy/nginx/kubijatra.com.conf \
           /etc/nginx/sites-available/kubijatra.com.conf
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

## 6 · Migrate the database from Render

Run `db-export-render.sh` from your laptop (where Render's `PG*` vars are easy to inject):

```bash
export RENDER_PGHOST=dpg-xxxxxx-a.oregon-postgres.render.com
export RENDER_PGUSER=cou_bus_tracker_user
export RENDER_PGPASSWORD=YaXEiykhEwHdALRFwBwRNpq8WnGcLncD
export RENDER_PGDATABASE=cou_bus_tracker
bash deploy/db-export-render.sh   # writes render_dump_YYYYMMDD_HHMMSS.sql
```

Copy the dump to the VPS:

```bash
scp render_dump_*.sql root@103.174.50.68:/opt/cou-bus-tracker/Backend/backups/
```

Import on the VPS:

```bash
cd /opt/cou-bus-tracker
bash deploy/db-import-vps.sh Backend/backups/render_dump_*.sql
```

> The dump includes the schema, so Flyway's `flyway_schema_history` may complain on the first boot. Either:
> * drop that table once after import: `docker exec -it cou-bus-tracker-postgres psql -U cou_bus_tracker -d cou_bus_tracker -c "DROP TABLE flyway_schema_history;"` and let Flyway re-baseline, **or**
> * leave the table — Flyway will see all migrations as already applied and skip them.

## 7 · Bring up the Spring Boot app

```bash
cd /opt/cou-bus-tracker
bash deploy/deploy.sh
```

This rebuilds the image, restarts the `app` container, builds the two React SPAs with `VITE_API_BASE_URL=https://api.kubijatra.com/api`, syncs their `dist/` to `/var/www/`, and reloads Nginx.

Verify:

```bash
curl -fsS https://api.kubijatra.com/api/buses | head
curl -fsS -o /dev/null -w '%{http_code}\n' https://admin.kubijatra.com/
curl -fsS -o /dev/null -w '%{http_code}\n' https://super.kubijatra.com/
```

## 8 · Flutter web (when you're ready)

The Flutter source is not in this repo (it lives in a separate Vercel project). Two options:

**Option A — keep Vercel, just point it at the new API.**
On Vercel → Project → Settings → Environment Variables set
`API_BASE_URL = https://api.kubijatra.com/api` and redeploy.

**Option B — build Flutter web on the VPS.**
```bash
flutter --version        # if flutter isn't installed: snap install flutter --classic
cd /opt/cou-bus-tracker-flutter
flutter build web --release --dart-define=API_BASE_URL=https://api.kubijatra.com/api
sudo rsync -a build/web/ /var/www/flutter-web/
sudo systemctl reload nginx
```

The Nginx `app.kubijatra.com` block already serves whatever is in `/var/www/flutter-web/`.

## 9 · Updating later

```bash
cd /opt/cou-bus-tracker
git pull
bash deploy/deploy.sh
```

`docker-compose.postgres.yml` is **never** restarted by `deploy.sh`, so you can rebuild the app 100× without disturbing the database.

## 10 · Useful operational commands

```bash
# App logs
docker logs -f cou-bus-tracker-app

# Postgres shell
docker exec -it cou-bus-tracker-postgres psql -U cou_bus_tracker -d cou_bus_tracker

# Restart app only
cd /opt/cou-bus-tracker/Backend
docker compose -f docker-compose.prod.yml restart app

# Tail Nginx access logs for one host
sudo tail -f /var/log/nginx/admin.kubijatra.com.access.log
```

## 11 · Secrets you must replace in `.env.production`

| Variable                       | Why                                          |
| ------------------------------ | -------------------------------------------- |
| `DB_PASSWORD`                  | Real DB password                             |
| `SPRING_DATASOURCE_PASSWORD`   | Same as above                                |
| `JWT_SECRET`                   | `openssl rand -base64 48`                    |
| `BULKSMSBD_API_KEY`            | Already in your message — paste the value    |
| `CLOUDINARY_*`                 | Already in your message                      |
| `GOOGLE_OAUTH_CLIENT_ID`       | Already in your message                      |

`MAILERSEND_*` and `RESEND_*` are **not** wired into the current Spring code — email verification is deprecated (`EmailVerificationService.java` always throws `UnsupportedOperationException`). They are intentionally omitted from `.env.production`.

## 12 · What changed in code (this repo)

* `Backend/docker-compose.postgres.yml` — Postgres-only stack. Bound to `127.0.0.1:5432`.
* `Backend/docker-compose.prod.yml` — App-only stack, reads `.env.production` via `env_file`.
* `Backend/.env.production` — single source of truth for env vars.
* `Backend/src/main/resources/application-prod.yaml` — new Spring `prod` profile.
* `Backend/src/main/java/com/cou/bustracker/config/SecurityConfig.java` & `WebMvcConfig.java` — added `kubijatra.com`, `api/admin/super/app.kubijatra.com` to the CORS allow-list.
* `Backend/.gitignore` — `.env.production` excluded.
* `deploy/nginx/kubijatra.com.conf` — full site config (API + 2 SPAs + Flutter web + redirects).
* `deploy/deploy.sh` — `git pull` → rebuild app → rebuild SPAs → reload Nginx.
* `deploy/first-boot.sh` — one-time server provisioning.
* `deploy/db-export-render.sh` — dump Render DB from your laptop.
* `deploy/db-import-vps.sh` — load that dump into the VPS container.