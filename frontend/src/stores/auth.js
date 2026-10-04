/**
 * stores/auth.js — Store Pinia de autenticación (§15.7 ARCHITECTURE.md).
 *
 * Sin persistencia: el access token vive solo en memoria (ADR-009).
 * Nunca escribe en localStorage, sessionStorage ni cookies accesibles por JS.
 *
 * Al inicializarse, llama a configureAuth para inyectar las dependencias en http.js (DIP).
 * refreshPromise deduplica los refrescos concurrentes: si ya hay uno en curso, los llamadores
 * comparten la misma Promise en lugar de lanzar múltiples peticiones.
 */

import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { configureAuth, ApiError } from '../api/http.js'
import { authApi } from '../api/authApi.js'

export const useAuthStore = defineStore('auth', () => {
  // ── Estado ────────────────────────────────────────────────────────────────
  const status = ref('unknown')        // 'unknown' | 'anonymous' | 'authenticated'
  const user = ref(null)               // { id, username, role, tenantId } | null
  const accessToken = ref(null)        // string | null  (solo memoria)
  const refreshPromise = ref(null)     // Promise | null (deduplicación)

  // ── Getters ───────────────────────────────────────────────────────────────
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isTenant = computed(() => user.value?.role === 'ARRENDATARIO')
  const isAuthenticated = computed(() => status.value === 'authenticated')

  // ── Acciones ──────────────────────────────────────────────────────────────

  /**
   * Intenta restaurar la sesión al cargar la app usando la cookie HttpOnly de refresh.
   * Si tiene éxito → status = 'authenticated'.
   * Si el backend responde 401 (cookie expirada o ausente) → status = 'anonymous'.
   */
  async function restoreSession() {
    try {
      const data = await authApi.refresh()
      _applyAuthData(data)
    } catch (err) {
      // 401 = no hay sesión activa; cualquier otro error también → anónimo
      status.value = 'anonymous'
    }
  }

  /**
   * Inicia sesión con usuario y contraseña.
   * Guarda el access token y los datos del usuario en memoria.
   * @param {string} username
   * @param {string} password
   * @throws {ApiError} Si el backend responde 401 u otro error.
   */
  async function login(username, password) {
    const data = await authApi.login(username, password)
    _applyAuthData(data)
  }

  /**
   * Renueva el access token usando la cookie de refresh.
   * Deduplica: si ya hay un refresh en curso, reutiliza la misma Promise.
   * Llamado automáticamente por http.js cuando una petición devuelve 401.
   * @returns {Promise}
   */
  async function refreshAccessToken() {
    if (!refreshPromise.value) {
      refreshPromise.value = authApi.refresh()
        .then(data => {
          _applyAuthData(data)
          return data
        })
        .finally(() => {
          refreshPromise.value = null
        })
    }
    return refreshPromise.value
  }

  /**
   * Cierra sesión. Revoca la cookie de refresh en el servidor y limpia el estado local.
   */
  async function logout() {
    try {
      await authApi.logout()
    } catch {
      // Ignorar errores de red: la sesión local se limpia de todas formas.
    }
    clearSession()
  }

  /**
   * Cambia la contraseña del usuario autenticado.
   * El backend revoca todas las sesiones; el frontend vuelve al login.
   * @param {string} currentPassword
   * @param {string} newPassword
   * @throws {ApiError} Si la contraseña actual es incorrecta (401/400) u otro error.
   */
  async function changePassword(currentPassword, newPassword) {
    await authApi.changePassword(currentPassword, newPassword)
    clearSession()
  }

  /**
   * Limpia el estado de autenticación. Llamado en logout, changePassword y tras refresh fallido.
   * También resetea el store de tenants para no dejar datos de otro usuario en memoria.
   */
  function clearSession() {
    status.value = 'anonymous'
    user.value = null
    accessToken.value = null
    refreshPromise.value = null

    // Resetear el store de tenants para limpiar datos del usuario anterior.
    // La importación dinámica evita dependencia circular en el módulo.
    import('./tenants.js').then(({ useTenantsStore }) => {
      try {
        useTenantsStore().reset()
      } catch {
        // Si Pinia no está activa (p.ej. en tests unitarios aislados), ignorar.
      }
    })
  }

  // ── Privado ───────────────────────────────────────────────────────────────

  /** Aplica la respuesta AuthResponse al estado del store. */
  function _applyAuthData(data) {
    accessToken.value = data.accessToken
    user.value = data.user
    status.value = 'authenticated'
  }

  // ── Inyección de dependencias en http.js (DIP, §7 ARCHITECTURE.md) ────────
  // Se ejecuta una vez al inicializar el store. Conecta http.js con este store
  // sin que http.js importe el store directamente (evita dependencias circulares).
  configureAuth({
    getAccessToken: () => accessToken.value,
    refreshAccessToken: refreshAccessToken,
    onAuthFailure: clearSession
  })

  // ── API pública del store ─────────────────────────────────────────────────
  return {
    // Estado
    status,
    user,
    accessToken,
    refreshPromise,
    // Getters
    isAdmin,
    isTenant,
    isAuthenticated,
    // Acciones
    restoreSession,
    login,
    refreshAccessToken,
    logout,
    changePassword,
    clearSession
  }
})
