package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.NoticeResponse;
import com.cou.bustracker.service.CurrentUser;
import com.cou.bustracker.service.CurrentUserService;
import com.cou.bustracker.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Rider-facing notices.
 *
 * <p>Authenticated since the audience filter needs to know <em>who</em> is
 * asking: students get {@code ALL} + {@code STUDENT} notices, teachers get
 * {@code ALL} + {@code TEACHER}. Secured via {@code /api/notices/**} →
 * authenticated in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@Tag(name = "Notice API", description = "Notice endpoints for the Flutter app (JWT required)")
public class NoticeController {

    private final CurrentUserService currentUserService;
    private final NoticeService noticeService;

    @GetMapping("/active")
    @Operation(summary = "Get active notices for the caller",
            description = "Active, non-expired notices visible to the caller's role, newest first")
    public ResponseEntity<List<NoticeResponse>> getActiveNotices(Authentication authentication) {
        CurrentUser user = currentUserService.resolve(authentication);
        return ResponseEntity.ok(noticeService.getActiveNotices(user.role()));
    }
}
