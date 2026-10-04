package com.rentals.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for POST /api/tenants (ADMIN).
 * Creates a tenant and a linked ARRENDATARIO user in a single transaction.
 * ARCHITECTURE §6.3, §15.5.
 *
 * username is stored in lowercase (normalized in UserAccountService).
 * initialPassword is never returned, logged, or stored as plaintext.
 */
public record CreateTenantRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Size(max = 40) String unit,
        @NotBlank @Size(max = 120) String contact,
        @NotNull @Min(1) @Max(28) Integer paymentDay,
        @NotNull LocalDate startDate,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal monthlyAmount,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$") String username,
        @NotBlank @Size(min = 10, max = 72) String initialPassword
) {}
