package com.rentals.service;

import com.rentals.domain.TenantStatus;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * Resumen de deuda de un arrendatario en un momento dado.
 *
 * Campos:
 *   monthsOwed  — lista ordenada de meses adeudados (desde startDate hasta currentMonth, menos los pagados)
 *   totalOwed   — meses × monthlyAmount + cargos únicos pendientes (BigDecimal escala 2)
 *   status      — AL_DIA si totalOwed == 0; ATRASADO en caso contrario
 *
 * Este record es inmutable y se calcula en runtime por DebtCalculatorService (ADR-006).
 * Nunca se persiste en la base de datos.
 */
public record DebtSummary(
        List<YearMonth> monthsOwed,
        BigDecimal totalOwed,
        TenantStatus status
) {}
