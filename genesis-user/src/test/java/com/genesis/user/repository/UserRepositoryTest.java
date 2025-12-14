package com.genesis.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.genesis.user.config.UserTestConfiguration;
import com.genesis.user.entity.AuthProvider;
import com.genesis.user.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * TDD Tests for UserRepository.
 * These tests are written FIRST before the implementation (RED phase).
 */
@DataJpaTest
@ContextConfiguration(classes = UserTestConfiguration.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword123");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setAuthProvider(AuthProvider.LOCAL);
        testUser.setEnabled(true);
        testUser.setEmailVerified(false);
        testUser.setAccountLocked(false);

        // Use repository.save() instead of entityManager to trigger JPA Auditing
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("findByUsername - existing user - returns user")
    void findByUsername_existingUser_returnsUser() {
        Optional<User> found = userRepository.findByUsername("testuser");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("findByUsername - non-existent user - returns empty")
    void findByUsername_nonExistent_returnsEmpty() {
        Optional<User> found = userRepository.findByUsername("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByEmail - existing user - returns user")
    void findByEmail_existingUser_returnsUser() {
        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("findByEmail - non-existent email - returns empty")
    void findByEmail_nonExistent_returnsEmpty() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByUsernameOrEmail - find by username - returns user")
    void findByUsernameOrEmail_byUsername_returnsUser() {
        Optional<User> found = userRepository.findByUsernameOrEmail("testuser", "testuser");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("findByUsernameOrEmail - find by email - returns user")
    void findByUsernameOrEmail_byEmail_returnsUser() {
        Optional<User> found = userRepository.findByUsernameOrEmail("test@example.com", "test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("existsByUsername - existing user - returns true")
    void existsByUsername_existingUser_returnsTrue() {
        boolean exists = userRepository.existsByUsername("testuser");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByUsername - non-existent user - returns false")
    void existsByUsername_nonExistent_returnsFalse() {
        boolean exists = userRepository.existsByUsername("nonexistent");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByEmail - existing email - returns true")
    void existsByEmail_existingEmail_returnsTrue() {
        boolean exists = userRepository.existsByEmail("test@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail - non-existent email - returns false")
    void existsByEmail_nonExistent_returnsFalse() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
    }
}
