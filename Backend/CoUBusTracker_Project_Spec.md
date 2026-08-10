# CoU Bus Tracker

> **Complete Project Specification**
> Comilla University Transport Management System

| | |
|---|---|
| **Project** | CoU Bus Tracker |
| **Developer** | Md. Tareq Hasan |
| **University** | Comilla University, CSE Batch 16 |
| **Type** | Live Production System |
| **Users** | ~5,000 students, teachers, and staff |
| **Version** | 1.0.0 |

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [System Architecture](#2-system-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Database Design](#4-database-design)
5. [Backend Specification](#5-backend--spring-boot-specification)
6. [API Specification](#6-api-specification)
7. [Admin Panel Specification](#7-react-admin-panel-specification)
8. [Android Application Specification](#8-android-application-specification)
9. [Deployment](#9-deployment-and-operations)
10. [Delivery Checklist](#10-full-delivery-checklist)

---

## 1. Product Overview

CoU Bus Tracker is a complete university transport platform for Comilla University. It consists of three connected products working together to deliver real-time bus tracking, schedule management, and public announcements to the university community.

### System Components

| Component | Technology | Users | Responsibility |
|---|---|---|---|
| **Backend** | Spring Boot 3 + MySQL | All clients | Secure REST API, authentication, business rules, database access |
| **Admin Panel** | React 19 + Vite + Tailwind | Transport administrators | Manage buses, live tracker links, schedules, notices, students, teachers |
| **Android App** | Java/Kotlin + Retrofit | Students, teachers, guests | Bengali-first bus, schedule, live-location, and notice experience |

### Key Principles

- All bus, route, schedule, tracker-link, and notice information is **server-driven**
- Neither the admin panel nor Android app should rely on hard-coded operational data
- The backend is the **only** component that talks to MySQL
- Admin panel **creates and changes** data; Android app primarily **reads** public data

---

## 2. System Architecture

```text
React Admin Panel ── JWT / HTTPS ──┐
                                   ├── Spring Boot REST API ── JPA / Flyway ── MySQL 8
Android App ──────── HTTPS ────────┘
```

### Data Flow

1. **Admin** logs into the React panel, manages buses/schedules/notices/tracker links
2. **Backend** validates and persists all changes via JPA, enforces JWT security
3. **Student/Teacher** opens the Android app, authenticates, and reads public data
4. **Guest** can browse buses, schedules, and notices without authentication

---

## 3. Technology Stack

| Layer | Technology | Details |
|---|---|---|
| **Language** | Java 21 | LTS version |
| **Framework** | Spring Boot 3.3 | Spring Web, Spring Security, Spring Data JPA |
| **Authentication** | JWT | Access token only, BCrypt password hashing |
| **Database** | MySQL 8 | Production database |
| **Migration** | Flyway | Versioned SQL migrations, runs on startup |
| **API Docs** | Springdoc OpenAPI | Swagger UI at `/swagger-ui.html` |
| **Admin Panel** | React 19 | Vite bundler, Axios HTTP client, Tailwind CSS |
| **Android** | Java (Kotlin optional) | Retrofit networking, Glide image loading |
| **Deployment** | Docker + Docker Compose | Nginx reverse proxy, HTTPS via TLS |

---

## 4. Database Design

### 4.1 Core Tables

#### `buses` — Main Transport Records

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | Bus identifier |
| `bus_number` | VARCHAR(20) | UNIQUE, NOT NULL | e.g. `BUS 03`, `BUS 11` |
| `bus_name` | VARCHAR(100) | NULLABLE | Optional friendly display name |
| `category` | VARCHAR(20) | NOT NULL | `BLUE`, `RED`, `TEACHER`, `OFFICER`, `STAFF` |
| `route` | TEXT | NULLABLE | e.g. `Kandirpar → Campus via Policeline` |
| `driver_name` | VARCHAR(100) | NULLABLE | Assigned driver |
| `driver_phone` | VARCHAR(20) | NULLABLE | Driver contact |
| `bus_image_url` | TEXT | NULLABLE | Optional bus photo |
| `is_active` | BOOLEAN | DEFAULT TRUE | Whether bus appears in public lists |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |

#### `schedules` — Individual Bus Trips

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | Schedule identifier |
| `bus_id` | BIGINT | FK → buses(id) | Parent bus |
| `departure_time` | TIME | NOT NULL | e.g. `08:30:00` |
| `arrival_time` | TIME | NULLABLE | e.g. `09:00:00` |
| `direction` | VARCHAR(10) | NOT NULL | `UP` (to campus) or `DOWN` (from campus) |
| `start_point` | VARCHAR(100) | NULLABLE | Route origin |
| `end_point` | VARCHAR(100) | NULLABLE | Route destination |
| `days` | VARCHAR(50) | DEFAULT `SAT-THU` | Service days |
| `is_active` | BOOLEAN | DEFAULT TRUE | Whether schedule is published |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |

#### `tracker_links` — Live Location URLs

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | Link identifier |
| `bus_id` | BIGINT | UNIQUE, FK → buses(id) | One link per bus |
| `tracker_url` | TEXT | NOT NULL | Full URL from GPS tracker device |
| `expires_at` | TIMESTAMP | NULLABLE | Auto-expiry (20 days from creation) |
| `updated_at` | TIMESTAMP | DEFAULT NOW() | Last update timestamp |
| `updated_by` | VARCHAR(100) | NULLABLE | Admin who updated |

#### `notices` — Public Announcements

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | Notice identifier |
| `title` | VARCHAR(200) | NOT NULL | e.g. `Blue Bus 4` |
| `body` | TEXT | NOT NULL | Notice content |
| `expiry_hours` | INT | DEFAULT 24 | Auto-expire after N hours |
| `is_active` | BOOLEAN | DEFAULT TRUE | Whether notice is visible |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |
| `expires_at` | TIMESTAMP | NULLABLE | Computed: created_at + expiry_hours |

#### `admins` — Admin Panel Accounts

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | Admin identifier |
| `email` | VARCHAR(100) | UNIQUE, NOT NULL | Login email |
| `password` | VARCHAR(255) | NOT NULL | BCrypt hashed |
| `name` | VARCHAR(100) | NULLABLE | Display name |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |

#### `students` — Student Accounts

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | Student identifier |
| `name` | VARCHAR(100) | NOT NULL | Full name |
| `email` | VARCHAR(100) | UNIQUE, NOT NULL | Accepts `@cou.ac.bd` (edu) or personal |
| `password` | VARCHAR(255) | NOT NULL | BCrypt hashed |
| `student_id` | VARCHAR(20) | UNIQUE, NOT NULL | e.g. `1607041` |
| `department` | VARCHAR(100) | NULLABLE | Department name |
| `varsity_batch` | VARCHAR(20) | NULLABLE | e.g. `16` |
| `is_edu_mail` | BOOLEAN | DEFAULT FALSE | True if `@cou.ac.bd` |
| `id_card_image_url` | TEXT | NULLABLE | Uploaded ID card image path |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |

#### `teachers` — Teacher Accounts

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | Teacher identifier |
| `name` | VARCHAR(100) | NOT NULL | Full name |
| `email` | VARCHAR(100) | UNIQUE, NOT NULL | Login email |
| `password` | VARCHAR(255) | NOT NULL | BCrypt hashed |
| `designation` | VARCHAR(100) | NULLABLE | e.g. `Professor` |
| `department` | VARCHAR(100) | NULLABLE | Department name |
| `phone` | VARCHAR(20) | NULLABLE | Contact number |
| `created_at` | TIMESTAMP | DEFAULT NOW() | Creation timestamp |

### 4.2 Entity Relationships

```text
buses ──1:N── schedules
buses ──1:1── tracker_links
buses ──1:N── notices (indirect, notices are global)
admins ── standalone
students ── standalone
teachers ── standalone
```

### 4.3 Business Rules

- `busNumber` is required and unique across the system
- `busName` is optional and used as a friendly display name
- A bus can have **multiple schedules** and **one tracker link**
- When a tracker URL is absent, clients **must hide** the live-location action
- Notice `expires_at` is computed automatically from `created_at + expiry_hours`
- Student `is_edu_mail` is auto-detected from `@cou.ac.bd` domain

### 4.4 Flyway Migration History

| Migration | Purpose |
|---|---|
| `V1` | Create `buses` table |
| `V2` | Create `schedules` table |
| `V3` | Create `tracker_links` table |
| `V4` | Create `notices` table |
| `V5` | Create `admins` table |
| `V6` | Seed initial bus and admin data |
| `V7` | Create `students` table |
| `V8` | Create `teachers` table |
| `V9` | Fix initial admin password hash |
| `V10` | Add optional `bus_name` to buses |

---

## 5. Backend — Spring Boot Specification

### 5.1 Project Structure

```text
src/main/java/com/cou/bustracker/
├── config/
│   ├── SecurityConfig.java          # Spring Security + JWT filter chain
│   └── OpenApiConfig.java           # Swagger/OpenAPI configuration
├── security/
│   ├── JwtService.java              # JWT generation, validation, extraction
│   ├── JwtAuthFilter.java           # OncePerRequestFilter for JWT
│   └── CustomUserDetailsService.java
├── entity/
│   ├── Bus.java
│   ├── Schedule.java
│   ├── TrackerLink.java
│   ├── Notice.java
│   ├── Admin.java
│   ├── Student.java
│   └── Teacher.java
├── repository/
│   ├── BusRepository.java
│   ├── ScheduleRepository.java
│   ├── TrackerLinkRepository.java
│   ├── NoticeRepository.java
│   ├── AdminRepository.java
│   ├── StudentRepository.java
│   └── TeacherRepository.java
├── dto/request/
│   ├── LoginRequest.java
│   ├── CreateBusRequest.java
│   ├── UpdateTrackerLinkRequest.java
│   ├── CreateScheduleRequest.java
│   ├── CreateNoticeRequest.java
│   ├── StudentRegisterRequest.java
│   └── TeacherRegisterRequest.java
├── dto/response/
│   ├── AuthResponse.java
│   ├── BusResponse.java
│   ├── BusDetailResponse.java
│   ├── ScheduleResponse.java
│   └── NoticeResponse.java
├── service/
│   ├── AuthService.java
│   ├── BusService.java
│   ├── ScheduleService.java
│   ├── TrackerLinkService.java
│   ├── NoticeService.java
│   ├── StudentService.java
│   ├── TeacherService.java
│   └── FileStorageService.java
├── controller/
│   ├── BusController.java           # GET /api/buses
│   ├── ScheduleController.java      # GET /api/schedules
│   ├── NoticeController.java        # GET /api/notices/active
│   ├── StudentAuthController.java   # /api/auth/student/**
│   ├── TeacherAuthController.java   # /api/auth/teacher/**
│   └── admin/
│       ├── AuthController.java      # POST /api/auth/admin/login
│       ├── AdminBusController.java  # /api/admin/buses/**
│       ├── AdminScheduleController.java
│       ├── AdminNoticeController.java
│       ├── AdminStudentController.java
│       ├── AdminTeacherController.java
│       └── AdminDashboardController.java
└── exception/
    ├── GlobalExceptionHandler.java  # @ControllerAdvice
    ├── ResourceNotFoundException.java
    └── UnauthorizedException.java

src/main/resources/
├── application.yaml                 # Base config, profiles
├── application-dev.yaml             # Local MySQL config
└── db/migration/
    ├── V1__create_buses_table.sql
    ├── V2__create_schedules_table.sql
    ├── V3__create_tracker_links_table.sql
    ├── V4__create_notices_table.sql
    ├── V5__create_admins_table.sql
    ├── V6__seed_initial_data.sql
    ├── V7__create_students_table.sql
    ├── V8__create_teachers_table.sql
    ├── V9__fix_admin_password.sql
    └── V10__add_bus_name.sql
```

### 5.2 Configuration

#### `application.yaml`

| Setting | Value |
|---|---|
| Profile | `spring.profiles.active: dev` |
| Server port | `8080` |
| Flyway | Enabled, locations: `classpath:db/migration` |
| JWT secret | From environment variable |
| JWT expiration | 86400000 ms (24 hours) |
| Swagger UI | `/swagger-ui.html` |
| API docs | `/api-docs` |

#### `application-dev.yaml`

| Setting | Value |
|---|---|
| Database URL | `jdbc:mysql://localhost:3306/cou_bus_tracker` |
| Username | `root` |
| Password | `root1234` |
| DDL auto | `none` (Flyway manages schema) |
| Show SQL | `false` |
| File upload path | `./uploads/` |

### 5.3 Security Configuration

- **Public endpoints** (no auth required):
  - `/api/buses/**`
  - `/api/schedules/**`
  - `/api/notices/active`
  - `/api/auth/**`
  - `/uploads/**`

- **Protected endpoints** (JWT required):
  - `/api/admin/**`
  - `/api/auth/student/upload-id-card`
  - `/api/auth/student/me`

- **JWT flow:**
  1. Client sends credentials to `/api/auth/*/login`
  2. Backend validates and returns `accessToken`
  3. Client includes `Authorization: Bearer <token>` on protected calls
  4. `JwtAuthFilter` validates token on every request
  5. On 401, client clears session and redirects to login

### 5.4 File Upload

- **Endpoint:** `POST /api/auth/student/upload-id-card`
- **Format:** `multipart/form-data`, field name: `file`
- **Storage:** `./uploads/student-id-cards/`
- **Access:** Public via `/uploads/student-id-cards/{filename}`
- **Auth:** Requires student JWT

---

## 6. API Specification

### 6.1 Base URL

All paths below are relative to `/api`.

| Environment | Base URL |
|---|---|
| Local development | `http://localhost:8080/api` |
| Emulator | `http://10.0.2.2:8080/api` |
| LAN device | `http://<computer-ip>:8080/api` |
| Production | `https://<domain>/api` |

### 6.2 Public APIs (No Auth Required)

#### `GET /buses` — List Active Buses

**Query Parameters:**
| Parameter | Type | Required | Description |
|---|---|---|---|
| `category` | string | No | Filter by `BLUE`, `RED`, `TEACHER`, `OFFICER`, `STAFF` |

**Response 200:**
```json
[
  {
    "id": 1,
    "busNumber": "BUS 03",
    "busName": "Kandirpar Express",
    "category": "BLUE",
    "route": "Kandirpar → Campus via Policeline",
    "driverName": "Rahman",
    "driverPhone": "01712345678",
    "busImageUrl": "https://...",
    "trackerUrl": "https://tracker.com/bus/03",
    "isActive": true
  }
]
```

---

#### `GET /buses/{id}` — Bus Details with Schedules

**Response 200:**
```json
{
  "id": 1,
  "busNumber": "BUS 03",
  "busName": "Kandirpar Express",
  "category": "BLUE",
  "route": "Kandirpar → Campus",
  "driverName": "Rahman",
  "driverPhone": "01712345678",
  "busImageUrl": "https://...",
  "trackerUrl": "https://tracker-platform.com/live/bus03",
  "schedules": [
    {
      "id": 10,
      "busNumber": "BUS 03",
      "busName": "Kandirpar Express",
      "category": "BLUE",
      "departureTime": "08:30",
      "arrivalTime": "09:00",
      "direction": "UP",
      "startPoint": "Kandirpar",
      "endPoint": "Campus",
      "days": "SAT-THU"
    }
  ]
}
```

---

#### `GET /schedules` — All Schedules

**Response 200:**
```json
[
  {
    "id": 10,
    "busNumber": "BUS 03",
    "busName": "Kandirpar Express",
    "category": "BLUE",
    "departureTime": "08:30",
    "arrivalTime": "09:00",
    "direction": "UP",
    "startPoint": "Kandirpar",
    "endPoint": "Campus",
    "days": "SAT-THU"
  }
]
```

---

#### `GET /schedules/bus/{busId}` — Bus-Specific Schedules

**Response 200:** Array of schedule objects for the specified bus.

---

#### `GET /notices/active` — Active Notices

**Response 200:**
```json
[
  {
    "id": 1,
    "title": "Blue Bus 4",
    "body": "Today this bus will stop due to maintenance.",
    "isActive": true,
    "createdAt": "2025-08-07T15:22:00",
    "expiresAt": "2025-08-08T15:22:00"
  }
]
```

---

### 6.3 Authentication APIs

#### `POST /auth/admin/login` — Admin Login

**Request:**
```json
{
  "email": "admin@cou.ac.bd",
  "password": "Admin@123"
}
```

**Response 200:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "adminName": "Admin"
}
```

**Response 401:**
```json
{
  "message": "Invalid email or password"
}
```

---

#### `POST /auth/student/register` — Student Registration

**Request:**
```json
{
  "name": "Tareq Hasan",
  "email": "1607041@cou.ac.bd",
  "password": "securepass",
  "studentId": "1607041",
  "department": "CSE",
  "varsityBatch": "16"
}
```

**Response 200:** `AuthResponse` with JWT token.

---

#### `POST /auth/student/login` — Student Login

**Request:** `LoginRequest` (email, password)

**Response 200:** `AuthResponse` with JWT token.

---

#### `POST /auth/student/upload-id-card` — Upload ID Card

**Request:** `multipart/form-data`, field: `file`

**Response 200:** `{ "imageUrl": "/uploads/student-id-cards/card.jpg" }`

---

#### `GET /auth/student/me` — Student Profile

**Response 200:** Student profile data.

---

#### `POST /auth/teacher/register` — Teacher Registration

**Request:**
```json
{
  "name": "Dr. Rahman",
  "email": "rahman@cou.ac.bd",
  "password": "securepass",
  "designation": "Professor",
  "department": "CSE",
  "phone": "01712345678"
}
```

**Response 200:** `AuthResponse` with JWT token.

---

#### `POST /auth/teacher/login` — Teacher Login

**Request:** `LoginRequest` (email, password)

**Response 200:** `AuthResponse` with JWT token.

---

### 6.4 Protected Admin APIs

> **Header required:** `Authorization: Bearer <token>`

#### Dashboard

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/dashboard` | Summary statistics |

#### Bus Management

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/buses` | List all buses |
| `POST` | `/admin/buses` | Create new bus |
| `PUT` | `/admin/buses/{id}` | Update bus info |
| `DELETE` | `/admin/buses/{id}` | Delete bus |

#### Tracker Link Management

| Method | Path | Description |
|---|---|---|
| `PUT` | `/admin/buses/{id}/tracker-link` | Create/update live tracker URL |

**Request:**
```json
{
  "trackerUrl": "https://tracker-platform.com/live/bus03-newlink",
  "expiresAt": "2025-08-27T00:00:00"
}
```

#### Schedule Management

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/schedules` | List all schedules |
| `POST` | `/admin/schedules` | Add schedule |
| `PUT` | `/admin/schedules/{id}` | Update schedule |
| `DELETE` | `/admin/schedules/{id}` | Delete schedule |

#### Notice Management

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/notices` | List all notices |
| `POST` | `/admin/notices` | Create notice |
| `DELETE` | `/admin/notices/{id}` | Delete notice |

#### Student Management

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/students` | List all students |
| `GET` | `/admin/students/pending` | List pending students |
| `PUT` | `/admin/students/{id}/verify` | Verify student |
| `PUT` | `/admin/students/{id}/toggle-active` | Activate/deactivate |

#### Teacher Management

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/teachers` | List all teachers |
| `GET` | `/admin/teachers/pending` | List pending teachers |
| `PUT` | `/admin/teachers/{id}/verify` | Verify teacher |
| `PUT` | `/admin/teachers/{id}/toggle-active` | Activate/deactivate |

---

### 6.5 Data Conventions

| Convention | Rule |
|---|---|
| **Time format** | `HH:mm` or `HH:mm:ss` strings |
| **DateTime format** | ISO-8601 strings |
| **Direction** | `UP` = ক্যাম্পাস অভিমুখে (toward campus), `DOWN` = ক্যাম্পাস থেকে (from campus) |
| **Nullable fields** | Clients must handle null values gracefully |
| **Error messages** | Ready for Bangla presentation by the client |
| **Response format** | JSON with consistent field naming (camelCase) |

---

## 7. React Admin Panel Specification

### 7.1 Purpose

The admin panel is the operational control center for the transport system. It must be responsive, secure, and readable on both desktop and tablet.

### 7.2 Tech Stack

| Technology | Purpose |
|---|---|
| React 19 | UI framework |
| Vite | Build tool and dev server |
| Axios | HTTP client with JWT interceptor |
| Tailwind CSS | Utility-first styling |
| React Router | Client-side routing |

### 7.3 Pages

| Route | Page | Description |
|---|---|---|
| `/login` | Login | Admin sign-in, JWT stored in localStorage |
| `/dashboard` | Dashboard | Totals, quick links, operational overview |
| `/buses` | Buses | Create, search, edit, delete buses; set name and live link |
| `/schedules` | Schedules | Bengali schedule table; dropdown-based management |
| `/notices` | Notices | Create and remove notices |
| `/students` | Students | Review, verify, activate/deactivate students |
| `/teachers` | Teachers | Review, verify, activate/deactivate teachers |

### 7.4 Key Behaviors

- Store admin JWT in `localStorage` and send via Axios on protected calls
- Redirect to `/login` on HTTP 401
- Use confirmation dialogs before any deletion
- Present server validation and request failures in visible error states
- Do not show a tracker-link dropdown item unless that bus has a saved tracker URL
- Bus name is optional in both Add Bus and Add Schedule flows
- Keep the provided route list as the schedule route options

### 7.5 Visual Design

- Blue/indigo brand palette with white surfaces
- Soft shadows and accessible contrast ratios
- Bengali text in the schedule-management experience
- Modal forms focused and grouped by related fields
- Loading, empty, and error states for every data table

---

## 8. Android Application Specification

### 8.1 Product Scope

#### User-Facing Features

- Browse active buses and filter by category
- View bus number, optional bus name, route, driver information, and schedules
- Open a selected bus's live-tracker link in the browser
- Browse all schedules; filter by route, bus, direction, and time
- Read active notices
- Student registration, login, profile, and ID-card upload
- Teacher registration and login
- Bengali UI copy, Bengali digits for display, graceful empty/offline/error states

#### User Roles

| Role | Android Access |
|---|---|
| **Guest** | Buses, schedules, and notices (no login required) |
| **Student** | Guest features + registration, login, profile, ID-card upload |
| **Teacher** | Guest features + registration and login |
| **Admin** | Uses the separate React admin panel; no admin screens in Android app |

---

### 8.2 Project Structure

```text
com.cou.bustracker/
├── activities/
│   ├── SplashActivity.java
│   ├── LoginChoiceActivity.java
│   ├── StudentLoginActivity.java
│   ├── StudentRegisterActivity.java
│   ├── TeacherLoginActivity.java
│   ├── TeacherRegisterActivity.java
│   ├── BusListActivity.java
│   ├── BusDetailActivity.java
│   ├── NoticeActivity.java
│   ├── ScheduleActivity.java
│   └── ProfileActivity.java
├── adapters/
│   ├── BusAdapter.java
│   ├── ScheduleAdapter.java
│   └── NoticeAdapter.java
├── models/
│   ├── BusResponse.java
│   ├── BusDetailResponse.java
│   ├── ScheduleResponse.java
│   ├── NoticeResponse.java
│   ├── AuthResponse.java
│   ├── LoginRequest.java
│   ├── StudentRegisterRequest.java
│   └── TeacherRegisterRequest.java
├── network/
│   ├── RetrofitClient.java
│   └── ApiService.java
└── utils/
    └── SessionManager.java
```

---

### 8.3 Backend Contract

#### Base URL Configuration

| Environment | Base URL |
|---|---|
| Emulator (local backend) | `http://10.0.2.2:8080/api` |
| Physical device (same Wi-Fi) | `http://<computer-LAN-IP>:8080/api` |
| Production | `https://<domain>/api` |

> **Important:** Production must use HTTPS. Do not ship a release APK with an IP address or development URL embedded in source code. Keep the base URL in a build configuration value.

#### Authentication Flow

1. Public bus, schedule, and notice APIs require **no token**
2. After login/register, save `accessToken`, `tokenType`, user role, and display name securely
3. Add `Authorization: Bearer <accessToken>` to authenticated API calls
4. On HTTP `401`, clear the session and send the user to the login screen
5. Current backend uses JWT access tokens; refresh-token support is not available

#### Public APIs Used by Android

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/buses` | All active buses |
| `GET` | `/buses?category=BLUE` | Active buses by category |
| `GET` | `/buses/{id}` | One bus, tracker link, and schedules |
| `GET` | `/schedules` | All schedules |
| `GET` | `/schedules/bus/{busId}` | Schedules for one bus |
| `GET` | `/notices/active` | Active, non-expired notices |

#### Authentication APIs Used by Android

| Method | Path | Purpose | Token Required |
|---|---|---|---|
| `POST` | `/auth/student/register` | Register a student | No |
| `POST` | `/auth/student/login` | Student login | No |
| `POST` | `/auth/student/upload-id-card` | Upload ID-card image (`multipart/form-data`, field: `file`) | Yes |
| `GET` | `/auth/student/me` | Student profile | Yes |
| `POST` | `/auth/teacher/register` | Register a teacher | No |
| `POST` | `/auth/teacher/login` | Teacher login | No |

#### Error Handling

The app must safely handle these HTTP status codes:

| Code | Meaning | App Action |
|---|---|---|
| `400` | Validation error | Show field-specific Bangla error |
| `401` | Unauthenticated | Clear session, redirect to login |
| `403` | Denied account | Show account status message |
| `404` | Missing record | Show not-found message |
| `409` | Duplicate email/ID | Show "already exists" message |
| `5xx` | Server error | Show generic error + retry |
| Timeout | No response | Show timeout message + retry |
| No connection | Offline | Show offline state + cached data |

> Display a short Bengali explanation plus a retry action; do not expose raw server errors to users.

---

### 8.4 Data Models

#### Bus List Item

| Field | Type | Notes |
|---|---|---|
| `id` | number | Bus identifier |
| `busNumber` | string | Required, e.g. `BUS 03` |
| `busName` | string/null | Optional display name |
| `category` | string | `BLUE`, `RED`, `TEACHER`, `OFFICER`, or `STAFF` |
| `route` | string/null | Admin-selected route |
| `driverName` | string/null | Optional |
| `driverPhone` | string/null | Render as dial action only with user confirmation |
| `busImageUrl` | string/null | Optional image |
| `trackerUrl` | string/null | Open externally only after deliberate tap |
| `isActive` | boolean | Public list contains active buses |

#### Bus Details

Contains all bus-list fields plus `schedules` (array of schedule objects). Use this endpoint for the Bus Details screen so live-link availability is current.

#### Schedule

| Field | Type | Display Rule |
|---|---|---|
| `id` | number | Internal list key |
| `busId` | number | Navigation target |
| `busNumber` | string | Primary label |
| `busName` | string/null | Display below/next to bus number when present |
| `category` | string | Small coloured badge |
| `departureTime` | string | `HH:mm` or `HH:mm:ss`; render in Bengali-friendly format |
| `arrivalTime` | string/null | Hide or show "নির্ধারিত নয়" when absent |
| `direction` | string | `UP` = ক্যাম্পাস অভিমুখে; `DOWN` = ক্যাম্পাস থেকে |
| `startPoint` | string/null | Route start |
| `endPoint` | string/null | Route end |
| `days` | string/null | Service days |

#### Notice

| Field | Type | Notes |
|---|---|---|
| `id` | number | List key |
| `title` | string | Notice heading |
| `body` | string | Notice content |
| `isActive` | boolean | Filter inactive notices defensively |
| `createdAt` | ISO-8601 datetime | Display in Bangladesh time zone |
| `expiresAt` | ISO-8601 datetime/null | Optional expiry display |

#### Authentication Result

Registration and login responses provide an access token and user identity information. Persist only the values needed for the session; never log passwords or tokens.

---

### 8.5 Approved Route Options

The Android app receives the selected route from the API. For filters or route chips, use these labels exactly:

| # | Route (Bengali) |
|---|---|
| 1 | কান্দিরপাড় > টমসমব্রিজ > ক্যাম্পাস |
| 2 | কান্দিরপাড় > পুলিশলাইন > ক্যাম্পাস |
| 3 | ধর্মপুর > কোটবাড়ি > ক্যাম্পাস |
| 4 | কান্দিরপাড় > ধর্মপুর > ক্যাম্পাস |
| 5 | কান্দিরপাড় > পদুয়ার বাজার বিশ্বরোড > ক্যাম্পাস |
| 6 | ক্যাম্পাস > পুলিশলাইন > কান্দিরপাড় |
| 7 | ক্যাম্পাস > কোটবাড়ি > ধর্মপুর |
| 8 | ক্যাম্পাস > টমসমব্রিজ > কান্দিরপাড় |
| 9 | ক্যাম্পাস > পদুয়ার বাজার বিশ্বরোড > কান্দিরপাড় |
| 10 | ক্যাম্পাস > ধর্মপুর > কান্দিরপাড় |

---

### 8.6 Technical Blueprint

#### Recommended Stack

| Area | Recommendation |
|---|---|
| Language | Kotlin preferred; Java acceptable if required |
| UI | Jetpack Compose; XML layouts acceptable for Java-only |
| Architecture | MVVM with Repository pattern |
| Networking | Retrofit + OkHttp + Gson/Moshi |
| State | ViewModel + StateFlow/LiveData |
| Navigation | Navigation Component |
| Images | Coil (Compose) or Glide (Views) |
| Local data | DataStore for session/preferences; Room for optional caching |
| Background | WorkManager only for intentional periodic refreshes |

#### Project Layering

```text
data/       API interfaces, DTOs, repositories
domain/     Models, use cases (if used)
ui/         Screens, components, view models
```

DTOs should match the API contracts; map them to presentation models where formatting is needed.

#### Network Rules

- Use an OkHttp interceptor for the JWT header
- Set explicit connect/read timeouts and make requests cancellable with the screen lifecycle
- Cache the most recent successful bus, schedule, and notice response locally
- Show a visible "সর্বশেষ হালনাগাদ" marker when offline data is displayed
- Never follow or preload tracker URLs; open only after a deliberate tap using the external browser

---

### 8.7 Screen and Navigation

#### Primary Navigation

Use a bottom navigation bar with:

| Tab | Bengali Label |
|---|---|
| Home | হোম |
| Schedule | শিডিউল |
| Notices | নোটিশ |
| Profile | প্রোফাইল |

#### Screen Specifications

| Screen | Content and Interactions |
|---|---|
| **Splash** | Brief logo screen; restore saved session; load initial public data |
| **Home / Bus List** | Search, category chips, bus cards, pull-to-refresh |
| **Bus Detail** | Bus number/name, route, driver info, current schedules, prominent live-location button only when `trackerUrl` exists |
| **Schedule** | Day/direction filters, route filter, chronological list grouped by time period |
| **Notices** | Active notices ordered newest first, refresh and empty state |
| **Login** | Separate student/teacher tabs or role selection |
| **Registration** | Validation messages in Bangla; student ID-card upload after authenticated registration |
| **Profile** | User information, verification/status, logout |

#### UI Design Guidelines

- Calm blue/indigo transport theme with white cards, soft shadows, clear contrast
- Bengali-capable fonts: Noto Sans Bengali or Hind Siliguri
- Bengali labels: `বাসসমূহ`, `লাইভ লোকেশন`, `আজকের শিডিউল`, `কোনো শিডিউল পাওয়া যায়নি`, `আবার চেষ্টা করুন`
- All tap targets at least 48dp
- Support large font sizes and provide content descriptions for icons
- Use skeleton/loading cards rather than a blank screen while fetching data

---

### 8.8 Acceptance Checklist

- [ ] App runs against local, staging, and production base URLs without source edits
- [ ] Bus list, bus detail, schedules, and notices work while logged out
- [ ] Optional `busName`, `route`, image, driver data, and tracker link render safely when null
- [ ] Live-location button is hidden when no tracker link exists
- [ ] Student and teacher authentication handles token persistence and 401 logout
- [ ] All user-facing UI and errors are understandable in Bangla
- [ ] Offline, loading, empty, and retry states are implemented for every public list
- [ ] Screen rotation/process recreation does not lose selected filters or active screen state
- [ ] Release build uses HTTPS and has no development credentials or URLs

---

### 8.9 Backend Notes for Android Development

- API documentation is available at `/swagger-ui.html` when the backend is running
- Flyway migration `V10__add_bus_name.sql` adds the optional `bus_name` field
- Administrators configure buses, schedules, and live links in the React admin panel
- The Android app is read-oriented and should refresh API data instead of duplicating admin controls

---

## 9. Deployment and Operations

### 9.1 Backend Deployment

1. Build the Spring Boot JAR with Maven
2. Run with a production profile
3. Configure database credentials and JWT secret through environment variables
4. **Never** commit production secrets
5. Run MySQL with persistent storage
6. Allow Flyway to execute exactly once during a deploy
7. Put Nginx, Caddy, or a managed load balancer in front of the API for TLS/HTTPS
8. Restrict database access to the backend network
9. Back up MySQL regularly

### 9.2 Admin Panel Deployment

1. Run production build from `admin-panel/`
2. Serve static files through a CDN, Nginx, or Vercel
3. Proxy `/api` to the backend or set the production API base URL deliberately
4. Restrict admin access using valid JWTs
5. **Change the seeded admin password before launch**

### 9.3 Android Release

1. Use the production HTTPS base URL through build configuration
2. Configure a release signing key outside the repository
3. Test with a real low-bandwidth mobile network
4. Publish a privacy policy covering account data and optional student ID-card uploads

---

## 10. Full Delivery Checklist

### Spring Boot Backend

- [x] Public bus, schedule, and notice APIs
- [x] Admin JWT authentication and admin management APIs
- [x] Student and teacher registration/login APIs
- [x] Bus, schedule, live-link, and notice persistence
- [x] Flyway migrations V1–V10, including optional bus name
- [x] Student and teacher entity/service/controller
- [x] File upload service for ID cards
- [ ] Swagger smoke test against deployed environment
- [ ] Automated integration tests for authentication and admin CRUD
- [ ] Production configuration review and database backup test

### React Admin Panel

- [x] Login, dashboard, bus, schedule, notice, student, and teacher pages
- [x] Bus name and live-link entry in the Add Bus flow
- [x] Bengali schedule management with route/time dropdowns
- [x] Refined visual styling with Tailwind CSS
- [ ] Error/success toast standardisation across every page
- [ ] Production deployment and browser acceptance test

### Android Application

- [ ] Implement API client, session storage, and JWT interceptor
- [ ] Implement bus list/detail, schedules, and notices
- [ ] Implement student/teacher authentication and student ID-card upload
- [ ] Implement Bengali accessibility, offline cache, and retry states
- [ ] Test against production HTTPS API and prepare Play Store release

---

**Built by Md. Tareq Hasan — Comilla University CSE Batch 16**
