package com.bash.Event.ticketing.email.service.impl;

import com.bash.Event.ticketing.authentication.domain.User;
import com.bash.Event.ticketing.email.exception.EmailSendException;
import com.bash.Event.ticketing.email.service.EmailService;
import com.bash.Event.ticketing.email.service.EmailTemplateService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    @Value("${app.base-url}")
    private String baseUrl;
    
    @Override
    @Async
    public void sendVerificationEmail(User user, String token) {
        String verificationLink = baseUrl + "/api/v1/auth/verify-email?token=" + token;
        String subject = "Verify Your Email Address";
        
        String content = templateService.getVerificationEmailContent(
                user.getFirstName(), 
                verificationLink, 
                "24 hours"
        );
        
        sendEmail(user.getEmail(), subject, content);
        log.info("Verification email sent to: {}", user.getEmail());
    }
    
    @Override
    @Async
    public void sendPasswordResetEmail(User user, String token) {
        String resetLink = baseUrl + "/reset-password?token=" + token;
        String subject = "Password Reset Request";
        
        String content = templateService.getPasswordResetEmailContent(
                user.getFirstName(),
                resetLink,
                "1 hour"
        );
        
        sendEmail(user.getEmail(), subject, content);
        log.info("Password reset email sent to: {}", user.getEmail());
    }
    
    private void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true indicates HTML content
            
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new EmailSendException("Failed to send email", e);
        }
    }
}