package com.rentals.repository;

import com.rentals.domain.AppUser;
import com.rentals.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoke all active refresh tokens for a user (used on password change and token reuse detection).
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now WHERE rt.user = :user AND rt.revokedAt IS NULL")
    void revokeAllByUser(@Param("user") AppUser user, @Param("now") OffsetDateTime now);

    /**
     * Delete expired tokens for a user (cleanup on login).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user AND rt.expiresAt < :now")
    void deleteExpiredByUser(@Param("user") AppUser user, @Param("now") OffsetDateTime now);
}
