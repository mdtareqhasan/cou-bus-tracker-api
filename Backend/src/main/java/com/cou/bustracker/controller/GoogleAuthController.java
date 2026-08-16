package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.GoogleLoginRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.service.StudentService;
import com.cou.bustracker.service.TeacherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {
    private final StudentService studentService;
    private final TeacherService teacherService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = request.getRole() == GoogleLoginRequest.UserRole.STUDENT
                ? studentService.loginWithGoogle(request.getIdToken())
                : teacherService.loginWithGoogle(request.getIdToken());
        return ResponseEntity.ok(response);
    }
}
