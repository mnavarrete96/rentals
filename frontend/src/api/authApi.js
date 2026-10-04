/**
 * authApi.js — Endpoints de autenticación (§6.2 ARCHITECTURE.md).
 *
 * Todas las llamadas usan apiFetch. Las rutas /api/auth/* van con auth: false
 * (nunca llevan Bearer, conforme §15.7 punto 5).
 */

import { apiFetch } from './http.js'

export const authApi = {
  /**
   * Inicia sesión.
   * POST /api/auth/login → 200 AuthResponse | 401
   * @param {string} username
   * @param {string} password
   * @returns {Promise<{accessToken, tokenType, expiresIn, user}>}
   */
  login: (username, password) =>
    apiFetch('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
      auth: false
    }),

  /**
   * Renueva el access token usando la cookie HttpOnly rentals_refresh.
   * POST /api/auth/refresh → 200 AuthResponse | 401
   * @returns {Promise<{accessToken, tokenType, expiresIn, user}>}
   */
  refresh: () =>
    apiFetch('/api/auth/refresh', {
      method: 'POST',
      auth: false
    }),

  /**
   * Cierra sesión. Revoca la cookie de refresh. Siempre 204.
   * POST /api/auth/logout → 204
   * @returns {Promise<null>}
   */
  logout: () =>
    apiFetch('/api/auth/logout', {
      method: 'POST',
      auth: false
    }),

  /**
   * Cambia la contraseña del usuario autenticado. Cierra todas las sesiones.
   * PUT /api/auth/password → 204 | 400 | 401
   * @param {string} currentPassword
   * @param {string} newPassword
   * @returns {Promise<null>}
   */
  changePassword: (currentPassword, newPassword) =>
    apiFetch('/api/auth/password', {
      method: 'PUT',
      body: JSON.stringify({ currentPassword, newPassword })
    })
}
