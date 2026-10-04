package com.rentals.web.dto;

import java.math.BigDecimal;

/**
 * DTO for a one-time charge attached to a tenant.
 * ARCHITECTURE §6.3.
 */
public record OneTimeChargeDto(Long id, String description, BigDecimal amount, boolean paid) {}
