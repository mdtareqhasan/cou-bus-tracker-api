# CoU Bus Tracker - VPS Project Structure & Database Schema

> **Last Updated:** 2026-09-16
> **VPS Host:** kubijatra (root@kubijatra)
> **Project Path:** `/opt/cou-bus-tracker/`
> **API URL:** `https://api.kubijatra.com`
> **Admin Panel:** `https://admin.kubijatra.com`

---

## 1. VPS Directory Structure

```
/opt/cou-bus-tracker/
├── Backend/                          # Spring Boot Backend (Java 21)
│   ├── backups/
│   │   └── dump-cou_bus_tracker-202609131203.sql
│   ├── docker-compose.yml
│   ├── docker-compose.prod.yml
│   ├── docker-compose.postgres.yml
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/cou/bustracker/
│       │   │   ├── CouBusTrackerApplication.java
│       │   │   ├── config/
│       │   │   │   ├── CloudinaryConfig.java
│       │   │   │   ├── OpenApiConfig.java
│       │   │   │   ├── SecurityConfig.java
│       │   │   │   ├── SuperAdminDataInitializer.java
│       │   │   │   └── WebMvcConfig.java
│       │   │   ├── controller/
│       │   │   │   ├── BusController.java
│       │   │   │   ├── ConfigController.java
│       │   │   │   ├── EmailVerificationController.java
│       │   │   │   ├── GoogleAuthController.java
│       │   │   │   ├── NoticeController.java
│       │   │   │   ├── PhoneVerificationController.java
│       │   │   │   ├── ScheduleController.java
│       │   │   │   ├── SmsDebugController.java
│       │   │   │   ├── StudentAuthController.java
│       │   │   │   ├── TeacherAuthController.java
│       │   │   │   ├── admin/
│       │   │   │   │   ├── AdminBusController.java
│       │   │   │   │   ├── AdminDashboardController.java
│       │   │   │   │   ├── AdminManagementController.java
│       │   │   │   │   ├── AdminNoticeController.java
│       │   │   │   │   ├── AdminProfileController.java
│       │   │   │   │   ├── AdminScheduleController.java
│       │   │   │   │   ├── AdminStudentController.java
│       │   │   │   │   ├── AdminTeacherController.java
│       │   │   │   │   └── AuthController.java
│       │   │   │   └── superadmin/
│       │   │   │       ├── SuperAdminAuthController.java
│       │   │   │       ├── SuperAdminConfigController.java
│       │   │   │       ├── SuperAdminManagementController.java
│       │   │   │       └── SuperAdminNoticeController.java
│       │   │   ├── dto/
│       │   │   │   ├── config/
│       │   │   │   │   ├── AppConfigUpdateRequest.java
│       │   │   │   │   └── PublicConfigResponse.java
│       │   │   │   ├── notice/
│       │   │   │   │   └── BroadcastNoticeRequest.java
│       │   │   │   ├── request/
│       │   │   │   │   ├── CreateAdminRequest.java
│       │   │   │   │   ├── CreateBusRequest.java
│       │   │   │   │   ├── CreateNoticeRequest.java
│       │   │   │   │   ├── CreateScheduleRequest.java
│       │   │   │   │   ├── EmailVerificationRequest.java
│       │   │   │   │   ├── GoogleLoginRequest.java
│       │   │   │   │   ├── LoginRequest.java
│       │   │   │   │   ├── PhoneVerificationInitRequest.java
│       │   │   │   │   ├── SendPhoneOtpRequest.java
│       │   │   │   │   ├── StudentRegisterRequest.java
│       │   │   │   │   ├── TeacherRegisterRequest.java
│       │   │   │   │   ├── UpdateAdminProfileRequest.java
│       │   │   │   │   ├── UpdateBusRequest.java
│       │   │   │   │   ├── UpdateTrackerLinkRequest.java
│       │   │   │   │   ├── VerifyEmailOtpRequest.java
│       │   │   │   │   └── VerifyPhoneOtpRequest.java
│       │   │   │   ├── response/
│       │   │   │   │   ├── AdminProfileResponse.java
│       │   │   │   │   ├── AuthResponse.java
│       │   │   │   │   ├── BusDetailResponse.java
│       │   │   │   │   ├── BusResponse.java
│       │   │   │   │   ├── DashboardStatsResponse.java
│       │   │   │   │   ├── FileUploadResponse.java
│       │   │   │   │   ├── MessageResponse.java
│       │   │   │   │   ├── NoticeResponse.java
│       │   │   │   │   ├── ScheduleResponse.java
│       │   │   │   │   ├── StudentResponse.java
│       │   │   │   │   └── TeacherResponse.java
│       │   │   │   └── superadmin/
│       │   │   │       ├── SuperAdminCreateRequest.java
│       │   │   │       ├── SuperAdminLoginRequest.java
│       │   │   │       ├── SuperAdminLoginResponse.java
│       │   │   │       ├── SuperAdminResponse.java
│       │   │   │       └── SuperAdminUpdateRequest.java
│       │   │   ├── entity/
│       │   │   │   ├── Admin.java
│       │   │   │   ├── AppConfig.java
│       │   │   │   ├── Bus.java
│       │   │   │   ├── EmailVerificationOtp.java
│       │   │   │   ├── Notice.java
│       │   │   │   ├── PhoneVerificationOtp.java
│       │   │   │   ├── Schedule.java
│       │   │   │   ├── Student.java
│       │   │   │   ├── SuperAdmin.java
│       │   │   │   ├── Teacher.java
│       │   │   │   └── TrackerLink.java
│       │   │   ├── exception/
│       │   │   │   ├── GlobalExceptionHandler.java
│       │   │   │   ├── ResourceNotFoundException.java
│       │   │   │   └── UnauthorizedException.java
│       │   │   ├── repository/
│       │   │   │   ├── AdminRepository.java
│       │   │   │   ├── AppConfigRepository.java
│       │   │   │   ├── BusRepository.java
│       │   │   │   ├── EmailVerificationOtpRepository.java
│       │   │   │   ├── NoticeRepository.java
│       │   │   │   ├── PhoneVerificationOtpRepository.java
│       │   │   │   ├── ScheduleRepository.java
│       │   │   │   ├── StudentRepository.java
│       │   │   │   ├── SuperAdminRepository.java
│       │   │   │   ├── TeacherRepository.java
│       │   │   │   └── TrackerLinkRepository.java
│       │   │   ├── security/
│       │   │   │   ├── CustomUserDetailsService.java
│       │   │   │   ├── JwtAuthFilter.java
│       │   │   │   └── JwtService.java
│       │   │   ├── service/
│       │   │   │   ├── AdminManagementService.java
│       │   │   │   ├── AdminProfileService.java
│       │   │   │   ├── AppConfigService.java
│       │   │   │   ├── AuthService.java
│       │   │   │   ├── BusService.java
│       │   │   │   ├── CloudinaryService.java
│       │   │   │   ├── EmailVerificationService.java
│       │   │   │   ├── FileStorageService.java
│       │   │   │   ├── GoogleTokenService.java
│       │   │   │   ├── NoticeService.java
│       │   │   │   ├── PhoneVerificationService.java
│       │   │   │   ├── ScheduleService.java
│       │   │   │   ├── SmsService.java
│       │   │   │   ├── StudentService.java
│       │   │   │   ├── SuperAdminService.java
│       │   │   │   ├── TeacherService.java
│       │   │   │   └── TrackerLinkService.java
│       │   │   └── util/
│       │   │       └── PhoneUtils.java
│       │   └── resources/
│       │       ├── application.yaml
│       │       ├── application-dev.yaml
│       │       ├── application-docker.yaml
│       │       ├── application-prod.yaml
│       │       ├── application-render.yaml
│       │       └── db/migration/
│       │           ├── V1__create_buses_table.sql
│       │           ├── V2__create_schedules_table.sql
│       │           ├── V3__create_tracker_links_table.sql
│       │           ├── V4__create_notices_table.sql
│       │           ├── V5__create_admins_table.sql
│       │           ├── V6__seed_initial_data.sql
│       │           ├── V7__create_students_table.sql
│       │           ├── V8__create_teachers_table.sql
│       │           ├── V9__fix_admin_password.sql
│       │           ├── V10__add_bus_name.sql
│       │           ├── V11__move_legacy_saturday_schedules_to_weekdays.sql
│       │           ├── V12__reseed_data_postgresql.sql
│       │           ├── V13__fix_boolean_columns.sql
│       │           ├── V14__add_teacher_identity_cards_and_google_auth.sql
│       │           ├── V15__add_email_verification.sql
│       │           ├── V16__create_super_admins_and_app_config.sql
│       │           ├── V17__add_phone_verification.sql
│       │           ├── V18__remove_email_from_students_teachers.sql
│       │           └── V19__add_pending_registration_to_phone_verification.sql
│       └── test/java/com/cou/bustracker/
│           ├── service/AdminManagementServiceTest.java
│           └── util/PhoneUtilsTest.java
├── admin/admin-panel/                # React Admin Panel (Vite + Tailwind)
│   └── src/
│       ├── api.js
│       ├── App.jsx
│       ├── main.jsx
│       ├── context/AuthContext.jsx
│       ├── pages/
│       │   ├── AdminProfilePage.jsx
│       │   ├── AdminUsersPage.jsx
│       │   ├── BusesPage.jsx
│       │   ├── DashboardPage.jsx
│       │   ├── Layout.jsx
│       │   ├── LoginPage.jsx
│       │   ├── NoticesPage.jsx
│       │   ├── SchedulesPage.jsx
│       │   ├── StudentsPage.jsx
│       │   └── TeachersPage.jsx
│       ├── components/
│       │   ├── ExportPdfButton.jsx
│       │   └── IdCardThumb.jsx
│       └── utils/
│           ├── format.js
│           └── pdfExporter.js
├── super_admin/                      # React Super Admin Panel (Vite + MUI)
│   └── src/
│       ├── api/axios.js
│       ├── context/AuthContext.jsx
│       ├── pages/
│       │   ├── Login.jsx
│       │   ├── Dashboard.jsx
│       │   ├── SuperAdmins.jsx
│       │   ├── ServerConfig.jsx
│       │   ├── AppVersion.jsx
│       │   └── BroadcastNotice.jsx
│       └── ...
├── render.yaml                       # Render deployment config
└── deploy/                           # Deployment scripts
```

