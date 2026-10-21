package com.ecommerce.security;

import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Service for generating, parsing, and validating signed JSON Web Tokens (JWT).
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:900000}")
    private long jwtExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.trim().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT signing secret must be configured and at least 256 bits (32 bytes) long.");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.trim().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT access token for an authenticated user.
     *
     * @param user user details
     * @return signed compact JWT string
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtExpirationMs);

        return Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates a JWT string for signature integrity and expiration.
     *
     * @param token compact JWT string
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        } catch (JwtException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extracts all claims from a verified token.
     *
     * @param token verified JWT string
     * @return Claims payload
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUserIdFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    public String getEmailFromToken(String token) {
        return getClaimsFromToken(token).get("email", String.class);
    }

    public UserRole getRoleFromToken(String token) {
        String roleName = getClaimsFromToken(token).get("role", String.class);
        return UserRole.valueOf(roleName);
    }

    public long getExpirationDurationSeconds() {
        return jwtExpirationMs / 1000;
    }
}

