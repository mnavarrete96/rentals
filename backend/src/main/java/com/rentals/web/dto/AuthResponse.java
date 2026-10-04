package com.rentals.web.dto;

/**
 * Response body for POST /api/auth/login and POST /api/auth/refresh.
 * ARCHITECTURE §6.3.
 */
public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UserInfo user
) {
    public record UserInfo(
        Long id,
        String username,
        String role,
        Long tenantId
    ) {}
}
