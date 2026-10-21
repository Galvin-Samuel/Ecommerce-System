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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Service managing user authentication, registration, credential validation,
 * and profile retrieval.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Registers a new user account with default CUSTOMER role.
     *
     * @param request registration details
     * @return safe UserResponse without sensitive credentials
     * @throws ConflictException if the email address is already registered
     */
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String normalizedName = request.name().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration rejected: email '{}' is already in use", normalizedEmail);
            throw new ConflictException("User with email '" + normalizedEmail + "' already exists.");
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = new User(
                normalizedName,
                normalizedEmail,
                encodedPassword,
                UserRole.CUSTOMER
        );

        try {
            User savedUser = userRepository.save(user);
            log.info("User registered successfully with ID: {}", savedUser.getId());
            return UserResponse.fromEntity(savedUser);
        } catch (DuplicateKeyException ex) {
            log.warn("Concurrent registration collision for email '{}': {}", normalizedEmail, ex.getMessage());
            throw new ConflictException("User with email '" + normalizedEmail + "' already exists.");
        }
    }

    /**
     * Authenticates user credentials and issues a signed JWT access token.
     *
     * @param request login credentials
     * @return AuthResponse containing safe user details and JWT access token
     * @throws BadCredentialsException if the email or password is invalid
     */
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login failed: account not found for email '{}'", normalizedEmail);
                    return new BadCredentialsException("Invalid email or password.");
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password for email '{}'", normalizedEmail);
            throw new BadCredentialsException("Invalid email or password.");
        }

        String token = jwtTokenProvider.generateToken(user);
        long expiresIn = jwtTokenProvider.getExpirationDurationSeconds();

        log.info("User '{}' authenticated successfully", user.getId());
        return AuthResponse.of(UserResponse.fromEntity(user), token, expiresIn);
    }

    /**
     * Retrieves the profile of the currently authenticated principal.
     *
     * @param principal authenticated user principal
     * @return safe UserResponse
     * @throws ResourceNotFoundException if the user profile cannot be located
     */
    public UserResponse getCurrentUser(UserPrincipal principal) {
        if (principal == null || principal.id() == null) {
            throw new BadCredentialsException("No valid authentication principal found.");
        }

        User user = userRepository.findById(principal.id())
                .or(() -> userRepository.findByEmail(principal.email()))
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user profile could not be found."));

        return UserResponse.fromEntity(user);
    }
}

