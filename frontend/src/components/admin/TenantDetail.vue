<script setup>
/**
 * TenantDetail.vue — Panel de detalle del arrendatario seleccionado.
 *
 * Props:
 *   tenant   TenantDetailDto | null  — datos completos del arrendatario
 *   loading  boolean                 — estado de carga
 *   error    string | null           — mensaje de error
 *
 * Emite:
 *   markMonthPaid(yearMonth)   — al pulsar "Marcar pagado" en un mes adeudado
 *   markChargePaid(chargeId)   — al pulsar "Marcar pagado" en un cargo único
 *   addCharge                  — al pulsar "Añadir cargo"
 */
import { formatUSD, formatMonth } from '../../utils/format.js'

defineProps({
  tenant: {
    type: Object,
    default: null
  },
  loading: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: null
  }
})

const emit = defineEmits(['markMonthPaid', 'markChargePaid', 'addCharge'])
</script>

<template>
  <section class="detail-panel" aria-live="polite" aria-atomic="true">
    <!-- Estado vacío -->
    <div v-if="!tenant && !loading" class="detail-panel--empty">
      <p>Selecciona un arrendatario para ver su detalle.</p>
    </div>

    <!-- Estado de carga -->
    <div v-else-if="loading" class="detail-panel--empty" role="status" aria-label="Cargando detalle">
      <p class="detail-loading">Cargando…</p>
    </div>

    <!-- Error -->
    <div
      v-else-if="error"
      class="error-banner"
      role="alert"
      aria-live="assertive"
      aria-atomic="true"
    >
      {{ error }}
    </div>

    <!-- Detalle -->
    <template v-else-if="tenant">
      <!-- Encabezado -->
      <header class="detail-header">
        <div class="detail-header-info">
          <h2 class="detail-name">{{ tenant.fullName }}</h2>
          <p class="detail-meta">
            <span>{{ tenant.unit }}</span>
            <span v-if="tenant.contact"> · {{ tenant.contact }}</span>
          </p>
          <p class="detail-meta">
            <span>Día de pago: {{ tenant.paymentDay }}</span>
            <span> · Inicio: {{ tenant.startDate }}</span>
          </p>
          <p class="detail-meta">
            Mensualidad: <strong>{{ formatUSD(tenant.monthlyAmount) }}</strong>
          </p>
          <p v-if="tenant.username" class="detail-meta">
            Usuario: <code class="detail-username">{{ tenant.username }}</code>
          </p>
        </div>
        <span
          class="badge"
          :class="tenant.status === 'AL_DIA' ? 'badge--ok' : 'badge--late'"
        >
          {{ tenant.status === 'AL_DIA' ? 'Al día' : 'Atrasado' }}
        </span>
      </header>

      <!-- Meses adeudados -->
      <div class="section-label">Meses adeudados</div>
      <div v-if="tenant.monthsOwed && tenant.monthsOwed.length > 0" class="months-list">
        <div
          v-for="ym in tenant.monthsOwed"
          :key="ym"
          class="month-tag"
        >
          <span>{{ formatMonth(ym) }}</span>
          <button
            type="button"
            class="btn-mark-paid"
            :aria-label="`Marcar ${formatMonth(ym)} como pagado`"
            @click="emit('markMonthPaid', ym)"
          >
            Marcar pagado
          </button>
        </div>
      </div>
      <p v-else class="detail-empty-state">Sin meses adeudados.</p>

      <!-- Cargos únicos -->
      <div class="section-label">
        <span>Cargos únicos</span>
        <button
          type="button"
          class="btn-add-charge"
          @click="emit('addCharge')"
        >
          + Añadir cargo
        </button>
      </div>
      <div v-if="tenant.oneTimeCharges && tenant.oneTimeCharges.length > 0" class="charges-list">
        <div
          v-for="charge in tenant.oneTimeCharges"
          :key="charge.id"
          class="charge-row"
          :class="{ 'charge-row--paid': charge.paid }"
        >
          <div class="charge-info">
            <span class="charge-description">{{ charge.description }}</span>
            <span class="charge-amount">{{ formatUSD(charge.amount) }}</span>
          </div>
          <div class="charge-actions">
            <span v-if="charge.paid" class="charge-paid-label">Pagado</span>
            <button
              v-else
              type="button"
              class="btn-mark-one-time-paid"
              :aria-label="`Marcar cargo ${charge.description} como pagado`"
              @click="emit('markChargePaid', charge.id)"
            >
              Marcar pagado
            </button>
          </div>
        </div>
      </div>
      <p v-else class="detail-empty-state">Sin cargos únicos.</p>

      <!-- Total adeudado -->
      <div class="total-row">
        <span class="total-label">Total adeudado</span>
        <span class="total-amount">{{ formatUSD(tenant.totalOwed) }}</span>
      </div>
    </template>
  </section>
