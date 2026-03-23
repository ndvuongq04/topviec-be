package com.topviec.topviec_be.service;

public interface EmailService {
    void sendVerifyEmail(String toEmail, String verifyUrl);

    void sendResetPasswordEmail(String toEmail, String token, String fullName);

}