package com.rentals.service;

import com.rentals.domain.OneTimeCharge;
import com.rentals.domain.Tenant;
import com.rentals.repository.AppUserRepository;
import com.rentals.repository.OneTimeChargeRepository;
import com.rentals.repository.TenantRepository;
import com.rentals.web.dto.CreateChargeRequest;
import com.rentals.web.dto.CreateTenantRequest;
import com.rentals.web.dto.OneTimeChargeDto;
import com.rentals.web.dto.TenantDetailDto;
import com.rentals.web.dto.TenantSummaryDto;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

/**
 * Business use-cases for tenants: create, list, detail, mark months paid, manage charges.
 *
 * All write operations are transactional. If UsernameTakenException is thrown inside create(),
 * the outer transaction rolls back and no Tenant is persisted (atomic).
 *
 * ARCHITECTURE §4.2, §6.1, §15.5.
 */
@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;
    private final OneTimeChargeRepository chargeRepository;
    private final AppUserRepository userRepository;
    private final DebtCalculatorService debtCalculatorService;
    private final UserAccountService userAccountService;
    private final Clock clock;

    public TenantService(TenantRepository tenantRepository,
                         OneTimeChargeRepository chargeRepository,
                         AppUserRepository userRepository,
                         DebtCalculatorService debtCalculatorService,
                         UserAccountService userAccountService,
                         Clock clock) {
        this.tenantRepository = tenantRepository;
        this.chargeRepository = chargeRepository;
        this.userRepository = userRepository;
        this.debtCalculatorService = debtCalculatorService;
        this.userAccountService = userAccountService;
        this.clock = clock;
    }

    /**
     * Lists all tenants sorted by unit, including debt summary.
     * Uses hibernate.default_batch_fetch_size=50 to avoid N+1 on collections.
     */
    @Transactional(readOnly = true)
    public List<TenantSummaryDto> listSummaries() {
        YearMonth currentMonth = YearMonth.now(clock);
        return tenantRepository.findAll(Sort.by("unit")).stream()
                .map(tenant -> {
                    // Touch collections to initialize them within this transaction
                    tenant.getPaidMonths().size();
                    tenant.getOneTimeCharges().size();
                    DebtSummary debt = debtCalculatorService.calculate(tenant, currentMonth);
                    return new TenantSummaryDto(
                            tenant.getId(),
                            tenant.getFullName(),
                            tenant.getUnit(),
                            debt.status(),
                            debt.totalOwed(),
                            debt.monthsOwed().size()
                    );
                })
                .toList();
    }

    /**
     * Returns full detail for a single tenant, including debt calculation.
     */
    @Transactional(readOnly = true)
    public TenantDetailDto getDetail(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
        return buildDetailDto(tenant);
    }

    /**
     * Creates a new tenant and an ARRENDATARIO user account in a single transaction.
     * Throws UsernameTakenException (→ 409) if the username is already in use.
     * ARCHITECTURE §6.1 endpoint #3, §15.5.
     */
    public TenantDetailDto create(CreateTenantRequest req) {
        Tenant tenant = new Tenant(
                req.fullName(),
                req.unit(),
                req.contact(),
                req.paymentDay(),
                req.startDate(),
                req.monthlyAmount()
        );
        tenantRepository.save(tenant);
        // Participates in the same transaction (default REQUIRED propagation).
        // UsernameTakenException causes the whole transaction to roll back.
        userAccountService.createTenantUser(tenant, req.username(), req.initialPassword());
        return buildDetailDto(tenant);
    }

    /**
     * Marks a month as paid for the given tenant (idempotent via Set + PK).
     * ARCHITECTURE §6.1 endpoint #6.
     */
    public TenantDetailDto markMonthPaid(Long tenantId, YearMonth yearMonth) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        tenant.getPaidMonths().add(yearMonth);
        return buildDetailDto(tenant);
    }

    /**
     * Removes a month from the paid set (idempotent).
     * ARCHITECTURE §6.1 endpoint #7.
     */
    public TenantDetailDto unmarkMonthPaid(Long tenantId, YearMonth yearMonth) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        tenant.getPaidMonths().remove(yearMonth);
        return buildDetailDto(tenant);
    }

    /**
     * Adds a one-time charge to the tenant.
     * ARCHITECTURE §6.1 endpoint #4.
     */
    public TenantDetailDto addCharge(Long tenantId, CreateChargeRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        OneTimeCharge charge = new OneTimeCharge(tenant, req.description(), req.amount());
        tenant.getOneTimeCharges().add(charge);
        return buildDetailDto(tenant);
    }

    /**
     * Marks an existing charge as paid.
     * Returns 404 if the charge does not exist or does not belong to the tenant.
     * ARCHITECTURE §6.1 endpoint #5.
     */
    public TenantDetailDto markChargePaid(Long tenantId, Long chargeId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        OneTimeCharge charge = chargeRepository.findById(chargeId)
                .orElseThrow(() -> new TenantNotFoundException(
                        "Cargo no encontrado: " + chargeId));
        if (!charge.getTenant().getId().equals(tenantId)) {
            throw new TenantNotFoundException("Cargo no pertenece al arrendatario: " + chargeId);
        }
        charge.setPaid(true);
        return buildDetailDto(tenant);
    }

    /**
     * Returns the tenant detail for the currently authenticated ARRENDATARIO.
     * tenantId is extracted from the JWT claim by the controller.
     * ARCHITECTURE §6.1 endpoint #8.
     */
    @Transactional(readOnly = true)
    public TenantDetailDto getMyTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        return buildDetailDto(tenant);
    }

    // -----------------------------------------------------------------------
    // Private mapping helper
    // -----------------------------------------------------------------------

    /**
     * Maps a managed Tenant entity to TenantDetailDto.
     * Collections must be initialized (or will be loaded lazily within the active transaction).
     */
    private TenantDetailDto buildDetailDto(Tenant tenant) {
        YearMonth currentMonth = YearMonth.now(clock);

        // Ensure lazy collections are initialized
        tenant.getPaidMonths().size();
        tenant.getOneTimeCharges().size();

        DebtSummary debt = debtCalculatorService.calculate(tenant, currentMonth);

        String username = userRepository.findByTenantId(tenant.getId())
                .map(u -> u.getUsername())
                .orElse(null);

        List<String> paidMonths = tenant.getPaidMonths().stream()
                .sorted()
                .map(YearMonth::toString)
                .toList();

        List<OneTimeChargeDto> charges = tenant.getOneTimeCharges().stream()
                .map(c -> new OneTimeChargeDto(c.getId(), c.getDescription(), c.getAmount(), c.isPaid()))
                .toList();

        List<String> monthsOwed = debt.monthsOwed().stream()
                .map(YearMonth::toString)
                .toList();

        return new TenantDetailDto(
                tenant.getId(),
                tenant.getFullName(),
                tenant.getUnit(),
                tenant.getContact(),
                username,
                tenant.getPaymentDay(),
                tenant.getStartDate(),
                tenant.getMonthlyAmount(),
                paidMonths,
                charges,
                monthsOwed,
                debt.totalOwed(),
                debt.status()
        );
    }
}
