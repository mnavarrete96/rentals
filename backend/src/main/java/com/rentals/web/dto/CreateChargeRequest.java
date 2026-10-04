package com.rentals.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body for POST /api/tenants/{id}/charges (ADMIN).
 * ARCHITECTURE §6.3.
 */
public record CreateChargeRequest(
        @NotBlank @Size(max = 200) String description,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal amount
) {}
