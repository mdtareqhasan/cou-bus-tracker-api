package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.TeacherRegisterRequest;
import com.cou.bustracker.dto.response.AuthResponse;
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

import java.util.Map;

@RestController
@RequestMapping("/api/auth/teacher")
@RequiredArgsConstructor
@Tag(name = "Teacher Auth", description = "Teacher registration and authentication")
public class TeacherAuthController {

    private final TeacherService teacherService;
    private final FileStorageService fileStorageService;

    @PostMapping("/register")
    @Operation(summary = "Teacher registration")
    public ResponseEntity<AuthResponse> register(@Valid @ModelAttribute TeacherRegisterRequest request,
                                                  @RequestParam("idCard") MultipartFile idCard) throws Exception {
        return ResponseEntity.ok(teacherService.register(request, idCard));
    }

    @PostMapping("/login")
    @Operation(summary = "Teacher login")
    public ResponseEntity<AuthResponse> login(@RequestBody Map<String, String> request) {
        return ResponseEntity.ok(teacherService.login(request.get("email"), request.get("password")));
    }

    @PostMapping("/upload-id-card")
    @Operation(summary = "Replace teacher ID card image")
    public ResponseEntity<FileUploadResponse> uploadIdCard(@RequestParam("file") MultipartFile file,
                                                            Authentication authentication) throws Exception {
        Teacher teacher = teacherService.getTeacherByEmail(authentication.getName());
        String filePath = fileStorageService.storeIdCard(file, "teacher-id-cards");
        teacherService.uploadIdCard(teacher.getId(), filePath);
        return ResponseEntity.ok(FileUploadResponse.builder().message("ID card uploaded successfully").filePath(filePath).build());
    }

    @GetMapping("/me")
    @Operation(summary = "Get current teacher profile")
    public ResponseEntity<TeacherResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(teacherService.getProfile(authentication.getName()));
    }
}
