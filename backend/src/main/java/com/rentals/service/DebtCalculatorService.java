package com.rentals.service;

import com.rentals.domain.OneTimeCharge;
import com.rentals.domain.Tenant;
import com.rentals.domain.TenantStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica pura de deuda: puerto de src/js/calc.js (v1) a Java.
 *
 * Paridad con calc.js garantizada por DebtCalculatorServiceTest con los mismos
 * datos de src/js/data.js.
 *
 * Reglas (ARCHITECTURE §4.3):
 *   - monthsOwed: desde el mes de startDate hasta currentMonth (ambos inclusive)
 *     en orden ascendente, menos los meses ya en paidMonths.
 *     Si startDate es futuro respecto a currentMonth → lista vacía.
 *   - paymentDay NO afecta al conteo (igual que v1). Cambiar esa regla requiere
 *     actualizar el SPEC primero.
 *   - chargesOwed: suma de oneTimeCharges donde paid == false.
 *   - totalOwed = monthsOwed.size() × monthlyAmount + chargesOwed.
 *   - status: AL_DIA si totalOwed == 0; ATRASADO si totalOwed > 0.
 *
 * Sin dependencias de Spring Data ni de la hora del sistema.
 * El llamador (TenantService, ChargeService) obtiene currentMonth del bean Clock.
 */
@Service
public class DebtCalculatorService {

    /**
     * Calcula la deuda de un arrendatario en el mes indicado.
     *
     * @param tenant       entidad con paidMonths y oneTimeCharges ya cargados (no lazy)
     * @param currentMonth mes de referencia, obtenido por el llamador desde el bean Clock
     * @return             DebtSummary inmutable con la deuda calculada
     */
    public DebtSummary calculate(Tenant tenant, YearMonth currentMonth) {

        YearMonth startMonth = YearMonth.from(tenant.getStartDate());

        // 1. Generar todos los meses desde startMonth hasta currentMonth (inclusive).
        //    Si startDate es futuro → lista vacía (la condición del while no se cumple).
        List<YearMonth> monthsOwed = new ArrayList<>();
        YearMonth cursor = startMonth;
        while (!cursor.isAfter(currentMonth)) {
            if (!tenant.getPaidMonths().contains(cursor)) {
                monthsOwed.add(cursor);
            }
            cursor = cursor.plusMonths(1);
        }

        // 2. Cargos únicos pendientes.
        BigDecimal chargesOwed = BigDecimal.ZERO;
        for (OneTimeCharge charge : tenant.getOneTimeCharges()) {
            if (!charge.isPaid()) {
                chargesOwed = chargesOwed.add(charge.getAmount());
            }
        }

        // 3. Total adeudado.
        BigDecimal recurringDebt = tenant.getMonthlyAmount()
                .multiply(BigDecimal.valueOf(monthsOwed.size()));
        BigDecimal totalOwed = recurringDebt.add(chargesOwed);

        // 4. Estado.
        TenantStatus status = totalOwed.compareTo(BigDecimal.ZERO) == 0
                ? TenantStatus.AL_DIA
                : TenantStatus.ATRASADO;

        return new DebtSummary(monthsOwed, totalOwed, status);
    }
}
