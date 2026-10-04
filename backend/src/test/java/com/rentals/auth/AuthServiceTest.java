package com.rentals.auth;

import com.rentals.domain.AppUser;
import com.rentals.domain.RefreshToken;
import com.rentals.domain.Role;
import com.rentals.repository.AppUserRepository;
import com.rentals.security.RefreshTokenService;
import com.rentals.service.AccessTokenIssuer;
import com.rentals.service.AuthResult;
import com.rentals.service.AuthService;
import com.rentals.service.InvalidRefreshTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthService.
 * Uses Mockito to avoid Spring context or database.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AccessTokenIssuer tokenIssuer;

    @Mock
    private RefreshTokenService refreshTokenService;

    private PasswordEncoder passwordEncoder;
    private Clock clock;
    private AuthService authService;

    private AppUser adminUser;
    private AppUser tenantUser;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4); // fast for tests
        clock = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC);

        authService = new AuthService(
            userRepository, authenticationManager, tokenIssuer,
            refreshTokenService, passwordEncoder, clock
        );

        adminUser = new AppUser("admin", passwordEncoder.encode("admin-pass-123"), Role.ADMIN, null);
        tenantUser = new AppUser("ana.garcia", passwordEncoder.encode("dev-password-123"), Role.ARRENDATARIO, null);
    }

    // ======= Login tests =======

    @Test
    void login_withCorrectCredentials_returnsAuthResult() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(tokenIssuer.issue(adminUser)).thenReturn(new AccessTokenIssuer.IssuedToken("jwt-token", 900));
        when(refreshTokenService.createToken(adminUser)).thenReturn("raw-refresh-token");

        AuthResult result = authService.login("admin", "admin-pass-123");

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("jwt-token");
        assertThat(result.expiresIn()).isEqualTo(900);
        assertThat(result.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(result.user()).isEqualTo(adminUser);
    }

    @Test
    void login_withWrongPassword_throwsBadCredentials() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        doThrow(new BadCredentialsException("bad"))
            .when(authenticationManager)
            .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login("admin", "wrong-password"))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("incorrectos");
    }

    @Test
    void login_withDisabledUser_throwsBadCredentials() {
        when(userRepository.findByUsername("ana.garcia")).thenReturn(Optional.of(tenantUser));
        // AuthenticationManager throws for disabled users too
        doThrow(new org.springframework.security.authentication.DisabledException("disabled"))
            .when(authenticationManager)
            .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login("ana.garcia", "dev-password-123"))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("incorrectos");
    }

    // ======= Lockout tests (D-6) =======

    @Test
    void login_withLockedAccount_throwsBadCredentials_withoutCallingAuthManager() {
        // Lock the account by simulating 5 prior failures
        for (int i = 0; i < 5; i++) {
            adminUser.incrementFailedAttempts(clock);
        }
        assertThat(adminUser.isLocked(clock)).isTrue();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> authService.login("admin", "admin-pass-123"))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessageContaining("incorrectos");

        // AuthenticationManager must NOT be called when account is locked
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_with5ConsecutiveFailures_locksAccount() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        doThrow(new BadCredentialsException("bad"))
            .when(authenticationManager)
            .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // First 4 failures: account should not be locked yet
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> authService.login("admin", "wrong"))
                .isInstanceOf(BadCredentialsException.class);
            assertThat(adminUser.isLocked(clock)).isFalse();
            assertThat(adminUser.getFailedAttempts()).isEqualTo(i + 1);
        }

        // 5th failure: account must become locked and counter resets to 0
        assertThatThrownBy(() -> authService.login("admin", "wrong"))
            .isInstanceOf(BadCredentialsException.class);
        assertThat(adminUser.isLocked(clock)).isTrue();
        assertThat(adminUser.getFailedAttempts()).isEqualTo(0);
        assertThat(adminUser.getLockedUntil()).isNotNull();
        assertThat(adminUser.getLockedUntil()).isAfter(OffsetDateTime.now(clock));
    }

    @Test
    void login_afterFailedAttempts_successfulLogin_resetsCounter() {
        // Set up 3 prior failed attempts (not yet locked)
        adminUser.incrementFailedAttempts(clock); // 1
        adminUser.incrementFailedAttempts(clock); // 2
        adminUser.incrementFailedAttempts(clock); // 3
        assertThat(adminUser.getFailedAttempts()).isEqualTo(3);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(tokenIssuer.issue(adminUser)).thenReturn(new AccessTokenIssuer.IssuedToken("jwt-token", 900));
        when(refreshTokenService.createToken(adminUser)).thenReturn("raw-refresh-token");

        // Successful login must reset the counter
        authService.login("admin", "admin-pass-123");

        assertThat(adminUser.getFailedAttempts()).isEqualTo(0);
        assertThat(adminUser.getLockedUntil()).isNull();
        verify(userRepository).save(adminUser);
    }

    // ======= Refresh tests =======

    @Test
    void refresh_withValidToken_rotatesTokenAndReturnsAuthResult() {
        OffsetDateTime expiresAt = OffsetDateTime.now(clock).plusDays(7);
        RefreshToken refreshToken = new RefreshToken(adminUser, "hash123", expiresAt);

        when(refreshTokenService.validateAndGet("raw-token")).thenReturn(Optional.of(refreshToken));
        when(tokenIssuer.issue(adminUser)).thenReturn(new AccessTokenIssuer.IssuedToken("new-jwt", 900));
        when(refreshTokenService.createToken(adminUser)).thenReturn("new-raw-refresh");

        AuthResult result = authService.refresh("raw-token");

        assertThat(result.accessToken()).isEqualTo("new-jwt");
        assertThat(result.refreshToken()).isEqualTo("new-raw-refresh");
        assertThat(result.user()).isEqualTo(adminUser);
        // old token must be revoked
        assertThat(refreshToken.isRevoked()).isTrue();
    }

    @Test
    void refresh_withRevokedToken_throwsInvalidRefreshToken() {
        // validateAndGet returns empty when token is revoked (after revoking all)
        when(refreshTokenService.validateAndGet("revoked-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("revoked-token"))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_withMissingToken_throwsInvalidRefreshToken() {
        when(refreshTokenService.validateAndGet("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("nonexistent"))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    // ======= Logout tests =======

    @Test
    void logout_withValidToken_revokesToken() {
        authService.logout("some-token");
        verify(refreshTokenService).revoke("some-token");
    }

    @Test
    void logout_withNullToken_doesNothing() {
        // Should not throw
        authService.logout(null);
    }

    // ======= Change password tests =======

    @Test
    void changePassword_withCorrectCurrentPassword_updatesHash() {
        String originalHash = adminUser.getPasswordHash();
        authService.changePassword(adminUser, "admin-pass-123", "new-password-456");

        assertThat(adminUser.getPasswordHash()).isNotEqualTo(originalHash);
        assertThat(passwordEncoder.matches("new-password-456", adminUser.getPasswordHash())).isTrue();
        verify(refreshTokenService).revokeAllForUser(adminUser);
    }

    @Test
    void changePassword_withWrongCurrentPassword_throwsBadCredentials() {
        assertThatThrownBy(() ->
            authService.changePassword(adminUser, "wrong-old-pass", "new-password-456")
        ).isInstanceOf(BadCredentialsException.class);
    }
}
