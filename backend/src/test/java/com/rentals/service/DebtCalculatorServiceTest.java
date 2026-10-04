package com.rentals.service;

import com.rentals.domain.OneTimeCharge;
import com.rentals.domain.Tenant;
import com.rentals.domain.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de paridad entre DebtCalculatorService (Java, v2) y calc.js (v1).
 *
 * Los datos de t-001, t-002 y t-003 corresponden exactamente a src/js/data.js
 * para garantizar paridad de comportamiento (ARCHITECTURE §4.3).
 *
 * Sin Spring, sin BD: lógica pura (JUnit 5 + AssertJ).
 * currentMonth = YearMonth.of(2026, 10) ↔ today = new Date(2026, 9, 3) en calc.js.
 */
class DebtCalculatorServiceTest {

    private DebtCalculatorService service;

    /** Mes de referencia usado en todos los tests que emulan calc.js con "today = 2026-10". */
    private static final YearMonth OCT_2026 = YearMonth.of(2026, 10);

    @BeforeEach
    void setUp() {
        service = new DebtCalculatorService();
    }

    // -------------------------------------------------------------------------
    // Helpers para construir Tenants de prueba sin JPA/BD
    // -------------------------------------------------------------------------

    /** Construye un Tenant con los datos dados. paidMonths y oneTimeCharges se añaden en el test. */
    private Tenant buildTenant(String fullName, String unit, int paymentDay,
                               String startDate, double monthlyAmount) {
        return new Tenant(
                fullName, unit, "contact@test.com",
                paymentDay,
                LocalDate.parse(startDate),
                BigDecimal.valueOf(monthlyAmount).setScale(2)
        );
    }

    /** Añade meses pagados al tenant y devuelve el mismo tenant (fluent). */
    private Tenant withPaidMonths(Tenant tenant, String... months) {
        for (String m : months) {
            tenant.getPaidMonths().add(YearMonth.parse(m));
        }
        return tenant;
    }

    /** Añade un cargo único pagado. */
    private void addPaidCharge(Tenant tenant, String description, double amount) {
        OneTimeCharge charge = new OneTimeCharge(tenant, description, BigDecimal.valueOf(amount).setScale(2));
        charge.setPaid(true);
        tenant.getOneTimeCharges().add(charge);
    }

    /** Añade un cargo único pendiente. */
    private void addUnpaidCharge(Tenant tenant, String description, double amount) {
        OneTimeCharge charge = new OneTimeCharge(tenant, description, BigDecimal.valueOf(amount).setScale(2));
        tenant.getOneTimeCharges().add(charge);
    }

    // -------------------------------------------------------------------------
    // Tests con datos de src/js/data.js
    // -------------------------------------------------------------------------

    /**
     * t-001: María Pérez — todos los meses pagados hasta 2026-10.
     * Esperado: monthsOwed vacío, totalOwed=0, status=AL_DIA.
     */
    @Test
    void t001_alDia_todosLosMesesPagados() {
        Tenant tenant = buildTenant("María Pérez", "Depto 301", 5, "2026-07-01", 450.00);
        withPaidMonths(tenant, "2026-07", "2026-08", "2026-09", "2026-10");
        addPaidCharge(tenant, "Garantía", 450.00);

        DebtSummary result = service.calculate(tenant, OCT_2026);

        assertThat(result.monthsOwed()).isEmpty();
        assertThat(result.totalOwed()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.status()).isEqualTo(TenantStatus.AL_DIA);
    }

    /**
     * t-002: Carlos Ramos — atrasado 2 meses (2026-09 y 2026-10 sin pagar).
     * Esperado: monthsOwed=[2026-09, 2026-10], totalOwed=1000.00, status=ATRASADO.
     */
    @Test
    void t002_atrasado_dosMesesSinPagar() {
        Tenant tenant = buildTenant("Carlos Ramos", "Depto 302", 10, "2026-06-01", 500.00);
        withPaidMonths(tenant, "2026-06", "2026-07", "2026-08");
        addPaidCharge(tenant, "Garantía", 500.00);

        DebtSummary result = service.calculate(tenant, OCT_2026);

        assertThat(result.monthsOwed()).containsExactly(
                YearMonth.of(2026, 9),
                YearMonth.of(2026, 10)
        );
        assertThat(result.totalOwed()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.status()).isEqualTo(TenantStatus.ATRASADO);
    }

