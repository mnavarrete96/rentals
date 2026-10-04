package com.rentals.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Cargo único asociado a un arrendatario (garantía, daños, etc.).
 * Puede estar pagado o pendiente; se incluye en el cálculo de deuda si paid=false.
 */
@Entity
@Table(name = "one_time_charge")
public class OneTimeCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "description", nullable = false, length = 200)
    private String description;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid", nullable = false)
    private boolean paid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Constructor requerido por JPA. */
    protected OneTimeCharge() {}

    /**
     * Crea un cargo único no pagado.
     * tenant debe ser una entidad ya gestionada o adjuntada antes de persistir.
     */
    public OneTimeCharge(Tenant tenant, String description, BigDecimal amount) {
        this.tenant = tenant;
        this.description = description;
        this.amount = amount;
        this.paid = false;
        this.createdAt = OffsetDateTime.now();
    }

    // --- Getters ---

    public Long getId() { return id; }

    public Tenant getTenant() { return tenant; }

    public String getDescription() { return description; }

    public BigDecimal getAmount() { return amount; }

    public boolean isPaid() { return paid; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    // --- Setter para marcar pagado ---

    public void setPaid(boolean paid) { this.paid = paid; }
}