---

## 2. Docker Containers

| Container | Image | Port | Status |
|-----------|-------|------|--------|
| `cou-bus-tracker-app` | Custom (Spring Boot) | 8080 (internal) | Running (healthy) |
| `cou-bus-tracker-postgres` | `postgres:17-alpine` | 5432 (localhost only) | Running (healthy) |

**Useful Commands:**
```bash
# Check container status
docker ps

# View app logs
docker logs cou-bus-tracker-app --tail 50

# Restart app
docker restart cou-bus-tracker-app

# Access PostgreSQL
docker exec -it cou-bus-tracker-postgres psql -U cou_bus_tracker_user -d cou_bus_tracker

# Backup database
docker exec cou-bus-tracker-postgres pg_dump -U cou_bus_tracker_user cou_bus_tracker > backup.sql
```

---

## 3. Database Schema (PostgreSQL)

### Database Credentials
```
Database: cou_bus_tracker
Username: cou_bus_tracker_user
Password: (stored in Docker environment / .env)
Host: host.docker.internal:5432 (from app container) / localhost:5432 (from host)
```

### Table: `students`
```sql
CREATE TABLE public.students (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(100),
    password          VARCHAR(255),
    student_id        VARCHAR(50),          -- Currently stores Roll Number
    department        VARCHAR(100),
    varsity_batch     VARCHAR(20),          -- Currently stores Session
    id_card_image_url TEXT,
    is_verified       BOOLEAN DEFAULT FALSE,
    is_active         BOOLEAN DEFAULT TRUE,
    created_at        TIMESTAMP,
    google_subject    VARCHAR(255),         -- Deprecated (Google Auth)
    phone             VARCHAR(20) NOT NULL,
    is_phone_verified BOOLEAN NOT NULL DEFAULT FALSE
);

Indexes:
  - "students_phone_key" UNIQUE (phone)
  - "uk_students_google_subject" UNIQUE (google_subject) WHERE google_subject IS NOT NULL
```

