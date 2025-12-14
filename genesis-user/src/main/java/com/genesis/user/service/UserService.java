package com.genesis.user.service;

import com.genesis.user.dto.SignupRequest;
import com.genesis.user.dto.UserResponse;
import com.genesis.user.entity.AuthProvider;
import com.genesis.user.entity.User;
import com.genesis.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user-related operations.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a new user from signup request.
     *
     * @param request the signup request
     * @return the created user response
     * @throws IllegalArgumentException if username or email already exists
     */
    public UserResponse createUser(SignupRequest request) {
        // Check for duplicate username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Create new user entity
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setOrganizationName(request.getOrganizationName());
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setAccountLocked(false);

        // Save and return response
        User savedUser = userRepository.save(user);
        return UserResponse.fromEntity(savedUser);
    }

    /**
     * Find user by username or email.
     *
     * @param usernameOrEmail username or email to search
     * @return optional containing the user if found
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
    }

    /**
     * Update the last login timestamp for a user.
     *
     * @param user the user to update
     */
    public void updateLastLogin(User user) {
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
    }

    /**
     * Check if username exists.
     */
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if email exists.
     */
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
