package com.rentals.service;

/**
 * Thrown when trying to create a user with a username that already exists.
 * Results in HTTP 409 Conflict.
 */
public class UsernameTakenException extends RuntimeException {

    public UsernameTakenException(String username) {
        super("Username already taken: " + username);
    }
}
