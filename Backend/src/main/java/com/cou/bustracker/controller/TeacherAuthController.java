package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.TeacherRegisterRequest;
import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.TeacherResponse;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/teacher")
@RequiredArgsConstructor
@Tag(name = "Teacher Auth", description = "Teacher registration and authentication")
public class TeacherAuthController {

    private final TeacherService teacherService;

    @PostMapping("/register")
    @Operation(summary = "Teacher registration")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody TeacherRegisterRequest request) {
        return ResponseEntity.ok(teacherService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Teacher login")
    public ResponseEntity<AuthResponse> login(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(teacherService.login(request.get("email"), request.get("password")));
    }
}
