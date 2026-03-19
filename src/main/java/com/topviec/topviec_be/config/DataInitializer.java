package com.topviec.topviec_be.config;

import com.topviec.topviec_be.entity.AdminUser;
import com.topviec.topviec_be.entity.User;
import com.topviec.topviec_be.enums.adminUsers.AdminRoleConstants;
import com.topviec.topviec_be.enums.users.UserStatus;
import com.topviec.topviec_be.enums.users.UserType;
import com.topviec.topviec_be.repository.AdminUserRepository;
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
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createAdminIfNotExists("superadmin@topviec.vn", "Super Admin", AdminRoleConstants.SUPER_ADMIN, "Management");
        createAdminIfNotExists("moderator@topviec.vn", "Content Moderator", AdminRoleConstants.CONTENT_MODERATOR,
                "Content Team");
        createAdminIfNotExists("support@topviec.vn", "Support Admin", AdminRoleConstants.SUPPORT_ADMIN,
                "Customer Support");
        createAdminIfNotExists("finance@topviec.vn", "Finance Admin", AdminRoleConstants.FINANCE_ADMIN, "Finance");
    }

    private void createAdminIfNotExists(String email, String fullName, String adminRole, String department) {
        if (userRepository.existsByEmail(email)) {
            log.info("Admin [{}] đã tồn tại, bỏ qua khởi tạo", email);
            return;
        }

        // Bước 1: Tạo User
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode("123456"))
                .userType(UserType.ADMIN)
                .status(UserStatus.ACTIVE)
                .twoFactorEnabled(false)
                .build();

        User savedUser = userRepository.save(user);

        // Bước 2: Tạo AdminUser gắn với User vừa tạo
        AdminUser adminUser = AdminUser.builder()
                .user(savedUser)
                .adminRole(adminRole)
                .fullName(fullName)
                .department(department)
                .createdBy(savedUser.getId())
                .build();

        adminUserRepository.save(adminUser);
        log.info(">>>>>>>>: Đã tạo tài khoản [{}] - {} ({})", adminRole, fullName, email);
    }
}