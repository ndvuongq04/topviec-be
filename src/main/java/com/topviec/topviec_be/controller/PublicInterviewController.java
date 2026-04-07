package com.topviec.topviec_be.controller;

import com.topviec.topviec_be.service.InterviewService;
import com.topviec.topviec_be.dto.response.ResInterviewHistoryDTO;
import com.topviec.topviec_be.dto.response.ResInterviewRoundDTO;
import com.topviec.topviec_be.dto.response.ResInterviewScheduleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * GET /interview-schedules/applications/{applicationId}
     * Lấy danh sách lịch phỏng vấn của ứng viên trong đơn ứng tuyển này (yêu cầu đăng nhập)
     */
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<List<ResInterviewScheduleDTO>> getMyInterviews(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(interviewService.getMyInterviews(userId, applicationId));
    }

    /**
     * GET /interview-schedules/applications/{applicationId}/history
     * Lấy lịch sử phỏng vấn của ứng viên trong đơn ứng tuyển này (yêu cầu đăng nhập)
     */
    @GetMapping("/applications/{applicationId}/history")
    public ResponseEntity<ResInterviewHistoryDTO> getMyInterviewHistory(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(interviewService.getMyInterviewHistory(userId, applicationId));
    }

    /**
     * GET /interview-schedules/interview-rounds/{roundId}
     * Lấy thông tin chi tiết 1 vòng phỏng vấn
     */
    @GetMapping("/interview-rounds/{roundId}")
    public ResponseEntity<ResInterviewRoundDTO> getRoundDetail(
            @PathVariable Long roundId) {
        return ResponseEntity.ok(interviewService.getRoundDetail(roundId));
    }

    /**
     * PUT /interview-schedules/{scheduleId}/confirm
     * UV xác nhận lịch PV (yêu cầu đăng nhập) - dùng khi lịch bị NĐT cập nhật hoặc cài đặt thủ công.
     */
    @PutMapping("/{scheduleId}/confirm")
    public ResponseEntity<Map<String, String>> confirmUpdatedSchedule(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        String message = interviewService.confirmUpdatedSchedule(scheduleId, userId);
        return ResponseEntity.ok(Map.of("message", message));
    }
}
