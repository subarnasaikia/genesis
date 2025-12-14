package com.genesis.user.repository;

import com.genesis.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email.
     * Used for login where user can provide either username or email.
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Check if username already exists.
     */
    boolean existsByUsername(String username);

    /**
     * Check if email already exists.
     */
    boolean existsByEmail(String email);
}
