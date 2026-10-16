package com.cou.bustracker.exception;

import org.springframework.http.HttpStatus;

/**
 * An error with a stable {@link ErrorCode} and an English, user-facing message.
 *
 * <p>Throwing this lets {@link GlobalExceptionHandler} emit a {@code code} field
 * alongside the English {@code message}, so clients can localize by code and fall
 * back to the message. The HTTP status comes from the code but can be overridden.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    /** Uses the code's default English message and default HTTP status. */
    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), errorCode.getStatus(), null);
    }

    /** Overrides the English message (e.g. to include a field name). */
    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, errorCode.getStatus(), null);
    }

    /** Overrides both message and HTTP status. */
    public ApiException(ErrorCode errorCode, String message, HttpStatus status) {
        this(errorCode, message, status, null);
    }

    /** Carries the causing exception for logging. */
    public ApiException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, errorCode.getStatus(), cause);
    }

    private ApiException(ErrorCode errorCode, String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /** The stable string clients switch on. */
    public String getCode() {
        return errorCode.getCode();
    }

    public HttpStatus getStatus() {
        return status;
    }
}
