package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.GoogleLoginRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.service.StudentService;
import com.cou.bustracker.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Google Sign-In for the Flutter app.
 *
 * <p>The client obtains a Google ID token for a {@code @cou.ac.bd} /
 * {@code @stud.cou.ac.bd} account and posts it here. The token is verified
 * server-side and, if no Student/Teacher row exists yet, a pending profile is
 * created and must be verified by an admin.
 *
 * <p>The chosen {@code role} restricts the accepted domain:
 * {@code TEACHER} only accepts {@code name@cou.ac.bd} (plus a small hard-coded
 * exception list that may use either role) — other student addresses
 * ({@code name@stud.cou.ac.bd}) are rejected with 401 — while
 * {@code STUDENT} accepts both institutional domains and the same exceptions.
 *
 * <p>An email already registered under the opposite role is rejected with 403
 * ({@code EMAIL_REGISTERED_AS_TEACHER} / {@code EMAIL_REGISTERED_AS_STUDENT}):
 * one account, one role.
 */
@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
@Tag(name = "Google Auth", description = "Sign in / auto-register with a Comilla University Google account")
public class GoogleAuthController {

    private final StudentService studentService;
    private final TeacherService teacherService;

    @PostMapping("/login")
    @Operation(summary = "Sign in (or create a pending account) with a Google ID token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = request.getRole() == GoogleLoginRequest.UserRole.STUDENT
                ? studentService.loginWithGoogle(request.getIdToken())
                : teacherService.loginWithGoogle(request.getIdToken());
        return ResponseEntity.ok(response);
    }
}
