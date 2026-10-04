package com.rentals.service;

import com.rentals.domain.AppUser;

/**
 * Result of a successful authentication (login or refresh).
 * Contains the issued access token info, the refresh token raw value, and the user.
 */
public record AuthResult(
    String accessToken,
    long expiresIn,
    String refreshToken,
    AppUser user
) {}
