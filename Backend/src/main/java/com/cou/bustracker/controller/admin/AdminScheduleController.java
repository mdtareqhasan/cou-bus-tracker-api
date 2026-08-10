package com.cou.bustracker.controller.admin;

import com.cou.bustracker.dto.request.CreateScheduleRequest;
import com.cou.bustracker.dto.response.MessageResponse;
import com.cou.bustracker.dto.response.ScheduleResponse;
import com.cou.bustracker.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/schedules")
@RequiredArgsConstructor
@Tag(name = "Admin Schedule Management", description = "Admin schedule management endpoints (JWT required)")
public class AdminScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    @Operation(summary = "Get all schedules (Admin)", description = "Retrieve all schedules")
    public ResponseEntity<List<ScheduleResponse>> getAllSchedules() {
        return ResponseEntity.ok(scheduleService.getAllActiveSchedules());
    }

    @PostMapping
    @Operation(summary = "Add schedule", description = "Add a new schedule for a bus")
    public ResponseEntity<MessageResponse> addSchedule(@Valid @RequestBody CreateScheduleRequest request) {
        scheduleService.addSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageResponse.builder()
                        .message("Schedule added successfully")
                        .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update schedule", description = "Update an existing bus schedule")
    public ResponseEntity<MessageResponse> updateSchedule(@PathVariable Long id,
                                                           @Valid @RequestBody CreateScheduleRequest request) {
        scheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Schedule updated successfully")
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete schedule", description = "Delete a schedule")
    public ResponseEntity<MessageResponse> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Schedule deleted successfully")
                .build());
    }
}
