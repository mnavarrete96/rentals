package com.rentals.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Application user (ADMIN or ARRENDATARIO).
 *
 * The relationship with Tenant is unidirectional from AppUser to Tenant
 * (Tenant does not reference AppUser). ARCHITECTURE §5.3.
 *
 * password_hash stores DelegatingPasswordEncoder format: "{bcrypt}$2a$10$..."
 *
 * failedAttempts / lockedUntil implement brute-force lockout (D-6):
 * 5 consecutive failures lock the account for 15 minutes.
 * The counter resets to 0 on every successful login.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "password_changed_at", nullable = false)
    private OffsetDateTime passwordChangedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Consecutive failed login attempts since last success. Resets on success or when account locks. */
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    /** If non-null and in the future, the account is temporarily locked. */
    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    /** JPA required. */
    protected AppUser() {}

    public AppUser(String username, String passwordHash, Role role, Tenant tenant) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.tenant = tenant;
        this.enabled = true;
        OffsetDateTime now = OffsetDateTime.now();
        this.passwordChangedAt = now;
        this.createdAt = now;
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    // --- Getters ---

    public Long getId() { return id; }

    public String getUsername() { return username; }

    public String getPasswordHash() { return passwordHash; }

    public Role getRole() { return role; }

    public Tenant getTenant() { return tenant; }

    public boolean isEnabled() { return enabled; }

    public OffsetDateTime getPasswordChangedAt() { return passwordChangedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public int getFailedAttempts() { return failedAttempts; }

    public OffsetDateTime getLockedUntil() { return lockedUntil; }

    // --- Setters ---

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void setPasswordChangedAt(OffsetDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    // --- Lockout methods (D-6) ---

    /**
     * Returns true if the account is temporarily locked at the given instant.
     * A null or past lockedUntil means the account is not locked.
     */
    public boolean isLocked(Clock clock) {
        return lockedUntil != null && OffsetDateTime.now(clock).isBefore(lockedUntil);
    }

    /**
     * Records one failed login attempt.
     * When the counter reaches 5, the account is locked for 15 minutes and the counter resets to 0.
     */
    public void incrementFailedAttempts(Clock clock) {
        failedAttempts++;
        if (failedAttempts >= 5) {
            lockedUntil = OffsetDateTime.now(clock).plusMinutes(15);
            failedAttempts = 0;
        }
    }

    /**
     * Clears the failed-attempt counter and removes any temporary lock.
     * Called on every successful login.
     */
    public void resetFailedAttempts() {
        failedAttempts = 0;
        lockedUntil = null;
    }
}
