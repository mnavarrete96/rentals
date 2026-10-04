import { defineStore } from 'pinia'

/**
 * tenants.js — Store Pinia de arrendatarios (§7 ARCHITECTURE.md).
 * Sin persistencia. No calcula deuda (eso lo hace el backend).
 *
 * TODO: implementar en F-02
 *   state:
 *     - summaries: TenantSummaryDto[]         (lista admin)
 *     - detailsById: { [id]: TenantDetailDto } (cache de detalles)
 *     - selectedId: number | null
 *     - myAccount: TenantDetailDto | null      (vista arrendatario)
 *     - loading: boolean
 *     - error: string | null
 *   actions (admin):
 *     - loadSummaries()
 *     - loadDetail(id)
 *     - selectTenant(id)
 *     - createTenant(data)
 *     - addCharge(tenantId, data)
 *     - markChargePaid(tenantId, chargeId)
 *     - markMonthPaid(tenantId, yearMonth)
 *     - unmarkMonth(tenantId, yearMonth)
 *   actions (arrendatario):
 *     - loadMyAccount()
 *   actions (sesión):
 *     - reset()  ← llamado por auth.clearSession()
 */
export const useTenantsStore = defineStore('tenants', {
  state: () => ({
    // TODO: implementar en F-02
    summaries: [],
    detailsById: {},
    selectedId: null,
    myAccount: null,
    loading: false,
    error: null
  }),
  actions: {
    // TODO: implementar en F-02
    reset() {
      this.summaries = []
      this.detailsById = {}
      this.selectedId = null
      this.myAccount = null
      this.loading = false
      this.error = null
    }
  }
})
