package com.genesis.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.genesis.user.dto.SignupRequest;
import com.genesis.user.dto.UserResponse;
import com.genesis.user.entity.AuthProvider;
import com.genesis.user.entity.User;
import com.genesis.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * TDD Tests for UserService.
 * Tests are written FIRST before implementation.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private SignupRequest validSignupRequest;

    @BeforeEach
    void setUp() {
        validSignupRequest = new SignupRequest();
        validSignupRequest.setUsername("testuser");
        validSignupRequest.setEmail("test@example.com");
        validSignupRequest.setPassword("password123");
        validSignupRequest.setFirstName("Test");
        validSignupRequest.setLastName("User");
        validSignupRequest.setOrganizationName("Test Org");
    }

    @Test
    @DisplayName("createUser - valid request - creates user")
    void createUser_validRequest_createsUser() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        User savedUser = createMockUser();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse response = userService.createUser(validSignupRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFirstName()).isEqualTo("Test");
        assertThat(response.getLastName()).isEqualTo("User");
    }

    @Test
    @DisplayName("createUser - duplicate username - throws exception")
    void createUser_duplicateUsername_throwsException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(validSignupRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser - duplicate email - throws exception")
    void createUser_duplicateEmail_throwsException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(validSignupRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser - password is hashed using encoder")
    void createUser_passwordIsHashed() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashedValue");

        User savedUser = createMockUser();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        userService.createUser(validSignupRequest);

        // Assert - Verify password encoding was called
        verify(passwordEncoder).encode("password123");

        // Capture the saved user to verify password was hashed
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getPassword()).isEqualTo("$2a$12$hashedValue");
    }

    @Test
    @DisplayName("findByUsernameOrEmail - existing user - returns user")
    void findByUsernameOrEmail_existingUser_returnsUser() {
        // Arrange
        User user = createMockUser();
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findByUsernameOrEmail("testuser");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("findByUsernameOrEmail - non-existent user - returns empty")
    void findByUsernameOrEmail_nonExistent_returnsEmpty() {
        // Arrange
        when(userRepository.findByUsernameOrEmail("unknown", "unknown"))
                .thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByUsernameOrEmail("unknown");

        // Assert
        assertThat(result).isEmpty();
    }

    private User createMockUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedPassword");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setOrganizationName("Test Org");
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setAccountLocked(false);
        return user;
    }
}
