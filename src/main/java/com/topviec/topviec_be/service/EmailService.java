package com.topviec.topviec_be.service;

public interface EmailService {
    void sendVerifyEmail(String toEmail, String verifyUrl);

    void sendResetPasswordEmail(String toEmail, String token, String fullName);

    /**
     * TH1: Email mời thành viên mới chưa có tài khoản.
     * Gửi kèm mật khẩu tạm + link xác thực.
     */
    void sendMemberInviteNewUser(String toEmail, String tempPassword, String verifyToken);

    /**
     * Thông báo thay đổi quyền (CN-NTT-019).
     */
    void sendPermissionChangedEmail(String toEmail, String companyName, String newRoleName);

    /**
     * Thông báo thay đổi lịch phỏng vấn (updateSchedule).
     */
    void sendUpdateScheduleEmail(String toEmail, String candidateName, String companyName, String jobTitle,
                                 String oldSchedule, String newScheduleTime, String newScheduleDate,
                                 String interviewLocation, String interviewerName, String confirmLink);
}