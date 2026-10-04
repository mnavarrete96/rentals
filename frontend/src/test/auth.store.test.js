/**
 * auth.store.test.js — Tests de stores/auth.js (§15.8 ARCHITECTURE.md).
 *
 * Verifica:
 * 1. login exitoso → status=authenticated, token y user guardados.
 * 2. login fallido (401) → lanza ApiError, status no cambia.
 * 3. logout → clearSession, status=anonymous.
 * 4. restoreSession con cookie válida → status=authenticated.
 * 5. restoreSession sin cookie (401) → status=anonymous.
 * 6. El access token NUNCA se escribe en localStorage.
 * 7. refreshAccessToken deduplica: múltiples llamadas concurrentes comparten la misma Promise.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../stores/auth.js'
import { ApiError } from '../api/http.js'

// ── Mock de authApi ────────────────────────────────────────────────────────────
// Interceptamos el módulo completo para controlar las respuestas del servidor.
vi.mock('../api/authApi.js', () => ({
  authApi: {
    login: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
    changePassword: vi.fn()
  }
}))

// Importamos el mock después de declararlo para obtener las referencias.
import { authApi } from '../api/authApi.js'

// ── Mock de tenantsApi para evitar errores en clearSession ────────────────────
vi.mock('../api/tenantsApi.js', () => ({
  tenantsApi: {
    listTenants: vi.fn(),
    getTenant: vi.fn(),
    createTenant: vi.fn(),
    addCharge: vi.fn(),
    markChargePaid: vi.fn(),
    markMonthPaid: vi.fn(),
    unmarkMonth: vi.fn(),
    getMyTenant: vi.fn()
  }
}))

// ── Fixture ───────────────────────────────────────────────────────────────────

const ADMIN_AUTH_RESPONSE = {
  accessToken: 'eyJhbGciOiJIUzI1NiJ9.admin',
  tokenType: 'Bearer',
  expiresIn: 900,
  user: { id: 1, username: 'admin', role: 'ADMIN', tenantId: null }
}

const TENANT_AUTH_RESPONSE = {
  accessToken: 'eyJhbGciOiJIUzI1NiJ9.tenant',
  tokenType: 'Bearer',
  expiresIn: 900,
  user: { id: 2, username: 'maria.perez', role: 'ARRENDATARIO', tenantId: 1 }
}

// ── Setup ─────────────────────────────────────────────────────────────────────

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
})

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('login', () => {
  it('login exitoso → status=authenticated, accessToken y user guardados en memoria', async () => {
    authApi.login.mockResolvedValue(ADMIN_AUTH_RESPONSE)

    const auth = useAuthStore()
    await auth.login('admin', 'adminpass123')

    expect(auth.status).toBe('authenticated')
    expect(auth.accessToken).toBe(ADMIN_AUTH_RESPONSE.accessToken)
    expect(auth.user).toEqual(ADMIN_AUTH_RESPONSE.user)
    expect(auth.isAdmin).toBe(true)
    expect(auth.isTenant).toBe(false)
    expect(auth.isAuthenticated).toBe(true)
  })

  it('login con credenciales incorrectas (401) → lanza ApiError, status no cambia', async () => {
    const error = new ApiError(401, { title: 'Unauthorized', detail: 'Usuario o contraseña incorrectos' })
    authApi.login.mockRejectedValue(error)

    const auth = useAuthStore()
    // El status inicial es 'unknown'; no debe cambiar si el login falla.
    const statusBefore = auth.status

    await expect(auth.login('admin', 'wrongpass')).rejects.toBeInstanceOf(ApiError)
    expect(auth.status).toBe(statusBefore)
    expect(auth.accessToken).toBeNull()
    expect(auth.user).toBeNull()
  })

  it('login como arrendatario → isTenant=true, isAdmin=false', async () => {
    authApi.login.mockResolvedValue(TENANT_AUTH_RESPONSE)

    const auth = useAuthStore()
    await auth.login('maria.perez', 'password-1234')

    expect(auth.status).toBe('authenticated')
    expect(auth.isTenant).toBe(true)
    expect(auth.isAdmin).toBe(false)
    expect(auth.user.tenantId).toBe(1)
  })
})

describe('restoreSession', () => {
  it('con cookie de refresh válida → status=authenticated', async () => {
    authApi.refresh.mockResolvedValue(ADMIN_AUTH_RESPONSE)

    const auth = useAuthStore()
    await auth.restoreSession()

    expect(auth.status).toBe('authenticated')
    expect(auth.accessToken).toBe(ADMIN_AUTH_RESPONSE.accessToken)
    expect(auth.user).toEqual(ADMIN_AUTH_RESPONSE.user)
  })

  it('sin cookie de refresh o expirada (401) → status=anonymous', async () => {
    authApi.refresh.mockRejectedValue(
      new ApiError(401, { title: 'Unauthorized' })
    )

    const auth = useAuthStore()
    await auth.restoreSession()

    expect(auth.status).toBe('anonymous')
    expect(auth.accessToken).toBeNull()
    expect(auth.user).toBeNull()
  })

  it('error de red → status=anonymous (fallo silencioso)', async () => {
    authApi.refresh.mockRejectedValue(new Error('Network error'))

    const auth = useAuthStore()
    await auth.restoreSession()

    expect(auth.status).toBe('anonymous')
  })
})

describe('logout', () => {
  it('logout → llama authApi.logout, limpia estado, status=anonymous', async () => {
    // Primero hacer login para tener estado.
    authApi.login.mockResolvedValue(ADMIN_AUTH_RESPONSE)
    authApi.logout.mockResolvedValue(null)

    const auth = useAuthStore()
    await auth.login('admin', 'adminpass123')
    expect(auth.status).toBe('authenticated')

    await auth.logout()

    expect(authApi.logout).toHaveBeenCalledOnce()
    expect(auth.status).toBe('anonymous')
    expect(auth.accessToken).toBeNull()
    expect(auth.user).toBeNull()
  })

  it('logout funciona aunque authApi.logout falle (error de red)', async () => {
    authApi.login.mockResolvedValue(ADMIN_AUTH_RESPONSE)
    authApi.logout.mockRejectedValue(new Error('Network error'))

    const auth = useAuthStore()
    await auth.login('admin', 'adminpass123')

    // No debe lanzar error; la sesión local se limpia de todas formas.
    await expect(auth.logout()).resolves.toBeUndefined()
    expect(auth.status).toBe('anonymous')
    expect(auth.accessToken).toBeNull()
  })
})

describe('clearSession', () => {
  it('clearSession → borra token, user y refreshPromise, status=anonymous', async () => {
    authApi.login.mockResolvedValue(ADMIN_AUTH_RESPONSE)

    const auth = useAuthStore()
    await auth.login('admin', 'adminpass123')

    auth.clearSession()

    expect(auth.status).toBe('anonymous')
    expect(auth.accessToken).toBeNull()
    expect(auth.user).toBeNull()
    expect(auth.refreshPromise).toBeNull()
  })
})

describe('seguridad: localStorage', () => {
  it('el access token NUNCA se escribe en localStorage', async () => {
    authApi.login.mockResolvedValue(ADMIN_AUTH_RESPONSE)
    authApi.refresh.mockResolvedValue(ADMIN_AUTH_RESPONSE)

    const setItemSpy = vi.spyOn(Storage.prototype, 'setItem')

    const auth = useAuthStore()
    await auth.login('admin', 'adminpass123')
    await auth.restoreSession()

    // localStorage.setItem no debe haber sido llamado con el token.
    const calls = setItemSpy.mock.calls
    const tokenStored = calls.some(([, value]) =>
      typeof value === 'string' && value.includes(ADMIN_AUTH_RESPONSE.accessToken)
    )
    expect(tokenStored).toBe(false)
  })
})

describe('refreshAccessToken — deduplicación', () => {
  it('múltiples llamadas concurrentes comparten la misma Promise de refresh', async () => {
    let resolveRefresh
    const sharedPromise = new Promise(res => { resolveRefresh = res })
    authApi.refresh.mockImplementation(() => sharedPromise)

    const auth = useAuthStore()

    // Lanzar 3 llamadas concurrentes.
    const p1 = auth.refreshAccessToken()
    const p2 = auth.refreshAccessToken()
    const p3 = auth.refreshAccessToken()

    // Las tres deben ser la misma Promise (o resolverse con el mismo resultado).
    expect(auth.refreshPromise).not.toBeNull()

    // Resolver el refresh.
    resolveRefresh(ADMIN_AUTH_RESPONSE)
    const [r1, r2, r3] = await Promise.all([p1, p2, p3])

    // authApi.refresh solo debe haberse llamado UNA vez.
    expect(authApi.refresh).toHaveBeenCalledTimes(1)
    expect(r1).toEqual(ADMIN_AUTH_RESPONSE)
    expect(r2).toEqual(ADMIN_AUTH_RESPONSE)
    expect(r3).toEqual(ADMIN_AUTH_RESPONSE)
  })
})

describe('changePassword', () => {
  it('cambio exitoso → clearSession (status=anonymous)', async () => {
    authApi.login.mockResolvedValue(ADMIN_AUTH_RESPONSE)
    authApi.changePassword.mockResolvedValue(null)

    const auth = useAuthStore()
    await auth.login('admin', 'adminpass123')

    await auth.changePassword('adminpass123', 'newadminpass456')

    expect(authApi.changePassword).toHaveBeenCalledWith('adminpass123', 'newadminpass456')
    expect(auth.status).toBe('anonymous')
    expect(auth.accessToken).toBeNull()
  })

  it('cambio fallido (contraseña actual incorrecta) → lanza ApiError', async () => {
    authApi.login.mockResolvedValue(ADMIN_AUTH_RESPONSE)
    authApi.changePassword.mockRejectedValue(
      new ApiError(401, { detail: 'Contraseña actual incorrecta' })
    )

    const auth = useAuthStore()
    await auth.login('admin', 'adminpass123')

    await expect(auth.changePassword('wrongpass', 'newpass1234')).rejects.toBeInstanceOf(ApiError)
  })
})
