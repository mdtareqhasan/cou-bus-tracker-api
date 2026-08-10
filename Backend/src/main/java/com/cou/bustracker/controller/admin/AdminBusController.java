package com.cou.bustracker.controller.admin;

import com.cou.bustracker.dto.request.CreateBusRequest;
import com.cou.bustracker.dto.request.UpdateBusRequest;
import com.cou.bustracker.dto.request.UpdateTrackerLinkRequest;
import com.cou.bustracker.dto.response.BusResponse;
import com.cou.bustracker.dto.response.MessageResponse;
import com.cou.bustracker.service.BusService;
import com.cou.bustracker.service.TrackerLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/buses")
@RequiredArgsConstructor
@Tag(name = "Admin Bus Management", description = "Admin bus management endpoints (JWT required)")
public class AdminBusController {

    private final BusService busService;
    private final TrackerLinkService trackerLinkService;

    @GetMapping
    @Operation(summary = "Get all buses (Admin)", description = "Retrieve all buses including inactive ones")
    public ResponseEntity<List<BusResponse>> getAllBuses() {
        return ResponseEntity.ok(busService.getAllActiveBuses());
    }

    @PostMapping
    @Operation(summary = "Create new bus", description = "Add a new bus to the system")
    public ResponseEntity<MessageResponse> createBus(@Valid @RequestBody CreateBusRequest request) {
        BusResponse bus = busService.createBus(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageResponse.builder()
                        .message("Bus created successfully")
                        .id(bus.getId())
                        .busNumber(bus.getBusNumber())
                        .build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update bus info", description = "Update bus information")
    public ResponseEntity<MessageResponse> updateBus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBusRequest request) {
        busService.updateBus(id, request);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Bus updated successfully")
                .build());
    }

    @PutMapping("/{busId}/tracker-link")
    @Operation(summary = "Update tracker link", description = "Update or add tracker link for a bus")
    public ResponseEntity<MessageResponse> updateTrackerLink(
            @PathVariable Long busId,
            @Valid @RequestBody UpdateTrackerLinkRequest request,
            Authentication authentication) {
        trackerLinkService.updateTrackerLink(busId, request, authentication.getName());
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Tracker link updated successfully")
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete bus", description = "Delete a bus from the system")
    public ResponseEntity<MessageResponse> deleteBus(@PathVariable Long id) {
        busService.deleteBus(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Bus deleted successfully")
                .build());
    }
}
