package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller public — UV xác nhận chọn slot PV (không cần đăng nhập).
 * Base URL: /api/v1/interview-schedules
 */
@RestController
@RequestMapping("/interview-schedules")
@RequiredArgsConstructor
public class PublicInterviewController {

    private final InterviewService interviewService;

    /**
     * GET /interview-schedules/confirm?token=xxx&slotId=123
     * UV click link từ email để chọn slot. Không cần auth.
     */
    @GetMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirmSlot(
            @RequestParam String token,
            @RequestParam Long slotId) {

        String message = interviewService.confirmSlot(token, slotId);
        return ResponseEntity.ok(Map.of("message", message));
    }
}
