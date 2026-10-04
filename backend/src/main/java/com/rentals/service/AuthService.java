package com.rentals.service;

import com.rentals.domain.AppUser;
import com.rentals.domain.RefreshToken;
import com.rentals.repository.AppUserRepository;
import com.rentals.security.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Authentication service: login, refresh, logout, change password.
 * Returns AuthResult (which includes the raw refresh token for the cookie).
 * ARCHITECTURE §15.2, §15.3.
 *
 * Brute-force lockout (D-6): 5 consecutive failed logins lock the account for
 * 15 minutes. The counter resets on every successful login.
 */
@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final AccessTokenIssuer tokenIssuer;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthService(AppUserRepository userRepository,
                       AuthenticationManager authenticationManager,
                       AccessTokenIssuer tokenIssuer,
                       RefreshTokenService refreshTokenService,
                       PasswordEncoder passwordEncoder,
                       Clock clock) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Login: authenticates credentials and issues tokens.
     *
     * Always returns a generic 401 on failure (no user enumeration).
     * Enforces brute-force lockout: 5 consecutive failures lock the account
     * for 15 minutes (D-6). The counter resets on the first successful login.
     *
     * @throws BadCredentialsException on any failure (wrong password, locked,
     *         disabled, or user not found) — all produce the same message.
     */
    @Transactional
    public AuthResult login(String username, String password) {
        String normalizedUsername = username.trim().toLowerCase();

        // Load user first so we can check lockout before verifying the password.
        // If user does not exist we still throw the same generic error (no enumeration).
        AppUser user = userRepository.findByUsername(normalizedUsername)
            .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

        // Reject immediately when the account is temporarily locked.
        if (user.isLocked(clock)) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        // Delegate credential verification to Spring Security's AuthenticationManager.
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedUsername, password)
            );
        } catch (AuthenticationException e) {
            // Record the failed attempt; lock if threshold is reached.
            user.incrementFailedAttempts(clock);
            userRepository.save(user);
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        // Successful login: clear the failure counter and issue tokens.
        user.resetFailedAttempts();
        userRepository.save(user);

        AccessTokenIssuer.IssuedToken issued = tokenIssuer.issue(user);
        String refreshToken = refreshTokenService.createToken(user);

        return new AuthResult(issued.value(), issued.expiresIn(), refreshToken, user);
    }

    /**
     * Refresh: rotates the refresh token and issues a new access token.
     *
     * @throws InvalidRefreshTokenException if the token is invalid, expired, or revoked
     */
    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        Optional<RefreshToken> tokenOpt = refreshTokenService.validateAndGet(rawRefreshToken);

        if (tokenOpt.isEmpty()) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken token = tokenOpt.get();
        AppUser user = token.getUser();

        // Revoke the used token
        token.revoke(clock);

        // Issue new tokens
        AccessTokenIssuer.IssuedToken issued = tokenIssuer.issue(user);
        String newRefreshToken = refreshTokenService.createToken(user);

        return new AuthResult(issued.value(), issued.expiresIn(), newRefreshToken, user);
    }

    /**
     * Logout: revokes the refresh token (if present and valid).
     * Always succeeds (idempotent) — no exception even if token is absent.
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    /**
     * Change password for the authenticated user.
     * Verifies the current password, updates the hash, revokes all refresh tokens.
     *
     * @throws BadCredentialsException if currentPassword doesn't match
     */
    @Transactional
    public void changePassword(AppUser user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(OffsetDateTime.now(clock));
        refreshTokenService.revokeAllForUser(user);
    }
}
