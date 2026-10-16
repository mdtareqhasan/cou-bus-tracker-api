package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.service.StudentService;
import com.cou.bustracker.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Email Login", description = "Unified email/password login for students and teachers")
public class EmailLoginController {

    private final StudentService studentService;
    private final TeacherService teacherService;

    @PostMapping("/email-login")
    @Operation(summary = "Unified email login for student or teacher")
    public ResponseEntity<AuthResponse> login(@RequestBody Map<String, String> request) {
        String role = request.get("role");
        String email = request.get("email");
        String password = request.get("password");

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("রোল নির্বাচন করুন");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("ইমেইল দিন");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("পাসওয়ার্ড দিন");
        }

        String normalizedRole = role.trim().toUpperCase();
        return switch (normalizedRole) {
            case "STUDENT" -> ResponseEntity.ok(studentService.loginWithEmail(email, password));
            case "TEACHER", "EMPLOYEE" -> ResponseEntity.ok(teacherService.loginWithEmail(email, password));
            default -> throw new IllegalArgumentException("রোল অবশ্যই STUDENT, TEACHER, অথবা EMPLOYEE হতে হবে");
        };
    }
}
