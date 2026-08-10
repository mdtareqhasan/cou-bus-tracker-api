package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.BusDetailResponse;
import com.cou.bustracker.dto.response.BusResponse;
import com.cou.bustracker.service.BusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buses")
@RequiredArgsConstructor
@Tag(name = "Bus API", description = "Public bus endpoints")
public class BusController {

    private final BusService busService;

    @GetMapping
    @Operation(summary = "Get all active buses", description = "Retrieve all active buses, optionally filtered by category")
    public ResponseEntity<List<BusResponse>> getBuses(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isEmpty()) {
            return ResponseEntity.ok(busService.getBusesByCategory(category));
        }
        return ResponseEntity.ok(busService.getAllActiveBuses());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bus by ID", description = "Retrieve a single bus with its tracker link and schedules")
    public ResponseEntity<BusDetailResponse> getBusById(@PathVariable Long id) {
        return ResponseEntity.ok(busService.getBusById(id));
    }
}
