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