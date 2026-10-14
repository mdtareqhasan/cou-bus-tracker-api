package com.cou.bustracker.controller;

import com.cou.bustracker.dto.response.MessageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
@Tag(name = "Google Auth (Deprecated)", description = "Google Sign-In removed. Use phone auth.")
public class GoogleAuthController {

    @PostMapping("/login")
    public ResponseEntity<MessageResponse> login() {
        return ResponseEntity.status(410).body(MessageResponse.builder()
                .message("Google Sign-In is deprecated. Please use phone number registration and login.")
                .build());
    }
}
