package com.ecommerce.security;

import com.ecommerce.model.User;
import com.ecommerce.model.UserRole;

/**
 * Lightweight principal representing an authenticated user in the security context.
 *
 * @param id    User's unique database ID
 * @param email User's normalized email address
 * @param role  User's authorization role
 */
public record UserPrincipal(
        String id,
        String email,
        UserRole role
) {
    public static UserPrincipal fromUser(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getRole());
    }
}

