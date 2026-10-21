package com.ecommerce.repository;

import com.ecommerce.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for User entity persistence operations.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * Finds a user by their unique email address.
     *
     * @param email user's email address
     * @return Optional containing the User if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user already exists with the given email address.
     *
     * @param email email address to check
     * @return true if an account exists with the email, false otherwise
     */
    boolean existsByEmail(String email);
}