    /**
     * t-003: Ana Torres — meses recurrentes al día, cargo único "Reparación ventana" pendiente.
     * Esperado: monthsOwed vacío, totalOwed=120.00, status=ATRASADO.
     */
    @Test
    void t003_atrasadoConCargoUnicoPendiente() {
        Tenant tenant = buildTenant("Ana Torres", "Depto 201", 1, "2026-08-01", 400.00);
        withPaidMonths(tenant, "2026-08", "2026-09", "2026-10");
        addPaidCharge(tenant, "Garantía", 400.00);
        addUnpaidCharge(tenant, "Reparación ventana", 120.00);

        DebtSummary result = service.calculate(tenant, OCT_2026);

        assertThat(result.monthsOwed()).isEmpty();
        assertThat(result.totalOwed()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(result.status()).isEqualTo(TenantStatus.ATRASADO);
    }

    // -------------------------------------------------------------------------
    // Pruebas de bordes
    // -------------------------------------------------------------------------

    /**
     * startDate futuro respecto a currentMonth → monthsOwed vacío.
     */
    @Test
    void startDateFuturo_monthsOwedVacio() {
        Tenant tenant = buildTenant("Futuro Tenant", "101", 1, "2027-01-01", 300.00);

        DebtSummary result = service.calculate(tenant, OCT_2026);

        assertThat(result.monthsOwed()).isEmpty();
        assertThat(result.totalOwed()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.status()).isEqualTo(TenantStatus.AL_DIA);
    }

    /**
     * startDate en el mismo mes que currentMonth, sin pagar → 1 mes adeudado.
     */
    @Test
    void startDateEnMesActual_sinPagar_unMesAdeudado() {
        Tenant tenant = buildTenant("Nuevo Tenant", "102", 15, "2026-10-15", 350.00);

        DebtSummary result = service.calculate(tenant, OCT_2026);

        assertThat(result.monthsOwed()).containsExactly(YearMonth.of(2026, 10));
        assertThat(result.totalOwed()).isEqualByComparingTo(new BigDecimal("350.00"));
        assertThat(result.status()).isEqualTo(TenantStatus.ATRASADO);
    }

    /**
     * Cruce de año: startDate 2025-11, currentMonth 2026-02 → meses [2025-11, 2025-12, 2026-01, 2026-02].
     */
    @Test
    void cruceDeCaldeoAnio_mesesEnOrdenAscendente() {
        Tenant tenant = buildTenant("Cruce Anio", "103", 1, "2025-11-01", 500.00);
        // ningún mes pagado

        DebtSummary result = service.calculate(tenant, YearMonth.of(2026, 2));

        assertThat(result.monthsOwed()).containsExactly(
                YearMonth.of(2025, 11),
                YearMonth.of(2025, 12),
                YearMonth.of(2026, 1),
                YearMonth.of(2026, 2)
        );
        assertThat(result.totalOwed()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(result.status()).isEqualTo(TenantStatus.ATRASADO);
    }

    /**
     * Meses y cargos únicos pendientes simultáneos → total incluye ambos.
     */
    @Test
    void mesesYCargosAdeudadosCombinados() {
        Tenant tenant = buildTenant("Combinado", "104", 1, "2026-08-01", 400.00);
        // 2026-08 pagado; 2026-09, 2026-10 sin pagar
        withPaidMonths(tenant, "2026-08");
        addUnpaidCharge(tenant, "Daños", 200.00);

        DebtSummary result = service.calculate(tenant, OCT_2026);

        // 2 meses × 400 = 800, + cargo 200 = 1000
        assertThat(result.monthsOwed()).hasSize(2);
        assertThat(result.totalOwed()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.status()).isEqualTo(TenantStatus.ATRASADO);
    }

    /**
     * Status AL_DIA solo cuando totalOwed == 0 (sin meses ni cargos pendientes).
     */
    @Test
    void statusAlDia_soloSiTotalEsCero() {
        Tenant tenant = buildTenant("Tenant AlDia", "105", 5, "2026-10-01", 500.00);
        withPaidMonths(tenant, "2026-10");
        addPaidCharge(tenant, "Garantía", 500.00);

        DebtSummary result = service.calculate(tenant, OCT_2026);

        assertThat(result.totalOwed()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.status()).isEqualTo(TenantStatus.AL_DIA);
    }

    /**
     * Status ATRASADO cuando solo hay un cargo único pendiente (sin meses adeudados).
     */
    @Test
    void statusAtrasado_soloConCargoUnico() {
        Tenant tenant = buildTenant("Solo Cargo", "106", 1, "2026-10-01", 0.00);
        withPaidMonths(tenant, "2026-10");
        addUnpaidCharge(tenant, "Reparación", 50.00);

        DebtSummary result = service.calculate(tenant, OCT_2026);

        assertThat(result.monthsOwed()).isEmpty();
        assertThat(result.totalOwed()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.status()).isEqualTo(TenantStatus.ATRASADO);
    }

    /**
     * Sin cargos únicos: totalOwed = monthsOwed.size() × monthlyAmount exactamente.
     */
    @Test
    void sinCargos_totalEsSoloMesesPorMonto() {
        Tenant tenant = buildTenant("Solo Mensual", "107", 1, "2026-09-01", 600.00);
        // 2026-09 y 2026-10 sin pagar, sin cargos

        DebtSummary result = service.calculate(tenant, OCT_2026);

        assertThat(result.monthsOwed()).hasSize(2);
        assertThat(result.totalOwed()).isEqualByComparingTo(new BigDecimal("1200.00"));
    }

    /**
     * Múltiples cargos, algunos pagados y otros no: solo se suman los no pagados.
     */
    @Test
    void multiplesCargos_soloSeContabilizanLosPendientes() {
        Tenant tenant = buildTenant("Multi Cargo", "108", 1, "2026-10-01", 0.00);
        withPaidMonths(tenant, "2026-10");
        addPaidCharge(tenant, "Cargo A (pagado)", 100.00);
        addPaidCharge(tenant, "Cargo B (pagado)", 200.00);
        addUnpaidCharge(tenant, "Cargo C (pendiente)", 75.00);
        addUnpaidCharge(tenant, "Cargo D (pendiente)", 25.00);

        DebtSummary result = service.calculate(tenant, OCT_2026);

        assertThat(result.totalOwed()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.status()).isEqualTo(TenantStatus.ATRASADO);
    }
}
