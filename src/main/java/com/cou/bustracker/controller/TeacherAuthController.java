package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.TeacherAcademicDetailsRequest;
import com.cou.bustracker.dto.response.TeacherResponse;
import com.cou.bustracker.dto.response.FileUploadResponse;
import com.cou.bustracker.entity.Teacher;
import com.cou.bustracker.service.FileStorageService;
import com.cou.bustracker.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Teacher authentication endpoints.
 *
 * <p>Registration happens via e-mail OTP ({@code POST /api/auth/email-verification/init})
 * or Google Sign-In — the Teacher row is only created after a successful
 * verification. This controller therefore exposes ID-card replace, academic
 * details, and profile only.
 */
@RestController
@RequestMapping("/api/auth/teacher")
@RequiredArgsConstructor
@Tag(name = "Teacher Auth", description = "Teacher profile endpoints (registration via /api/auth/email-verification/init or Google Sign-In)")
public class TeacherAuthController {

    private final TeacherService teacherService;
    private final FileStorageService fileStorageService;

    @PostMapping("/upload-id-card")
    @Operation(summary = "Replace teacher ID card image")
    public ResponseEntity<FileUploadResponse> uploadIdCard(@RequestParam("file") MultipartFile file,
                                                            Authentication authentication) throws Exception {
        Teacher teacher = teacherService.getTeacherByPrincipal(authentication.getName());
        String filePath = fileStorageService.storeIdCard(file, "teacher-id-cards");
        teacherService.uploadIdCard(teacher.getId(), filePath);
        return ResponseEntity.ok(FileUploadResponse.builder().message("ID card uploaded successfully").filePath(filePath).build());
    }

    @PutMapping("/academic")
    @Operation(summary = "Set department, employee ID and designation (write-once, used after Google Sign-In)")
    public ResponseEntity<TeacherResponse> updateAcademicDetails(
            @Valid @RequestBody TeacherAcademicDetailsRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                teacherService.updateAcademicDetails(authentication.getName(), request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current teacher profile")
    public ResponseEntity<TeacherResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(teacherService.getProfile(authentication.getName()));
    }
}
