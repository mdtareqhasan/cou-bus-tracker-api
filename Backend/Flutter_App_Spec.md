# CoU Bus Tracker — Flutter Application Specification

## 1. Purpose and scope

Build a Bengali-first Flutter Android application for Comilla University bus users. The app consumes the existing Spring Boot API; it **does not** use the React admin APIs except where an administrator later builds a separate Flutter admin app.

The public application must let users:

- view active buses, routes, schedules, tracker links, and active notices;
- register and sign in as a student or teacher;
- view a student profile and upload an ID-card image after the backend authentication issue in section 13 is fixed;
- use the app comfortably in Bengali or English, with light/dark appearance.

The first release should be useful without login: Home, buses, schedules, notices, and bus details are public.

## 2. Platform and technical baseline

| Area | Decision |
|---|---|
| SDK | Flutter stable, Android first; keep widgets responsive for iOS later |
| State management | Riverpod (`flutter_riverpod`) |
| HTTP | Dio with interceptors |
| Routing | `go_router` with shell navigation |
| Models | Immutable Dart models with `freezed` + `json_serializable`, or equivalent manual `fromJson` |
| Secure data | `flutter_secure_storage` for JWT only; `shared_preferences` for theme/language/non-sensitive cache |
| Cache | Hive or Isar for the last successful public API responses |
| Upload / image | `image_picker`, `dio` multipart, `cached_network_image` |
| External tracking link | `url_launcher` (open tracker in browser or embedded web view only if product approves) |
| Connectivity | `connectivity_plus`; still handle real request failures because connectivity status is not proof of internet access |

Recommended package structure:

```text
lib/
  app/                 # App, router, theme, localization
  core/                # Dio client, result/error types, storage, constants
  features/
    home/              # Home dashboard and combined providers
    buses/             # List, filter, details, tracker launcher
    schedules/
    notices/
    auth/              # role choice, login, registration
    profile/           # student profile and ID upload
  shared/widgets/      # dashboard shell, cards, empty/error/loading views
```

## 3. Environment configuration

Never hard-code a local host address in a production screen. Inject the base URL with `--dart-define`.

| Target | `API_BASE_URL` |
|---|---|
| Android emulator calling local backend | `http://10.0.2.2:8080/api` |
| Physical device on the same Wi-Fi | `http://<developer-LAN-IP>:8080/api` |
| Production | `https://<production-domain>/api` |

Example run command:

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api
```

`localhost` from an Android emulator/device points to the device itself, not the developer computer. For Android 9+ HTTP development servers may require `android:usesCleartextTraffic="true"`; production must use HTTPS.

## 4. API client rules

- Base URL includes `/api`, for example `http://10.0.2.2:8080/api`.
- Send `Accept: application/json` on every call and `Content-Type: application/json` for JSON bodies.
- For logged-in calls, attach `Authorization: Bearer <accessToken>`.
- For image upload, use `multipart/form-data`; do not set a JSON content type.
- Use a 15-second connect/receive timeout and display a retry action.
- Treat 401 and 403 as a session failure: erase the token, clear protected in-memory state, and route to sign-in with the message `সেশন শেষ হয়েছে। আবার সাইন ইন করুন।`.
- JWTs currently expire after 24 hours. Decode the `exp` claim locally before a protected request where practical, but always rely on the server response as final authority.
- Parse date-time strings as ISO-8601. The server returns local date-time values without an explicit offset; display them in the device locale without converting their zone unless the API later sends an offset.

Use a single `ApiClient` and repositories. Widgets must not call Dio directly.

## 5. API contract

### Public endpoints — required for release 1

| Method | Path relative to base URL | Parameters / body | Response |
|---|---|---|---|
| GET | `/buses` | optional query `category` | `List<Bus>`; active buses only |
| GET | `/buses/{id}` | path `id` | `BusDetail`, including `schedules` |
| GET | `/schedules` | — | `List<Schedule>`; active schedules |
| GET | `/schedules/bus/{busId}` | path `busId` | `List<Schedule>` |
| GET | `/notices/active` | — | `List<Notice>`; only active, non-expired notices |

### Authentication and profile endpoints

| Method | Path | JSON body / form | Response |
|---|---|---|---|
| POST | `/auth/student/register` | `name`, `email`, `password`, `studentId`, `department`, `varsityBatch` | `AuthResponse` |
| POST | `/auth/student/login` | `email`, `password` | `AuthResponse` |
| POST | `/auth/teacher/register` | `name`, `email`, `password`, optional `designation`, `department`, `phone` | `AuthResponse` |
| POST | `/auth/teacher/login` | `email`, `password` | `AuthResponse` |
| GET | `/auth/student/me` | Bearer token | `Student` |
| POST | `/auth/student/upload-id-card` | Bearer token, multipart field named `file` | `FileUploadResponse` |

