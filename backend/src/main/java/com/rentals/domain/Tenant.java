package com.rentals.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Arrendatario.
 *
 * paidMonths se mapea como @ElementCollection sobre la tabla paid_month:
 * es un valor (no una entidad), la PK compuesta (tenant_id, year_month)
 * garantiza la idempotencia de "marcar pagado" (ARCHITECTURE §5.3).
 *
 * El campo status NO existe en la entidad: se calcula en runtime por
 * DebtCalculatorService (ADR-006).
 */
@Entity
@Table(name = "tenant")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "unit", nullable = false, length = 40)
    private String unit;

    @Column(name = "contact", nullable = false, length = 120)
    private String contact;

    @Column(name = "payment_day", nullable = false)
    private int paymentDay;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "monthly_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Meses de arriendo pagados.
     * Mapeado a la tabla paid_month con PK (tenant_id, year_month).
     * Se usa Set para que YearMonth.equals() / hashCode() prevengan duplicados
     * a nivel Java antes de llegar a la BD.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "paid_month",
        joinColumns = @JoinColumn(name = "tenant_id")
    )
    @Column(name = "year_month", nullable = false, length = 7)
    @Convert(converter = YearMonthConverter.class)
    private Set<YearMonth> paidMonths = new HashSet<>();

    @OneToMany(
        mappedBy = "tenant",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<OneTimeCharge> oneTimeCharges = new ArrayList<>();

    /** Constructor requerido por JPA. */
    protected Tenant() {}

    /**
     * Constructor con los campos obligatorios al registrar un arrendatario.
     * createdAt lo fija el llamador; la BD tiene DEFAULT now() como respaldo.
     */
    public Tenant(String fullName, String unit, String contact,
                  int paymentDay, LocalDate startDate, BigDecimal monthlyAmount) {
        this.fullName = fullName;
        this.unit = unit;
        this.contact = contact;
        this.paymentDay = paymentDay;
        this.startDate = startDate;
        this.monthlyAmount = monthlyAmount;
        this.createdAt = OffsetDateTime.now();
    }

    // --- Getters ---

    public Long getId() { return id; }

    public String getFullName() { return fullName; }

    public String getUnit() { return unit; }

    public String getContact() { return contact; }

    public int getPaymentDay() { return paymentDay; }

    public LocalDate getStartDate() { return startDate; }

    public BigDecimal getMonthlyAmount() { return monthlyAmount; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public Set<YearMonth> getPaidMonths() { return paidMonths; }

    public List<OneTimeCharge> getOneTimeCharges() { return oneTimeCharges; }

    // --- Setters para campos mutables ---

    public void setFullName(String fullName) { this.fullName = fullName; }

    public void setUnit(String unit) { this.unit = unit; }

    public void setContact(String contact) { this.contact = contact; }

    public void setPaymentDay(int paymentDay) { this.paymentDay = paymentDay; }

    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public void setMonthlyAmount(BigDecimal monthlyAmount) { this.monthlyAmount = monthlyAmount; }
}
