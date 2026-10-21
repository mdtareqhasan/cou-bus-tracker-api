package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.RoutePreferenceRequest;
import com.cou.bustracker.dto.response.RoutePreferenceResponse;
import com.cou.bustracker.service.CurrentUser;
import com.cou.bustracker.service.CurrentUserService;
import com.cou.bustracker.service.UserRoutePreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Authenticated per-route preferences: pin on the home screen, opt in to
 * schedule-change pushes.
 *
 * <p>The rider is always resolved from the JWT — the client never supplies a
 * user id. Secured via {@code /api/me/**} → authenticated in
 * {@code SecurityConfig}, and the service rejects non-rider roles (only
 * students and teachers have routes to follow).
 */
@RestController
@RequestMapping("/api/me/route-preferences")
@RequiredArgsConstructor
@Tag(name = "Route Preferences", description = "Per-route pins and schedule-alert opt-in for the authenticated rider")
public class UserRoutePreferenceController {

    private final CurrentUserService currentUserService;
    private final UserRoutePreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Get my route preferences", description = "Every route the caller has pinned or enabled alerts for")
    public ResponseEntity<List<RoutePreferenceResponse>> getMyPreferences(Authentication authentication) {
        CurrentUser user = currentUserService.resolve(authentication);
        return ResponseEntity.ok(preferenceService.getMyPreferences(user));
    }

    @PutMapping("/{routeId}")
    @Operation(summary = "Set my preferences for a route",
            description = "Idempotent upsert; omitted fields keep their current value. Both flags false removes the preference")
    public ResponseEntity<RoutePreferenceResponse> upsertPreference(
            @PathVariable Long routeId,
            @Valid @RequestBody RoutePreferenceRequest request,
            Authentication authentication) {
        CurrentUser user = currentUserService.resolve(authentication);
        return ResponseEntity.ok(preferenceService.upsertPreference(user, routeId, request));
    }

    @DeleteMapping("/{routeId}")
    @Operation(summary = "Forget my preferences for a route",
            description = "Unpins and disables alerts; idempotent")
    public ResponseEntity<Void> removePreference(@PathVariable Long routeId,
                                                 Authentication authentication) {
        CurrentUser user = currentUserService.resolve(authentication);
        preferenceService.removePreference(user, routeId);
        return ResponseEntity.noContent().build();
    }
}
