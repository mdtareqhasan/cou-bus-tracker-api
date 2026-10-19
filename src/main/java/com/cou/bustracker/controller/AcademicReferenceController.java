package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.AcademicOptionResponse;
import com.cou.bustracker.service.AcademicReferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public, unauthenticated reference data for the registration pickers.
 *
 * <p>These lists used to be hardcoded in the Flutter app; they now come from
 * the database so adding a department or session is a row, not an app
 * release. Every option carries both language forms (Bangla + English) and
 * the client renders whichever matches its locale.
 *
 * <p>Unauthenticated because the academic-details step can run before a
 * profile is complete, and the data is non-sensitive — the same category as
 * the public buses and schedules endpoints.
 */
@RestController
@RequestMapping("/api/academic")
@RequiredArgsConstructor
@Tag(name = "Academic Reference", description = "Public department/office/session options for registration")
public class AcademicReferenceController {

    private final AcademicReferenceService academicReferenceService;

    @GetMapping("/departments")
    @Operation(summary = "Teaching departments (student + teacher pickers)")
    public ResponseEntity<List<AcademicOptionResponse>> getDepartments() {
        return ResponseEntity.ok(academicReferenceService.getDepartments());
    }

    @GetMapping("/offices")
    @Operation(summary = "Administrative offices (teacher picker only)")
    public ResponseEntity<List<AcademicOptionResponse>> getOffices() {
        return ResponseEntity.ok(academicReferenceService.getOffices());
    }

    @GetMapping("/units")
    @Operation(summary = "Departments then offices, in the teacher picker's combined order")
    public ResponseEntity<List<AcademicOptionResponse>> getAllUnits() {
        return ResponseEntity.ok(academicReferenceService.getAllUnits());
    }

    @GetMapping("/sessions")
    @Operation(summary = "Academic sessions (student picker)")
    public ResponseEntity<List<AcademicOptionResponse>> getSessions() {
        return ResponseEntity.ok(academicReferenceService.getSessions());
    }
}