</template>

<style scoped>
.detail-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--space-xl);
  min-height: 300px;
}

.detail-panel--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.detail-loading {
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.error-banner {
  background: rgba(192, 57, 43, 0.08);
  border: 1px solid rgba(192, 57, 43, 0.28);
  color: var(--color-danger);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.5;
}

/* Header */
.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--color-border);
}

.detail-header-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.detail-name {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
  line-height: 1.3;
}

.detail-meta {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin: 0;
}

.detail-username {
  background: var(--color-surface-alt);
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  font-family: monospace;
  font-size: 0.8125rem;
  color: var(--color-text);
}

/* Badges */
.badge {
  flex-shrink: 0;
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.badge--ok {
  background: rgba(76, 175, 130, 0.12);
  color: var(--color-success);
  border: 1px solid rgba(76, 175, 130, 0.3);
}

.badge--late {
  background: rgba(224, 92, 107, 0.12);
  color: var(--color-danger);
  border: 1px solid rgba(224, 92, 107, 0.3);
}

/* Section labels */
.section-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-xl);
  margin-bottom: var(--space-md);
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

/* Month tags */
.months-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-md);
}

.month-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-accent-dim);
  border: 1px solid rgba(168, 86, 50, 0.22);
  color: var(--color-accent);
  border-radius: var(--radius-md);
  font-size: 0.875rem;
}

/* Mark-paid buttons */
.btn-mark-paid,
.btn-mark-one-time-paid {
  font-size: 0.75rem;
  font-weight: 600;
  font-family: var(--font-body);
  padding: 3px 10px;
  background: rgba(46, 125, 82, 0.10);
  border: 1px solid rgba(46, 125, 82, 0.25);
  color: var(--color-success);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background var(--transition), box-shadow var(--transition);
}

.btn-mark-paid:hover,
.btn-mark-one-time-paid:hover {
  background: rgba(46, 125, 82, 0.18);
  box-shadow: 0 0 0 2px rgba(46, 125, 82, 0.15);
}

.btn-mark-paid:focus-visible,
.btn-mark-one-time-paid:focus-visible {
  outline: 2px solid var(--color-success);
  outline-offset: 2px;
}

/* Add charge button */
.btn-add-charge {
  font-size: 0.75rem;
  font-weight: 600;
  font-family: var(--font-body);
  padding: 3px 10px;
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: border-color var(--transition), color var(--transition);
}

.btn-add-charge:hover {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.btn-add-charge:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

/* Charges */
.charges-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.charge-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.charge-row--paid {
  opacity: 0.6;
}

.charge-info {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex: 1;
  min-width: 0;
}

.charge-description {
  font-size: 0.875rem;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.charge-amount {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text);
  flex-shrink: 0;
}

.charge-actions {
  flex-shrink: 0;
}

.charge-paid-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-success);
}

/* Empty state */
.detail-empty-state {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin: 0;
  padding: var(--space-sm) 0;
}

/* Total row */
.total-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-lg);
  padding: var(--space-lg);
  background: var(--color-surface-alt);
  border-left: 3px solid var(--color-accent);
  border-radius: var(--radius-sm);
}

.total-label {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  font-weight: 500;
}

.total-amount {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text);
}

@media (max-width: 639px) {
  .detail-panel {
    padding: var(--space-lg);
  }

  .detail-header {
    flex-direction: column;
  }
}
</style>
