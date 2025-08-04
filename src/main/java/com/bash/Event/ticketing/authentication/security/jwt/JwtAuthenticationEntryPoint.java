package com.bash.Event.ticketing.authentication.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Authentication Entry Point implementation.
 * This class handles the authentication entry point for JWT-based authentication
 * in a Spring Security configuration by implementing the AuthenticationEntryPoint interface.

 * When an unauthenticated user tries to access a secured resource and authentication fails,
 * this entry point is triggered to commence the authentication scheme appropriate to the resource.
 * It typically responds with a 401 Unauthorized status code and may include additional
 * information about the authentication failure.

 * The @Component annotation registers this class as a Spring bean to be managed by the Spring container.
 * The @Slf4j annotation provides a logger field named 'log' for logging purposes.
 */

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    // Inject ObjectMapper instead of creating a new one each time
    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        try {
            // Log detailed error information
            logger.error("Authentication failed for path: {}", request.getServletPath());
            logger.error("Authentication error: {}", authException.getMessage());

            // Log the entire exception stack trace for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Authentication exception details:", authException.getMessage());
            }

            // Get the actual cause if available
            Throwable cause = authException.getCause();
            if (cause != null) {
                logger.error("Root cause: {}", cause.getMessage());
            }

            // Set proper response headers
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            // Create error response body
            final Map<String, Object> body = new HashMap<>();
            body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
            body.put("error", "Unauthorized");
            body.put("message", authException.getMessage() != null ?
                    authException.getMessage() : "Authentication failed");
            body.put("path", request.getServletPath());
            body.put("timestamp", System.currentTimeMillis());

            objectMapper.writeValue(response.getOutputStream(), body);

        } catch (Exception e) {
            logger.error("Error during authentication failure handling", e.getMessage());


            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("Authentication failed");
        }
    }
}