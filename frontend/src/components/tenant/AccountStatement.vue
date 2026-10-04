<script setup>
/**
 * AccountStatement.vue — Estado de cuenta del arrendatario autenticado (F-04).
 *
 * Muestra nombre, unidad, badge de estado, meses adeudados, cargos únicos no pagados
 * y el total adeudado. El arrendatario no puede marcar pagos: es solo lectura.
 *
 * Props:
 *   tenant   {Object|null}  TenantDetailDto del backend (GET /api/me/tenant)
 *   loading  {boolean}
 *   error    {string|null}
 */
import { computed } from 'vue'
import { formatUSD, formatMonth } from '../../utils/format.js'

const props = defineProps({
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

const unpaidCharges = computed(() =>
  props.tenant?.oneTimeCharges?.filter(c => !c.paid) ?? []
)

const totalOwed = computed(() => props.tenant?.totalOwed ?? 0)
const isUpToDate = computed(() => totalOwed.value === 0)
</script>

<template>
  <!-- Estado de carga -->
  <div
    v-if="loading"
    class="account-loading"
    role="status"
    aria-label="Cargando estado de cuenta"
  >
    <p class="loading-text">Cargando…</p>
  </div>

  <!-- Estado de error -->
  <div
    v-else-if="error"
    class="error-banner"
    role="alert"
    aria-live="assertive"
    aria-atomic="true"
  >
    {{ error }}
  </div>

  <!-- Sin datos aún (no error, no cargando) -->
  <div v-else-if="!tenant" class="account-empty">
    <p>No hay datos disponibles.</p>
  </div>

  <!-- Contenido principal -->
  <div v-else class="detail-panel">
    <!-- Cabecera: nombre + badge -->
    <div class="account-header">
      <div class="account-info">
        <h2 class="account-name">{{ tenant.fullName }}</h2>
        <p class="account-unit">{{ tenant.unit }}</p>
      </div>
      <span
        class="badge"
        :class="tenant.status === 'AL_DIA' ? 'badge--ok' : 'badge--late'"
      >
        {{ tenant.status === 'AL_DIA' ? 'Al día' : 'Atrasado' }}
      </span>
    </div>

    <!-- Total adeudado -->
    <div v-if="isUpToDate" class="up-to-date-message">
      Estás al día. No tienes deudas pendientes.
    </div>

    <div v-else>
      <!-- Meses adeudados -->
      <div v-if="tenant.monthsOwed && tenant.monthsOwed.length > 0">
        <p class="section-label">Meses que debes</p>
        <ul class="months-list" role="list">
          <li
            v-for="month in tenant.monthsOwed"
            :key="month"
            class="month-tag month-tag--danger"
          >
            {{ formatMonth(month) }}
          </li>
        </ul>
      </div>

      <!-- Cargos únicos no pagados -->
      <div v-if="unpaidCharges.length > 0">
        <p class="section-label">Cargos adicionales</p>
        <ul class="charges-list" role="list">
          <li
            v-for="charge in unpaidCharges"
            :key="charge.id"
            class="charge-row"
          >
            <span class="charge-description">{{ charge.description }}</span>
            <span class="charge-amount">{{ formatUSD(charge.amount) }}</span>
          </li>
        </ul>
      </div>

      <!-- Total -->
      <div class="total-row">
        <span class="total-label">Total adeudado</span>
        <span class="total-amount">{{ formatUSD(totalOwed) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Panel base (reutiliza tokens de DESIGN.md §5) */
.detail-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--space-xl);
}

/* Cabecera de cuenta */
.account-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--color-border);
}

.account-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.account-name {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
  line-height: 1.3;
}

.account-unit {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-muted);
  margin: 0;
}

/* Badge de estado */
.badge {
  display: inline-block;
  border-radius: 999px;
  padding: 3px 10px;
  font-size: 0.75rem;
  font-weight: 600;
  flex-shrink: 0;
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

/* Mensaje al día */
.up-to-date-message {
  text-align: center;
  padding: var(--space-xl);
  color: var(--color-success);
  font-size: 1rem;
  font-weight: 600;
}

/* Etiqueta de sección */
.section-label {
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  margin: var(--space-xl) 0 var(--space-md) 0;
}

/* Lista de meses */
.months-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-md);
}

.month-tag {
  display: inline-flex;
  align-items: center;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  font-weight: 500;
}

.month-tag--danger {
  background: rgba(192, 57, 43, 0.08);
  border: 1px solid rgba(192, 57, 43, 0.22);
  color: var(--color-danger);
}

/* Lista de cargos */
.charges-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.charge-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-surface-alt);
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  color: var(--color-text);
}

.charge-description {
  flex: 1;
  margin-right: var(--space-md);
}

.charge-amount {
  font-weight: 600;
  color: var(--color-danger);
  white-space: nowrap;
}

/* Total row */
.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--color-surface-alt);
  border-left: 3px solid var(--color-accent);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  padding: var(--space-lg);
  margin-top: var(--space-lg);
}

.total-label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-muted);
}

.total-amount {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-text);
}

/* Estado de carga */
.account-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
}

.loading-text {
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

/* Error banner */
.error-banner {
  background: rgba(192, 57, 43, 0.08);
  border: 1px solid rgba(192, 57, 43, 0.28);
  color: var(--color-danger);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  font-size: 0.875rem;
  font-weight: 600;
}

/* Sin datos */
.account-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

/* Responsive */
@media (max-width: 639px) {
  .detail-panel {
    padding: var(--space-lg);
  }

  .account-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .total-row {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-xs);
  }
}
</style>
