# Flutter Implementation Prompt: Phone-Based Registration & OTP Login (BulkSMSBD)

## Overview

Replace email-based registration/login with **phone number + OTP** authentication in the CoU Bus Tracker Flutter app. Use **BulkSMSBD** SMS API for OTP delivery. No Gmail/email authentication — only phone number.

**Backend Base URL**: `<API_BASE_URL>` (inject via `--dart-define=API_BASE_URL=...`)

---

## 1. Registration Flow

### Role Selection
Offer a role selector: **Student** or **Teacher**. After role selection, show a registration form with:

| Field | Student | Teacher |
|-------|---------|---------|
| Name | Required | Required |
| Phone Number | Required | Required |
| Student/Teacher ID | Required | Required |
| Department | Required | Required |
| Varsity Batch | Required | — |
| Designation | — | Optional |
| ID Card Image | Required | Required |

**Phone Number Format**: Bangladeshi numbers only. Accept formats like `01XXXXXXXXX`, `8801XXXXXXXXX`, `+8801XXXXXXXXX`. Normalize to `8801XXXXXXXXX` before sending to backend.

### Registration Submission

Submit as `multipart/form-data` (NOT JSON).

**Student**: `POST /api/auth/student/register`
- Fields: `name`, `email` (use phone number as email: `phone@cou.bus`), `password` (generate random), `phone`, `studentId`, `department`, `varsityBatch`
- File field: `idCard`

**Teacher**: `POST /api/auth/teacher/register`
- Fields: `name`, `email` (use phone number as email: `phone@cou.bus`), `password` (generate random), `phone`, `teacherId`, `department`, `designation` (optional)
- File field: `idCard`

**IMPORTANT**: Since backend requires email field, use phone number as email placeholder: `{phone}@cou.bus` (e.g., `8801712345678@cou.bus`). This is a temporary solution until backend is fully migrated.

### Registration Response

```json
{
  "accessToken": null,
  "tokenType": null,
  "role": "STUDENT",
  "id": 1,
  "name": "Student Name",
  "email": "8801712345678@cou.bus",
  "phone": "8801712345678",
  "isVerified": false,
  "isEmailVerified": false,
  "isPhoneVerified": false,
  "isEduMail": false
}
```

After registration, **automatically send OTP** to the phone number. Navigate to OTP verification screen.

---

## 2. Phone OTP Verification

### Send OTP

After registration (or on resend), call:

`POST /api/auth/phone-verification/send`

```json
{
  "phone": "8801712345678",
  "role": "STUDENT"
}
```

Response:
```json
{
  "message": "OTP sent successfully to 8801712345678"
}
```

### Verify OTP

`POST /api/auth/phone-verification/verify`

```json
{
  "phone": "8801712345678",
  "role": "STUDENT",
  "otp": "123456"
}
```

Success Response (returns JWT):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "role": "STUDENT",
  "id": 1,
  "name": "Student Name",
  "phone": "8801712345678",
  "isVerified": true,
  "isPhoneVerified": true
}
```

### Resend OTP

`POST /api/auth/phone-verification/resend`

```json
{
  "phone": "8801712345678",
  "role": "STUDENT"
}
```

### OTP Rules
- **6-digit numeric** OTP
- **Expires in 2 minutes**
- **Max 5 wrong attempts** (OTP deleted after that)
- **Resend cooldown**: 60 seconds

### OTP Screen UI Requirements

1. **Phone number display**: Show masked phone number (e.g., `8801****5678`)
2. **6-digit OTP input**: Use 6 individual text fields or a single field with input formatters
3. **Auto-submit**: When all 6 digits are entered, auto-submit for verification
4. **Countdown timer**: Show `MM:SS` countdown (2 minutes)
5. **Resend button**: Disabled during countdown, enabled after expiry
6. **Verify button**: Show loading spinner during API call
7. **Error display**: Show backend error messages (e.g., "Invalid OTP", "OTP expired")
8. **Change phone option**: Allow user to go back and change phone number
9. **Keyboard**: Use numeric keyboard only

---

## 3. Phone + Password Login (NO OTP)

### Login Flow

Simple phone + password login. No OTP needed for login.

`POST /api/auth/student/login-phone` (Student)
`POST /api/auth/teacher/login-phone` (Teacher)

```json
{
  "phone": "8801712345678",
  "password": "mypassword123"
}
```

Success Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "role": "STUDENT",
  "id": 1,
  "name": "Student Name",
  "phone": "8801712345678",
  "isVerified": true,
  "isPhoneVerified": true
}
```

