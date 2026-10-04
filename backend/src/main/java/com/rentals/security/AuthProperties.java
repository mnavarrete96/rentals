package com.rentals.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;

/**
 * Configuration properties for authentication.
 * Bound to prefix "rentals.auth".
 *
 * jwt-secret must be set and decode to at least 32 bytes (fail-fast on startup).
 */
@Validated
@ConfigurationProperties(prefix = "rentals.auth")
public record AuthProperties(
    @NotBlank String jwtSecret,
    String issuer,
    @DefaultValue("PT15M") Duration accessTokenTtl,
    @DefaultValue("P7D") Duration refreshTokenTtl,
    @DefaultValue("true") boolean refreshCookieSecure,
    @Valid BootstrapAdmin bootstrapAdmin
) {

    public record BootstrapAdmin(
        String username,
        String password
    ) {}
}
