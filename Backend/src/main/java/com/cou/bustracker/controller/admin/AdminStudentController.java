package com.cou.bustracker.controller.admin;

import com.cou.bustracker.dto.response.StudentResponse;
import com.cou.bustracker.dto.response.MessageResponse;
import com.cou.bustracker.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
@Tag(name = "Admin Student Management", description = "Admin student management endpoints (JWT required)")
public class AdminStudentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(summary = "Get all students")
    public ResponseEntity<List<StudentResponse>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending students")
    public ResponseEntity<List<StudentResponse>> getPendingStudents() {
        return ResponseEntity.ok(studentService.getPendingStudents());
    }

    @PutMapping("/{id}/verify")
    @Operation(summary = "Verify student")
    public ResponseEntity<MessageResponse> verifyStudent(@PathVariable Long id) {
        studentService.verifyStudent(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Student verified successfully")
                .build());
    }

    @PutMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle student active status")
    public ResponseEntity<MessageResponse> toggleActive(@PathVariable Long id) {
        studentService.deactivateStudent(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Student status updated")
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete student")
    public ResponseEntity<MessageResponse> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Student deleted successfully")
                .build());
    }
}
