package com.bash.Event.ticketing.authentication.security.jwt;

import com.bash.Event.ticketing.authentication.security.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private String jwtExpirationMs;

    @Value("${app.jwt.issuer}")
    private String jwtIssuer;

    private Key key;

    @PostConstruct
    public void init() {
        try {
            // Try to decode as Base64 first
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
            key = Keys.hmacShaKeyFor(keyBytes);
            log.info("JWT secret key initialized from Base64");
        } catch (Exception e) {
            // If Base64 decoding fails, use the string directly
            log.warn("Base64 decoding failed, using secret as plain string: {}", e.getMessage());
            key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            log.info("JWT secret key initialized from plain string");
        }
    }

    public String generateToken(Authentication authentication){
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();


        Map<String, Object> claims = new HashMap<>();

        claims.put("purpose" ,"authentication");

        String tokenId = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(tokenId)
                .setClaims(claims)
                .setSubject(customUserDetails.getUsername())
                .setIssuer(jwtIssuer)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(jwtExpirationMs)))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateTokenFromEmail(String email){
        Map<String, Object> claims = new HashMap<>();
        claims.put("purpose", "refresh-token");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuer(jwtIssuer)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(jwtExpirationMs)))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromToken(String token){
        return Jwts.parser()
               .setSigningKey(key)
               .build()
               .parseClaimsJws(token)
               .getBody()
               .getSubject();
    }

    public String generateVerificationToken(String email) {

        String tokenId = UUID.randomUUID().toString();

        Map<String, Object> claims = Map.of(
                "email", email,
                "purpose", "verification"
        );

        return Jwts.builder()
                .setId(tokenId)
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(jwtExpirationMs)))
                .signWith(key(), SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token){
        try {
            log.debug("Validating JWT token with length: {}", token.length());
            Jwts.parser()
                    .setSigningKey(key())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration()
                    .after(new Date());

            return checkAccess(token);

        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation: {}", e.getMessage(), e);
        }

        return false;
    }

    private Key key() {
        // Use the same key initialization logic as in init()
        return this.key;
    }
    
    
    protected boolean checkAccess(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("purpose", String.class).equals("authentication");
    }

}