### Table: `teachers`
```sql
CREATE TABLE public.teachers (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(100),
    password          VARCHAR(255),
    designation       VARCHAR(100),
    department        VARCHAR(100),
    phone             VARCHAR(20) NOT NULL,
    is_verified       BOOLEAN DEFAULT FALSE,
    is_active         BOOLEAN DEFAULT TRUE,
    created_at        TIMESTAMP,
    teacher_id        VARCHAR(50),          -- Employee ID (numeric)
    id_card_image_url TEXT,
    google_subject    VARCHAR(255),         -- Deprecated
    is_phone_verified BOOLEAN NOT NULL DEFAULT FALSE
);

Indexes:
  - "uk_teachers_phone" UNIQUE (phone)
  - "uk_teachers_teacher_id" UNIQUE (teacher_id) WHERE teacher_id IS NOT NULL
```

### Table: `phone_verification_otps`
```sql
CREATE TABLE public.phone_verification_otps (
    id                        BIGSERIAL PRIMARY KEY,
    phone                     VARCHAR(20) NOT NULL,
    user_role                 VARCHAR(20) NOT NULL,   -- 'STUDENT' or 'TEACHER'
    otp_hash                  VARCHAR(255) NOT NULL,
    expires_at                TIMESTAMP NOT NULL,
    last_sent_at              TIMESTAMP NOT NULL,
    failed_attempts           INTEGER NOT NULL DEFAULT 0,
    created_at                TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pending_registration_json TEXT,                   -- Staged registration data
    pending_id_card_url       TEXT                    -- Cloudinary URL of ID card
);

Indexes:
  - "uk_phone_verification_otps_phone_role" UNIQUE (phone, user_role)
```

