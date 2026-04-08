package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.verify-email-url}")
    private String verifyEmailPath;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerifyEmail(String toEmail, String token) {
        // 1. Tạo context chứa các biến truyền vào template
        Context context = new Context();
        context.setVariable("verifyLink", verifyEmailPath + "?token=" + token);

        // 2. Render template thành HTML string
        String htmlBody = templateEngine.process("email/verify-email", context);

        // 3. Gửi email
        sendHtmlEmail(toEmail, "[Topviec] Xác thực tài khoản", htmlBody);
    }

    @Override
    public void sendResetPasswordEmail(String toEmail, String token, String fullName) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("resetLink", baseUrl + "/reset-password?token=" + token);

        String htmlBody = templateEngine.process("email/reset-password", context);
        sendHtmlEmail(toEmail, "[Topviec] Đặt lại mật khẩu", htmlBody);
    }

    @Override
    public void sendMemberInviteNewUser(String toEmail, String tempPassword, String verifyToken) {
        Context context = new Context();
        context.setVariable("email", toEmail);
        context.setVariable("tempPassword", tempPassword);
        context.setVariable("verifyLink", verifyEmailPath + "?token=" + verifyToken);

        String htmlBody = templateEngine.process("email/member-invite-new-user", context);
        sendHtmlEmail(toEmail, "[Topviec] Bạn được mời tham gia công ty", htmlBody);
    }

    @Override
    public void sendPermissionChangedEmail(String toEmail, String companyName, String newRoleName) {
        Context context = new Context();
        context.setVariable("companyName", companyName);
        context.setVariable("newRoleName", newRoleName);

        String htmlBody = templateEngine.process("email/member-permission-changed", context);
        sendHtmlEmail(toEmail, "[Topviec] Thay đổi phân quyền thành viên công ty", htmlBody);
    }

    @Override
    public void sendUpdateScheduleEmail(String toEmail, String candidateName, String companyName, String jobTitle,
                                        String oldSchedule, String newScheduleTime, String newScheduleDate,
                                        String interviewLocation, String interviewerName, String confirmLink) {
        Context context = new Context();
        context.setVariable("candidateName", candidateName);
        context.setVariable("companyName", companyName);
        context.setVariable("jobTitle", jobTitle);
        context.setVariable("oldSchedule", oldSchedule);
        context.setVariable("newScheduleTime", newScheduleTime);
        context.setVariable("newScheduleDate", newScheduleDate);
        context.setVariable("interviewLocation", interviewLocation);
        context.setVariable("interviewerName", interviewerName);
        context.setVariable("confirmLink", confirmLink);

        String htmlBody = templateEngine.process("email/update-schedule", context);
        sendHtmlEmail(toEmail, "[Topviec] Thông báo thay đổi lịch phỏng vấn", htmlBody);
    }

    @Override
    public void sendCancelScheduleEmail(String toEmail, String candidateName, String companyName, String jobTitle,
                                        String scheduledTime, String scheduledDate, String roundName) {
        Context context = new Context();
        context.setVariable("candidateName", candidateName);
        context.setVariable("companyName", companyName);
        context.setVariable("jobTitle", jobTitle);
        context.setVariable("scheduledTime", scheduledTime);
        context.setVariable("scheduledDate", scheduledDate);
        context.setVariable("roundName", roundName);

        String htmlBody = templateEngine.process("email/cancel-schedule", context);
        sendHtmlEmail(toEmail, "[Topviec] Thông báo hủy lịch phỏng vấn", htmlBody);
    }

    @Override
    public void sendFailInterviewEmail(String toEmail, String candidateName, String companyName, String jobTitle,
                                       String roundName, Integer rating, String note) {
        Context context = new Context();
        context.setVariable("candidateName", candidateName);
        context.setVariable("companyName", companyName);
        context.setVariable("jobTitle", jobTitle);
        context.setVariable("roundName", roundName);
        context.setVariable("rating", rating);
        context.setVariable("note", note);

        String htmlBody = templateEngine.process("email/fail-interview", context);
        sendHtmlEmail(toEmail, "[Topviec] Thông báo kết quả phỏng vấn", htmlBody);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Gửi email thất bại: " + e.getMessage());
        }
    }
}