package com.topviec.topviec_be.service;

import jakarta.servlet.http.HttpServletRequest;

public interface UserSessionService {
    void createSession(Long userId, String refreshToken, HttpServletRequest request);
}
