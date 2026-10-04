import { defineStore } from 'pinia'

/**
 * auth.js — Store Pinia de autenticación (§15.7 ARCHITECTURE.md).
 * Sin persistencia: el access token vive solo en memoria (ADR-009).
 *
 * TODO: implementar en F-02
 *   state:
 *     - status: 'unknown' | 'anonymous' | 'authenticated'
 *     - user:   { id, username, role, tenantId } | null
 *     - accessToken: string | null
 *     - refreshPromise: Promise | null  (deduplica refrescos concurrentes)
 *   getters: isAdmin, isTenant
 *   actions:
 *     - restoreSession()     → POST /api/auth/refresh; ok→authenticated; 401→anonymous
 *     - login(u, p)          → POST /api/auth/login; guarda accessToken + user
 *     - refreshAccessToken() → reutiliza refreshPromise si ya en curso
 *     - logout()             → POST /api/auth/logout; clearSession()
 *     - changePassword(cur, new) → PUT /api/auth/password; clearSession()
 *     - clearSession()       → borra token y user, status='anonymous', tenantsStore.reset()
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    // TODO: implementar en F-02
    status: 'unknown',
    user: null,
    accessToken: null,
    refreshPromise: null
  }),
  getters: {
    // TODO: implementar en F-02
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isTenant: (state) => state.user?.role === 'ARRENDATARIO'
  },
  actions: {
    // TODO: implementar en F-02
    async restoreSession() { this.status = 'anonymous' },
    async login(_username, _password) { throw new Error('auth.login: pendiente F-02') },
    async refreshAccessToken() { throw new Error('auth.refreshAccessToken: pendiente F-02') },
    async logout() { this.clearSession() },
    async changePassword(_current, _next) { throw new Error('auth.changePassword: pendiente F-02') },
    clearSession() {
      this.status = 'anonymous'
      this.user = null
      this.accessToken = null
      this.refreshPromise = null
    }
  }
})