`AuthResponse` uses the misleading server key `adminName` for *all* roles. Map it in Flutter to a neutral `displayName`:

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "adminName": "User Name"
}
```

### Admin endpoints — out of scope for the user app

These exist for the web admin panel and must not be exposed in a normal user application: `/admin/dashboard`, `/admin/buses`, `/admin/schedules`, `/admin/notices`, `/admin/students`, and `/admin/teachers`.

## 6. Dart data models

All nullable fields must stay nullable in Dart—do not invent values such as an empty tracker URL.

```text
Bus
  id: int
  busNumber, busName, category, route: String?
  driverName, driverPhone, busImageUrl, trackerUrl: String?
  isActive: bool?

BusDetail extends Bus
  schedules: List<Schedule>

Schedule
  id, busId: int
  busNumber, busName, category: String?
  departureTime, arrivalTime, direction, startPoint, endPoint, days: String?

Notice
  id: int
  title, body: String
  isActive: bool?
  createdAt, expiresAt: DateTime?

Student
  id: int
  name, email, studentId, department, varsityBatch: String?
  idCardImageUrl: String?
  isEduMail, isVerified, isActive: bool?
  createdAt: DateTime?
```

For a relative image/file value such as `/uploads/student-id-cards/x.jpg`, derive its display URL from the API origin (`http://host:8080`), not the `/api` base. Preserve a fully qualified URL unchanged.

## 7. Navigation and screen flow

Bottom navigation has four tabs, mirroring the reference image’s simple icon-plus-label navigation:

| Tab | Route | Purpose |
|---|---|---|
| হোম | `/home` | dashboard summary, notices, quick actions |
| বাস | `/buses` | bus catalog and category filter |
| সময়সূচি | `/schedules` | searchable schedule list |
| প্রোফাইল | `/profile` | sign in/register or user profile |

Additional routes: `/bus/:id`, `/notices`, `/auth/role`, `/auth/login?role=student|teacher`, `/auth/register?role=student|teacher`, and `/profile/id-card-upload`.

The app starts at Home. Login is optional. A profile action that requires authentication must open the role selector rather than showing an unauthorised error.

## 8. Dashboard UI — visual direction from the supplied reference

The dashboard should borrow the reference’s warmth and hierarchy, not its medical data. It is a **bus information dashboard**.

### Layout

1. Use a near-white/light blue page background (`#F7FAFF`) and a scrollable `CustomScrollView`.
2. Top hero: a full-width blue gradient (`#155EEF` → `#2E79FF`), rounded bottom corners of 32–36 px, soft blue shadow.
3. Hero content: greeting (`আসসালামু আলাইকুম,`), display name or `CoU যাত্রী`, a small `আজকের বাস তথ্য` subtitle, language chip (`বাংলা`/`EN`), and theme toggle. Use a local bus icon rather than health/device icons.
4. Connection strip under the hero: pale green when fresh data is available (`● আপডেটেড • এখনই`), amber when showing cached data (`অফলাইন • সংরক্ষিত তথ্য দেখানো হচ্ছে`). It must reflect actual app state; never claim live tracking.
5. On request failure, show a pale amber error card with clear Bengali text and a `আবার চেষ্টা করুন` button—no infinite loader.
6. Main area: a two-column grid of large, white, rounded cards, as in the reference. On a narrow device use two equal cards; make text compact enough to avoid overflow.
7. Below cards: `আজকের সময়সূচি` preview (first 3–5 schedules) and `সাম্প্রতিক নোটিশ` preview. Each section has `সব দেখুন`.
8. Persistent bottom navigation with a rounded blue-tinted active pill, blue active icon/label, and dark-gray inactive icons.

### Dashboard cards

| Card | Source | Primary value | Tap action | Accent |
|---|---|---|---|---|
| চলমান বাস | `GET /buses` | active-bus count | buses tab | blue / bus icon |
| আজকের ট্রিপ | `GET /schedules` | schedule count | schedules tab | violet / calendar icon |
| লাইভ ট্র্যাকিং | buses with non-empty `trackerUrl` | available count | buses filtered to tracking-capable | teal / location icon |
| জরুরি নোটিশ | `GET /notices/active` | active notice count | notices route | orange / bell icon |

The existing backend has no real-time GPS location or a "bus currently moving" status. Therefore, label the first card `সক্রিয় বাস` unless a future API introduces live status. The tracking card launches a bus’s supplied tracker URL; it is not native GPS tracking.

### Accessibility and polish

- Minimum 48×48 dp tap targets; respect system text scale.
- Do not rely on red/green alone; add an icon and written status.
- Use Noto Sans Bengali (or another bundled Bengali-capable font) with fallback to system sans.
- Long route names use two lines and ellipsis in cards; show complete routes on the detail screen.
- Add shimmer skeletons for the initial dashboard load. Replace them with an error/empty state after failure, never a permanent spinner.

