package com.bash.Event.ticketing.email.service.impl;

import com.bash.Event.ticketing.email.exception.TemplateProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockedStatic;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@Timeout(10)
public class EmailTemplateServiceImplTest {

    private EmailTemplateServiceImpl emailTemplateService;

    @BeforeEach
    void setUp() {
        emailTemplateService = new EmailTemplateServiceImpl();
    }

    @Test
    void testGetVerificationEmailContentSuccessfully() throws IOException {
        String templateContent = "Hello {{name}}, please verify your account using this link: {{verificationLink}}. This link expires in {{expirationTime}}.";
        ClassPathResource mockResource = mock(ClassPathResource.class);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(templateContent.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<FileCopyUtils> mockedFileCopyUtils = mockStatic(FileCopyUtils.class)) {
            when(mockResource.getInputStream()).thenReturn(inputStream);
            mockedFileCopyUtils.when(() -> FileCopyUtils.copyToString(any(Reader.class))).thenReturn(templateContent);
            String result = emailTemplateService.getVerificationEmailContent("John", "http://example.com/verify", "24 hours");
            assertThat(result, is(equalTo("Hello John, please verify your account using this link: http://example.com/verify. This link expires in 24 hours.")));
        }
    }

    @Test
    void testGetVerificationEmailContentWithEmptyParameters() throws IOException {
        String templateContent = "Hello {{name}}, please verify your account using this link: {{verificationLink}}. This link expires in {{expirationTime}}.";
        ClassPathResource mockResource = mock(ClassPathResource.class);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(templateContent.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<FileCopyUtils> mockedFileCopyUtils = mockStatic(FileCopyUtils.class)) {
            when(mockResource.getInputStream()).thenReturn(inputStream);
            mockedFileCopyUtils.when(() -> FileCopyUtils.copyToString(any(Reader.class))).thenReturn(templateContent);
            String result = emailTemplateService.getVerificationEmailContent("", "", "");
            assertThat(result, is(equalTo("Hello , please verify your account using this link: . This link expires in .")));
        }
    }

    @Test
    void testGetVerificationEmailContentThrowsTemplateProcessingException() throws IOException {
        try (MockedStatic<FileCopyUtils> mockedFileCopyUtils = mockStatic(FileCopyUtils.class)) {
            mockedFileCopyUtils.when(() -> FileCopyUtils.copyToString(any(Reader.class))).thenThrow(new IOException("Template not found"));
            TemplateProcessingException exception = assertThrows(TemplateProcessingException.class, () -> emailTemplateService.getVerificationEmailContent("John", "http://example.com/verify", "24 hours"));
            assertThat(exception.getMessage(), is(equalTo("Failed to process verification email template")));
            assertNotNull(exception.getCause());
        }
    }

    @Test
    void testGetPasswordResetEmailContentSuccessfully() throws IOException {
        String templateContent = "Hello {{name}}, please reset your password using this link: {{resetLink}}. This link expires in {{expirationTime}}.";
        ClassPathResource mockResource = mock(ClassPathResource.class);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(templateContent.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<FileCopyUtils> mockedFileCopyUtils = mockStatic(FileCopyUtils.class)) {
            when(mockResource.getInputStream()).thenReturn(inputStream);
            mockedFileCopyUtils.when(() -> FileCopyUtils.copyToString(any(Reader.class))).thenReturn(templateContent);
            String result = emailTemplateService.getPasswordResetEmailContent("Jane", "http://example.com/reset", "30 minutes");
            assertThat(result, is(equalTo("Hello Jane, please reset your password using this link: http://example.com/reset. This link expires in 30 minutes.")));
        }
    }

    @Test
    void testGetPasswordResetEmailContentWithEmptyParameters() throws IOException {
        String templateContent = "Hello {{name}}, please reset your password using this link: {{resetLink}}. This link expires in {{expirationTime}}.";
        ClassPathResource mockResource = mock(ClassPathResource.class);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(templateContent.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<FileCopyUtils> mockedFileCopyUtils = mockStatic(FileCopyUtils.class)) {
            when(mockResource.getInputStream()).thenReturn(inputStream);
            mockedFileCopyUtils.when(() -> FileCopyUtils.copyToString(any(Reader.class))).thenReturn(templateContent);
            String result = emailTemplateService.getPasswordResetEmailContent("", "", "");
            assertThat(result, is(equalTo("Hello , please reset your password using this link: . This link expires in .")));
        }
    }

    @Test
    void testGetPasswordResetEmailContentThrowsTemplateProcessingException() throws IOException {
        try (MockedStatic<FileCopyUtils> mockedFileCopyUtils = mockStatic(FileCopyUtils.class)) {
            mockedFileCopyUtils.when(() -> FileCopyUtils.copyToString(any(Reader.class))).thenThrow(new IOException("Template loading failed"));
            TemplateProcessingException exception = assertThrows(TemplateProcessingException.class, () -> emailTemplateService.getPasswordResetEmailContent("Jane", "http://example.com/reset", "30 minutes"));
            assertThat(exception.getMessage(), is(equalTo("Failed to process password reset email template")));
            assertNotNull(exception.getCause());
        }
    }

    @Test
    void testGetVerificationEmailContentWithMultiplePlaceholders() throws IOException {
        String templateContent = "{{name}} {{name}} {{verificationLink}} {{expirationTime}} {{name}}";
        ClassPathResource mockResource = mock(ClassPathResource.class);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(templateContent.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<FileCopyUtils> mockedFileCopyUtils = mockStatic(FileCopyUtils.class)) {
            when(mockResource.getInputStream()).thenReturn(inputStream);
            mockedFileCopyUtils.when(() -> FileCopyUtils.copyToString(any(Reader.class))).thenReturn(templateContent);
            String result = emailTemplateService.getVerificationEmailContent("Alice", "http://verify.com", "1 hour");
            assertThat(result, is(equalTo("Alice Alice http://verify.com 1 hour Alice")));
        }
    }

    @Test
    void testGetPasswordResetEmailContentWithMultiplePlaceholders() throws IOException {
        String templateContent = "{{name}} {{resetLink}} {{name}} {{expirationTime}} {{resetLink}}";
        ClassPathResource mockResource = mock(ClassPathResource.class);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(templateContent.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<FileCopyUtils> mockedFileCopyUtils = mockStatic(FileCopyUtils.class)) {
            when(mockResource.getInputStream()).thenReturn(inputStream);
            mockedFileCopyUtils.when(() -> FileCopyUtils.copyToString(any(Reader.class))).thenReturn(templateContent);
            String result = emailTemplateService.getPasswordResetEmailContent("Bob", "http://reset.com", "2 hours");
            assertThat(result, is(equalTo("Bob http://reset.com Bob 2 hours http://reset.com")));
        }
    }
}
