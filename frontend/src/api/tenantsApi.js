/**
 * tenantsApi.js — Endpoints de negocio (§6.1 del contrato dev1↔dev2).
 *
 * TODO: implementar en F-02
 *   - listTenants()                          → GET  /api/tenants
 *   - getTenant(id)                          → GET  /api/tenants/{id}
 *   - createTenant(data)                     → POST /api/tenants
 *   - addCharge(tenantId, data)              → POST /api/tenants/{id}/charges
 *   - markChargePaid(tenantId, chargeId)     → PUT  /api/tenants/{id}/charges/{chargeId}/paid
 *   - markMonthPaid(tenantId, yearMonth)     → PUT  /api/tenants/{id}/paid-months/{yearMonth}
 *   - unmarkMonth(tenantId, yearMonth)       → DELETE /api/tenants/{id}/paid-months/{yearMonth}
 *   - getMyTenant()                          → GET  /api/me/tenant
 */

// TODO: implementar en F-02
export const tenantsApi = {
  listTenants: async () => { throw new Error('tenantsApi.listTenants: pendiente F-02') },
  getTenant: async (_id) => { throw new Error('tenantsApi.getTenant: pendiente F-02') },
  createTenant: async (_data) => { throw new Error('tenantsApi.createTenant: pendiente F-02') },
  addCharge: async (_tenantId, _data) => { throw new Error('tenantsApi.addCharge: pendiente F-02') },
  markChargePaid: async (_tenantId, _chargeId) => { throw new Error('tenantsApi.markChargePaid: pendiente F-02') },
  markMonthPaid: async (_tenantId, _yearMonth) => { throw new Error('tenantsApi.markMonthPaid: pendiente F-02') },
  unmarkMonth: async (_tenantId, _yearMonth) => { throw new Error('tenantsApi.unmarkMonth: pendiente F-02') },
  getMyTenant: async () => { throw new Error('tenantsApi.getMyTenant: pendiente F-02') }
}
