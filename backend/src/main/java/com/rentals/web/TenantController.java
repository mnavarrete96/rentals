package com.rentals.web;

import com.rentals.service.TenantService;
import com.rentals.web.dto.CreateChargeRequest;
import com.rentals.web.dto.CreateTenantRequest;
import com.rentals.web.dto.TenantDetailDto;
import com.rentals.web.dto.TenantSummaryDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * REST endpoints for tenant management (ADMIN role required — enforced by SecurityConfig §15.4).
 *
 * #1 GET  /api/tenants                              → 200 TenantSummaryDto[]
 * #2 GET  /api/tenants/{id}                         → 200 TenantDetailDto | 404
 * #3 POST /api/tenants                              → 201 TenantDetailDto + Location | 409
 * #4 POST /api/tenants/{id}/charges                 → 201 TenantDetailDto
 * #5 PUT  /api/tenants/{id}/charges/{chargeId}/paid → 200 TenantDetailDto
 * #6 PUT  /api/tenants/{id}/paid-months/{yearMonth} → 200 TenantDetailDto (idempotent)
 * #7 DELETE /api/tenants/{id}/paid-months/{yearMonth} → 200 TenantDetailDto (idempotent)
 *
 * ARCHITECTURE §6.1.
 */
@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /** #1 — List all tenants (sorted by unit). */
    @GetMapping
    public ResponseEntity<List<TenantSummaryDto>> list() {
        return ResponseEntity.ok(tenantService.listSummaries());
    }

    /** #2 — Get tenant detail. */
    @GetMapping("/{id}")
    public ResponseEntity<TenantDetailDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.getDetail(id));
    }

    /**
     * #3 — Create tenant + ARRENDATARIO user in one transaction.
     * Returns 409 if the username is already taken.
     */
    @PostMapping
    public ResponseEntity<TenantDetailDto> create(@Valid @RequestBody CreateTenantRequest request) {
        TenantDetailDto created = tenantService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** #4 — Add a one-time charge. Returns 201 with updated detail. */
    @PostMapping("/{id}/charges")
    public ResponseEntity<TenantDetailDto> addCharge(
            @PathVariable Long id,
            @Valid @RequestBody CreateChargeRequest request) {
        TenantDetailDto updated = tenantService.addCharge(id, request);
        return ResponseEntity.status(201).body(updated);
    }

    /** #5 — Mark a charge as paid (idempotent). */
    @PutMapping("/{id}/charges/{chargeId}/paid")
    public ResponseEntity<TenantDetailDto> markChargePaid(
            @PathVariable Long id,
            @PathVariable Long chargeId) {
        return ResponseEntity.ok(tenantService.markChargePaid(id, chargeId));
    }

    /** #6 — Mark a month as paid (idempotent). yearMonth format: YYYY-MM. */
    @PutMapping("/{id}/paid-months/{yearMonth}")
    public ResponseEntity<TenantDetailDto> markMonthPaid(
            @PathVariable Long id,
            @PathVariable String yearMonth) {
        YearMonth ym = parseYearMonth(yearMonth);
        return ResponseEntity.ok(tenantService.markMonthPaid(id, ym));
    }

    /** #7 — Unmark a month as paid (idempotent). */
    @DeleteMapping("/{id}/paid-months/{yearMonth}")
    public ResponseEntity<TenantDetailDto> unmarkMonthPaid(
            @PathVariable Long id,
            @PathVariable String yearMonth) {
        YearMonth ym = parseYearMonth(yearMonth);
        return ResponseEntity.ok(tenantService.unmarkMonthPaid(id, ym));
    }

    // -----------------------------------------------------------------------

    private YearMonth parseYearMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de mes inválido: '" + value + "'. Use YYYY-MM.");
        }
    }
}
