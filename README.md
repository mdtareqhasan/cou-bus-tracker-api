# CoU Bus Tracker

Real-time bus information and fleet-management platform for **Comilla University**. It consists of a Spring Boot REST API (backend), a React admin panel (frontend), and is designed to serve a separate Bengali Android application.

| Component | Location | Technology | Default URL |
|---|---|---|---|
| REST API (backend) | `Backend/` | Java 21, Spring Boot 3.3, Spring Security + JWT, Spring Data JPA, **PostgreSQL**, Flyway, OpenAPI | `http://localhost:8080` |
| Admin panel (frontend) | `Backend/admin-panel/` | React 19, Vite, Tailwind CSS 4, React Router 7, Axios | `http://localhost:5173` |
| Android app spec | `Backend/CoUBusTracker_Project_Spec.md` | Android-ready API & UI specification | — |

---

## Table of contents

- [Features](#features)
- [Project structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Backend (Spring Boot)](#backend-spring-boot)
  - [Configuration](#configuration)
  - [Run locally](#run-locally)
  - [Docker deployment](#docker-deployment)
  - [Database migrations](#database-migrations)
  - [Data model](#data-model)
  - [API reference](#api-reference)
  - [Authentication & security](#authentication--security)
- [Frontend (React admin panel)](#frontend-react-admin-panel)
- [Android application](#android-application)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Production checklist](#production-checklist)

---

## Features

### Backend (Spring Boot API)
- **Public APIs** for buses, schedules, and active notices — no authentication required.
- **Student & teacher accounts** with registration, login, ID-card image upload, and profile retrieval.
- **JWT-protected admin APIs** to fully manage buses, live tracker links, schedules, notices, students, teachers, and other admin users.
- **Admin dashboard** endpoints returning aggregated statistics.
- **File uploads** (e.g., student ID cards) stored under a configurable directory.
- **PostgreSQL schema versioning** through Flyway migrations (seeded with real Comilla University bus data).
- **OpenAPI / Swagger UI** documentation generated automatically.
- Optional **bus name** and **live tracker link** support.

### Frontend (React admin panel)
- Secure login with JWT stored in `localStorage`, auto-redirect on 401/403.
- Dashboard with live statistics.
- Full CRUD screens for buses, schedules, and notices.
- Student & teacher management (list, pending approval, verify, activate/deactivate, delete).
- Admin user management and admin profile editing.
- Responsive Tailwind CSS layout.

---

## Project structure

```
Backend/
├── admin-panel/                     # Frontend (React + Vite)
│   ├── public/                      # Static assets (favicon, icons)
│   └── src/
│       ├── api.js                   # Axios client + grouped API calls, auth interceptor
│       ├── App.jsx                  # Routes + auth guards
│       ├── context/AuthContext.jsx  # Global admin auth state
│       └── pages/                   # Login, Dashboard, Buses, Schedules, Notices,
│                                    # Students, Teachers, Admins, Profile, Layout
├── src/
│   ├── main/
│   │   ├── java/com/cou/bustracker/
│   │   │   ├── config/              # Security, OpenAPI, WebMVC config
│   │   │   ├── controller/          # Public + admin REST controllers
│   │   │   ├── dto/                 # request/ and response/ DTOs
│   │   │   ├── entity/              # Bus, Schedule, Notice, Student, Teacher, Admin, TrackerLink
│   │   │   ├── exception/           # Global exception handler
│   │   │   ├── repository/          # Spring Data JPA repositories
│   │   │   ├── security/            # JWT service, filter, user details service
│   │   │   ├── service/             # Business logic services
│   │   │   └── CouBusTrackerApplication.java
│   │   └── resources/
│   │       ├── db/migration/        # Flyway SQL migrations (V1–V13, PostgreSQL)
│   │       ├── application.yaml     # Base config (profile, JWT, server, swagger)
│   │       └── application-dev.yaml # Dev profile PostgreSQL datasource + settings
│   └── test/                        # Unit/integration tests
├── Dockerfile                       # Builds backend image
├── docker-compose.yml               # PostgreSQL + backend services
├── pom.xml                          # Maven build (Spring Boot 3.3.2)
├── .env / .env.example              # Environment variables for Docker
└── …Spec.md                         # Android app & project specifications
```

---

## Prerequisites

- **Java 21 or newer**
- **Maven 3.8 or newer**
- **PostgreSQL 14+** (or Docker)
- **Node.js 18 or newer** and **npm**

---

# Backend (Spring Boot)

The backend is a single-module Maven project (`com.cou:cou-bus-tracker:1.0.0`).

## Main technologies

- Spring Boot 3.3.2 (Web, Data JPA, Security, Validation)
- Springdoc OpenAPI 2.6.0 (`springdoc-openapi-starter-webmvc-ui`)
- JJWT 0.12.6 (JWT auth)
- PostgreSQL Driver
- Flyway 10 (core + PostgreSQL)
- Lombok 1.18.40
- HikariCP connection pool

## Configuration

The backend reads its setup file(s) from `src/main/resources/`:

| File | Purpose |
|---|---|
| `application.yaml` | Global defaults: `server.port=8080`, JWT secret + 24h expiration, Swagger paths, active profile = `dev`. |
| `application-dev.yaml` | Dev PostgreSQL connection, extra SQL logging, multipart size limits (5 MB), upload folder `./uploads`. |

### Database connection (dev profile)

By default it connects to:

```
jdbc:postgresql://localhost:5432/cou_bus_tracker
```

with `username: postgres` / `password: root1234`. Change these values in `application-dev.yaml` or via environment variables.

### Environment variables (for Docker)

When running with Docker Compose you can override settings from `.env` (copy `.env.example` to `.env`):

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `cou_bus_tracker` | Database name |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `root1234` | Database password |
| `JWT_SECRET` | `YourSuperSecretKeyForJWTTokenGenerationMustBeLongEnough2024!` | JWT signing key (>= 32 chars) |
| `UPLOAD_DIR` | `./uploads` | Local upload folder |

> Do not reuse the default JWT secret in production; a leaked secret lets anyone forge admin tokens.

### JWT

The app also needs `admin` initialization data. The default admin (created by migration `V6__seed_initial_data.sql`) is:

- **Email:** `admin@cou.ac.bd`
- **Password:** `Admin@123`

**Change this password before any public deployment.**

## Run locally

### 1. Backend only

1. Make sure PostgreSQL is running and you have a `cou_bus_tracker` database.
   ```sql
   CREATE DATABASE cou_bus_tracker;
   ```
2. (Optional) Adjust `application-dev.yaml`. Set your DB username/password.
3. Start the API:

```bash
cd Backend
mvn spring-boot:run
```

Flyway will automatically apply all migrations (V1–V13) on first startup, creating tables and seeding data.

The API is then available at:

- Base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

### 2. Build an artifact (e.g. jar)

```bash
cd Backend
mvn clean package
java -jar target/cou-bus-tracker-1.0.0.jar
```

The Spring Boot fat jar output is `target/cou-bus-tracker-1.0.0.jar`.

## Docker deployment

From `Backend/`, first build the application JAR, then start the stack:

```bash
cd Backend
mvn clean package
docker-compose up --build -d
```

This starts two services:

1. **cou-bus-tracker-postgres**: PostgreSQL 16 container, port `5432`, with a named volume `postgres_data` and a healthcheck.
2. **cou-bus-tracker-app**: the backend container (from the `Dockerfile`, based on `eclipse-temurin:21-jre-alpine`) on port `8080`.

The backend starts only after PostgreSQL is healthy. Flyway applies the schema migrations on first startup.

Stop / tear down:

```bash
docker-compose down
docker-compose down -v   # also deletes the PostgreSQL volume (drops data!)
```

## Database migrations

Flyway runs automatically whenever the backend starts. Migrations live at `src/main/resources/db/migration/`:

| Migration | Content |
|---|---|
| `V1__create_buses_table.sql` | Buses table |
| `V2__create_schedules_table.sql` | Schedules table (FK to buses) |
| `V3__create_tracker_links_table.sql` | Tracker links (FK to buses) |
| `V4__create_notices_table.sql` | Notices |
| `V5__create_admins_table.sql` | Admin users |
| `V6__seed_initial_data.sql` | Seed buses, admin account |
| `V7__create_students_table.sql` | Students |
| `V8__create_teachers_table.sql` | Teachers |
| `V9__fix_admin_password.sql` | Fix admin password hash |
| `V10__add_bus_name.sql` | Optional `bus_name` column |
| `V11__move_legacy_saturday_schedules_to_weekdays.sql` | Re-route legacy Saturday schedules |
| `V12__reseed_data_postgresql.sql` | Seed/repair data + fix missing sequences (PostgreSQL) |
| `V13__fix_boolean_columns.sql` | Convert INTEGER boolean columns to native BOOLEAN |

> **PostgreSQL note:** All migrations use PostgreSQL-compatible syntax (`BIGSERIAL`, native `BOOLEAN`, no `ENGINE` clauses). When migrating from MySQL via DBeaver, boolean columns may be imported as `INTEGER` — V13 fixes this automatically.

> Never edit an applied migration—create a new `V{n+1}__…sql` file instead (Flyway validates checksums).

## Data model

| Entity | Fields | Relationships |
|---|---|---|
| **Bus** | id, busNumber, busName, category (BLUE/TEACHER/STAFF), route, driverName, driverPhone, busImageUrl, isActive, createdAt | 1→many schedules, one TrackerLink |
| **Schedule** | id, departureTime, arrivalTime, direction, startPoint, endPoint, days (e.g. `SUN-THU`), isActive, createdAt | Many→1 Bus |
| **TrackerLink** | id, url | 1→1 Bus |
| **Notice** | id, title, body, expiryHours (default 24), isActive, expiresAt, createdAt | — |
| **Student** | id, name, email, password, studentId, department, varsityBatch, idCardImageUrl, isEduMail, isVerified, isActive, createdAt | — |
| **Teacher** | id, name, email, password, designation, department, phone, isEduMail, isVerified, isActive, createdAt | — |
| **Admin** | id, email, password, name, createdAt | — |

## API reference

### Public endpoints (no auth)

| Method | Path | Description |
|---|---|---|
| GET | `/api/buses` | List active buses |
| GET | `/api/buses/{id}` | Bus detail (incl. tracker link) |
| GET | `/api/schedules` | All schedules |
| GET | `/api/schedules/bus/{busId}` | Schedules for one bus |
| GET | `/api/notices/active` | Currently active notices |
| POST | `/api/auth/student/register` | Student registration (`StudentRegisterRequest`) |
| POST | `/api/auth/student/login` | Student login (`{ email, password }`) |
| POST | `/api/auth/teacher/register` | Teacher registration |
| POST | `/api/auth/teacher/login` | Teacher login |

### Authenticated student endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/student/upload-id-card` | Upload image (multipart `file`); stored under `student-id-cards/` |
| GET | `/api/auth/student/me` | Current student profile |

### Admin endpoints (JWT required, `Authorization: Bearer <token>`)

#### Auth
| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/admin/login` | Admin login → returns JWT |

#### Dashboard
| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/dashboard` | Aggregated statistics |

#### Buses
| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/buses` | All buses (incl. inactive) |
| POST | `/api/admin/buses` | Create bus |
| PUT | `/api/admin/buses/{id}` | Update bus |
| PUT | `/api/admin/buses/{id}/tracker-link` | Update / add live tracker link |
| DELETE | `/api/admin/buses/{id}` | Delete bus |

#### Schedules
| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/schedules` | All schedules |
| POST | `/api/admin/schedules` | Create schedule |
| PUT | `/api/admin/schedules/{id}` | Update schedule |
| DELETE | `/api/admin/schedules/{id}` | Delete schedule |

#### Notices
| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/notices` | All notices |
| POST | `/api/admin/notices` | Create notice |
| DELETE | `/api/admin/notices/{id}` | Delete notice |

#### Students
| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/students` | All students |
| GET | `/api/admin/students/pending` | Unverified students |
| PUT | `/api/admin/students/{id}/verify` | Verify student |
| PUT | `/api/admin/students/{id}/toggle-active` | Activate / deactivate |
| DELETE | `/api/admin/students/{id}` | Delete student |

#### Teachers
| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/teachers` | All teachers |
| GET | `/api/admin/teachers/pending` | Unverified teachers |
| PUT | `/api/admin/teachers/{id}/verify` | Verify teacher |
| PUT | `/api/admin/teachers/{id}/toggle-active` | Activate / deactivate |
| DELETE | `/api/admin/teachers/{id}` | Delete teacher |

#### Admin users
| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/manage` | List admin users |
| POST | `/api/admin/manage` | Create admin |
| PUT | `/api/admin/manage/{id}` | Update admin |
| DELETE | `/api/admin/manage/{id}` | Delete admin |

#### Profile
| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/profile` | Current admin's profile |
| PUT | `/api/admin/profile` | Update profile |

## Authentication & security

- **Passwords** are stored hashed (bcrypt - seeded admin uses a `$2b$10$` hash).
- **JWT** is issued on login; default expiration from configuration is 24 hours (86400000 ms).
- The `JwtAuthFilter` examines the `Authorization: Bearer ...` header, validates the token, and populates the security context.
- `CustomUserDetailsService` loads user details from the admins (and student/users as needed).
- Protected admin routes require the admin role; unauthorized requests return `401`, forbidden ones `403`.

**Custom error handling:** `GlobalExceptionHandler` and custom exceptions (`ResourceNotFoundException`, `UnauthorizedException`) return structured JSON error bodies. See also tests in `Backend/src/test/java/com/cou/bustracker/service/AdminManagementServiceTest.java`.

---

# Frontend (React admin panel)

The panel lives in `Backend/admin-panel/`. It is a Vite + React SPA using Tailwind CSS v4 and React Router 7.

## Stack

- React 19 + React DOM
- Vite
- Tailwind CSS 4 (`@tailwindcss/vite` plugin)
- React Router DOM 7
- Axios (HTTP)
- lucide-react (icons)
- oxlint (linter)

## Run locally

```bash
cd Backend/admin-panel
npm install
npm run dev
```

Open `http://localhost:5173`. The Vite dev server proxies requests to the backend:

- `/api -> http://localhost:8080`
- `/uploads -> http://localhost:8080`

This means the panel works out of the box as long as the backend runs on port `8080`.

## Build for production

```bash
cd Backend/admin-panel
npm run build    # outputs to dist/
npm run lint     # run oxlint
npm run preview  # serve the production build locally
```

Deploy the contents of `admin-panel/dist/` behind a static file server (or serve it from the same origin as the API to keep the `/api` and `/uploads` relative URLs working).

## Pages / features

| Route | Page | Description |
|---|---|---|
| `/login` | `LoginPage` | Admin login (calls `/api/auth/admin/login`) |
| `/` | `DashboardPage` | Statistics from `/api/admin/dashboard` |
| `/buses` | `BusesPage` | Create / edit / delete buses, set live tracker links |
| `/schedules` | `SchedulesPage` | Manage schedules |
| `/notices` | `NoticesPage` | Create / delete notices |
| `/students` | `StudentsPage` | Approve pending, verify, activate/deactivate, delete |
| `/teachers` | `TeachersPage` | Same for teachers |
| `/admins` | `AdminUsersPage` | Manage admin accounts |
| `/profile` | `AdminProfilePage` | Edit own profile |

## How the panel talks to the API

All requests go through `src/api.js`, which:

1. Creates an Axios instance with base URL `/api`.
2. Automatically adds the `Authorization: Bearer <token>` header from `localStorage.getItem('admin_token')`.
3. On any `401`/`403` response, clears the token and redirects to `/login`. Therefore session expiry is handled globally.

Export groups: `authAPI`, `profileAPI`, `dashboardAPI`, `noticeAPI`, `scheduleAPI`, `studentAPI`, `busAPI`, `teacherAPI`, `adminAPI`.

---

# Android application

The Android team uses [CoUBusTracker_Project_Spec.md](Backend/CoUBusTracker_Project_Spec.md) as the single source of truth for the mobile app. That document contains:

- Data contracts / request & response payloads
- Authentication rules (JWT for students & teachers)
- Screen flows & navigation
- Recommended Android architecture
- Bengali UI guidelines
- Route options
- Release checklist

The app should use a **configurable base URL** and keep list of buses, schedules, notices, tracker links all server-driven.

---

# Testing

- Backend tests live in `Backend/src/test/java/`. Run them with:

  ```bash
  cd Backend
  mvn test
  ```

  The tests use `spring-boot-starter-test` and `spring-security-test`.

- Frontend lint (no unit test suite configured):

  ```bash
  cd Backend/admin-panel
  npm run lint
  ```

---

# Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `Connection refused` to PostgreSQL | Ensure PostgreSQL is running on port `5432` and the database `cou_bus_tracker` exists. |
| `Access denied for user` | Update `username`/`password` in `application-dev.yaml` to match your PostgreSQL credentials. |
| `Port 8080 already in use` | Change `server.port` in `application.yaml` or stop the process on 8080. |
| Flyway validation error (`checksum mismatch`) | Never edit an applied migration file. Add a new `V{n+1}__...sql` migration. |
| `column "is_active" is of type integer but expression is of type boolean` | DBeaver imported MySQL `BOOLEAN` (TINYINT) as PostgreSQL `INTEGER`. Run V13 migration or manually alter: `ALTER TABLE <table> ALTER COLUMN is_active TYPE BOOLEAN USING is_active::BOOLEAN;` |
| Panel can't reach API | Make sure the backend is running on port `8080` (Vite proxies `/api` and `/uploads` to it). |
| 401/403 in the panel | Session token expired or rebooted server; the panel auto-logs out. |

---

# Production checklist

- [ ] Override the **default JWT secret**.
- [ ] Change the **default admin password** (`admin@cou.ac.bd` / `Admin@123`).
- [ ] Use strong DB credentials (not `root1234`), ideally from secrets/Env.
- [ ] Switch to a production profile (or harden `application-dev.yaml`).
- [ ] Deploy `admin-panel/dist/` behind a static server / CDN and enable TLS.
- [ ] Restrict file uploads (extensions, size) and protect the `uploads/` directory.
- [ ] Run Flyway migrations before deploying the Android version that uses `bus_name`.
- [ ] Set up backups for the PostgreSQL database and the `uploads/` folder.