## 9. Feature-specific behaviour

### Buses

- Fetch `/buses` once, cache it, then expose category chips using server category values: `BLUE`, `RED`, `TEACHER`, `OFFICER`, `STAFF` when present.
- Provide Bengali display labels but send the original uppercase category value in API filters.
- Card: bus number, optional bus name, category badge, route, and tracker availability.
- Bus detail fetches `/buses/{id}` on entry. Show driver phone as a `tel:` action only with user confirmation. Show “লাইভ ট্র্যাকিং উপলভ্য নয়” when the link is null/blank.
- Validate any tracker URL is HTTPS/HTTP before launching; show a non-fatal error if the OS cannot open it.

### Schedules

- Use server fields directly; `departureTime`/`arrivalTime` are strings, so do not assume time-zone conversion.
- Show start → end, departure time, optional arrival time, direction, bus identity, and days.
- Offer client-side search by bus number/name and both route points. Filter by bus from bus detail using `/schedules/bus/{busId}` or the detail payload.

### Notices

- Fetch active notices on app resume and pull-to-refresh.
- Display title, body, publish date, and optional expiry. Long content expands on the notices screen.
- Empty state: `এখন কোনো সক্রিয় নোটিশ নেই`.

### Registration / profile

- Begin with a role selector: `শিক্ষার্থী` and `শিক্ষক`.
- Student registration validates name, email, password, student ID, department, and batch; teacher requires name, email, and password. Show API validation messages where safely available.
- Store `accessToken`, role, and display name only after a successful response. Never store password.
- Student profile shows verification status clearly: pending, verified, or inactive. ID upload uses image size/type validation before sending field `file`.

## 10. Refresh, cache, and state rules

- Home performs public requests in parallel: buses, schedules, notices.
- Cache successful public responses with timestamp and render cached data instantly on the next launch.
- Refresh on first app start, pull-to-refresh, and resume if cached data is older than 5 minutes. Do not continuously poll.
- If refresh fails and cache exists: retain the cache, show the amber offline strip and retry option.
- If refresh fails without cache: show a full friendly error state.
- Ensure provider states are explicit: `loading`, `data`, `empty`, `error`, `refreshing`. Every failure transitions out of `loading`.

## 11. Error behaviour and user copy

| Condition | UI action |
|---|---|
| Timeout / no connection | Show cached data if available; otherwise `ইন্টারনেট সংযোগ পরীক্ষা করুন` + retry |
| 400 validation failure | Keep form input, highlight relevant fields, show server message when available |
| 401 / 403 protected request | Clear session, send to sign in, preserve intended route if useful |
| 404 bus/schedule | `তথ্যটি পাওয়া যায়নি` and back action |
| 500+ | `সার্ভারে সমস্যা হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন।` + retry |
| Empty list | Context-specific empty artwork/text, never a blank screen |

Log full technical errors only in debug/Crashlytics; do not expose JWTs, server stack traces, or raw exception text in the UI.

## 12. Definition of done

- Public dashboard, buses, bus details, schedules, notices, Bengali/English switch, light/dark mode, pull-to-refresh, cache, and error/empty states work against a real API.
- Every response model has decoding tests using representative JSON.
- Repositories have success, 401/403, timeout, malformed JSON, and 500 tests.
- Test on Android emulator with `10.0.2.2`, then a physical device via LAN IP.
- Widget tests cover dashboard loading/error/cached states, category filters, and token-expired redirect.
- No user page invokes `/api/admin/**`.

## 13. Backend integration blockers to resolve before protected mobile features

Public read-only features are ready to integrate. However, the current backend implementation has two authentication problems that will block reliable student/teacher login and profile use:

1. `CustomUserDetailsService` only loads `Admin` records. The single Spring `AuthenticationManager` therefore cannot authenticate student or teacher credentials in the current `StudentService.login` / `TeacherService.login` flows.
2. `JwtAuthFilter` also uses that admin-only service for every Bearer token. A student token cannot establish an authenticated security context, so `/auth/student/me` and `/auth/student/upload-id-card` return 401/403.

Required backend resolution: introduce a role-aware principal/user-details service (or separate token subject/role claims plus matching authentication providers) that supports `ADMIN`, `STUDENT`, and `TEACHER`. Add role claims to issued JWTs, protect endpoints with appropriate role checks, and rename or supplement `AuthResponse.adminName` with `displayName`. After that change, update this specification’s auth mapping and add integration tests for all three roles.

Until fixed, ship the Flutter app as public-information-first: registration may return a token, but do not promise a working persisted student/teacher session or ID-card upload.