### Login Error Handling

| Error | Action |
|-------|--------|
| "Student not found with this phone number" | Show: "এই ফোন নম্বরে কোনো ব্যবহারকারী নেই। প্রথমে রেজিস্ট্রেশন করুন।" |
| "Invalid phone number or password" | Show: "ভুল ফোন নম্বর বা পাসওয়ার্ড।" |
| "Please verify your phone number before logging in" | Auto-navigate to registration/OTP verification |
| "Account is deactivated" | Show: "আপনার অ্যাকাউন্ট নিষ্ক্রিয়। অ্যাডমিনের সাথে যোগাযোগ করুন।" |

---

## 4. Token Storage & Auth State

### Save to `flutter_secure_storage`

```dart
await secureStorage.write(key: 'accessToken', value: response.accessToken);
await secureStorage.write(key: 'role', value: response.role);
await secureStorage.write(key: 'userId', value: response.id.toString());
await secureStorage.write(key: 'userName', value: response.name);
await secureStorage.write(key: 'userPhone', value: response.phone);
```

### Dio Interceptor

```dart
class AuthInterceptor extends Interceptor {
  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    final token = await secureStorage.read(key: 'accessToken');
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    if (err.response?.statusCode == 401) {
      // Clear storage and route to login
      secureStorage.deleteAll();
      // Navigate to login screen
    }
    handler.next(err);
  }
}
```

---

## 5. Screen Structure

### Auth Screens

```
lib/
  features/
    auth/
      screens/
        role_selection_screen.dart      # Student/Teacher choice
        phone_registration_screen.dart  # Registration form with phone
        phone_otp_verification_screen.dart  # OTP input screen
        phone_login_screen.dart         # Phone + OTP login
      providers/
        auth_provider.dart              # Auth state management
      services/
        auth_service.dart               # API calls
      models/
        auth_response.dart              # Auth response model
```

### Navigation Flow

```
Splash Screen
    ↓
Role Selection (Student/Teacher)
    ↓
Phone Registration Form (enter phone + details + set password)
    ↓ (submit)
Phone OTP Verification Screen (verify phone ownership)
    ↓ (verify success)
Home Screen (Authenticated)
```

Login Flow:
```
Phone Login Screen (enter phone + password)
    ↓ (login success)
Home Screen (Authenticated)
```

---

## 6. Bengali UI Strings

### Registration Screen
- Title: "রেজিস্ট্রেশন"
- Phone Label: "ফোন নম্বর"
- Phone Hint: "01XXXXXXXXX"
- Student ID Label: "ছাত্র/ছাত্রী ID"
- Department Label: "বিভাগ"
- Batch Label: "ব্যাচ"
- Submit Button: "রেজিস্ট্রেশন করুন"
- ID Card Warning: "শুধু আপনার বিশ্ববিদ্যালয়ের বৈধ ID card-এর পরিষ্কার ছবি আপলোড করুন। অন্য কোনো ছবি দিলে registration বাতিল হতে পারে।"

### OTP Verification Screen
- Title: "ফোন যাচাইকরণ"
- Subtitle: "আপনার ফোন নম্বরে ৬ ডিজিটের OTP পাঠানো হয়েছে"
- OTP Label: "OTP কোড"
- Verify Button: "যাচাই করুন"
- Resend Button: "OTP পুনরায় পাঠান"
- Change Phone: "ফোন নম্বর পরিবর্তন করুন"
- Timer: "সময় বাকি: MM:SS"

### Login Screen
- Title: "লগইন"
- Phone Label: "ফোন নম্বর"
- Password Label: "পাসওয়ার্ড"
- Login Button: "লগইন করুন"
- No OTP for login - just phone + password

