package com.topviec.topviec_be.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.topviec.topviec_be.entity.User;
import com.topviec.topviec_be.entity.UserSession;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.UserRepository;
import com.topviec.topviec_be.repository.UserSessionRepository;
import com.topviec.topviec_be.service.UserSessionService;
import com.topviec.topviec_be.util.HashUtil;
import com.topviec.topviec_be.util.IpUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;
    private final HashUtil hashUtil;
    private final IpUtil ipUtil;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Override
    public void createSession(Long userId, String refreshToken, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User không tồn tại"));

        // Hash refresh token trước khi lưu
        String refreshTokenHash = hashUtil.hashSHA256(refreshToken);

        // Lấy device info
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("userAgent", request.getHeader("User-Agent"));

        UserSession session = UserSession.builder()
                .user(user)
                .refreshTokenHash(refreshTokenHash)
                .deviceInfo(deviceInfo)
                .ipAddress(ipUtil.getClientIp(request))
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();

        userSessionRepository.save(session);
    }
}