### Table: `admins`
```sql
CREATE TABLE public.admins (
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(100),
    password   VARCHAR(255),
    name       VARCHAR(100),
    created_at TIMESTAMP
);
```

### Table: `buses`
```sql
CREATE TABLE public.buses (
    id             BIGSERIAL PRIMARY KEY,
    bus_number     VARCHAR(20),
    bus_name       VARCHAR(100),
    category       VARCHAR(20),          -- BLUE/RED/TEACHER/OFFICER/STAFF
    route          TEXT,
    driver_name    VARCHAR(100),
    driver_phone   VARCHAR(20),
    bus_image_url  TEXT,
    is_active      BOOLEAN DEFAULT TRUE,
    created_at     TIMESTAMP
);
```

### Table: `schedules`
```sql
CREATE TABLE public.schedules (
    id             BIGSERIAL PRIMARY KEY,
    bus_id         BIGINT,
    departure_time TIME,
    arrival_time   TIME,
    direction      VARCHAR(10),          -- UP/DOWN
    start_point    VARCHAR(200),
    end_point      VARCHAR(200),
    days           VARCHAR(100),         -- e.g. "Sun,Mon,Tue,Wed,Thu"
    is_active      BOOLEAN DEFAULT TRUE,
    created_at     TIMESTAMP
);
```

### Table: `notices`
```sql
CREATE TABLE public.notices (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200),
    body        TEXT,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP,
    expires_at  TIMESTAMP
);
```

### Table: `super_admins`
```sql
CREATE TABLE public.super_admins (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(100) UNIQUE,
    password    VARCHAR(255),
    full_name   VARCHAR(150),
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);
```

