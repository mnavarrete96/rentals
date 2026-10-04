package com.rentals.service;

/**
 * Thrown when a tenant (or a resource belonging to a tenant) is not found.
 * Results in HTTP 404 Not Found.
 */
public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(Long id) {
        super("Arrendatario no encontrado: " + id);
    }

    public TenantNotFoundException(String message) {
        super(message);
    }
}
