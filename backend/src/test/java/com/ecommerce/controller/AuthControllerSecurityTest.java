package com.ecommerce.controller;

import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.dto.UserResponse;
import com.ecommerce.exception.ConflictException;
import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.ecommerce.security.JwtTokenProvider;
import com.ecommerce.security.UserPrincipal;
import com.ecommerce.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/register with valid payload returns HTTP 201 Created and safe response")
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest("Bob Builder", "bob@example.com", "securePassword123");
        UserResponse mockResponse = new UserResponse(
                "user-bob-1",
                "Bob Builder",
                "bob@example.com",
                UserRole.CUSTOMER,
                Instant.now()
        );

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("User registered successfully")))
                .andExpect(jsonPath("$.data.id", is("user-bob-1")))
                .andExpect(jsonPath("$.data.email", is("bob@example.com")))
                .andExpect(jsonPath("$.data.role", is("CUSTOMER")))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with invalid data returns HTTP 400 with validation errors")
    void shouldRejectInvalidRegistration() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "not-an-email", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.validationErrors", hasSize(3)));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with duplicate email returns HTTP 409 Conflict")
    void shouldRejectDuplicateEmailRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest("Existing User", "existing@example.com", "validPass123");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new ConflictException("User with email 'existing@example.com' already exists."));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.error", is("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with valid credentials returns HTTP 200 OK and JWT token")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("bob@example.com", "securePassword123");
        UserResponse userResponse = new UserResponse(
                "user-bob-1",
                "Bob Builder",
                "bob@example.com",
                UserRole.CUSTOMER,
                Instant.now()
        );
        AuthResponse authResponse = AuthResponse.of(userResponse, "mocked.jwt.token", 900);

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", is("mocked.jwt.token")))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.expiresIn", is(900)))
                .andExpect(jsonPath("$.data.user.email", is("bob@example.com")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with invalid credentials returns HTTP 401 Unauthorized")
    void shouldRejectInvalidLoginCredentials() throws Exception {
        LoginRequest request = new LoginRequest("bob@example.com", "wrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password."));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Invalid email or password.")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with valid Bearer token returns HTTP 200 OK and profile")
    void shouldReturnProfileWithValidJwt() throws Exception {
        User testUser = new User("user-bob-1", "Bob Builder", "bob@example.com", "hash", UserRole.CUSTOMER, Instant.now(), Instant.now());
        String validToken = jwtTokenProvider.generateToken(testUser);

        UserResponse userResponse = new UserResponse(
                "user-bob-1",
                "Bob Builder",
                "bob@example.com",
                UserRole.CUSTOMER,
                Instant.now()
        );

        when(authService.getCurrentUser(any(UserPrincipal.class))).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is("user-bob-1")))
                .andExpect(jsonPath("$.data.email", is("bob@example.com")))
                .andExpect(jsonPath("$.data.role", is("CUSTOMER")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me without token returns HTTP 401 via JwtAuthenticationEntryPoint")
    void shouldRejectUnauthenticatedRequestToMe() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.path", is("/api/v1/auth/me")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with invalid token returns HTTP 401 Unauthorized")
    void shouldRejectInvalidJwtToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer completelyInvalidToken12345"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("Public health endpoints remain accessible without authentication")
    void shouldAllowUnauthenticatedAccessToHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}

