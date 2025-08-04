package com.bash.Event.ticketing.authentication.controller;


import com.bash.Event.ticketing.authentication.dto.request.LoginRequest;
import com.bash.Event.ticketing.authentication.dto.request.PasswordResetRequest;
import com.bash.Event.ticketing.authentication.dto.request.RegistrationRequest;
import com.bash.Event.ticketing.authentication.dto.request.TokenRefreshRequest;
import com.bash.Event.ticketing.authentication.dto.response.JwtResponse;
import com.bash.Event.ticketing.authentication.dto.response.MessageResponse;
import com.bash.Event.ticketing.authentication.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@Component("authenticationController")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> registerUser(
            @Valid @RequestBody RegistrationRequest registrationRequest) {
        MessageResponse messageResponse = authenticationService.registerUser(registrationRequest);
        return ResponseEntity.ok(messageResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> authenticateUser(
            @Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authenticationService.authenticateUser(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }


    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam("token") String token) {
        MessageResponse messageResponse = authenticationService.verifyEmail(token);
        return ResponseEntity.ok(messageResponse);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestParam("email") String email) {
        MessageResponse messageResponse = authenticationService.requestPasswordReset(email);
        return ResponseEntity.ok(messageResponse);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody PasswordResetRequest passwordResetRequest
    ) {
        MessageResponse messageResponse = authenticationService.resetPassword(passwordResetRequest);
        return ResponseEntity.ok(messageResponse);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<JwtResponse> refreshToken(
            @RequestParam TokenRefreshRequest refreshTokenRequest
    ) {
        JwtResponse jwtResponse = authenticationService.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(jwtResponse);
    }
}
