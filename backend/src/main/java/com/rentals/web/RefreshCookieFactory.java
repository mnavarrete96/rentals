package com.rentals.web;

import com.rentals.security.AuthProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds and clears the HttpOnly refresh token cookie.
 * ARCHITECTURE §15: cookie name rentals_refresh, HttpOnly, SameSite=Strict, Path=/api/auth.
 */
@Component
public class RefreshCookieFactory {

    private static final String COOKIE_NAME = "rentals_refresh";
    private static final String COOKIE_PATH = "/api/auth";

    private final AuthProperties authProperties;

    public RefreshCookieFactory(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    /**
     * Creates a Set-Cookie header value for the refresh token.
     */
    public ResponseCookie create(String rawToken) {
        return ResponseCookie.from(COOKIE_NAME, rawToken)
            .httpOnly(true)
            .secure(authProperties.refreshCookieSecure())
            .sameSite("Strict")
            .path(COOKIE_PATH)
            .maxAge(authProperties.refreshTokenTtl())
            .build();
    }

    /**
     * Creates an expiring cookie (maxAge=0) to clear the refresh token.
     */
    public ResponseCookie clear() {
        return ResponseCookie.from(COOKIE_NAME, "")
            .httpOnly(true)
            .secure(authProperties.refreshCookieSecure())
            .sameSite("Strict")
            .path(COOKIE_PATH)
            .maxAge(Duration.ZERO)
            .build();
    }
}