### Table: `app_config`
```sql
CREATE TABLE public.app_config (
    id           BIGSERIAL PRIMARY KEY,
    config_key   VARCHAR(100) NOT NULL,
    config_value TEXT NOT NULL,
    description  VARCHAR(500),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by   VARCHAR(150)
);
```

### Table: `tracker_links`
```sql
CREATE TABLE public.tracker_links (
    id          BIGSERIAL PRIMARY KEY,
    bus_id      BIGINT,
    tracker_url TEXT,
    is_active   BOOLEAN DEFAULT TRUE,
    updated_at  TIMESTAMP
);
```

### Table: `email_verification_otps`
```sql
CREATE TABLE public.email_verification_otps (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(100) NOT NULL,
    user_role      VARCHAR(20) NOT NULL,
    otp_hash       VARCHAR(255) NOT NULL,
    expires_at     TIMESTAMP NOT NULL,
    last_sent_at   TIMESTAMP NOT NULL,
    failed_attempts INTEGER DEFAULT 0 NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

---

## 4. Flyway Migrations

| Version | Description |
|---------|-------------|
| V1 | Create buses table |
| V2 | Create schedules table |
| V3 | Create tracker_links table |
| V4 | Create notices table |
| V5 | Create admins table |
| V6 | Seed initial data |
| V7 | Create students table |
| V8 | Create teachers table |
| V9 | Fix admin password |
| V10 | Add bus name |
| V11 | Move legacy Saturday schedules to weekdays |
| V12 | Reseed data (PostgreSQL) |
| V13 | Fix boolean columns |
| V14 | Add teacher identity cards and Google auth |
| V15 | Add email verification |
| V16 | Create super admins and app config |
| V17 | Add phone verification |
| V18 | Remove email from students/teachers |
| V19 | Add pending registration to phone verification |

**Flyway Config (application-prod.yaml):**
```yaml
flyway:
  enabled: true
  locations: classpath:db/migration
  baseline-on-migrate: true
  baseline-version: 11
  repair-on-migrate: true
```

> **IMPORTANT:** New migrations must be V20 or higher. Flyway baseline is set to V11.

---

## 5. API Endpoints

### Public Endpoints (No Auth Required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/phone-verification/init` | Registration (multipart: form data + ID card) |
| POST | `/api/auth/phone-verification/verify` | Verify OTP |
| POST | `/api/auth/phone-verification/resend` | Resend OTP |
| POST | `/api/auth/phone-verification/send` | Legacy send OTP |
| POST | `/api/auth/student/login` | Student login (phone + password) |
| POST | `/api/auth/teacher/login` | Teacher login (phone + password) |
| POST | `/api/auth/admin/login` | Admin login (email + password) |
| POST | `/api/super-admin/auth/login` | Super admin login |
| GET | `/api/buses` | List active buses |
| GET | `/api/buses/{id}` | Bus detail |
| GET | `/api/schedules` | List active schedules |
| GET | `/api/schedules/bus/{busId}` | Schedules by bus |
| GET | `/api/notices/active` | Active notices |
| GET | `/api/config` | Public app config |
| GET | `/api/app/version` | App version check |
| GET | `/swagger-ui.html` | API docs |

### Student Endpoints (JWT Required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/auth/student/me` | Get profile |
| POST | `/api/auth/student/upload-id-card` | Upload/replace ID card |

### Teacher Endpoints (JWT Required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/auth/teacher/me` | Get profile |
| POST | `/api/auth/teacher/upload-id-card` | Upload/replace ID card |

