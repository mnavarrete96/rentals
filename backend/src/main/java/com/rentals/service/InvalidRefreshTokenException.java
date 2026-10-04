package com.rentals.service;

/**
 * Thrown when a refresh token is invalid, expired, or revoked.
 * Results in HTTP 401.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}
