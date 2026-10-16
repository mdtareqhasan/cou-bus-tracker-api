package com.cou.bustracker.exception;

import org.springframework.http.HttpStatus;

/**
 * Stable, machine-readable error codes returned to clients.
 *
 * <p>The API deliberately sends a single language (English) in the human-readable
 * {@code message} field. Localization is the client's job: the Flutter app maps
 * each {@code code} below to a localized string and falls back to the English
 * message when a code is unknown. This keeps the wire contract stable even if
 * English wording ever changes.
 *
 * <p>Each constant also carries the HTTP status it should produce and a default
 * English message, so call sites stay terse:
 * {@code throw new ApiException(ErrorCode.OTP_EXPIRED);}.
 */
public enum ErrorCode {

    // ---- Generic / transport -------------------------------------------------
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Validation failed"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Forbidden"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not found"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "The requested resource was not found"),

    // ---- Field presence / format --------------------------------------------
    ROLE_REQUIRED(HttpStatus.BAD_REQUEST, "Select a role"),
    NAME_REQUIRED(HttpStatus.BAD_REQUEST, "Name is required"),
    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "Email is required"),
    PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "Password is required"),
    PHONE_REQUIRED(HttpStatus.BAD_REQUEST, "Phone number is required"),
    OTP_REQUIRED(HttpStatus.BAD_REQUEST, "OTP is required"),
    NEW_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "A new password is required"),
    DEPARTMENT_REQUIRED(HttpStatus.BAD_REQUEST, "Department is required"),
    ROLL_NUMBER_REQUIRED(HttpStatus.BAD_REQUEST, "Roll number is required"),
    SESSION_REQUIRED(HttpStatus.BAD_REQUEST, "Session is required"),
    EMPLOYEE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "Employee ID is required"),
    DESIGNATION_REQUIRED(HttpStatus.BAD_REQUEST, "Designation is required"),
    TEACHER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "Teacher ID is required"),
    EMAIL_INVALID(HttpStatus.BAD_REQUEST, "Enter a valid email address"),
    PHONE_INVALID(HttpStatus.BAD_REQUEST, "Enter a valid phone number"),
    OTP_INVALID_LENGTH(HttpStatus.BAD_REQUEST, "OTP must be 6 digits"),
    PASSWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters"),
    EMAIL_DOMAIN_NOT_ALLOWED(HttpStatus.BAD_REQUEST,
            "Only @stud.cou.ac.bd or @cou.ac.bd email addresses are allowed"),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "Role must be STUDENT, TEACHER, or EMPLOYEE"),

    // ---- Write-once academic fields -----------------------------------------
    DEPARTMENT_IMMUTABLE(HttpStatus.BAD_REQUEST,
            "Department cannot be changed once it has been set"),
    ROLL_NUMBER_IMMUTABLE(HttpStatus.BAD_REQUEST,
            "Roll number cannot be changed once it has been set"),
    SESSION_IMMUTABLE(HttpStatus.BAD_REQUEST,
            "Session cannot be changed once it has been set"),
    EMPLOYEE_ID_IMMUTABLE(HttpStatus.BAD_REQUEST,
            "Employee ID cannot be changed once it has been set"),
    DESIGNATION_IMMUTABLE(HttpStatus.BAD_REQUEST,
            "Designation cannot be changed once it has been set"),

    // ---- Uniqueness conflicts ------------------------------------------------
    PHONE_ALREADY_USED(HttpStatus.CONFLICT, "This phone number is already in use"),
    ROLL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "This roll number is already registered"),
    TEACHER_ID_ALREADY_REGISTERED(HttpStatus.CONFLICT, "This teacher ID is already registered"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "An account with this email already exists"),
    STUDENT_EMAIL_EXISTS(HttpStatus.CONFLICT, "A student with this email already exists"),
    EMPLOYEE_EMAIL_EXISTS(HttpStatus.CONFLICT, "An employee with this email already exists"),

    // ---- Account lookup / state ---------------------------------------------
    STUDENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Student not found"),
    TEACHER_NOT_FOUND(HttpStatus.NOT_FOUND, "Teacher not found"),
    NO_STUDENT_WITH_PHONE(HttpStatus.NOT_FOUND, "No student is registered with this phone number"),
    NO_TEACHER_WITH_PHONE(HttpStatus.NOT_FOUND, "No teacher is registered with this phone number"),
    NO_STUDENT_WITH_EMAIL(HttpStatus.NOT_FOUND, "No student is registered with this email address"),
    NO_TEACHER_WITH_EMAIL(HttpStatus.NOT_FOUND,
            "No teacher or staff member is registered with this email address"),
    ACCOUNT_DEACTIVATED(HttpStatus.UNAUTHORIZED,
            "Your account is inactive. Please contact an administrator."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    CANNOT_DELETE_SELF(HttpStatus.BAD_REQUEST, "You cannot delete your own account"),

    // ---- OTP lifecycle -------------------------------------------------------
    OTP_NOT_FOUND(HttpStatus.BAD_REQUEST, "No OTP found. Please request a new one"),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "The OTP has expired. Please request a new one"),
    OTP_TOO_MANY_ATTEMPTS(HttpStatus.BAD_REQUEST,
            "Too many wrong attempts. Please request a new one"),
    OTP_INVALID(HttpStatus.UNAUTHORIZED, "Wrong OTP. Please try again"),
    OTP_RESEND_COOLDOWN(HttpStatus.BAD_REQUEST,
            "Please wait a moment before requesting another OTP"),

    // ---- Registration flow ---------------------------------------------------
    REGISTRATION_NOT_FOUND(HttpStatus.BAD_REQUEST,
            "Registration not found. Please submit the registration form first"),
    PENDING_REGISTRATION_NOT_FOUND(HttpStatus.BAD_REQUEST,
            "No pending registration found for this phone. Please submit the registration form first"),

    // ---- Google Sign-In ------------------------------------------------------
    GOOGLE_TOKEN_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED,
            "Could not verify the Google sign-in token. Please try again"),
    GOOGLE_TOKEN_INVALID(HttpStatus.UNAUTHORIZED,
            "The Google sign-in token is invalid or the email is not verified"),
    GOOGLE_EMAIL_NOT_ALLOWED(HttpStatus.FORBIDDEN,
            "Only official Comilla University emails (name@cou.ac.bd or name@stud.cou.ac.bd) can sign in"),
    FACULTY_EMAIL_REQUIRED(HttpStatus.UNAUTHORIZED,
            "To sign in as a teacher, use a staff email (name@cou.ac.bd). To sign in as a student, select role STUDENT"),

    // ---- Profile completion --------------------------------------------------
    PROFILE_INCOMPLETE(HttpStatus.FORBIDDEN, "Complete your profile to use the app");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    /** The stable string serialized to clients in the {@code code} field. */
    public String getCode() {
        return name();
    }
}
