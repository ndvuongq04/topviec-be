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
import com.topviec.topviec_be.service.CandidateProfileService;
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
    private final CandidateProfileService candidateProfileService;

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

        user = userRepository.save(user);

        // Gọi qua service, không gọi thẳng repo
        candidateProfileService.createDefaultProfile(user.getId(), request.getEmail());

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
        // Không báo lỗi nếu email không tồn tại → tránh email enumeration attack
        // (kẻ tấn công không biết email nào đã đăng ký)
        if (!userRepository.existsByEmail(email))
            return;

        User user = userRepository.findByEmail(email).get();

        String token = tokenService.generateResetPasswordToken(email);
        emailService.sendResetPasswordEmail(email, token, user.getEmail());
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        // 1. Xác thực token → lấy email (token tự động bị xóa)
        String email = tokenService.verifyResetPasswordToken(token);

        // 2. Tìm user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("User không tồn tại"));

        // 3. Cập nhật password mới
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void resendVerifyEmail(String email) {
        // 1. Kiểm tra user tồn tại
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("Email không tồn tại"));

        // 2. Đã verify rồi thì không cần gửi lại
        if (user.getEmailVerifiedAt() != null) {
            throw AppException.badRequest("Email này đã được xác thực rồi");
        }

        // 3. Tạo token mới → gửi lại email
        String token = tokenService.resendVerifyEmailToken(email);
        emailService.sendVerifyEmail(email, token);
    }
}