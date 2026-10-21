package com.ecommerce.security;

import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String TEST_SECRET = "superSecretKeyForJwtTokenGenerationMustBeAtLeast256BitsLong!";
    private static final long TEST_EXPIRATION_MS = 900000; // 15 mins

    private JwtTokenProvider tokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationMs", TEST_EXPIRATION_MS);
        tokenProvider.init();

        testUser = new User(
                "user-123",
                "Alice Smith",
                "alice@example.com",
                "hashedPassword",
                UserRole.CUSTOMER,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should generate valid token containing expected claims")
    void shouldGenerateValidToken() {
        String token = tokenProvider.generateToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo("user-123");
        assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo("alice@example.com");
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo(UserRole.CUSTOMER);
        assertThat(tokenProvider.getExpirationDurationSeconds()).isEqualTo(900);
    }

    @Test
    @DisplayName("Should fail validation on invalid signature")
    void shouldRejectTokenWithDifferentSignature() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "anotherVerySecretKeyThatIsCompletelyDifferentFromTestKey123!".getBytes(StandardCharsets.UTF_8)
        );

        String forgedToken = Jwts.builder()
                .subject("attacker-id")
                .claim("email", "attacker@example.com")
                .claim("role", "ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(otherKey)
                .compact();

        assertThat(tokenProvider.validateToken(forgedToken)).isFalse();
    }

    @Test
    @DisplayName("Should fail validation on expired token")
    void shouldRejectExpiredToken() {
        SecretKey signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("user-123")
                .claim("email", "alice@example.com")
                .claim("role", "CUSTOMER")
                .issuedAt(new Date(System.currentTimeMillis() - 120000))
                .expiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(signingKey)
                .compact();

        assertThat(tokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Should fail validation on malformed token")
    void shouldRejectMalformedToken() {
        assertThat(tokenProvider.validateToken("not-a-valid-jwt-token")).isFalse();
        assertThat(tokenProvider.validateToken("")).isFalse();
        assertThat(tokenProvider.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("Should throw IllegalStateException if configured secret is shorter than 256 bits")
    void shouldThrowExceptionWhenSecretIsTooShort() {
        JwtTokenProvider shortSecretProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortSecretProvider, "jwtSecret", "too-short-secret");
        ReflectionTestUtils.setField(shortSecretProvider, "jwtExpirationMs", 60000L);

        assertThatThrownBy(shortSecretProvider::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 256 bits");
    }
}

