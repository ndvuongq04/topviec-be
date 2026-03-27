package com.topviec.topviec_be.service;

import java.time.Duration;
import java.time.LocalDateTime;

import com.topviec.topviec_be.dto.response.ReminderInfo;
import com.topviec.topviec_be.service.impl.TokenServiceImpl;

public interface TokenService {
    // Verify email
    String generateVerifyEmailToken(String email);

    String verifyEmailToken(String token);

    // Reset password
    String generateResetPasswordToken(String email);

    String verifyResetPasswordToken(String token);

    String resendVerifyEmailToken(String email);

    // Interview Slot Selection
    String generateInterviewSlotToken(Long applicationId, Long roundId, java.time.Duration ttl);

    String verifyInterviewSlotToken(String token);

    void invalidateInterviewSlotToken(String token);

    void storeReminderInfo(Long applicationId, Long roundId, LocalDateTime deadline, Duration ttl);

    ReminderInfo getReminderInfo(Long applicationId, Long roundId);

    void updateReminderInfo(Long applicationId, Long roundId, int count, LocalDateTime lastRemindedAt);

    void deleteReminderInfo(Long applicationId, Long roundId);

}