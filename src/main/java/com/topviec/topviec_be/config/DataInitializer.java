package com.topviec.topviec_be.config;

import com.topviec.topviec_be.entity.AdminUser;
import com.topviec.topviec_be.entity.User;
import com.topviec.topviec_be.enums.adminUsers.AdminRoleConstants;
import com.topviec.topviec_be.enums.users.UserStatus;
import com.topviec.topviec_be.enums.users.UserType;
import com.topviec.topviec_be.entity.RoleDefault;
import com.topviec.topviec_be.enums.companyMember.MemberRole;
import com.topviec.topviec_be.repository.AdminUserRepository;
import com.topviec.topviec_be.repository.RoleDefaultRepository;
import com.topviec.topviec_be.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final RoleDefaultRepository roleDefaultRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Initialize Default Roles
        initRoleDefaults();

        // Initialize Admin Users
        createAdminIfNotExists("superadmin@topviec.vn", "Super Admin", AdminRoleConstants.SUPER_ADMIN, "Management");
        createAdminIfNotExists("moderator@topviec.vn", "Content Moderator", AdminRoleConstants.CONTENT_MODERATOR,
                "Content Team");
        createAdminIfNotExists("support@topviec.vn", "Support Admin", AdminRoleConstants.SUPPORT_ADMIN,
                "Customer Support");
        createAdminIfNotExists("finance@topviec.vn", "Finance Admin", AdminRoleConstants.FINANCE_ADMIN, "Finance");
    }

    private void initRoleDefaults() {
        createRoleDefaultIfNotExists(MemberRole.OWNER, createOwnerActions());
        createRoleDefaultIfNotExists(MemberRole.MANAGER, createManagerActions());
        createRoleDefaultIfNotExists(MemberRole.RECRUITER, createRecruiterActions());
        createRoleDefaultIfNotExists(MemberRole.VIEWER, createViewerActions());
    }

    private void createRoleDefaultIfNotExists(MemberRole role, Map<String, Boolean> actions) {
        if (roleDefaultRepository.existsByRoleName(role)) {
            log.info("Role Default [{}] đã tồn tại, bỏ qua khởi tạo", role);
            return;
        }

        RoleDefault roleDefault = RoleDefault.builder()
                .roleName(role)
                .actions(actions)
                .build();

        roleDefaultRepository.save(roleDefault);
        log.info(">>>>>>>>: Đã tạo Role Default [{}]", role);
    }

    private Map<String, Boolean> createOwnerActions() {
        Map<String, Boolean> actions = new HashMap<>();
        actions.put("company:edit", true);
        actions.put("company:delete", true);
        actions.put("company:verify", true);
        actions.put("member:add", true);
        actions.put("member:delete", true);
        actions.put("member:permission", true);
        actions.put("member:view_activity", true);
        actions.put("job:create", true);
        actions.put("job:edit_own", true);
        actions.put("job:edit_other", true);
        actions.put("job:delete_own", true);
        actions.put("job:delete_other", true);
        actions.put("job:toggle_own", true);
        actions.put("job:toggle_other", true);
        actions.put("job:assign", true);
        actions.put("job:view_all", true);
        actions.put("cv:view_assigned", true);
        actions.put("cv:view_unassigned", true);
        actions.put("cv:classify", true);
        actions.put("cv:invite_interview", true);
        actions.put("cv:record_interview", true);
        actions.put("cv:approve", true);
        actions.put("cv:reject", true);
        actions.put("talent:search", true);
        actions.put("talent:view_profile", true);
        actions.put("talent:invite", true);
        actions.put("talent:save_pool", true);
        actions.put("service:purchase", true);
        actions.put("service:view_history", true);
        actions.put("service:invoice", true);
        actions.put("report:dashboard", true);
        actions.put("report:export", true);
        actions.put("report:view_company", true);
        return actions;
    }

    private Map<String, Boolean> createManagerActions() {
        Map<String, Boolean> actions = new HashMap<>();
        actions.put("company:edit", true);
        actions.put("company:delete", false);
        actions.put("company:verify", false);
        actions.put("member:add", true);
        actions.put("member:delete", true);
        actions.put("member:permission", true);
        actions.put("member:view_activity", true);
        actions.put("job:create", true);
        actions.put("job:edit_own", true);
        actions.put("job:edit_other", true);
        actions.put("job:delete_own", true);
        actions.put("job:delete_other", true);
        actions.put("job:toggle_own", true);
        actions.put("job:toggle_other", true);
        actions.put("job:assign", true);
        actions.put("job:view_all", true);
        actions.put("cv:view_assigned", true);
        actions.put("cv:view_unassigned", true);
        actions.put("cv:classify", true);
        actions.put("cv:invite_interview", true);
        actions.put("cv:record_interview", true);
        actions.put("cv:approve", true);
        actions.put("cv:reject", true);
        actions.put("talent:search", true);
        actions.put("talent:view_profile", true);
        actions.put("talent:invite", true);
        actions.put("talent:save_pool", true);
        actions.put("service:purchase", false);
        actions.put("service:view_history", true);
        actions.put("service:invoice", false);
        actions.put("report:dashboard", true);
        actions.put("report:export", true);
        actions.put("report:view_company", true);
        return actions;
    }

    private Map<String, Boolean> createRecruiterActions() {
        Map<String, Boolean> actions = new HashMap<>();
        actions.put("company:edit", false);
        actions.put("company:delete", false);
        actions.put("company:verify", false);
        actions.put("member:add", false);
        actions.put("member:delete", false);
        actions.put("member:permission", false);
        actions.put("member:view_activity", false);
        actions.put("job:create", true);
        actions.put("job:edit_own", true);
        actions.put("job:edit_other", false);
        actions.put("job:delete_own", true);
        actions.put("job:delete_other", false);
        actions.put("job:toggle_own", true);
        actions.put("job:toggle_other", false);
        actions.put("job:assign", false);
        actions.put("job:view_all", true);
        actions.put("cv:view_assigned", true);
        actions.put("cv:view_unassigned", false);
        actions.put("cv:classify", true);
        actions.put("cv:invite_interview", true);
        actions.put("cv:record_interview", true);
        actions.put("cv:approve", true);
        actions.put("cv:reject", true);
        actions.put("talent:search", false);
        actions.put("talent:view_profile", false);
        actions.put("talent:invite", false);
        actions.put("talent:save_pool", true);
        actions.put("service:purchase", false);
        actions.put("service:view_history", false);
        actions.put("service:invoice", false);
        actions.put("report:dashboard", false);
        actions.put("report:export", false);
        actions.put("report:view_company", false);
        return actions;
    }

    private Map<String, Boolean> createViewerActions() {
        Map<String, Boolean> actions = new HashMap<>();
        actions.put("company:edit", false);
        actions.put("company:delete", false);
        actions.put("company:verify", false);
        actions.put("member:add", false);
        actions.put("member:delete", false);
        actions.put("member:permission", false);
        actions.put("member:view_activity", false);
        actions.put("job:create", false);
        actions.put("job:edit_own", false);
        actions.put("job:edit_other", false);
        actions.put("job:delete_own", false);
        actions.put("job:delete_other", false);
        actions.put("job:toggle_own", false);
        actions.put("job:toggle_other", false);
        actions.put("job:assign", false);
        actions.put("job:view_all", true);
        actions.put("cv:view_assigned", true);
        actions.put("cv:view_unassigned", false);
        actions.put("cv:classify", false);
        actions.put("cv:invite_interview", false);
        actions.put("cv:record_interview", false);
        actions.put("cv:approve", false);
        actions.put("cv:reject", false);
        actions.put("talent:search", false);
        actions.put("talent:view_profile", false);
        actions.put("talent:invite", false);
        actions.put("talent:save_pool", false);
        actions.put("service:purchase", false);
        actions.put("service:view_history", false);
        actions.put("service:invoice", false);
        actions.put("report:dashboard", true);
        actions.put("report:export", false);
        actions.put("report:view_company", false);
        return actions;
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