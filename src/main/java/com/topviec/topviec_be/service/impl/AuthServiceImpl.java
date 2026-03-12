package com.topviec.topviec_be.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.topviec.topviec_be.dto.request.ReqRegisterCandidateDTO;
import com.topviec.topviec_be.dto.request.ReqRegisterEmployerDTO;
import com.topviec.topviec_be.entity.User;
import com.topviec.topviec_be.enums.users.UserStatus;
import com.topviec.topviec_be.enums.users.UserType;
import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.repository.UserRepository;
import com.topviec.topviec_be.service.AuthService;
import com.topviec.topviec_be.service.EmailService;
import com.topviec.topviec_be.service.TokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmailService emailService;

    @Override
    public void registerCandidate(ReqRegisterCandidateDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AppException.conflict("Email đã được sử dụng");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(UserType.CANDIDATE)
                .status(UserStatus.PENDING) // chờ xác thực email
                .build();

        userRepository.save(user);

        // Tạo token → lưu Redis → gửi email
        String token = tokenService.generateVerifyEmailToken(request.getEmail());
        emailService.sendVerifyEmail(request.getEmail(), token);
    }

    @Override
    public void registerEmployer(ReqRegisterEmployerDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AppException.badRequest("Email đã được sử dụng");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(UserType.EMPLOYER)
                .status(UserStatus.PENDING) // chờ xác thực email
                .build();

        userRepository.save(user);

        // Tạo token → lưu Redis → gửi email
        String token = tokenService.generateVerifyEmailToken(request.getEmail());
        emailService.sendVerifyEmail(request.getEmail(), token);
    }

    @Override
    public void verifyEmail(String token) {
        // Lấy email từ Redis (tự động xóa token sau khi lấy)
        String email = tokenService.verifyEmailToken(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("User không tồn tại"));

        if (user.getEmailVerifiedAt() != null) {
            throw AppException.badRequest("Email đã được xác thực trước đó");
        }

        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setStatus(UserStatus.ACTIVE); // PENDING → ACTIVE
        userRepository.save(user);
    }

    @Override
    public void updateLastLogin(Long userId, String ip) {
        userRepository.updateLastLogin(userId, LocalDateTime.now(), ip);
    }

    @Override
    public void forgotPassword(String email) {
        throw new UnsupportedOperationException("Unimplemented method 'forgotPassword'");
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        throw new UnsupportedOperationException("Unimplemented method 'resetPassword'");
    }
}