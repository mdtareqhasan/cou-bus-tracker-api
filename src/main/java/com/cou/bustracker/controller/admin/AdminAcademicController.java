package com.cou.bustracker.controller.admin;

import com.cou.bustracker.dto.request.AcademicSessionRequest;
import com.cou.bustracker.dto.request.AcademicUnitRequest;
import com.cou.bustracker.dto.response.AcademicOptionResponse;
import com.cou.bustracker.dto.response.MessageResponse;
import com.cou.bustracker.service.AcademicManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin CRUD for the academic reference data behind the registration pickers
 * (departments, offices and sessions).
 *
 * <p>These rows used to be hardcoded in the Flutter app, so the whole point of
 * this endpoint is that adding or retiring an option is an admin action instead
 * of an app release. Listings include retired rows; the public
 * {@code /api/academic/**} endpoints keep serving only active ones.
 *
 * <p>Deleting an option that existing students/teachers reference is rejected
 * (the accounts point at it by FK) — deactivate it instead.
 */
@RestController
@RequestMapping("/api/admin/academic")
@RequiredArgsConstructor
@Tag(name = "Admin Academic Reference", description = "Admin department/office/session management (JWT required)")
public class AdminAcademicController {

    private final AcademicManagementService academicManagementService;

    // ---- Units (departments / offices) -------------------------------------

    @GetMapping("/units")
    @Operation(summary = "Get all academic units (Admin)", description = "Departments then offices, including retired rows")
    public ResponseEntity<List<AcademicOptionResponse>> getAllUnits() {
        return ResponseEntity.ok(academicManagementService.getAllUnits());
    }

    @PostMapping("/units")
    @Operation(summary = "Add academic unit", description = "Create a department or office option")
    public ResponseEntity<AcademicOptionResponse> createUnit(@Valid @RequestBody AcademicUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(academicManagementService.createUnit(request));
    }

    @PutMapping("/units/{id}")
    @Operation(summary = "Update academic unit", description = "Rename a department/office or change its order")
    public ResponseEntity<AcademicOptionResponse> updateUnit(@PathVariable Long id,
                                                             @Valid @RequestBody AcademicUnitRequest request) {
        return ResponseEntity.ok(academicManagementService.updateUnit(id, request));
    }

    @PatchMapping("/units/{id}/toggle")
    @Operation(summary = "Toggle academic unit active status", description = "Retire or re-activate an option")
    public ResponseEntity<MessageResponse> toggleUnit(@PathVariable Long id) {
        boolean active = academicManagementService.toggleUnit(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Option " + (active ? "activated" : "deactivated") + " successfully")
                .build());
    }

    @DeleteMapping("/units/{id}")
    @Operation(summary = "Delete academic unit", description = "Only allowed when no student/teacher references it")
    public ResponseEntity<MessageResponse> deleteUnit(@PathVariable Long id) {
        academicManagementService.deleteUnit(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Academic unit deleted successfully")
                .build());
    }

    // ---- Sessions ----------------------------------------------------------

    @GetMapping("/sessions")
    @Operation(summary = "Get all academic sessions (Admin)", description = "All sessions, including retired rows")
    public ResponseEntity<List<AcademicOptionResponse>> getAllSessions() {
        return ResponseEntity.ok(academicManagementService.getAllSessions());
    }

    @PostMapping("/sessions")
    @Operation(summary = "Add academic session", description = "Create a session option")
    public ResponseEntity<AcademicOptionResponse> createSession(
            @Valid @RequestBody AcademicSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(academicManagementService.createSession(request));
    }

    @PutMapping("/sessions/{id}")
    @Operation(summary = "Update academic session", description = "Rename a session or change its order")
    public ResponseEntity<AcademicOptionResponse> updateSession(@PathVariable Long id,
                                                                @Valid @RequestBody AcademicSessionRequest request) {
        return ResponseEntity.ok(academicManagementService.updateSession(id, request));
    }

    @PatchMapping("/sessions/{id}/toggle")
    @Operation(summary = "Toggle academic session active status", description = "Retire or re-activate a session")
    public ResponseEntity<MessageResponse> toggleSession(@PathVariable Long id) {
        boolean active = academicManagementService.toggleSession(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Session " + (active ? "activated" : "deactivated") + " successfully")
                .build());
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(summary = "Delete academic session", description = "Only allowed when no student references it")
    public ResponseEntity<MessageResponse> deleteSession(@PathVariable Long id) {
        academicManagementService.deleteSession(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Academic session deleted successfully")
                .build());
    }
}
