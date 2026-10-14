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

For regular registration require a password. For Google registration send `googleIdToken` and omit/leave blank `password`.

## Email OTP verification (regular password registration only)

After a successful password registration, the backend sends a six-digit OTP to the submitted email. The registration response contains `isEmailVerified: false` and **does not contain an access token**. Immediately navigate to an OTP verification screen; do not save a token or treat the user as logged in yet. Google registration is already email-verified and returns a normal token.

Create a six-digit numeric OTP screen with an email summary, a Verify button, a 60-second resend countdown, loading/error states, and a Change email / Back action. Use these JSON APIs:

`POST /api/auth/email-verification/verify`

```json
{ "email": "user@example.com", "role": "STUDENT", "otp": "123456" }
```

`POST /api/auth/email-verification/resend`

```json
{ "email": "user@example.com", "role": "STUDENT" }
```

Use `TEACHER` for teachers. OTP expires after five minutes, only five wrong attempts are allowed, and resend is rate-limited to once per minute. A successful verify response has the normal auth shape below: save its `accessToken`, role, and display name in `flutter_secure_storage`, then navigate to the authenticated area. For a 400/401 error, show the backend `message` without clearing the registration form. If login returns “Please verify your email before logging in”, route the user to this OTP screen and offer resend.

Normal authenticated success response:

```json
{ "accessToken": "...", "tokenType": "Bearer", "name": "Name", "role": "STUDENT", "isEmailVerified": true }
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
