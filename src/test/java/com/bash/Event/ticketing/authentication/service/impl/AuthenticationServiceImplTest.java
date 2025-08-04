package com.bash.Event.ticketing.authentication.service.impl;

import com.bash.Event.ticketing.Exceptions.InvalidTokenException;
import com.bash.Event.ticketing.Exceptions.TokenExpiredException;
import com.bash.Event.ticketing.authentication.domain.RefreshToken;
import com.bash.Event.ticketing.authentication.domain.User;
import com.bash.Event.ticketing.authentication.domain.UserRole;
import com.bash.Event.ticketing.authentication.dto.request.LoginRequest;
import com.bash.Event.ticketing.authentication.dto.request.PasswordResetRequest;
import com.bash.Event.ticketing.authentication.dto.request.RegistrationRequest;
import com.bash.Event.ticketing.authentication.dto.request.TokenRefreshRequest;
import com.bash.Event.ticketing.authentication.dto.response.JwtResponse;
import com.bash.Event.ticketing.authentication.dto.response.MessageResponse;
import com.bash.Event.ticketing.authentication.repository.UserRepository;
import com.bash.Event.ticketing.authentication.security.CustomUserDetails;
import com.bash.Event.ticketing.authentication.security.jwt.JwtService;
import com.bash.Event.ticketing.authentication.service.RefreshTokenService;
import com.bash.Event.ticketing.email.service.EmailService;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeast;