### Admin Endpoints (Admin JWT Required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/dashboard` | Dashboard stats |
| GET | `/api/admin/profile` | Admin profile |
| PUT | `/api/admin/profile` | Update admin profile |
| GET | `/api/admin/students` | List all students |
| GET | `/api/admin/students/pending` | Pending students |
| POST | `/api/admin/students/{id}/verify` | Verify student |
| PUT | `/api/admin/students/{id}/toggle-active` | Toggle student active |
| DELETE | `/api/admin/students/{id}` | Delete student |
| GET | `/api/admin/teachers` | List all teachers |
| POST | `/api/admin/teachers/{id}/verify` | Verify teacher |
| POST | `/api/admin/buses` | Create bus |
| PUT | `/api/admin/buses/{id}` | Update bus |
| DELETE | `/api/admin/buses/{id}` | Delete bus |
| POST | `/api/admin/schedules` | Create schedule |
| POST | `/api/admin/notices` | Create notice |

### Super Admin Endpoints (Super Admin JWT Required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/super-admin/dashboard` | Dashboard |
| GET | `/api/super-admin/admins` | List admins |
| POST | `/api/super-admin/admins` | Create admin |
| PUT | `/api/super-admin/admins/{id}` | Update admin |
| DELETE | `/api/super-admin/admins/{id}` | Delete admin |
| GET | `/api/super-admin/config` | Get all config |
| PUT | `/api/super-admin/config` | Update config |
| POST | `/api/super-admin/notices/broadcast` | Broadcast notice |

---

## 6. Registration Flow (Current)

```
┌─────────────────────────────────────────────────────────┐
│  Stage 1: POST /api/auth/phone-verification/init       │
│  (multipart/form-data)                                  │
│                                                         │
│  Fields: role, name, phone, password, department,       │
│          studentId, varsityBatch (for STUDENT)          │
│          teacherId, designation (for TEACHER)           │
│          idCard (file upload)                           │
│                                                         │
│  Backend:                                               │
│  1. Validates all fields                                │
│  2. Checks phone/ID uniqueness                          │
│  3. Uploads ID card to Cloudinary                       │
│  4. Serializes payload to JSON                          │
│  5. Generates 6-digit OTP                               │
│  6. Stores OTP hash + payload in phone_verification_otps│
│  7. Sends OTP via BulkSMSBD SMS                         │
│  8. Returns { "message": "OTP sent" }                   │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  Stage 2: POST /api/auth/phone-verification/verify     │
│  (JSON body)                                            │
│                                                         │
│  Fields: { phone, role, otp }                           │
│                                                         │
│  Backend:                                               │
│  1. Finds OTP record by phone + role                    │
│  2. Checks expiry and max attempts                      │
│  3. Verifies OTP hash                                   │
│  4. Creates Student/Teacher row from staged JSON        │
│  5. Generates JWT token                                 │
│  6. Returns AuthResponse with JWT                       │
└─────────────────────────────────────────────────────────┘
```

---

## 7. Login Flow

```
POST /api/auth/student/login  (or /teacher/login)
Body: { "phone": "01XXXXXXXXX", "password": "xxx" }

Backend:
1. Normalizes phone to 01XXXXXXXXX format
2. Finds student/teacher by phone
3. Verifies password hash
4. Checks isActive, isPhoneVerified
5. Generates JWT with phone as subject and role claim
6. Returns AuthResponse
```

---

## 8. JWT Configuration

```yaml
jwt:
  secret: ${JWT_SECRET:}        # HMAC-SHA key (set in env)
  expiration: 86400000           # 24 hours in milliseconds
```

**JWT Claims:**
- `sub`: Phone number (for student/teacher) or email (for admin)
- `role`: "STUDENT" / "TEACHER" / "ADMIN" / "SUPER_ADMIN"
- `exp`: 24 hours from issuance

---

## 9. External Services

### Cloudinary (Image Upload)
- Student ID cards: `student-id-cards/` folder
- Teacher ID cards: `teacher-id-cards/` folder
- Max 5MB, JPG/PNG only
- Auto-compressed to ~300KB

### BulkSMSBD (SMS Gateway)
- API: `https://bulksmsbd.net/api/smsapi`
- Balance Check: `https://bulksmsbd.net/api/getBalanceApi`
- Phone format: `8801XXXXXXXXX` (E.164)
- OTP: 6-digit, 2-minute expiry

---

