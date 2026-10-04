<script setup>
/**
 * AdminView.vue — Vista principal del administrador (F-03).
 *
 * Orquesta TenantList, TenantDetail, TenantForm y ChargeForm.
 * Solo esta view habla con el store (los componentes hijos reciben props y emiten eventos).
 *
 * Layout: columna izquierda (lista + botón "Nuevo arrendatario") · columna derecha (detalle)
 * Modal/overlay para TenantForm y ChargeForm.
 */
import { computed, onMounted, ref } from 'vue'
import { useTenantsStore } from '../stores/tenants.js'

import TenantList from '../components/admin/TenantList.vue'
import TenantDetail from '../components/admin/TenantDetail.vue'
import TenantForm from '../components/admin/TenantForm.vue'
import ChargeForm from '../components/admin/ChargeForm.vue'

const tenantsStore = useTenantsStore()

// ── Estado local de la UI ────────────────────────────────────────────────────
const showTenantForm = ref(false)
const showChargeForm = ref(false)
const tenantFormError = ref(null)
const tenantFormLoading = ref(false)
const chargeFormLoading = ref(false)

// ── Datos derivados ───────────────────────────────────────────────────────────
const selectedTenant = computed(() =>
  tenantsStore.selectedId !== null
    ? tenantsStore.detailsById[tenantsStore.selectedId] ?? null
    : null
)

// ── Ciclo de vida ─────────────────────────────────────────────────────────────
onMounted(() => {
  tenantsStore.loadSummaries()
})

// ── Manejadores ───────────────────────────────────────────────────────────────

function handleSelectTenant(tenant) {
  tenantsStore.selectTenant(tenant.id)
}

function openTenantForm() {
  tenantFormError.value = null
  showTenantForm.value = true
}

function closeTenantForm() {
  showTenantForm.value = false
  tenantFormError.value = null
}

async function handleCreateTenant(formData) {
  tenantFormLoading.value = true
  tenantFormError.value = null
  try {
    const created = await tenantsStore.createTenant(formData)
    // Seleccionar el arrendatario recién creado
    tenantsStore.selectTenant(created.id)
    closeTenantForm()
  } catch (err) {
    if (err?.status === 409) {
      tenantFormError.value = 'El nombre de usuario ya está en uso. Elige otro.'
    } else {
      tenantFormError.value = err?.detail ?? err?.message ?? 'Error al registrar arrendatario.'
    }
  } finally {
    tenantFormLoading.value = false
  }
}

async function handleMarkMonthPaid(yearMonth) {
  if (!tenantsStore.selectedId) return
  try {
    await tenantsStore.markMonthPaid(tenantsStore.selectedId, yearMonth)
  } catch {
    // El store expone el error; TenantDetail no necesita manejo propio.
  }
}

async function handleMarkChargePaid(chargeId) {
  if (!tenantsStore.selectedId) return
  try {
    await tenantsStore.markChargePaid(tenantsStore.selectedId, chargeId)
  } catch {
    // El store expone el error.
  }
}

function openChargeForm() {
  showChargeForm.value = true
}

function closeChargeForm() {
  showChargeForm.value = false
}

async function handleAddCharge(chargeData) {
  if (!tenantsStore.selectedId) return
  chargeFormLoading.value = true
  try {
    await tenantsStore.addCharge(tenantsStore.selectedId, chargeData)
    closeChargeForm()
  } catch {
    // El store ya guarda el error.
  } finally {
    chargeFormLoading.value = false
  }
}
</script>

<template>
  <main class="main-content">
    <!-- Estado de carga de la lista -->
    <div
      v-if="tenantsStore.loading && tenantsStore.summaries.length === 0"
      class="page-loading"
      role="status"
      aria-label="Cargando arrendatarios"
    >
      <p>Cargando…</p>
    </div>

    <div v-else class="admin-layout">
      <!-- ── Columna izquierda: lista + formulario ──────────────────────── -->
      <aside class="admin-sidebar">
        <div class="sidebar-header">
          <h2 class="section-title">Arrendatarios</h2>
          <button
            type="button"
            class="btn-new-tenant"
            @click="openTenantForm"
          >
            + Nuevo
          </button>
        </div>

        <TenantList
          :tenants="tenantsStore.summaries"
          :selected-id="tenantsStore.selectedId"
          @select="handleSelectTenant"
        />

        <!-- Error al cargar lista -->
        <div
          v-if="tenantsStore.error && tenantsStore.summaries.length === 0"
          class="error-banner"
          role="alert"
          aria-live="assertive"
          aria-atomic="true"
        >
          {{ tenantsStore.error }}
        </div>
      </aside>

      <!-- ── Columna derecha: detalle ──────────────────────────────────── -->
      <section class="admin-main">
        <TenantDetail
          :tenant="selectedTenant"
          :loading="tenantsStore.loading && tenantsStore.selectedId !== null && !selectedTenant"
          :error="tenantsStore.selectedId !== null && !selectedTenant && tenantsStore.error
            ? tenantsStore.error
            : null"
          @mark-month-paid="handleMarkMonthPaid"
          @mark-charge-paid="handleMarkChargePaid"
          @add-charge="openChargeForm"
        />
      </section>
    </div>

    <!-- ── Modal: Registrar arrendatario ──────────────────────────────── -->
    <div
      v-if="showTenantForm"
      class="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="tenant-form-title"
      @click.self="closeTenantForm"
    >
      <div class="modal modal--wide">
        <h2 id="tenant-form-title" class="sr-only">Registrar arrendatario</h2>
        <TenantForm
          :loading="tenantFormLoading"
          :error="tenantFormError"
          @submit="handleCreateTenant"
          @cancel="closeTenantForm"
        />
      </div>
    </div>

    <!-- ── Modal: Añadir cargo único ──────────────────────────────────── -->
    <div
      v-if="showChargeForm"
      class="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="charge-form-title"
      @click.self="closeChargeForm"
    >
      <div class="modal">
        <h2 id="charge-form-title" class="sr-only">Añadir cargo único</h2>
        <ChargeForm
          :loading="chargeFormLoading"
          @submit="handleAddCharge"
          @cancel="closeChargeForm"
        />
      </div>
    </div>
  </main>
</template>

<style scoped>
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-xl);
}

.page-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

/* Layout de dos columnas */
.admin-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: var(--space-xl);
  align-items: start;
}

/* Sidebar izquierdo */
.admin-sidebar {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-title {
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  margin: 0;
}

.btn-new-tenant {
  font-family: var(--font-body);
  font-size: 0.8125rem;
  font-weight: 600;
  background: var(--color-accent);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 6px 14px;
  cursor: pointer;
  transition: background var(--transition);
  min-height: 32px;
}

.btn-new-tenant:hover {
  background: var(--color-accent-hover);
}

.btn-new-tenant:focus-visible {
  outline: 2px solid #fff;
  outline-offset: 3px;
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

/* Columna derecha */
.admin-main {
  min-width: 0; /* evita desbordamiento en grid */
}

/* Modales */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  padding: var(--space-md);
}

.modal {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  max-width: 440px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
}

.modal--wide {
  max-width: 640px;
}

/* Visually hidden, accesible para lectores */
.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}

/* Responsive */
@media (max-width: 1023px) {
  .admin-layout {
    grid-template-columns: 280px 1fr;
    gap: var(--space-lg);
  }
}

@media (max-width: 639px) {
  .main-content {
    padding: var(--space-md);
  }

  .admin-layout {
    grid-template-columns: 1fr;
  }
}
</style>
