package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.AuthResponse;
import com.cou.bustracker.dto.response.FileUploadResponse;
import com.cou.bustracker.dto.response.StudentResponse;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.service.FileStorageService;
import com.cou.bustracker.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Student authentication endpoints.
 *
 * <p>Registration has been moved to the OTP-first flow at
 * {@code POST /api/auth/phone-verification/init} — the Student row is only
 * created after a successful OTP verification. This controller therefore
 * exposes login, ID-card replace, and profile only.
 */
@RestController
@RequestMapping("/api/auth/student")
@RequiredArgsConstructor
@Tag(name = "Student Auth", description = "Student login and profile (registration via /api/auth/phone-verification/init)")
public class StudentAuthController {

    private final StudentService studentService;
    private final FileStorageService fileStorageService;

    @PostMapping("/login")
    @Operation(summary = "Student login with phone and password")
    public ResponseEntity<AuthResponse> login(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(studentService.loginWithPhone(request.get("phone"), request.get("password")));
    }

    @PostMapping("/upload-id-card")
    @Operation(summary = "Replace student ID card image")
    public ResponseEntity<FileUploadResponse> uploadIdCard(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws Exception {

        Student student = studentService.getStudentByPhone(authentication.getName());
        String filePath = fileStorageService.storeIdCard(file, "student-id-cards");
        studentService.uploadIdCard(student.getId(), filePath);

        return ResponseEntity.ok(FileUploadResponse.builder()
                .message("ID card uploaded successfully")
                .filePath(filePath)
                .build());
    }

    @GetMapping("/me")
    @Operation(summary = "Get current student profile")
    public ResponseEntity<StudentResponse> getProfile(Authentication authentication) {
        Student student = studentService.getStudentByPhone(authentication.getName());
        return ResponseEntity.ok(StudentResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .phone(student.getPhone())
                .studentId(student.getStudentId())
                .department(student.getDepartment())
                .varsityBatch(student.getVarsityBatch())
                .idCardImageUrl(student.getIdCardImageUrl())
                .isVerified(student.getIsVerified())
                .isActive(student.getIsActive())
                .createdAt(student.getCreatedAt())
                .build());
    }
}
