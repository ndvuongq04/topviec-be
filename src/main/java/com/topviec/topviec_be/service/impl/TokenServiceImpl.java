package com.topviec.topviec_be.service.impl;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.service.TokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    @Value("${app.token.verify-email-ttl:1440}")
    private long VERIFY_EMAIL_TTL;
    @Value("${app.token.reset-password-ttl:15}")
    private long RESET_PASSWORD_TTL;
    @Value("${app.token.verify-email-prefix:verify-email}")
    private String VERIFY_EMAIL_PREFIX;
    @Value("${app.token.reset-password-prefix:reset-password}")
    private String RESET_PASSWORD_PREFIX;

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public String generateVerifyEmailToken(String email) {
        String token = UUID.randomUUID().toString();
        String key = VERIFY_EMAIL_PREFIX + token;

        // Lưu vào Redis: key=token, value=email, TTL=24h
        redisTemplate.opsForValue()
                .set(key, email, Duration.ofMinutes(VERIFY_EMAIL_TTL));

        return token;
    }

    @Override
    public String verifyEmailToken(String token) {
        String key = VERIFY_EMAIL_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);

        if (email == null) {
            throw AppException.badRequest("Link xác thực đã hết hạn hoặc không hợp lệ");
        }

        redisTemplate.delete(key); // Xóa ngay sau khi dùng → không dùng lại được
        return email;
    }
}