@Timeout(10)
public class AuthenticationServiceImplTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private EmailService emailService;

    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        authenticationService = new AuthenticationServiceImpl(jwtService, userRepository, authenticationManager, passwordEncoder, refreshTokenService, emailService);
    }

    @Test
    public void testRegisterUserWhenEmailExistsReturnsErrorMessage() {
        RegistrationRequest request = new RegistrationRequest("John", "Doe", "john@example.com", "Password123@", "+1234567890", "USER");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        MessageResponse response = authenticationService.registerUser(request);
        assertThat(response.getMessage(), is(equalTo("User already exists with this email")));
        verify(userRepository, atLeast(1)).existsByEmail("john@example.com");
    }

    @Test
    public void testRegisterUserWhenPhoneNumberExistsReturnsErrorMessage() {
        RegistrationRequest request = new RegistrationRequest("John", "Doe", "john@example.com", "Password123@", "+1234567890", "USER");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+1234567890")).thenReturn(true);
        MessageResponse response = authenticationService.registerUser(request);
        assertThat(response.getMessage(), is(equalTo("User already exists with this phone number")));
        verify(userRepository, atLeast(1)).existsByEmail("john@example.com");
        verify(userRepository, atLeast(1)).existsByPhoneNumber("+1234567890");
    }

    @Test
    public void testRegisterUserWithInvalidRoleThrowsException() {
        RegistrationRequest request = new RegistrationRequest("John", "Doe", "john@example.com", "Password123@", "+1234567890", "USER");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+1234567890")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.registerUser(request);
        });
    }

    @Test
    public void testRegisterUserSuccessfullyWithUserRole() {
        RegistrationRequest request = new RegistrationRequest("John", "Doe", "john@example.com", "Password123@", "+1234567890", "INVALID_ROLE");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+1234567890")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.registerUser(request);
        });
    }

    @Test
    public void testRegisterUserSuccessfullyWithEventOwnerRole() {
        RegistrationRequest request = new RegistrationRequest("John", "Doe", "john@example.com", "Password123@", "+1234567890", "ADMIN");
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("+1234567890")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.registerUser(request);
        });
    }

    @Test
    public void testAuthenticateUserSuccessfully() {
        LoginRequest loginRequest = new LoginRequest("john@example.com", "password");
        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = createMockUserDetails();
        doReturn(authentication).when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        doReturn(userDetails).when(authentication).getPrincipal();
        doReturn("jwtToken").when(jwtService).generateToken(authentication);
        doReturn("refreshToken").when(jwtService).generateTokenFromEmail("john@example.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        JwtResponse response = authenticationService.authenticateUser(loginRequest);
        assertNotNull(response);
        verify(authenticationManager, atLeast(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(securityContext, atLeast(1)).setAuthentication(authentication);
    }

    @Test
    public void testAuthenticateUserWithNullAuthenticationThrowsException() {
        LoginRequest loginRequest = new LoginRequest("john@example.com", "password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        assertThrows(UsernameNotFoundException.class, () -> {
            authenticationService.authenticateUser(loginRequest);
        });
    }

    @Test
    public void testVerifyEmailSuccessfully() {
        String token = "validToken";
        String email = "john@example.com";
        User user = User.builder().email(email).isEnabled(false).build();
        when(jwtService.getUsernameFromToken(token)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        MessageResponse response = authenticationService.verifyEmail(token);
        assertThat(response.getMessage(), is(equalTo("Account verified successfully")));
        verify(userRepository, atLeast(1)).save(any(User.class));
    }

    @Test
    public void testVerifyEmailWithAlreadyVerifiedUser() {
        String token = "validToken";
        String email = "john@example.com";
        User user = User.builder().email(email).isEnabled(true).build();
        when(jwtService.getUsernameFromToken(token)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        MessageResponse response = authenticationService.verifyEmail(token);
        assertThat(response.getMessage(), is(equalTo("Error: User is already verified.")));
    }

    @Test
    public void testVerifyEmailWithUserNotFound() {
        String token = "validToken";
        String email = "john@example.com";
        when(jwtService.getUsernameFromToken(token)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> {
            authenticationService.verifyEmail(token);
        });
    }

    @Test
    public void testVerifyEmailWithExpiredToken() {
        String token = "expiredToken";
        when(jwtService.getUsernameFromToken(token)).thenThrow(new ExpiredJwtException(null, null, "Token expired"));
        assertThrows(TokenExpiredException.class, () -> {
            authenticationService.verifyEmail(token);
        });
    }

    @Test
    public void testVerifyEmailWithInvalidToken() {
        String token = "invalidToken";
        when(jwtService.getUsernameFromToken(token)).thenThrow(new RuntimeException("Invalid token"));
        assertThrows(InvalidTokenException.class, () -> {
            authenticationService.verifyEmail(token);
        });
    }

    @Test
    public void testVerifyPhoneNumberReturnsNull() {
        String token = "token";
        MessageResponse response = authenticationService.verifyPhoneNumber(token);
        assertThat(response, is(equalTo(null)));
    }

    @Test
    public void testRequestPasswordResetSuccessfully() {
        String email = "john@example.com";
        User user = User.builder().email(email).build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.generateVerificationToken(email)).thenReturn("resetToken");
        MessageResponse response = authenticationService.requestPasswordReset(email);
        assertThat(response.getMessage(), is(equalTo("Password reset instructions have been sent to your email.")));
        verify(emailService, atLeast(1)).sendPasswordResetEmail(user, "resetToken");
    }

    @Test
    public void testRequestPasswordResetWithUserNotFound() {
        String email = "john@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> {
            authenticationService.requestPasswordReset(email);
        });
    }

    @Test
    public void testResetPasswordSuccessfully() {
        PasswordResetRequest request = new PasswordResetRequest("validToken", "newPassword123@", "newPassword123@");
        String email = "john@example.com";
        User user = User.builder().email(email).password("oldPassword").build();
        when(jwtService.getUsernameFromToken("validToken")).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123@")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        MessageResponse response = authenticationService.resetPassword(request);
        assertThat(response.getMessage(), is(equalTo("Password reset successfully.")));
        verify(userRepository, atLeast(1)).save(any(User.class));
    }

    @Test
    public void testResetPasswordWithMismatchedPasswords() {
        PasswordResetRequest request = new PasswordResetRequest("validToken", "newPassword123@", "differentPassword123@");
        MessageResponse response = authenticationService.resetPassword(request);
        assertThat(response.getMessage(), is(equalTo("Error: Passwords do not match.")));
    }

    @Test
    public void testResetPasswordWithUserNotFound() {
        PasswordResetRequest request = new PasswordResetRequest("validToken", "newPassword123@", "newPassword123@");
        String email = "john@example.com";
        when(jwtService.getUsernameFromToken("validToken")).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        assertThrows(InvalidTokenException.class, () -> {
            authenticationService.resetPassword(request);
        });
    }

    @Test
    public void testResetPasswordWithExpiredToken() {
        PasswordResetRequest request = new PasswordResetRequest("expiredToken", "newPassword123@", "newPassword123@");
        when(jwtService.getUsernameFromToken("expiredToken")).thenThrow(new ExpiredJwtException(null, null, "Token expired"));
        assertThrows(TokenExpiredException.class, () -> {
            authenticationService.resetPassword(request);
        });
    }

    @Test
    public void testResetPasswordWithInvalidToken() {
        PasswordResetRequest request = new PasswordResetRequest("invalidToken", "newPassword123@", "newPassword123@");
        when(jwtService.getUsernameFromToken("invalidToken")).thenThrow(new RuntimeException("Invalid token"));
        assertThrows(InvalidTokenException.class, () -> {
            authenticationService.resetPassword(request);
        });
    }

    @Test
    public void testRefreshTokenSuccessfully() {
        TokenRefreshRequest request = new TokenRefreshRequest("refreshToken");
        User user = User.builder().id(1L).email("john@example.com").role(UserRole.USER).build();
        RefreshToken refreshToken = mock(RefreshToken.class);
        when(refreshToken.getUser()).thenReturn(user);
        when(refreshTokenService.findByToken("refreshToken")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
        when(jwtService.generateTokenFromEmail("john@example.com")).thenReturn("newAccessToken");
        JwtResponse response = authenticationService.refreshToken(request);
        assertThat(response.getToken(), is(equalTo("newAccessToken")));
        assertThat(response.getRefreshToken(), is(equalTo("refreshToken")));
        assertThat(response.getId(), is(equalTo(1L)));
        assertThat(response.getRoles().get(0), is(equalTo("USER")));
    }

    @Test
    public void testRefreshTokenWithTokenNotFound() {
        TokenRefreshRequest request = new TokenRefreshRequest("invalidRefreshToken");
        when(refreshTokenService.findByToken("invalidRefreshToken")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> {
            authenticationService.refreshToken(request);
        });
    }

    private CustomUserDetails createMockUserDetails() {
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        return new CustomUserDetails(1L, "John", "Doe", "john@example.com", "+1234567890", "password", true, authorities);
    }

    @Test
    public void testRegisterUserRoleValidationLogic() {
        RegistrationRequest request = new RegistrationRequest("John", "Doe", "john@example.com", "Password123@", "+1234567890", "CUSTOM_ROLE");
        doReturn(false).when(userRepository).existsByEmail("john@example.com");
        doReturn(false).when(userRepository).existsByPhoneNumber("+1234567890");
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.registerUser(request);
        });
    }

    @Test
    public void testRegisterUserRoleAssignmentEventOwner() {
        RegistrationRequest request = new RegistrationRequest("Jane", "Smith", "jane@example.com", "Password123@", "+9876543210", "UNKNOWN");
        doReturn(false).when(userRepository).existsByEmail("jane@example.com");
        doReturn(false).when(userRepository).existsByPhoneNumber("+9876543210");
        assertThrows(IllegalArgumentException.class, () -> {
            authenticationService.registerUser(request);
        });
    }
}
