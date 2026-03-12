package com.topviec.topviec_be.service;

public interface TokenService {
    // Verify email
    String generateVerifyEmailToken(String email);

    String verifyEmailToken(String token);

    // Reset password
    String generateResetPasswordToken(String email);

    String verifyResetPasswordToken(String token);

    String resendVerifyEmailToken(String email);

}