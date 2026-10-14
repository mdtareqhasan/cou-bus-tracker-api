package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.NoticeResponse;
import com.cou.bustracker.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@Tag(name = "Notice API", description = "Public notice endpoints")
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/active")
    @Operation(summary = "Get active notices", description = "Retrieve all active and non-expired notices")
    public ResponseEntity<List<NoticeResponse>> getActiveNotices() {
        return ResponseEntity.ok(noticeService.getActiveNotices());
    }
}
