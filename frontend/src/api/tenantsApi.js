/**
 * tenantsApi.js — Endpoints de negocio (§6.1 ARCHITECTURE.md).
 *
 * Todas las llamadas requieren autenticación (auth: true por defecto).
 * El backend devuelve TenantDetailDto actualizado en las mutaciones,
 * así el store reemplaza el arrendatario sin un GET adicional.
 */

import { apiFetch } from './http.js'

export const tenantsApi = {
  /**
   * Lista todos los arrendatarios (ADMIN).
   * GET /api/tenants → 200 TenantSummaryDto[]
   * @returns {Promise<TenantSummaryDto[]>}
   */
  listTenants: () =>
    apiFetch('/api/tenants'),

  /**
   * Obtiene el detalle de un arrendatario (ADMIN).
   * GET /api/tenants/{id} → 200 TenantDetailDto | 404
   * @param {number} id
   * @returns {Promise<TenantDetailDto>}
   */
  getTenant: (id) =>
    apiFetch(`/api/tenants/${id}`),

  /**
   * Registra un nuevo arrendatario con sus credenciales (ADMIN).
   * POST /api/tenants → 201 TenantDetailDto | 409 username duplicado
   * @param {CreateTenantRequest} data
   * @returns {Promise<TenantDetailDto>}
   */
  createTenant: (data) =>
    apiFetch('/api/tenants', {
      method: 'POST',
      body: JSON.stringify(data)
    }),

  /**
   * Agrega un cargo único al arrendatario (ADMIN).
   * POST /api/tenants/{id}/charges → 201 TenantDetailDto
   * @param {number} tenantId
   * @param {CreateChargeRequest} data
   * @returns {Promise<TenantDetailDto>}
   */
  addCharge: (tenantId, data) =>
    apiFetch(`/api/tenants/${tenantId}/charges`, {
      method: 'POST',
      body: JSON.stringify(data)
    }),

  /**
   * Marca un cargo único como pagado (ADMIN, idempotente).
   * PUT /api/tenants/{id}/charges/{chargeId}/paid → 200 TenantDetailDto
   * @param {number} tenantId
   * @param {number} chargeId
   * @returns {Promise<TenantDetailDto>}
   */
  markChargePaid: (tenantId, chargeId) =>
    apiFetch(`/api/tenants/${tenantId}/charges/${chargeId}/paid`, {
      method: 'PUT'
    }),

  /**
   * Marca un mes de arriendo como pagado (ADMIN, idempotente).
   * PUT /api/tenants/{id}/paid-months/{yearMonth} → 200 TenantDetailDto
   * @param {number} tenantId
   * @param {string} yearMonth  - Formato 'YYYY-MM'
   * @returns {Promise<TenantDetailDto>}
   */
  markMonthPaid: (tenantId, yearMonth) =>
    apiFetch(`/api/tenants/${tenantId}/paid-months/${yearMonth}`, {
      method: 'PUT'
    }),

  /**
   * Desmarca un mes de arriendo (ADMIN, idempotente).
   * DELETE /api/tenants/{id}/paid-months/{yearMonth} → 200 TenantDetailDto
   * @param {number} tenantId
   * @param {string} yearMonth  - Formato 'YYYY-MM'
   * @returns {Promise<TenantDetailDto>}
   */
  unmarkMonth: (tenantId, yearMonth) =>
    apiFetch(`/api/tenants/${tenantId}/paid-months/${yearMonth}`, {
      method: 'DELETE'
    }),

  /**
   * Obtiene la cuenta propia del arrendatario autenticado.
   * GET /api/me/tenant → 200 TenantDetailDto
   * El backend obtiene el tenantId del JWT; nunca se expone en la URL (previene IDOR).
   * @returns {Promise<TenantDetailDto>}
   */
  getMyTenant: () =>
    apiFetch('/api/me/tenant')
}
