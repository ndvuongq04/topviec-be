package com.topviec.topviec_be.controller;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;

import com.topviec.topviec_be.dto.request.ReqForgotPasswordDTO;
import com.topviec.topviec_be.dto.request.ReqLoginDTO;
import com.topviec.topviec_be.dto.request.ReqRegisterCandidateDTO;
import com.topviec.topviec_be.dto.request.ReqRegisterEmployerDTO;
import com.topviec.topviec_be.dto.request.ReqResetPasswordDTO;
import com.topviec.topviec_be.dto.response.ResLoginDTO;
import com.topviec.topviec_be.security.CustomUserDetails;
import com.topviec.topviec_be.security.JwtService;
import com.topviec.topviec_be.service.AuthService;
import com.topviec.topviec_be.service.UserSessionService;
import com.topviec.topviec_be.util.IpUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final AuthService authService;
        private final JwtService jwtService;
        private final UserSessionService userSessionService;
        private final IpUtil ipUtil;

        @Value("${app.jwt.refresh-token-expiration}")
        private long refreshTokenExpiration;

        @PostMapping("/login")
        public ResponseEntity<ResLoginDTO> login(
                        @Valid @RequestBody ReqLoginDTO request,
                        HttpServletRequest httpRequest,
                        HttpServletResponse response) {
                // Bước 1 — Xác thực email + password
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword());

                Authentication authentication = authenticationManager.authenticate(authenticationToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Bước 2 — Lấy thông tin user
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

                // Bước 3 — Generate tokens
                String accessToken = jwtService.generateAccessToken(
                                userDetails.getId(),
                                userDetails.getEmail(),
                                userDetails.getUserType().name(),
                                userDetails.getStatus());
                String refreshToken = jwtService.generateRefreshToken(userDetails.getId());

                // Bước 4.0 — Cập nhật lastLoginAt + lastLoginIp
                authService.updateLastLogin(userDetails.getId(), ipUtil.getClientIp(httpRequest));

                // Bước 4.1 — Lưu session vào DB
                userSessionService.createSession(userDetails.getId(), refreshToken, httpRequest);

                // Bước 4.2 — Set refreshToken vào HttpOnly Cookie
                ResponseCookie resCookies = ResponseCookie
                                .from("refresh_token", refreshToken)
                                .httpOnly(true)
                                .secure(true)
                                .path("/api/v1/auth/refresh")
                                .maxAge(refreshTokenExpiration)
                                .build();

                // Bước 5 — Trả accessToken về body
                ResLoginDTO resLoginDTO = ResLoginDTO.builder()
                                .accessToken(accessToken)
                                .user(ResLoginDTO.UserInfo.builder()
                                                .id(userDetails.getId())
                                                .email(userDetails.getEmail())
                                                .role(userDetails.getUserType().name())
                                                .build())
                                .build();

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                                .body(resLoginDTO);
        }

        @PostMapping("/register/candidate")
        public ResponseEntity<String> register(@Valid @RequestBody ReqRegisterCandidateDTO request) {
                authService.registerCandidate(request);
                return ResponseEntity.ok("Đăng ký thành công");
        }

        @PostMapping("/register/employer")
        public ResponseEntity<String> registerEmployer(@Valid @RequestBody ReqRegisterEmployerDTO request) {
                authService.registerEmployer(request);
                return ResponseEntity.ok("Đăng ký thành công");
        }

        @GetMapping("/verify-email")
        public ResponseEntity<String> verifyEmail(@RequestParam String token) {
                authService.verifyEmail(token);
                return ResponseEntity.ok("Xác thực email thành công!");
        }

        // Endpoint này chỉ nhận email, không trả về thông tin nào → tránh email
        // enumeration attack
        @PostMapping("/forgot-password")
        public ResponseEntity<String> forgotPassword(@RequestBody @Valid ReqForgotPasswordDTO request) {
                authService.forgotPassword(request.getEmail());
                // Luôn trả về thông báo chung → tránh email enumeration attack
                return ResponseEntity.ok("Nếu email tồn tại, chúng tôi đã gửi link đặt lại mật khẩu");
        }

        // Endpoint này nhận token(verify của "/forgot-password") + newPassword, không
        // cần email nữa vì đã lấy email từ token trong service
        @PostMapping("/reset-password")
        public ResponseEntity<String> resetPassword(@RequestBody @Valid ReqResetPasswordDTO request) {
                authService.resetPassword(request.getToken(), request.getNewPassword());
                return ResponseEntity.ok("Đặt lại mật khẩu thành công!");
        }

        // Token verify email hết hạn → user có thể yêu cầu gửi lại token mới
        @PostMapping("/resend-verify-email")
        public ResponseEntity<String> resendVerifyEmail(
                        @RequestBody @Valid ReqForgotPasswordDTO request) {
                authService.resendVerifyEmail(request.getEmail());
                return ResponseEntity.ok("Email xác thực đã được gửi lại!");
        }

}
