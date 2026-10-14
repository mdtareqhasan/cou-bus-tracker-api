# CoU Bus Tracker — Admin Panel

> Web-based administration panel for the **CoU (Comilla University) Bus Tracker** system.
> Admins manage buses, schedules, notices, students, teachers and other admin accounts from one place.

## Overview

The Admin Panel is a single-page application (SPA) built with **React** and **Vite**. It talks to the CoU Bus Tracker backend (Spring Boot) over a REST API. The UI is fully responsive, uses **Tailwind CSS v4**, and mixes English + Bengali content — schedules, time stamps and the PDF report are displayed in Bengali with proper Bangla numerals and the **Noto Sans Bengali** font.

## Tech Stack

| Layer      | Technology                                             |
| ---------- | ------------------------------------------------------ |
| UI         | React 19, React Router 7, Lucide icons                 |
| Build      | Vite 8, @vitejs/plugin-react, rolldown                 |
| Styling    | Tailwind CSS v4 (@tailwindcss/vite)                    |
| HTTP       | Axios (interceptors for auth + 401/403 redirect)       |
| PDF report | jsPDF, jsPDF-autotable, html2canvas                    |
| Linting    | Oxlint (`.oxlintrc.json`)                              |

## Features

- **Login / Auth** — JWT-based. Token + admin name are stored in `localStorage`; axios automatically attaches `Authorization: Bearer <token>` and redirects to `/login` on `401/403`.
- **Dashboard** — live stats cards (`/admin/dashboard`): total/active buses, students, teachers, schedules, notices, verified count, plus pending-verification alerts.
- **Buses** — full CRUD (bus number, name, category, driver name/phone, image), plus a dedicated **tracker live-link** dialog with optional expiry.
- **Schedules** — Bengali bus schedule manager: create/edit/delete, active/inactive toggle, filters (student vs teacher bus, weekday/weekend, status) and search over predefined campus routes.
- **PDF Export** — export the current filtered view or all schedules to a **multi-page landscape A4 PDF report** (jsPDF + html2canvas). The report is grouped by weekday/weekend and student/teacher buses, sorted by departure time, with Bengali labels and page numbers.
- **Notices** — post/delete notices with an optional "expires after (hours)" option.
- **Students & Teachers** — searchable lists with filters (all / pending verification / phone verified / phone unverified), verify accounts, toggle active state, view ID-card image in a modal (Cloudinary thumbnail optimization), permanent delete.
- **Admin Users** — create, edit (name / password) and delete admin accounts (email is immutable while editing).
- **Admin Profile** — view and update your own name/password; reflects the change in the sidebar/header.

## Routes

| Path        | Page                   |
| ----------- | ---------------------- |
| `/login`    | Login                  |
| `/`         | Dashboard              |
| `/buses`    | Bus management         |
| `/schedules`| Bus schedules + PDF    |
| `/notices`  | Notice management      |
| `/students` | Student management     |
| `/teachers` | Teacher management     |
| `/admins`   | Admin user management  |
| `/profile`  | Admin profile          |

## Project Structure

```
admin-panel/
├── public/
│   ├── favicon.svg
│   ├── icons.svg
│   └── fonts/NotoSansBengali-{Bold,Regular}.ttf
├── src/
│   ├── api.js                  # Axios instance + all API modules
│   ├── App.jsx                 # Router + route guards
│   ├── main.jsx
│   ├── index.css / App.css
│   ├── context/
│   │   └── AuthContext.jsx     # Auth state, login/logout, token expiry
│   ├── components/
│   │   ├── ExportPdfButton.jsx # "current view" / "all schedules" export
│   │   └── IdCardThumb.jsx     # Circular ID-card thumbnail (Cloudinary)
│   ├── pages/
│   │   ├── LoginPage.jsx
│   │   ├── Layout.jsx          # Sidebar + header shell
│   │   ├── DashboardPage.jsx
│   │   ├── BusesPage.jsx
│   │   ├── SchedulesPage.jsx
│   │   ├── NoticesPage.jsx
│   │   ├── StudentsPage.jsx
│   │   ├── TeachersPage.jsx
│   │   ├── AdminUsersPage.jsx
│   │   └── AdminProfilePage.jsx
│   └── utils/
│       ├── format.js           # Bengali time/date/route/number helpers
│       └── pdfExporter.js      # PDF report generator
├── .env.local-backend          # VITE_API_PROXY_TARGET=http://localhost:8080
├── .env.docker-backend         # VITE_API_PROXY_TARGET=http://localhost:8081
├── vite.config.js              # dev server + /api & /uploads proxy
└── package.json
```

## Getting Started

### Prerequisites

- Node.js 20+ and npm
- The CoU Bus Tracker backend running (Spring Boot on port `8080`, or the Docker variant on port `8081`)

### Install

```bash
cd admin/admin-panel
npm install
```

### Run (development)

Two modes are defined via distinct `.env` files that set the backend proxy target:

```bash
# Backend running directly on host (port 8080)
npm run dev:local

# Backend running in Docker (port 8081) — also the default
npm run dev        # alias of dev:docker
npm run dev:docker
```

Vite's dev server listens on `http://localhost:5173` and proxies `/api` and `/uploads` to the configured backend target, so no CORS issues occur during development.

### Build & preview

```bash
npm run build     # production build -> dist/
npm run preview   # serve the production build
```

### Lint

```bash
npm run lint       # oxlint
```

## Configuration

The API base URL defaults to `/api` and is proxied in dev. For production, set a full URL:

```bash
# .env.production (example)
VITE_API_BASE_URL=https://backend.example.com/api
```

| Variable               | Description                                   | Default          |
| ---------------------- | --------------------------------------------- | ---------------- |
| `VITE_API_BASE_URL`    | Backend REST API base URL                     | `/api`           |
| `VITE_API_PROXY_TARGET`| Dev-server proxy target (backend origin)      | `http://localhost:8081` |

## Backend API Used

All endpoints are consumed via the shared modules in `src/api.js`:

- **Auth**: `POST /auth/admin/login`
- **Admin**: `GET/POST/PUT/DELETE /admin/manage`, `GET/PUT /admin/profile`, `GET /admin/dashboard`
- **Buses**: `GET/POST/PUT/DELETE /admin/buses`, `PATCH /admin/buses/:id/toggle`, `PUT /admin/buses/:id/tracker-link`
- **Schedules**: `GET/POST/PUT/DELETE /admin/schedules`, `PATCH /admin/schedules/:id/toggle`
- **Notices**: `GET/POST/DELETE /admin/notices`
- **Students**: `GET /admin/students`, `GET /admin/students/pending`, `PUT .../verify`, `PUT .../toggle-active`, `DELETE`
- **Teachers**: same pattern under `/admin/teachers`

> All secured endpoints expect a `Bearer` token obtained from the login call.

## Notes

- The panel is **bilingual**: management screens use English headers while schedule flows, time formatting and the PDF report use Bengali (with `bn-BD` numerals).
- Deleting a student/teacher also removes their account and ID-card image permanently (with a confirmation dialog).
- `dist/` contains the last production build and `node_modules/` the installed dependencies; neither should be edited manually.