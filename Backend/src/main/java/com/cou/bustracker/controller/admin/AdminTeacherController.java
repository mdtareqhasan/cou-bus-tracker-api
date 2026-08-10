package com.cou.bustracker.controller.admin;

import com.cou.bustracker.dto.response.MessageResponse;
import com.cou.bustracker.dto.response.TeacherResponse;
import com.cou.bustracker.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
@Tag(name = "Admin Teacher Management", description = "Admin teacher management endpoints (JWT required)")
public class AdminTeacherController {

    private final TeacherService teacherService;

    @GetMapping
    @Operation(summary = "Get all teachers")
    public ResponseEntity<List<TeacherResponse>> getAllTeachers() {
        return ResponseEntity.ok(teacherService.getAllTeachers());
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending teachers")
    public ResponseEntity<List<TeacherResponse>> getPendingTeachers() {
        return ResponseEntity.ok(teacherService.getPendingTeachers());
    }

    @PutMapping("/{id}/verify")
    @Operation(summary = "Verify teacher")
    public ResponseEntity<MessageResponse> verifyTeacher(@PathVariable Long id) {
        teacherService.verifyTeacher(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Teacher verified successfully")
                .build());
    }

    @PutMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle teacher active status")
    public ResponseEntity<MessageResponse> toggleActive(@PathVariable Long id) {
        teacherService.deactivateTeacher(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Teacher status updated")
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete teacher")
    public ResponseEntity<MessageResponse> deleteTeacher(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Teacher deleted successfully")
                .build());
    }
}
