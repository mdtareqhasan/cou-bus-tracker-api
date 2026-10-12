package com.cou.bustracker.controller;

import com.cou.bustracker.dto.request.RegisterFcmTokenRequest;
import com.cou.bustracker.dto.response.MessageResponse;
import com.cou.bustracker.service.CurrentUser;
import com.cou.bustracker.service.CurrentUserService;
import com.cou.bustracker.service.FcmTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authenticated FCM token endpoints.
 *
 * <p>The user is always resolved from the JWT — the client never supplies a
 * user id. Secured via {@code /api/notifications/**} → authenticated in
 * {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "FCM push-token registration for the authenticated user")
public class NotificationController {

    private final CurrentUserService currentUserService;
    private final FcmTokenService fcmTokenService;

    @PostMapping("/fcm-token")
    @Operation(summary = "Register (or refresh) the caller's FCM token for this device")
    public ResponseEntity<MessageResponse> registerFcmToken(
            @Valid @RequestBody RegisterFcmTokenRequest request,
            Authentication authentication) {

        CurrentUser user = currentUserService.resolve(authentication);
        fcmTokenService.register(user, request);

        return ResponseEntity.ok(MessageResponse.builder()
                .message("FCM token registered")
                .build());
    }

    @DeleteMapping("/fcm-token")
    @Operation(summary = "Remove the caller's FCM token (idempotent; only deletes tokens owned by the caller)")
    public ResponseEntity<?> unregisterFcmToken(
            @RequestParam("token") String token,
            Authentication authentication) {

        CurrentUser user = currentUserService.resolve(authentication);
        boolean removed = fcmTokenService.unregister(user, token);

        return ResponseEntity.ok(Map.of(
                "message", removed ? "FCM token removed" : "No matching token found",
                "removed", removed));
    }

    @DeleteMapping("/fcm-token/all")
    @Operation(summary = "Remove every FCM token for the caller (e.g. log out everywhere)")
    public ResponseEntity<?> unregisterAllFcmTokens(Authentication authentication) {
        CurrentUser user = currentUserService.resolve(authentication);
        long removed = fcmTokenService.unregisterAll(user);
        return ResponseEntity.ok(Map.of(
                "message", "Removed " + removed + " token(s)",
                "removed", removed));
    }
}
