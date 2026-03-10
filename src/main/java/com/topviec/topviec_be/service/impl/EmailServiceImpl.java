package com.topviec.topviec_be.service.impl;

import com.topviec.topviec_be.exception.AppException;
import com.topviec.topviec_be.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
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

    @Override
    public void sendVerifyEmail(String toEmail, String name, String verifyUrl) {
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("verifyUrl", verifyUrl);
            String htmlContent = templateEngine.process("verify-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản TopViec");
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Gửi email thất bại");
        }
    }
}