package com.rentals.service;

import com.rentals.domain.AppUser;

/**
 * Abstraction for issuing access tokens.
 * Services depend on this interface; they don't know Nimbus or JWT internals.
 */
public interface AccessTokenIssuer {

    /**
     * Issues an access token for the given user.
     *
     * @param user the authenticated user
     * @return issued token value and expiry (seconds)
     */
    IssuedToken issue(AppUser user);

    record IssuedToken(String value, long expiresIn) {}
}
