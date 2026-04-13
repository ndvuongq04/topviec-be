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

    /**
     * Thông báo hủy lịch phỏng vấn (deleteSchedule).
     */
    void sendCancelScheduleEmail(String toEmail, String candidateName, String companyName, String jobTitle,
                                 String scheduledTime, String scheduledDate, String roundName);

    /**
     * Thông báo kết quả phỏng vấn cho UV (PASS hoặc FAIL).
     */
    void sendInterviewResultEmail(String toEmail, String candidateName, String companyName, String jobTitle,
                                  String roundName, boolean passed, Integer rating, String note);

    /**
     * Thông báo UV chọn slot phỏng vấn (Cách 2: UV tự chọn lịch).
     */
    void sendSlotSelectionEmail(String toEmail, String candidateName, String companyName, String jobTitle,
                                String roundName, String deadline, String selectSlotLink);
}