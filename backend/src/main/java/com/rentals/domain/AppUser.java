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

    // --- Setters ---

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void setPasswordChangedAt(OffsetDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }
}
