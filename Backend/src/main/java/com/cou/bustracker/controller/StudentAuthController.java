package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.StudentRegisterRequest;
import com.cou.bustracker.dto.response.AuthResponse;
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

import java.util.Map;

@RestController
@RequestMapping("/api/auth/student")
@RequiredArgsConstructor
@Tag(name = "Student Auth", description = "Student registration and authentication")
public class StudentAuthController {

    private final StudentService studentService;
    private final FileStorageService fileStorageService;

    @PostMapping("/register")
    @Operation(summary = "Student registration")
    public ResponseEntity<AuthResponse> register(@Valid @ModelAttribute StudentRegisterRequest request,
                                                  @RequestParam("idCard") MultipartFile idCard) throws Exception {
        return ResponseEntity.ok(studentService.register(request, idCard));
    }

    @PostMapping("/login")
    @Operation(summary = "Student login")
    public ResponseEntity<AuthResponse> login(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(studentService.login(request.get("email"), request.get("password")));
    }

    @PostMapping("/upload-id-card")
    @Operation(summary = "Upload student ID card image")
    public ResponseEntity<FileUploadResponse> uploadIdCard(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws Exception {

        Student student = studentService.getStudentByEmail(authentication.getName());
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
        Student student = studentService.getStudentByEmail(authentication.getName());
        return ResponseEntity.ok(StudentResponse.builder()
                .id(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .studentId(student.getStudentId())
                .department(student.getDepartment())
                .varsityBatch(student.getVarsityBatch())
                .idCardImageUrl(student.getIdCardImageUrl())
                .isEduMail(student.getIsEduMail())
                .isVerified(student.getIsVerified())
                .isActive(student.getIsActive())
                .createdAt(student.getCreatedAt())
                .build());
    }
}
