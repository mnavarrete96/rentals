package com.rentals.web.dto;

import com.rentals.domain.TenantStatus;

import java.math.BigDecimal;

/**
 * Summary view of a tenant for GET /api/tenants list.
 * ARCHITECTURE §6.3.
 */
public record TenantSummaryDto(
        Long id,
        String fullName,
        String unit,
        TenantStatus status,
        BigDecimal totalOwed,
        int monthsOwedCount
) {}
