package com.bash.Event.ticketing.email.service.impl;

import com.bash.Event.ticketing.authentication.domain.User;
import com.bash.Event.ticketing.email.exception.EmailSendException;
import com.bash.Event.ticketing.email.service.EmailTemplateService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeast;
import org.junit.jupiter.api.Disabled;

@Timeout(10)
public class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailTemplateService templateService;

    private EmailServiceImpl emailService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        emailService = spy(new EmailServiceImpl(mailSender, templateService));
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "https://example.com");
    }

    @Test
    public void testSendVerificationEmailSuccessfully() {
        User user = new User();
        user.setFirstName("John");
        user.setEmail("john@example.com");
        String token = "test-token";
        String expectedLink = "https://example.com/api/v1/auth/verify-email?token=test-token";
        String expectedContent = "Verification email content";
        MimeMessage mimeMessage = mock(MimeMessage.class);
        doReturn(mimeMessage).when(mailSender).createMimeMessage();
        doNothing().when(mailSender).send(any(MimeMessage.class));
        doReturn(expectedContent).when(templateService).getVerificationEmailContent("John", expectedLink, "24 hours");
        emailService.sendVerificationEmail(user, token);
        verify(templateService, atLeast(1)).getVerificationEmailContent("John", expectedLink, "24 hours");
    }

    @Test
    public void testSendPasswordResetEmailSuccessfully() {
        User user = new User();
        user.setFirstName("Jane");
        user.setEmail("jane@example.com");
        String token = "reset-token";
        String expectedLink = "https://example.com/reset-password?token=reset-token";
        String expectedContent = "Password reset email content";
        MimeMessage mimeMessage = mock(MimeMessage.class);
        doReturn(mimeMessage).when(mailSender).createMimeMessage();
        doNothing().when(mailSender).send(any(MimeMessage.class));
        doReturn(expectedContent).when(templateService).getPasswordResetEmailContent("Jane", expectedLink, "1 hour");
        emailService.sendPasswordResetEmail(user, token);
        verify(templateService, atLeast(1)).getPasswordResetEmailContent("Jane", expectedLink, "1 hour");
    }

    @Test
    public void testSendEmailSuccessfully() throws MessagingException {
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        MimeMessage mimeMessage = mock(MimeMessage.class);
        MimeMessageHelper helper = mock(MimeMessageHelper.class);
        doReturn(mimeMessage).when(mailSender).createMimeMessage();
        doNothing().when(mailSender).send(any(MimeMessage.class));
        EmailServiceImpl realEmailService = new EmailServiceImpl(mailSender, templateService);
        ReflectionTestUtils.setField(realEmailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(realEmailService, "baseUrl", "https://example.com");
        User user = new User();
        user.setFirstName("Test");
        user.setEmail(to);
        String token = "test-token";
        doReturn("email content").when(templateService).getVerificationEmailContent(any(), any(), any());
        realEmailService.sendVerificationEmail(user, token);
        verify(mailSender, atLeast(1)).createMimeMessage();
        verify(mailSender, atLeast(1)).send(any(MimeMessage.class));
    }

    @Disabled()
    @Test
    public void testSendEmailThrowsEmailSendException() {
        String to = "test@example.com";
        String subject = "Test Subject";
        String content = "Test Content";
        MimeMessage mimeMessage = mock(MimeMessage.class);
        doReturn(mimeMessage).when(mailSender).createMimeMessage();
        doThrow(new MailSendException("Test exception")).when(mailSender).send(any(MimeMessage.class));
        EmailServiceImpl realEmailService = new EmailServiceImpl(mailSender, templateService);
        ReflectionTestUtils.setField(realEmailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(realEmailService, "baseUrl", "https://example.com");
        User user = new User();
        user.setFirstName("Test");
        user.setEmail(to);
        String token = "test-token";
        doReturn("email content").when(templateService).getVerificationEmailContent(any(), any(), any());
        assertThrows(EmailSendException.class, () -> {
            realEmailService.sendVerificationEmail(user, token);
        });
        verify(mailSender, atLeast(1)).createMimeMessage();
        verify(mailSender, atLeast(1)).send(any(MimeMessage.class));
    }

    @Test
    public void testConstructorCreatesValidInstance() {
        EmailServiceImpl service = new EmailServiceImpl(mailSender, templateService);
        assertThat(service, notNullValue());
    }
}
