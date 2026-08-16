# Flutter implementation prompt: Registration, JWT and Google Sign-In

Implement the authentication and registration UI for the CoU Bus Tracker Flutter app against the Spring Boot API at `<BASE_URL>`. Use `dio`, `flutter_secure_storage`, `image_picker`, and `google_sign_in` (current compatible versions). Do not put secrets in the app.

## Roles and registration

Offer a role selector: **Student** or **Teacher**. Before opening the gallery/camera picker, show this Bengali warning exactly: **"শুধু আপনার বিশ্ববিদ্যালয়ের বৈধ ID card-এর পরিষ্কার ছবি আপলোড করুন। অন্য কোনো ছবি দিলে registration বাতিল হতে পারে।"** Confirm before continuing. Only allow JPG/PNG, show a preview, and make the image compulsory.

Submit registration as `multipart/form-data`; do not send JSON.

Student: `POST /api/auth/student/register`

- fields: `name`, `email`, `password` (or `googleIdToken`), `studentId`, `department`, `varsityBatch`
- file field: `idCard`

Teacher: `POST /api/auth/teacher/register`

- fields: `name`, `email`, `password` (or `googleIdToken`), `teacherId`, `department`, optional `designation`, optional `phone`
- file field: `idCard`

For regular registration require a password. For Google registration send `googleIdToken` and omit/leave blank `password`. On success the app receives:

```json
{ "accessToken": "...", "tokenType": "Bearer", "adminName": "Name", "role": "STUDENT" }
```

Save `accessToken` and `role` only in `flutter_secure_storage`. Add `Authorization: Bearer <accessToken>` to all protected API calls. Never use the token as a URL parameter.

## Login and Google Sign-In

Password login uses `POST /api/auth/student/login` or `POST /api/auth/teacher/login` with JSON `{ "email": "...", "password": "..." }`.

For Google Sign-In, obtain the Google **ID token** (not an access token), then call:

`POST /api/auth/google/login`

```json
{ "idToken": "<google id token>", "role": "STUDENT" }
```

or role `TEACHER`. The backend verifies the token and issues the app JWT. If it returns 401 because no profile exists, take the user to the appropriate registration screen; Google login never bypasses the mandatory ID-card registration.

Use this supplied Android OAuth client ID for Android Google Sign-In:

`111634412431-t3hjk2g1fsfoguagsmaqmdc7ouflaivt.apps.googleusercontent.com`

For a production backend ID-token audience, create a **Web application** OAuth client in the same Google Cloud project and configure its client ID as `serverClientId` in Flutter and `GOOGLE_OAUTH_CLIENT_ID` on the backend. Keep Android SHA-1/SHA-256 fingerprints configured for debug and release. Do not ship a client secret in Flutter.

## Profiles and admin data

Use `GET /api/auth/student/me` and `GET /api/auth/teacher/me` for the logged-in profile. Allow a logged-in user to replace their card with `POST /api/auth/student/upload-id-card` or `/api/auth/teacher/upload-id-card`, multipart file field `file`.

Admin endpoints use an admin JWT only: `GET /api/admin/students` and `GET /api/admin/teachers`. Display teacher `teacherId`, `department`, and `idCardImageUrl`; display student `studentId`, `department`, `varsityBatch`, and `idCardImageUrl`. Render image URLs relative to BASE_URL when they begin with `/uploads/`.

Create a reusable Dio interceptor for token attachment, 401 handling (clear secure storage and route to login), and a friendly 400 error message. Do not log passwords, Google ID tokens, or JWTs.