### Error Messages
- "ভুল OTP। আবার চেষ্টা করুন।"
- "OTP মেয়াদোত্তীর্ণ হয়েছে। নতুন OTP পাঠানো হয়েছে।"
- "অনুগ্রহ করে অপেক্ষা করুন।"
- "এই ফোন নম্বরে কোনো ব্যবহারকারী নেই।"
- "আপনার অ্যাকাউন্ট নিষ্ক্রিয়। অ্যাডমিনের সাথে যোগাযোগ করুন।"

---

## 7. Backend API Summary

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/auth/student/register` | POST | Student registration (multipart) |
| `/api/auth/teacher/register` | POST | Teacher registration (multipart) |
| `/api/auth/student/login-phone` | POST | Student login with phone+password |
| `/api/auth/teacher/login-phone` | POST | Teacher login with phone+password |
| `/api/auth/phone-verification/send` | POST | Send OTP (for registration only) |
| `/api/auth/phone-verification/verify` | POST | Verify OTP (for registration only) |
| `/api/auth/phone-verification/resend` | POST | Resend OTP (for registration only) |
| `/api/auth/student/me` | GET | Student profile (JWT required) |
| `/api/auth/teacher/me` | GET | Teacher profile (JWT required) |
| `/api/auth/student/upload-id-card` | POST | Upload ID card image |
| `/api/auth/teacher/upload-id-card` | POST | Upload ID card image |

---

## 8. Environment Configuration

```dart
// lib/core/constants/api_constants.dart
class ApiConstants {
  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080/api',
  );
  
  // Auth endpoints
  static const String studentRegister = '/auth/student/register';
  static const String teacherRegister = '/auth/teacher/register';
  static const String studentLoginPhone = '/auth/student/login-phone';
  static const String teacherLoginPhone = '/auth/teacher/login-phone';
  static const String sendPhoneOtp = '/auth/phone-verification/send';
  static const String verifyPhoneOtp = '/auth/phone-verification/verify';
  static const String resendPhoneOtp = '/auth/phone-verification/resend';
  static const String studentMe = '/auth/student/me';
  static const String teacherMe = '/auth/teacher/me';
}
```

---

## 9. Dependencies (pubspec.yaml)

```yaml
dependencies:
  flutter_riverpod: ^2.5.0
  dio: ^5.4.0
  go_router: ^14.0.0
  flutter_secure_storage: ^9.0.0
  shared_preferences: ^2.2.0
  image_picker: ^1.0.0
  cached_network_image: ^3.3.0
  url_launcher: ^6.2.0
  connectivity_plus: ^6.0.0
  freezed_annotation: ^2.4.0
  json_annotation: ^4.8.0

dev_dependencies:
  build_runner: ^2.4.0
  freezed: ^2.5.0
  json_serializable: ^6.7.0
```

---

## 10. Important Notes

1. **No Email/Gmail**: Remove all email-based auth. Phone number is the primary identifier.
2. **OTP only for registration**: Phone OTP is only used during registration to verify phone ownership. Login uses phone + password.
3. **Password required**: User must set a password during registration. This password is used for login.
4. **OTP via BulkSMSBD**: Backend sends OTP using BulkSMSBD API. Flutter only needs to call the endpoints.
5. **Phone Normalization**: Always normalize phone numbers to `8801XXXXXXXXX` format before sending to backend.
6. **ID Card Upload**: Still required for registration. Use Bengali warning message before camera.
7. **Image Validation**: Only JPG/PNG allowed. Show preview before upload.
8. **Error Handling**: Always show Bengali error messages from backend. Parse `message` field from error response.
9. **Token Storage**: Only store `accessToken`, `role`, `userId`, `userName`, `userPhone` in `flutter_secure_storage`.
10. **Auto-submit OTP**: When user enters 6 digits, auto-submit for verification.
11. **Countdown Timer**: Show 2-minute countdown. After expiry, allow resend.
12. **Change Phone**: Allow user to go back and change phone number from OTP screen.
