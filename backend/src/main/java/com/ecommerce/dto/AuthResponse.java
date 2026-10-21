package com.ecommerce.dto;

/**
 * Response payload returned upon successful login authentication.
 *
 * @param user        Safe public user details
 * @param accessToken Signed JWT access token for authenticating subsequent requests
 * @param tokenType   Authentication scheme (e.g., "Bearer")
 * @param expiresIn   Token validity duration in seconds
 */
public record AuthResponse(
        UserResponse user,
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public static AuthResponse of(UserResponse user, String accessToken, long expiresIn) {
        return new AuthResponse(user, accessToken, "Bearer", expiresIn);
    }
}

