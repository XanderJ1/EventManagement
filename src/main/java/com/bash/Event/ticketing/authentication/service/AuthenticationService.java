package com.bash.Event.ticketing.authentication.service;

import com.bash.Event.ticketing.authentication.dto.request.LoginRequest;
import com.bash.Event.ticketing.authentication.dto.request.PasswordResetRequest;
import com.bash.Event.ticketing.authentication.dto.request.RegistrationRequest;
import com.bash.Event.ticketing.authentication.dto.request.TokenRefreshRequest;
import com.bash.Event.ticketing.authentication.dto.response.JwtResponse;
import com.bash.Event.ticketing.authentication.dto.response.MessageResponse;


public interface AuthenticationService {

    MessageResponse registerUser(RegistrationRequest request);

    JwtResponse authenticateUser(LoginRequest request);

    MessageResponse verifyEmail(String token);

    MessageResponse verifyPhoneNumber(String token);

    MessageResponse requestPasswordReset(String email);


    MessageResponse resetPassword(PasswordResetRequest request);

    JwtResponse refreshToken(TokenRefreshRequest request);
}
