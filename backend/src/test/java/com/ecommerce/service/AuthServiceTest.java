package com.ecommerce.service;

import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.UserResponse;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.JwtTokenProvider;
import com.ecommerce.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(
                "user-abc",
                "Alice Wonder",
                "alice@example.com",
                "$2a$10$hashedPasswordSample",
                UserRole.CUSTOMER,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Registration succeeds with normalized email, hashed password, and default CUSTOMER role")
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = new RegisterRequest(
                "  Alice Wonder  ",
                "  Alice@Example.COM  ",
                "securePassword123"
        );

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("$2a$10$hashedPasswordSample");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("user-abc");
        assertThat(response.name()).isEqualTo("Alice Wonder");
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo(UserRole.CUSTOMER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getName()).isEqualTo("Alice Wonder");
        assertThat(savedUser.getEmail()).isEqualTo("alice@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$10$hashedPasswordSample");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    @DisplayName("Registration throws ConflictException when email is already registered")
    void shouldThrowConflictWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "Alice",
                "alice@example.com",
                "password123"
        );

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registration throws ConflictException on concurrent duplicate key error")
    void shouldThrowConflictOnDuplicateKeyException() {
        RegisterRequest request = new RegisterRequest(
                "Alice",
                "alice@example.com",
                "password123"
        );

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("E11000 duplicate key error"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Login succeeds with valid credentials and returns AuthResponse with token")
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest("ALICE@example.COM", "password123");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", sampleUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(sampleUser)).thenReturn("sample.jwt.token");
        when(jwtTokenProvider.getExpirationDurationSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("sample.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThat(response.user().email()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("Login throws BadCredentialsException when email is not found")
    void shouldThrowBadCredentialsWhenUserNotFound() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password123");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    @DisplayName("Login throws BadCredentialsException when password does not match")
    void shouldThrowBadCredentialsWhenPasswordMismatch() {
        LoginRequest request = new LoginRequest("alice@example.com", "wrongPassword");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", sampleUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    @DisplayName("getCurrentUser returns safe profile for authenticated principal")
    void shouldReturnCurrentUserProfile() {
        UserPrincipal principal = new UserPrincipal("user-abc", "alice@example.com", UserRole.CUSTOMER);

        when(userRepository.findById("user-abc")).thenReturn(Optional.of(sampleUser));

        UserResponse response = authService.getCurrentUser(principal);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("user-abc");
        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("getCurrentUser throws ResourceNotFoundException if user record is missing")
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        UserPrincipal principal = new UserPrincipal("missing-id", "missing@example.com", UserRole.CUSTOMER);

        when(userRepository.findById("missing-id")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