## 10. Flutter App Structure (D:\myapp)

```
D:\myapp\lib\
├── main.dart
├── app/
│   ├── app.dart                    # App widget, session expiry handler
│   ├── router.dart                 # GoRouter routes
│   ├── shell_screen.dart           # Bottom nav shell
│   └── theme.dart                  # Colors, gradients, spacing
├── core/
│   ├── api_client.dart             # Dio client + interceptors
│   ├── api_service.dart            # Legacy http client
│   ├── constants.dart              # API URLs, endpoints, storage keys
│   ├── error_handler.dart          # Bengali error messages
│   ├── result.dart                 # Result<T> sealed class
│   ├── storage_service.dart        # Secure + shared prefs
│   ├── update_service.dart         # Version update check
│   └── config/
│       ├── app_version_checker.dart
│       └── remote_config_service.dart
│   └── utils/
│       ├── phone_utils.dart        # BD phone validation
│       └── time_utils.dart         # Bengali time formatting
├── features/
│   ├── providers.dart              # Riverpod DI providers
│   ├── about/about_screen.dart
│   ├── auth/
│   │   ├── auth_provider.dart      # Auth state management
│   │   ├── auth_repository.dart    # Auth API calls
│   │   ├── login_screen.dart       # Login UI
│   │   ├── phone_otp_verification_screen.dart
│   │   ├── register_screen.dart    # Registration UI
│   │   ├── role_screen.dart        # Role selection
│   │   └── upload_id_screen.dart   # Standalone ID upload
│   ├── buses/
│   │   ├── bus_detail_screen.dart
│   │   ├── bus_list_screen.dart
│   │   ├── bus_repository.dart
│   │   ├── buses_provider.dart
│   │   └── live_tracking_screen.dart
│   ├── home/
│   │   ├── home_provider.dart
│   │   └── home_screen.dart
│   ├── notices/
│   │   ├── notice_repository.dart
│   │   ├── notice_screen.dart
│   │   └── notices_provider.dart
│   ├── profile/profile_screen.dart
│   ├── schedules/
│   │   ├── schedule_repository.dart
│   │   ├── schedule_screen.dart
│   │   └── schedules_provider.dart
│   └── splash/
│       ├── force_update_screen.dart
│       ├── maintenance_screen.dart
│       └── splash_screen.dart
└── shared/
    ├── models/
    │   ├── auth_response.dart
    │   ├── bus.dart (+.g.dart)
    │   ├── bus_detail.dart (+.g.dart)
    │   ├── notice.dart (+.g.dart)
    │   ├── schedule.dart (+.g.dart)
    │   └── student.dart (+.g.dart)
    └── widgets/
        ├── bus_card.dart
        ├── live_indicator.dart
        ├── schedule_card.dart
        ├── stat_card.dart
        └── update_dialog.dart
```

### Flutter Dependencies
| Package | Version | Purpose |
|---------|---------|---------|
| `flutter_riverpod` | ^2.6.1 | State management |
| `dio` | ^5.7.0 | HTTP client |
| `go_router` | ^14.8.1 | Routing |
| `flutter_secure_storage` | ^9.2.4 | Token storage |
| `image_picker` | ^1.1.2 | Gallery image picking |
| `flutter_image_compress` | ^2.5.1 | Image compression |
| `cached_network_image` | ^3.4.1 | Image caching |
| `webview_flutter` | ^4.9.0 | Live tracking |
| `geolocator` | ^13.0.1 | GPS location |

### Flutter API Endpoints (from constants.dart)
```dart
static const String initPhoneRegistration = '/auth/phone-verification/init';
static const String verifyPhoneOtp = '/auth/phone-verification/verify';
static const String resendPhoneOtp = '/auth/phone-verification/resend';
static const String sendPhoneOtp = '/auth/phone-verification/send';
static const String studentLoginPhone = '/auth/student/login';
static const String teacherLoginPhone = '/auth/teacher/login';
static const String studentProfile = '/auth/student/me';
static const String teacherProfile = '/auth/teacher/me';
static const String studentUploadIdCard = '/auth/student/upload-id-card';
static const String teacherUploadIdCard = '/auth/teacher/upload-id-card';
static const String appVersion = '/app/version';
static const String publicConfig = '/config';
```

