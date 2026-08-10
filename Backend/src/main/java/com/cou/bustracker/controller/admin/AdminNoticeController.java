package com.cou.bustracker.controller.admin;

import com.cou.bustracker.dto.request.CreateNoticeRequest;
import com.cou.bustracker.dto.response.MessageResponse;
import com.cou.bustracker.dto.response.NoticeResponse;
import com.cou.bustracker.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
@Tag(name = "Admin Notice Management", description = "Admin notice management endpoints (JWT required)")
public class AdminNoticeController {

    private final NoticeService noticeService;

    @GetMapping
    @Operation(summary = "Get all notices (Admin)", description = "Retrieve all notices including inactive ones")
    public ResponseEntity<List<NoticeResponse>> getAllNotices() {
        return ResponseEntity.ok(noticeService.getAllNoticesForAdmin());
    }

    @PostMapping
    @Operation(summary = "Post notice", description = "Create a new notice")
    public ResponseEntity<MessageResponse> postNotice(@Valid @RequestBody CreateNoticeRequest request) {
        NoticeResponse notice = noticeService.createNotice(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageResponse.builder()
                        .message("Notice posted successfully")
                        .id(notice.getId())
                        .expiresAt(notice.getExpiresAt() != null ? notice.getExpiresAt().toString() : null)
                        .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notice", description = "Delete a notice")
    public ResponseEntity<MessageResponse> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Notice deleted successfully")
                .build());
    }
}
