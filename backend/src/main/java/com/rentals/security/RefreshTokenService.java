package com.rentals.security;

import com.rentals.domain.AppUser;
import com.rentals.domain.RefreshToken;
import com.rentals.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Manages opaque refresh tokens.
 * Tokens are stored as SHA-256 hex hashes; the raw token travels only in the cookie.
 * ADR-009: rotation on each use, revocation on logout/password change.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthProperties authProperties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               AuthProperties authProperties,
                               Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.authProperties = authProperties;
        this.clock = clock;
    }

    /**
     * Generates a new opaque refresh token, stores its hash, returns the raw token.
     * Also purges expired tokens for the user (cleanup on login).
     */
    @Transactional
    public String createToken(AppUser user) {
        // Clean up expired tokens for this user
        refreshTokenRepository.deleteExpiredByUser(user, OffsetDateTime.now(clock));

        // Generate 32 random bytes, Base64URL-encode as the opaque token
        byte[] rawBytes = new byte[32];
        secureRandom.nextBytes(rawBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);

        String tokenHash = sha256Hex(rawToken);
        OffsetDateTime expiresAt = OffsetDateTime.now(clock).plus(authProperties.refreshTokenTtl());

        RefreshToken token = new RefreshToken(user, tokenHash, expiresAt);
        refreshTokenRepository.save(token);

        return rawToken;
    }

    /**
     * Finds and validates a refresh token by its raw value.
     * Returns empty if not found, expired, or the user is disabled.
     * If the token is revoked (reuse detection), revokes ALL user tokens and returns empty.
     */
    @Transactional
    public Optional<RefreshToken> validateAndGet(String rawToken) {
        String hash = sha256Hex(rawToken);
        Optional<RefreshToken> opt = refreshTokenRepository.findByTokenHash(hash);

        if (opt.isEmpty()) {
            return Optional.empty();
        }

        RefreshToken token = opt.get();

        // Reuse detection: token was already revoked
        if (token.isRevoked()) {
            revokeAllForUser(token.getUser());
            return Optional.empty();
        }

        // Expired
        if (token.isExpired(clock)) {
            return Optional.empty();
        }

        // User disabled
        if (!token.getUser().isEnabled()) {
            return Optional.empty();
        }

        return Optional.of(token);
    }

    /**
     * Revokes the token identified by the raw value (used on logout).
     */
    @Transactional
    public void revoke(String rawToken) {
        String hash = sha256Hex(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> t.revoke(clock));
    }

    /**
     * Revokes all active refresh tokens for a user (used on password change).
     */
    @Transactional
    public void revokeAllForUser(AppUser user) {
        refreshTokenRepository.revokeAllByUser(user, OffsetDateTime.now(clock));
    }

    // --- Helpers ---

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
