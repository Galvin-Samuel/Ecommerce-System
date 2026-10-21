package com.ecommerce.dto;

import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

/**
 * Safe public representation of a user, omitting sensitive fields such as password hashes.
 *
 * @param id        User identifier
 * @param name      Display name
 * @param email     Email address
 * @param role      Assigned user role
 * @param createdAt Account creation timestamp in ISO-8601 UTC
 */
public record UserResponse(
        String id,
        String name,
        String email,
        UserRole role,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
        Instant createdAt
) {
    /**
     * Factory method creating a safe UserResponse from the User persistence model.
     *
     * @param user user entity
     * @return safe UserResponse
     */
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

