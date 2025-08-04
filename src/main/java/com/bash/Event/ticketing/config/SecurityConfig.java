package com.bash.Event.ticketing.config;

import com.bash.Event.ticketing.authentication.security.CustomUserDetailsService;
import com.bash.Event.ticketing.authentication.security.jwt.JwtAuthenticationEntryPoint;
import com.bash.Event.ticketing.authentication.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static com.bash.Event.ticketing.config.SecurityEndpoints.*;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {


    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;



    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(){
        return new ProviderManager(authenticationProvider());
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth ->
                        auth
                                // Public endpoints - no authentication required
                                .requestMatchers("/api/v1/auth/**").permitAll()
                                .requestMatchers("/api/v1/sse/**").permitAll()
                                .requestMatchers(
                                        "/swagger-ui.html",
                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/api-docs/**").permitAll()
                                .requestMatchers("/", "/index.html", "/admin.html", "/home.html", 
                                                "/static/**", "/css/**", "/js/**", "/images/**",
                                                "/favicon.ico", "/error", "/webjars/**").permitAll()
                                
                                // AUTHENTICATED ENDPOINTS FIRST (most specific)
                                .requestMatchers("/api/v1/events/my-events").authenticated()
                                .requestMatchers("/api/v1/events/dashboard/**").authenticated()
                                
                                // Event management - authenticated users only
                                .requestMatchers(HttpMethod.POST, "/api/v1/events").authenticated()
                                .requestMatchers(HttpMethod.PUT, "/api/v1/events/**").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/events/**").authenticated()
                                
                                // Ticket management - authenticated users only
                                .requestMatchers(HttpMethod.POST, "/api/v1/events/*/tickets").authenticated()
                                .requestMatchers("/api/v1/tickets/*/scan").authenticated()
                                
                                // PUBLIC EVENT ENDPOINTS (after authenticated ones)
                                .requestMatchers(HttpMethod.GET, "/api/v1/events").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/events/*/tickets").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/events/*").permitAll()
                                
                                // Public ticket purchase
                                .requestMatchers("/api/v1/events/*/tickets/*/purchase").permitAll()
                                
                                // Admin endpoints
                                .requestMatchers("/api/admin").hasRole("ADMIN")
                                .requestMatchers("/api/user").hasAnyRole("USER", "ADMIN")
                                
                                .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .authenticationManager(authenticationManager())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With",
            "Accept", "Origin", "Access-Control-Request-Method",
            "Access-Control-Request-Headers", "X-Auth-Token"
        ));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Auth-Token"));
        configuration.setMaxAge(3600L); // 1 hour
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
