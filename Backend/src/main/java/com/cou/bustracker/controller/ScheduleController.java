package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.ScheduleResponse;
import com.cou.bustracker.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedule API", description = "Public schedule endpoints")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    @Operation(summary = "Get all schedules", description = "Retrieve all active schedules with bus information")
    public ResponseEntity<List<ScheduleResponse>> getSchedules() {
        return ResponseEntity.ok(scheduleService.getAllActiveSchedules());
    }

    @GetMapping("/bus/{busId}")
    @Operation(summary = "Get schedules by bus ID", description = "Retrieve schedules for a specific bus")
    public ResponseEntity<List<ScheduleResponse>> getSchedulesByBusId(@PathVariable Long busId) {
        return ResponseEntity.ok(scheduleService.getSchedulesByBusId(busId));
    }
}
