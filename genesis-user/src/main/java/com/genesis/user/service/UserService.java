package com.genesis.user.service;

import com.genesis.common.exception.UnauthorizedException;
import com.genesis.common.exception.ValidationException;
import com.genesis.user.dto.SignupRequest;
import com.genesis.user.dto.UserResponse;
import com.genesis.user.entity.AuthProvider;
import com.genesis.user.entity.User;
import com.genesis.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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
     * @throws ValidationException if the provided credentials cannot be registered
     */
    public UserResponse createUser(SignupRequest request) {
        // Check for duplicate username and duplicate email. We deliberately fold both
        // outcomes into a single generic error message so the public signup endpoint
        // cannot be used to enumerate registered usernames or email addresses.
        boolean usernameTaken = userRepository.existsByUsername(request.getUsername());
        boolean emailTaken = userRepository.existsByEmail(request.getEmail());
        if (usernameTaken || emailTaken) {
            throw new ValidationException("Unable to register with the provided credentials");
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
     * Resolve the unique id of a user given their authenticated username.
     *
     * <p>This is a pure-domain resolver intended for callers that have already
     * extracted an authenticated principal name elsewhere (e.g. a security
     * component in {@code genesis-api}). The principal name is always a username,
     * so the lookup is strict {@code findByUsername} — matching on the email
     * column as well would let a username collide with another user's email and
     * resolve to the wrong identity.
     *
     * @param username the authenticated username of the user
     * @return the user's id
     * @throws UnauthorizedException if no matching user exists
     */
    @Transactional(readOnly = true)
    public UUID getUserIdByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"))
                .getId();
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
