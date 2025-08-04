package com.bash.Event.ticketing.authentication.util;

import com.bash.Event.ticketing.authentication.domain.User;
import com.bash.Event.ticketing.authentication.domain.UserRole;
import com.bash.Event.ticketing.authentication.dto.request.RegistrationRequest;
import com.bash.Event.ticketing.authentication.dto.response.JwtResponse;
import com.bash.Event.ticketing.authentication.security.CustomUserDetails;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceMapper {

    public static User toUser(RegistrationRequest request, String encodedPassword) {

        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(encodedPassword)
                .role(UserRole.valueOf(request.getRole()))
                .isEnabled(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a JwtResponse from user details and tokens
     */
    public static JwtResponse toJwtResponse(CustomUserDetails userDetails, String jwt, String refreshToken) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return JwtResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken)
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .roles(roles)
                .build();
    }
}
