package com.genesis.api.controller;

import com.genesis.common.response.ApiResponse;
import com.genesis.infra.security.JwtTokenProvider;
import com.genesis.infra.security.RefreshToken;
import com.genesis.infra.security.RefreshTokenService;
import com.genesis.infra.security.UserDetailsServiceImpl;
import com.genesis.user.dto.LoginRequest;
import com.genesis.user.dto.RefreshTokenRequest;
import com.genesis.user.dto.SignupRequest;
import com.genesis.user.dto.TokenResponse;
import com.genesis.user.dto.UserResponse;
import com.genesis.user.entity.User;
import com.genesis.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthController(AuthenticationManager authenticationManager,
            UserService userService,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            UserDetailsServiceImpl userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Register a new user.
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(
            @Valid @RequestBody SignupRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User registered successfully"));
    }

    /**
     * Authenticate user and return tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);

        // Get user entity for refresh token
        User user = userDetailsService.loadUserEntityByUsername(userDetails.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // Update last login
        userService.updateLastLogin(user);

        TokenResponse tokenResponse = TokenResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtTokenProvider.getAccessTokenExpiryMs() / 1000 // Convert to seconds
        );

        return ResponseEntity.ok(ApiResponse.success(tokenResponse, "Login successful"));
    }

    /**
     * Refresh access token using refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);

        TokenResponse tokenResponse = TokenResponse.of(
                accessToken,
                request.getRefreshToken(),
                jwtTokenProvider.getAccessTokenExpiryMs() / 1000);

        return ResponseEntity.ok(ApiResponse.success(tokenResponse, "Token refreshed successfully"));
    }

    /**
     * Logout user by revoking refresh token.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    /**
     * Get current authenticated user info.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userDetailsService.loadUserEntityByUsername(userDetails.getUsername());
        UserResponse response = UserResponse.fromEntity(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
