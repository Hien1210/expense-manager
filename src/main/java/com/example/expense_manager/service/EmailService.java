package com.example.expense_manager.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:noreply@expensemanager.com}") String fromEmail
    ) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Async
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Expense Manager — Mã xác thực OTP tài khoản");

            String htmlContent = buildOtpHtmlContent(otpCode);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Sent OTP email successfully to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}", toEmail, e);
        } catch (Exception e) {
            log.error("Unexpected error while sending email to {}", toEmail, e);
        }
    }

    private String buildOtpHtmlContent(String otpCode) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px; background-color: #ffffff;">
                    <h2 style="color: #2b6cb0; text-align: center; margin-bottom: 20px;">Expense Manager</h2>
                    <p style="font-size: 15px; color: #2d3748;">Xin chào,</p>
                    <p style="font-size: 15px; color: #2d3748;">Cảm ơn bạn đã đăng ký tài khoản tại <strong>Expense Manager</strong>. Mã OTP xác thực tài khoản của bạn là:</p>
                    <div style="text-align: center; margin: 28px 0;">
                        <span style="display: inline-block; font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #2b6cb0; background-color: #ebf8ff; padding: 12px 28px; border-radius: 8px; border: 1px dashed #3182ce;">
                            %s
                        </span>
                    </div>
                    <p style="font-size: 14px; color: #e53e3e; font-weight: 600;">Lưu ý: Mã xác thực có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>
                    <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;">
                    <p style="font-size: 12px; color: #718096; text-align: center;">Nếu bạn không yêu cầu đăng ký tài khoản, vui lòng bỏ qua email này.</p>
                </div>
                """.formatted(otpCode);
    }
}

