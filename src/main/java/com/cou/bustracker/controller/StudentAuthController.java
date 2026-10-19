package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.StudentAcademicDetailsRequest;
import com.cou.bustracker.dto.response.FileUploadResponse;
import com.cou.bustracker.dto.response.StudentResponse;
import com.cou.bustracker.entity.Student;
import com.cou.bustracker.service.FileStorageService;
import com.cou.bustracker.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Student authentication endpoints.
 *
 * <p>Registration happens via e-mail OTP ({@code POST /api/auth/email-verification/init})
 * or Google Sign-In — the Student row is only created after a successful
 * verification. This controller therefore exposes ID-card replace, academic
 * details, and profile only.
 */
@RestController
@RequestMapping("/api/auth/student")
@RequiredArgsConstructor
@Tag(name = "Student Auth", description = "Student profile endpoints (registration via /api/auth/email-verification/init or Google Sign-In)")
public class StudentAuthController {

    private final StudentService studentService;
    private final FileStorageService fileStorageService;

    @PostMapping("/upload-id-card")
    @Operation(summary = "Replace student ID card image")
    public ResponseEntity<FileUploadResponse> uploadIdCard(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws Exception {

        Student student = studentService.getStudentByPrincipal(authentication.getName());
        String filePath = fileStorageService.storeIdCard(file, "student-id-cards");
        studentService.uploadIdCard(student.getId(), filePath);

        return ResponseEntity.ok(FileUploadResponse.builder()
                .message("ID card uploaded successfully")
                .filePath(filePath)
                .build());
    }

    @PutMapping("/academic")
    @Operation(summary = "Set department, roll number and session (write-once, used after Google Sign-In)")
    public ResponseEntity<StudentResponse> updateAcademicDetails(
            @Valid @RequestBody StudentAcademicDetailsRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                studentService.updateAcademicDetails(authentication.getName(), request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current student profile")
    public ResponseEntity<StudentResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(studentService.getProfile(authentication.getName()));
    }
}
