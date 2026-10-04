package com.rentals.domain;

/**
 * Estado de deuda del arrendatario.
 * Se calcula en runtime por DebtCalculatorService; nunca se persiste (ADR-006).
 */
public enum TenantStatus {
    AL_DIA,
    ATRASADO
}
