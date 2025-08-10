package com.bash.Event.ticketing.authentication.service.impl;


import com.bash.Event.ticketing.Exceptions.InvalidTokenException;
import com.bash.Event.ticketing.Exceptions.TokenExpiredException;
import com.bash.Event.ticketing.Exceptions.TokenRefreshException;
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
import com.bash.Event.ticketing.authentication.service.AuthenticationService;
import com.bash.Event.ticketing.authentication.service.RefreshTokenService;
import com.bash.Event.ticketing.authentication.util.ResourceMapper;
import com.bash.Event.ticketing.email.service.EmailService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    @Override
    @Transactional
    public MessageResponse registerUser(RegistrationRequest request) {
        // Check if the user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            return new MessageResponse("User already exists with this email");
        }

        // Check if the phone number already exists
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            return new MessageResponse("User already exists with this phone number");
        }
        System.out.println(request.getRole());

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.valueOf(request.getRole()))
                .isEnabled(false)
                .build();

        // Save the user to the database
        userRepository.save(user);

        // Generate verification token
        String verificationToken = jwtService.generateVerificationToken(user.getEmail());

        // Send verification email
        emailService.sendVerificationEmail(user, verificationToken);

        return new MessageResponse("User registered successfully! Please check your email to verify your account.");
    }

    @Override
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
               new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
        log.info("I am done authenticating the user");

        if (authentication == null) {
            throw new UsernameNotFoundException("User not found with email/username: " + loginRequest.getEmail());
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String jwt = jwtService.generateToken(authentication);
        String refreshToken = jwtService.generateTokenFromEmail(userDetails.getUsername());

        return ResourceMapper.toJwtResponse(userDetails, jwt, refreshToken);
    }

    @Override
    public MessageResponse verifyEmail(String token) {
        try {
            String email = jwtService.getUsernameFromToken(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Error: User is not found."));

            if(user.isEnabled()) {
                return new MessageResponse("Error: User is already verified.");
            }
            user.setEnabled(true);
            userRepository.save(user);

            return new MessageResponse("Account verified successfully");
        }
        catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Verification token has expired");
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid verification token");
        }
    }

    @Override
    public MessageResponse verifyPhoneNumber(String token) {
        return null;
    }

    @Override
    public MessageResponse requestPasswordReset(String email) {
        // Check if the user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
                
        // Generate a password reset token
        String passwordResetToken = jwtService.generateVerificationToken(user.getEmail());
        
        // Send password reset email
        emailService.sendPasswordResetEmail(user, passwordResetToken);
        
        return new MessageResponse("Password reset instructions have been sent to your email.");
    }

    @Transactional
    public MessageResponse resetPassword(PasswordResetRequest passwordResetRequest) {

        // Check if password and confirm matches.
        if(!passwordResetRequest.newPassword().equals(passwordResetRequest.confirmPassword())  ) {
            return new MessageResponse("Error: Passwords do not match.");
        }

        try {
            String email = jwtService.getUsernameFromToken(passwordResetRequest.token());
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email/username: "+email));

            user.setPassword(passwordEncoder.encode(passwordResetRequest.newPassword()));
            userRepository.save(user);

            return new MessageResponse("Password reset successfully.");
        }
        catch (ExpiredJwtException e) {
            throw new TokenExpiredException("Password reset token has expired");
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid password reset token");
        }
    }

    @Override
    public JwtResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtService.generateTokenFromEmail(user.getEmail());
                    return JwtResponse.builder()
                            .token(accessToken)
                            .refreshToken(refreshToken)
                            .id(user.getId())
                            .roles(List.of(user.getRole().toString()))
                            .build();
                }).orElseThrow(() -> {
                    log.error("Refresh token is not in database!");
                    return new TokenRefreshException(request.toString(), "Refresh token is not in the database!");
                });
    }

}
