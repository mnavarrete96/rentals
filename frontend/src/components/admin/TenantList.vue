<script setup>
/**
 * TenantList.vue — Lista de tarjetas de arrendatarios (sidebar izquierdo de AdminView).
 *
 * Props:
 *   tenants  TenantSummaryDto[]  — lista de arrendatarios del store
 *   selectedId  number | null   — ID del arrendatario activo
 *
 * Emite:
 *   select(tenant)  — cuando el usuario hace clic en una tarjeta
 */

defineProps({
  tenants: {
    type: Array,
    default: () => []
  },
  selectedId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['select'])
</script>

<template>
  <div class="tenant-list-wrapper">
    <p
      v-if="tenants.length === 0"
      class="tenant-list-empty"
    >
      No hay arrendatarios registrados.
    </p>

    <ul
      v-else
      class="tenant-list"
      role="list"
      aria-label="Lista de arrendatarios"
    >
      <li
        v-for="tenant in tenants"
        :key="tenant.id"
      >
        <button
          type="button"
          class="card"
          :class="{ 'card--active': tenant.id === selectedId }"
          :aria-pressed="tenant.id === selectedId"
          :aria-label="`${tenant.fullName}, unidad ${tenant.unit}`"
          @click="emit('select', tenant)"
        >
          <div class="card-header">
            <span class="card-name">{{ tenant.fullName }}</span>
            <span
              class="badge"
              :class="tenant.status === 'AL_DIA' ? 'badge--ok' : 'badge--late'"
            >
              {{ tenant.status === 'AL_DIA' ? 'Al día' : 'Atrasado' }}
            </span>
          </div>
          <span class="card-unit">{{ tenant.unit }}</span>
        </button>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.tenant-list-wrapper {
  width: 100%;
}

.tenant-list-empty {
  color: var(--color-text-muted);
  font-size: 0.875rem;
  padding: var(--space-md);
  text-align: center;
}

.tenant-list {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 420px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  scrollbar-width: thin;
  scrollbar-color: var(--color-border) transparent;
}

.tenant-list::-webkit-scrollbar {
  width: 4px;
}

.tenant-list::-webkit-scrollbar-track {
  background: transparent;
}

.tenant-list::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 4px;
}

.card {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  width: 100%;
  padding: var(--space-md);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  text-align: left;
  transition: background var(--transition), border-color var(--transition), box-shadow var(--transition);
  font-family: var(--font-body);
}

.card:hover {
  background: var(--color-surface-alt);
  border-color: var(--color-border-hover);
  box-shadow: var(--shadow-md);
}

.card:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

.card--active {
  background: var(--color-surface-alt);
  border-color: var(--color-border-hover);
  border-left: 3px solid var(--color-accent);
  box-shadow: var(--shadow-md);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}

.card-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-unit {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  font-weight: 400;
}

.badge {
  flex-shrink: 0;
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  line-height: 1.4;
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

@media (max-width: 639px) {
  .tenant-list {
    max-height: 260px;
  }
}
</style>
