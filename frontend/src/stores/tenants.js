/**
 * stores/tenants.js — Store Pinia de arrendatarios (§7 ARCHITECTURE.md).
 *
 * Sin persistencia. No calcula deuda: muestra lo que devuelve el backend.
 * Las acciones de mutación reemplazan el TenantDetailDto con la respuesta del servidor,
 * así no hace falta un GET adicional después de cada cambio.
 */

import { ref } from 'vue'
import { defineStore } from 'pinia'
import { tenantsApi } from '../api/tenantsApi.js'

export const useTenantsStore = defineStore('tenants', () => {
  // ── Estado ────────────────────────────────────────────────────────────────
  const summaries = ref([])           // TenantSummaryDto[]  (lista admin)
  const detailsById = ref({})         // { [id]: TenantDetailDto }  (caché)
  const selectedId = ref(null)        // number | null
  const myAccount = ref(null)         // TenantDetailDto | null  (arrendatario)
  const loading = ref(false)
  const error = ref(null)

  // ── Helpers privados ──────────────────────────────────────────────────────

  function _setLoading(val) {
    loading.value = val
    if (val) error.value = null
  }

  function _updateDetail(detail) {
    detailsById.value = { ...detailsById.value, [detail.id]: detail }
    // Actualizar también el resumen si ya está en la lista.
    summaries.value = summaries.value.map(s =>
      s.id === detail.id
        ? { id: detail.id, fullName: detail.fullName, unit: detail.unit,
            status: detail.status, totalOwed: detail.totalOwed,
            monthsOwedCount: detail.monthsOwed?.length ?? 0 }
        : s
    )
  }

  // ── Acciones admin ────────────────────────────────────────────────────────

  /**
   * Carga la lista de arrendatarios (resúmenes) para la vista admin.
   */
  async function loadSummaries() {
    _setLoading(true)
    try {
      summaries.value = await tenantsApi.listTenants()
    } catch (err) {
      error.value = err.detail ?? err.message ?? 'Error al cargar arrendatarios'
    } finally {
      loading.value = false
    }
  }

  /**
   * Carga el detalle completo de un arrendatario (deuda, cargos, meses).
   * @param {number} id
   */
  async function loadDetail(id) {
    _setLoading(true)
    try {
      const detail = await tenantsApi.getTenant(id)
      _updateDetail(detail)
    } catch (err) {
      error.value = err.detail ?? err.message ?? 'Error al cargar arrendatario'
    } finally {
      loading.value = false
    }
  }

  /**
   * Selecciona el arrendatario activo en la vista admin y carga su detalle si no está en caché.
   * @param {number} id
   */
  async function selectTenant(id) {
    selectedId.value = id
    if (!detailsById.value[id]) {
      await loadDetail(id)
    }
  }

  /**
   * Registra un nuevo arrendatario con sus credenciales.
   * @param {CreateTenantRequest} data
   * @returns {Promise<TenantDetailDto>}
   */
  async function createTenant(data) {
    _setLoading(true)
    try {
      const detail = await tenantsApi.createTenant(data)
      _updateDetail(detail)
      summaries.value = [
        ...summaries.value,
        { id: detail.id, fullName: detail.fullName, unit: detail.unit,
          status: detail.status, totalOwed: detail.totalOwed,
          monthsOwedCount: detail.monthsOwed?.length ?? 0 }
      ]
      return detail
    } catch (err) {
      error.value = err.detail ?? err.message ?? 'Error al registrar arrendatario'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Agrega un cargo único a un arrendatario.
   * @param {number} tenantId
   * @param {CreateChargeRequest} data
   */
  async function addCharge(tenantId, data) {
    _setLoading(true)
    try {
      const detail = await tenantsApi.addCharge(tenantId, data)
      _updateDetail(detail)
    } catch (err) {
      error.value = err.detail ?? err.message ?? 'Error al agregar cargo'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Marca un cargo único como pagado.
   * @param {number} tenantId
   * @param {number} chargeId
   */
  async function markChargePaid(tenantId, chargeId) {
    _setLoading(true)
    try {
      const detail = await tenantsApi.markChargePaid(tenantId, chargeId)
      _updateDetail(detail)
    } catch (err) {
      error.value = err.detail ?? err.message ?? 'Error al marcar cargo'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Marca un mes de arriendo como pagado.
   * @param {number} tenantId
   * @param {string} yearMonth  - Formato 'YYYY-MM'
   */
  async function markMonthPaid(tenantId, yearMonth) {
    _setLoading(true)
    try {
      const detail = await tenantsApi.markMonthPaid(tenantId, yearMonth)
      _updateDetail(detail)
    } catch (err) {
      error.value = err.detail ?? err.message ?? 'Error al marcar mes'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Desmarca un mes de arriendo.
   * @param {number} tenantId
   * @param {string} yearMonth  - Formato 'YYYY-MM'
   */
  async function unmarkMonth(tenantId, yearMonth) {
    _setLoading(true)
    try {
      const detail = await tenantsApi.unmarkMonth(tenantId, yearMonth)
      _updateDetail(detail)
    } catch (err) {
      error.value = err.detail ?? err.message ?? 'Error al desmarcar mes'
      throw err
    } finally {
      loading.value = false
    }
  }

  // ── Acciones arrendatario ─────────────────────────────────────────────────

  /**
   * Carga la cuenta propia del arrendatario autenticado.
   */
  async function loadMyAccount() {
    _setLoading(true)
    try {
      myAccount.value = await tenantsApi.getMyTenant()
    } catch (err) {
      error.value = err.detail ?? err.message ?? 'Error al cargar cuenta'
    } finally {
      loading.value = false
    }
  }

  // ── Sesión ────────────────────────────────────────────────────────────────

  /** Vuelve al estado inicial. Llamado por auth.clearSession() al cerrar sesión. */
  function reset() {
    summaries.value = []
    detailsById.value = {}
    selectedId.value = null
    myAccount.value = null
    loading.value = false
    error.value = null
  }

  // ── API pública ───────────────────────────────────────────────────────────
  return {
    // Estado
    summaries,
    detailsById,
    selectedId,
    myAccount,
    loading,
    error,
    // Acciones admin
    loadSummaries,
    loadDetail,
    selectTenant,
    createTenant,
    addCharge,
    markChargePaid,
    markMonthPaid,
    unmarkMonth,
    // Acciones arrendatario
    loadMyAccount,
    // Sesión
    reset
  }
})
