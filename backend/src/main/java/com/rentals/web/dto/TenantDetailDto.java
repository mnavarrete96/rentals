package com.rentals.web.dto;

import com.rentals.domain.TenantStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Full tenant detail for GET /api/tenants/{id} and GET /api/me/tenant.
 * Debt fields (monthsOwed, totalOwed, status) are computed at request time
 * by DebtCalculatorService — never persisted (ADR-006).
 * ARCHITECTURE §6.3.
 */
public record TenantDetailDto(
        Long id,
        String fullName,
        String unit,
        String contact,
        String username,
        int paymentDay,
        LocalDate startDate,
        BigDecimal monthlyAmount,
        List<String> paidMonths,
        List<OneTimeChargeDto> oneTimeCharges,
        List<String> monthsOwed,
        BigDecimal totalOwed,
        TenantStatus status
) {}
