package com.cou.bustracker.controller.superadmin;

import com.cou.bustracker.dto.notice.BroadcastNoticeRequest;
import com.cou.bustracker.dto.response.NoticeResponse;
import com.cou.bustracker.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/super-admin/notices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Super Admin Notices", description = "Broadcast notices visible to all Flutter users")
public class SuperAdminNoticeController {

    private final NoticeService noticeService;

    @PostMapping("/broadcast")
    @Operation(summary = "Broadcast a notice to all users (reuses public /api/notices/active)")
    public ResponseEntity<NoticeResponse> broadcast(@Valid @RequestBody BroadcastNoticeRequest request) {
        // Map BroadcastNoticeRequest -> CreateNoticeRequest so we reuse NoticeService.
        com.cou.bustracker.dto.request.CreateNoticeRequest createReq =
                new com.cou.bustracker.dto.request.CreateNoticeRequest(
                        request.getTitle(),
                        request.getBody(),
                        request.getExpiryHours());
        return ResponseEntity.status(HttpStatus.CREATED).body(noticeService.createNotice(createReq));
    }
}