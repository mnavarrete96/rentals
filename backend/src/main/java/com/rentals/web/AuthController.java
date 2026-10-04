package com.rentals.web;

import com.rentals.domain.AppUser;
import com.rentals.repository.AppUserRepository;
import com.rentals.service.AuthResult;
import com.rentals.service.AuthService;
import com.rentals.web.dto.AuthResponse;
import com.rentals.web.dto.ChangePasswordRequest;
import com.rentals.web.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 * A1: POST /api/auth/login
 * A2: POST /api/auth/refresh
 * A3: POST /api/auth/logout
 * A4: PUT  /api/auth/password
 *
 * The refresh token travels ONLY via the HttpOnly cookie (never in JSON body).
 * ARCHITECTURE §6.2, §15.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieFactory cookieFactory;
    private final AppUserRepository userRepository;

    public AuthController(AuthService authService,
                          RefreshCookieFactory cookieFactory,
                          AppUserRepository userRepository) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
        this.userRepository = userRepository;
    }

    /**
     * A1: Login with username/password.
     * Returns AuthResponse + Set-Cookie with refresh token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request.username(), request.password());
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookieFactory.create(result.refreshToken()).toString())
            .body(toResponse(result));
    }

    /**
     * A2: Refresh access token using the refresh cookie.
     * Returns new AuthResponse + rotated cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "rentals_refresh", required = false) String refreshCookie) {

        if (refreshCookie == null || refreshCookie.isBlank()) {
            return ResponseEntity.status(401)
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .build();
        }

        AuthResult result = authService.refresh(refreshCookie);
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookieFactory.create(result.refreshToken()).toString())
            .body(toResponse(result));
    }

    /**
     * A3: Logout. Revokes the refresh token and clears the cookie.
     * Always returns 204 (idempotent).
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "rentals_refresh", required = false) String refreshCookie) {
        authService.logout(refreshCookie);
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
            .build();
    }

    /**
     * A4: Change password for the authenticated user.
     * Revokes all sessions and clears the cookie.
     */
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request) {

        Long userId = Long.parseLong(jwt.getSubject());
        AppUser user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));

        authService.changePassword(user, request.currentPassword(), request.newPassword());

        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
            .build();
    }

    // --- Helpers ---

    private AuthResponse toResponse(AuthResult result) {
        AppUser user = result.user();
        Long tenantId = user.getTenant() != null ? user.getTenant().getId() : null;

        return new AuthResponse(
            result.accessToken(),
            "Bearer",
            result.expiresIn(),
            new AuthResponse.UserInfo(user.getId(), user.getUsername(), user.getRole().name(), tenantId)
        );
    }
}
