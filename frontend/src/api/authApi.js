/**
 * authApi.js — Endpoints de autenticación (§6.2 del contrato dev1↔dev2).
 *
 * TODO: implementar en F-02
 *   - login(username, password)       → POST /api/auth/login  (auth: false)
 *   - refresh()                       → POST /api/auth/refresh (auth: false)
 *   - logout()                        → POST /api/auth/logout  (auth: false)
 *   - changePassword(current, next)   → PUT  /api/auth/password (auth: true)
 */

// TODO: implementar en F-02
export const authApi = {
  login: async (_username, _password) => { throw new Error('authApi.login: pendiente F-02') },
  refresh: async () => { throw new Error('authApi.refresh: pendiente F-02') },
  logout: async () => { throw new Error('authApi.logout: pendiente F-02') },
  changePassword: async (_current, _next) => { throw new Error('authApi.changePassword: pendiente F-02') }
}
