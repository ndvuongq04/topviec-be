package com.topviec.topviec_be.config;

import com.topviec.topviec_be.entity.User;
import com.topviec.topviec_be.enums.users.UserStatus;
import com.topviec.topviec_be.enums.users.UserType;
import com.topviec.topviec_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createSuperAdminIfNotExists();
    }

    private void createSuperAdminIfNotExists() {
        String email = "superadmin@topviec.vn";

        if (userRepository.existsByEmail(email)) {
            log.info("Super admin đã tồn tại, bỏ qua khởi tạo");
            return;
        }

        User superAdmin = User.builder()
                .email(email)
                .password(passwordEncoder.encode("S123456"))
                .userType(UserType.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(superAdmin);
        log.info(">>>>>>>>: Đã tạo tài khoản Super Admin: {}", email);
    }
}