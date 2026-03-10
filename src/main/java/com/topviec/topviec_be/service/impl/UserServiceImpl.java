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
import com.topviec.topviec_be.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void registerCandidate(ReqRegisterCandidateDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AppException.conflict("Email đã được sử dụng");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .userType(UserType.CANDIDATE)
                .status(UserStatus.PENDING)
                .build();

        userRepository.save(user);
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
                .status(UserStatus.PENDING)
                .build();

        userRepository.save(user);
    }

    @Override
    public void updateLastLogin(Long userId, String ip) {
        userRepository.updateLastLogin(userId, LocalDateTime.now(), ip);
    }
}