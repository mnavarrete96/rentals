package com.rentals.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Refresh token stored as SHA-256 hex hash (never the raw token).
 * ADR-009: opaque, rotated on each use.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** JPA required. */
    protected RefreshToken() {}

    public RefreshToken(AppUser user, String tokenHash, OffsetDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = OffsetDateTime.now();
    }

    // --- Getters ---

    public Long getId() { return id; }

    public AppUser getUser() { return user; }

    public String getTokenHash() { return tokenHash; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }

    public OffsetDateTime getRevokedAt() { return revokedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    // --- State queries ---

    public boolean isExpired(Clock clock) {
        return OffsetDateTime.now(clock).isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid(Clock clock) {
        return !isRevoked() && !isExpired(clock);
    }

    // --- Mutation ---

    public void revoke(Clock clock) {
        this.revokedAt = OffsetDateTime.now(clock);
    }
}
