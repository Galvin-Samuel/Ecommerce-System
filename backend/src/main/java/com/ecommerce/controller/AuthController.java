package com.ecommerce.controller;

import com.ecommerce.dto.ApiResponse;
import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.UserResponse;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing user registration, login authentication,
 * and current user profile verification endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user account with default customer role.
     *
     * @param request validated registration payload
     * @return 201 Created with safe user profile
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully.", response));
    }

    /**
     * Authenticates user credentials and issues a signed JWT access token.
     *
     * @param request validated login credentials
     * @return 200 OK with safe user details and JWT access token
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity
                .ok(ApiResponse.success("Authentication successful.", response));
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     * Protected endpoint requiring a valid Bearer JWT access token.
     *
     * @param principal authenticated user principal injected from SecurityContext
     * @return 200 OK with authenticated user profile
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = authService.getCurrentUser(principal);
        return ResponseEntity
                .ok(ApiResponse.success("Current user profile retrieved successfully.", response));
    }
}