---

## 11. Student ID Card Format (Comilla University)

```
┌─────────────────────────────────────┐
│  Comilla University                 │
│  [Hall Name]                        │
│  [Non Resident/Resident] ID Card   │
│                                     │
│  [Photo]                            │
│                                     │
│  Name: [Full Name]                  │
│  D.No: [Department No]              │
│  Roll No: [Roll Number]             │
│  Session: [Session Year]            │
│  Dept: [Department Code]            │
│  Blood Gr.: [Blood Group]           │
│                                     │
│  [Signature]                        │
│  Provost                            │
└─────────────────────────────────────┘
```

**Key Fields for Validation:**
- University name: "Comilla University" / "কমিলা বিশ্ববিদ্যালয়"
- Roll No: Numeric (e.g., 12208055)
- Session: Format "YYYY-YY" (e.g., 2021-22)
- Department: Short code (e.g., CSE, EEE, BBA)

---

## 12. Teacher ID Card Format (Comilla University)

```
┌─────────────────────────────────────┐
│  Comilla University                 │
│  Identity Card                      │
│                                     │
│  [Photo]                            │
│                                     │
│  Name: [Full Name]                  │
│  Designation: [Designation]         │
│  Department: [Department Name]      │
│  Blood Group: [Blood Group]         │
│  Contact No: [Phone Number]         │
│  Employee ID: [Numeric ID]          │
│                                     │
│  [Signature]                        │
│  Registrar                          │
└─────────────────────────────────────┘
```

---

## 13. Deployment

### Build & Deploy Backend
```bash
# SSH into VPS
ssh root@kubijatra

# Navigate to project
cd /opt/cou-bus-tracker/Backend

# Pull latest code
git pull origin main

# Rebuild and restart containers
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d --build

# Check logs
docker logs cou-bus-tracker-app --tail 50 -f
```

### Run Flyway Migration
```bash
# If using Flyway CLI
flyway -url=jdbc:postgresql://localhost:5432/cou_bus_tracker \
       -user=cou_bus_tracker_user -password=YOUR_PASSWORD \
       migrate

# OR just restart the app (Flyway runs on startup)
docker restart cou-bus-tracker-app
```

### Backup Database
```bash
docker exec cou-bus-tracker-postgres pg_dump -U cou_bus_tracker_user cou_bus_tracker > /opt/cou-bus-tracker/Backend/backups/backup-$(date +%Y%m%d%H%M).sql
```

---

## 14. Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL URL | `jdbc:postgresql://host.docker.internal:5432/cou_bus_tracker` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `cou_bus_tracker_user` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | (secret) |
| `JWT_SECRET` | JWT signing key | (secret) |
| `JWT_EXPIRATION` | Token expiry (ms) | `86400000` |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary cloud | (secret) |
| `CLOUDINARY_API_KEY` | Cloudinary key | (secret) |
| `CLOUDINARY_API_SECRET` | Cloudinary secret | (secret) |
| `SMS_API_KEY` | BulkSMSBD API key | (secret) |
| `SMS_SENDER_ID` | BulkSMSBD sender | (secret) |

---

## 15. Known Issues & Notes

1. **Google Auth is deprecated** - `GoogleAuthController` returns HTTP 410 Gone
2. **Email fields removed** - V18 migration removed email from students/teachers
3. **Phone is the primary identifier** - All auth uses phone + password
4. **ID card validation is basic** - Only checks file type and size, no content validation
5. **No forgot password endpoint exists** - Needs to be implemented
6. **Profile page doesn't show ID card** - Needs to be implemented
7. **Admin panel shows student_id and varsity_batch** - Will need update after column rename

---

*This document should be updated whenever significant changes are made to the VPS or codebase.*
