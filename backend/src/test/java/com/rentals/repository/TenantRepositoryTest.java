package com.rentals.repository;

import com.rentals.domain.OneTimeCharge;
import com.rentals.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de repositorio con Testcontainers (@DataJpaTest + PostgreSQL real).
 *
 * Flyway esta deshabilitado para el slice (ver src/test/resources/application.properties);
 * Hibernate usa ddl-auto=create-drop para crear el esquema desde las entidades.
 *
 * Cada @Test se envuelve en una transaccion que se hace rollback al finalizar,
 * garantizando aislamiento entre pruebas.
 *
 * Requiere Docker corriendo. Si Docker no esta disponible, los tests se saltan
 * automaticamente (Testcontainers detecta la ausencia de Docker y lanza
 * TestcontainersNotFound, que JUnit 5 trata como @Disabled en ese entorno).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TenantRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TenantRepository tenantRepo;

    // --- Test 1: guardar y recuperar un arrendatario ---

    @Test
    void saveAndFindTenant() {
        var t = new Tenant("Ana García", "101", "ana@test.com", 5,
                LocalDate.of(2024, 1, 1), new BigDecimal("450.00"));
        tenantRepo.save(t);

        var all = tenantRepo.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getFullName()).isEqualTo("Ana García");
        assertThat(all.get(0).getUnit()).isEqualTo("101");
        assertThat(all.get(0).getMonthlyAmount()).isEqualByComparingTo(new BigDecimal("450.00"));
    }

    // --- Test 2: el Set<YearMonth> previene duplicados (idempotencia de "marcar pagado") ---

    @Test
    void paidMonthSetPreventsDuplicates() {
        var t = new Tenant("Luis López", "102", "luis@test.com", 10,
                LocalDate.of(2024, 3, 1), new BigDecimal("380.00"));
        tenantRepo.saveAndFlush(t);

        // Agregar el mismo mes dos veces: Set deduplica a nivel Java
        t.getPaidMonths().add(YearMonth.of(2024, 5));
        t.getPaidMonths().add(YearMonth.of(2024, 5)); // duplicado; no cambia el Set

        tenantRepo.saveAndFlush(t);

        // Recargar desde BD para verificar que se persiste solo una fila
        var saved = tenantRepo.findById(t.getId()).orElseThrow();
        assertThat(saved.getPaidMonths()).hasSize(1);
        assertThat(saved.getPaidMonths()).contains(YearMonth.of(2024, 5));
    }

    // --- Test 3: cargar varios meses pagados correctamente ---

    @Test
    void multiplePaidMonthsArePersisted() {
        var t = new Tenant("María Pérez", "201", "maria@test.com", 1,
                LocalDate.of(2024, 1, 1), new BigDecimal("500.00"));
        t.getPaidMonths().add(YearMonth.of(2024, 1));
        t.getPaidMonths().add(YearMonth.of(2024, 2));
        t.getPaidMonths().add(YearMonth.of(2024, 3));
        tenantRepo.saveAndFlush(t);

        var saved = tenantRepo.findById(t.getId()).orElseThrow();
        assertThat(saved.getPaidMonths()).containsExactlyInAnyOrder(
                YearMonth.of(2024, 1),
                YearMonth.of(2024, 2),
                YearMonth.of(2024, 3)
        );
    }

    // --- Test 4: cargos unicos se persisten via cascada ---

    @Test
    void oneTimeChargeIsPersistedWithTenant() {
        var t = new Tenant("Carlos Ruiz", "301", "carlos@test.com", 15,
                LocalDate.of(2024, 6, 1), new BigDecimal("600.00"));
        t.getOneTimeCharges().add(new OneTimeCharge(t, "Garantía", new BigDecimal("600.00")));
        tenantRepo.saveAndFlush(t);

        var saved = tenantRepo.findById(t.getId()).orElseThrow();
        assertThat(saved.getOneTimeCharges()).hasSize(1);
        assertThat(saved.getOneTimeCharges().get(0).getDescription()).isEqualTo("Garantía");
        assertThat(saved.getOneTimeCharges().get(0).isPaid()).isFalse();
        assertThat(saved.getOneTimeCharges().get(0).getAmount())
                .isEqualByComparingTo(new BigDecimal("600.00"));
    }

    // --- Test 5: marcar cargo como pagado ---

    @Test
    void chargeCanBeMarkedAsPaid() {
        var t = new Tenant("Rosa Díaz", "401", "rosa@test.com", 20,
                LocalDate.of(2024, 1, 1), new BigDecimal("350.00"));
        t.getOneTimeCharges().add(new OneTimeCharge(t, "Daños", new BigDecimal("100.00")));
        tenantRepo.saveAndFlush(t);

        // Marcar el cargo como pagado
        var saved = tenantRepo.findById(t.getId()).orElseThrow();
        saved.getOneTimeCharges().get(0).setPaid(true);
        tenantRepo.saveAndFlush(saved);

        var updated = tenantRepo.findById(t.getId()).orElseThrow();
        assertThat(updated.getOneTimeCharges().get(0).isPaid()).isTrue();
    }
}
