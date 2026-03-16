package com.topviec.topviec_be.service;

import com.topviec.topviec_be.dto.request.ReqRegisterCandidateDTO;
import com.topviec.topviec_be.dto.request.ReqRegisterEmployerDTO;

public interface AuthService {
    void registerCandidate(ReqRegisterCandidateDTO request);

    void registerEmployer(ReqRegisterEmployerDTO request);

    void updateLastLogin(Long userId, String ip);

    void verifyEmail(String token);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword); // tự động lấy email từ token forgot, không cần truyền vào nữa

    void resendVerifyEmail(String email);

    /**
     * Lấy adminRole từ bảng admin_users theo userId.
     * Trả về null nếu user không có bản ghi admin_users hoặc đã bị xóa/inactive.
     * Dùng khi tạo JWT cho admin để đưa claim "adminRole" vào token.
     */
    String getAdminRoleByUserId(Long userId);
}
