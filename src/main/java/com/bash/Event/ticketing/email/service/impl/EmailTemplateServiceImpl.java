package com.bash.Event.ticketing.email.service.impl;


import com.bash.Event.ticketing.email.exception.TemplateProcessingException;
import com.bash.Event.ticketing.email.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateServiceImpl implements EmailTemplateService {

    @Override
    public String getVerificationEmailContent(String name, String verificationLink, String expirationTime) {
        try {
            String template = loadTemplate("email-templates/verification-email.html");
            return template
                    .replace("{{name}}", name)
                    .replace("{{verificationLink}}", verificationLink)
                    .replace("{{expirationTime}}", expirationTime);
        } catch (IOException e) {
            log.error("Failed to process verification email template: {}", e.getMessage());
            throw new TemplateProcessingException("Failed to process verification email template", e);
        }
    }

    @Override
    public String getPasswordResetEmailContent(String name, String resetLink, String expirationTime) {
        try {
            String template = loadTemplate("email-templates/password-reset-email.html");
            return template
                    .replace("{{name}}", name)
                    .replace("{{resetLink}}", resetLink)
                    .replace("{{expirationTime}}", expirationTime);
        } catch (IOException e) {
            log.error("Failed to process password reset email template: {}", e.getMessage());
            throw new TemplateProcessingException("Failed to process password reset email template", e);
        }
    }
    
    private String loadTemplate(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }
}