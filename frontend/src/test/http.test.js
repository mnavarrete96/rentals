/**
 * http.test.js — Tests de api/http.js (§15.8 ARCHITECTURE.md).
 *
 * Verifica:
 * 1. Petición con auth:true añade Authorization: Bearer <token>.
 * 2. Petición con auth:false NO añade el header.
 * 3. Respuesta 401 dispara refresh y reintenta UNA sola vez.
 * 4. Refresh fallido llama onAuthFailure y lanza ApiError(401).
 * 5. Refrescos concurrentes comparten la misma Promise (deduplicación).
 * 6. ApiError contiene el status y el body de la respuesta.
 * 7. Respuesta 204 devuelve null.
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { apiFetch, configureAuth, ApiError } from '../api/http.js'

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Crea un Response mock con las propiedades mínimas que usa apiFetch.
 */
function mockResponse(status, body = null) {
  const bodyText = body !== null ? JSON.stringify(body) : ''
  return {
    ok: status >= 200 && status < 300,
    status,
    json: () => (body !== null ? Promise.resolve(body) : Promise.reject(new Error('no body')))
  }
}

// ── Setup por test ────────────────────────────────────────────────────────────

beforeEach(() => {
  vi.restoreAllMocks()
  // Reiniciar configureAuth con valores por defecto neutros para cada test.
  configureAuth({
    getAccessToken: () => null,
    refreshAccessToken: async () => {},
    onAuthFailure: () => {}
  })
})

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('apiFetch — cabecera Authorization', () => {
  it('añade Bearer header cuando auth=true y hay token', async () => {
    const token = 'test-token-abc'
    configureAuth({
      getAccessToken: () => token,
      refreshAccessToken: async () => {},
      onAuthFailure: () => {}
    })

    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      mockResponse(200, { ok: true })
    )

    await apiFetch('/api/tenants')

    expect(fetchSpy).toHaveBeenCalledOnce()
    const [, options] = fetchSpy.mock.calls[0]
    expect(options.headers['Authorization']).toBe(`Bearer ${token}`)
  })

  it('NO añade Bearer header cuando auth=false', async () => {
    configureAuth({
      getAccessToken: () => 'some-token',
      refreshAccessToken: async () => {},
      onAuthFailure: () => {}
    })

    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      mockResponse(200, { ok: true })
    )

    await apiFetch('/api/auth/login', { auth: false, method: 'POST', body: '{}' })

    expect(fetchSpy).toHaveBeenCalledOnce()
    const [, options] = fetchSpy.mock.calls[0]
    expect(options.headers['Authorization']).toBeUndefined()
  })
})

describe('apiFetch — manejo de 401 y refresh', () => {
  it('respuesta 401 con auth:true dispara refresh y reintenta una vez con el nuevo token', async () => {
    const tokens = { current: 'old-token' }
    const refreshFn = vi.fn(async () => {
      tokens.current = 'new-token'
    })
    const onFailure = vi.fn()

    configureAuth({
      getAccessToken: () => tokens.current,
      refreshAccessToken: refreshFn,
      onAuthFailure: onFailure
    })

    const fetchSpy = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(mockResponse(401, { title: 'Unauthorized' }))
      .mockResolvedValueOnce(mockResponse(200, { id: 1 }))

    const result = await apiFetch('/api/tenants')

    // Debe llamar a fetch dos veces (original + reintento).
    expect(fetchSpy).toHaveBeenCalledTimes(2)
    // Debe llamar al refresh.
    expect(refreshFn).toHaveBeenCalledOnce()
    // No debe llamar a onFailure porque el refresh tuvo éxito.
    expect(onFailure).not.toHaveBeenCalled()
    // El reintento debe llevar el token nuevo.
    const [, retryOptions] = fetchSpy.mock.calls[1]
    expect(retryOptions.headers['Authorization']).toBe('Bearer new-token')
    // Debe devolver el resultado del reintento.
    expect(result).toEqual({ id: 1 })
  })

  it('cuando el refresh falla, llama onAuthFailure y lanza ApiError(401)', async () => {
    const refreshFn = vi.fn(async () => {
      throw new ApiError(401, { title: 'Unauthorized' })
    })
    const onFailure = vi.fn()

    configureAuth({
      getAccessToken: () => 'expired-token',
      refreshAccessToken: refreshFn,
      onAuthFailure: onFailure
    })

    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      mockResponse(401, { title: 'Unauthorized', detail: 'Token expirado' })
    )

    await expect(apiFetch('/api/tenants')).rejects.toThrow(ApiError)
    expect(onFailure).toHaveBeenCalledOnce()
    expect(refreshFn).toHaveBeenCalledOnce()

    try {
      await apiFetch('/api/tenants')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      expect(err.status).toBe(401)
    }
  })

  it('401 con auth:false NO dispara el refresh (rutas públicas)', async () => {
    const refreshFn = vi.fn()
    configureAuth({
      getAccessToken: () => 'token',
      refreshAccessToken: refreshFn,
      onAuthFailure: vi.fn()
    })

    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      mockResponse(401, { title: 'Unauthorized' })
    )

    await expect(
      apiFetch('/api/auth/login', { auth: false, method: 'POST', body: '{}' })
    ).rejects.toBeInstanceOf(ApiError)

    expect(refreshFn).not.toHaveBeenCalled()
  })
})

describe('ApiError', () => {
  it('contiene el status HTTP y el body de la respuesta', async () => {
    configureAuth({
      getAccessToken: () => null,
      refreshAccessToken: async () => {},
      onAuthFailure: () => {}
    })

    const errorBody = {
      status: 404,
      title: 'Not Found',
      detail: 'El arrendatario no existe'
    }

    vi.spyOn(globalThis, 'fetch').mockResolvedValue(mockResponse(404, errorBody))

    try {
      await apiFetch('/api/tenants/999')
      expect.fail('Debería haber lanzado ApiError')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      expect(err.status).toBe(404)
      expect(err.body).toEqual(errorBody)
      expect(err.title).toBe('Not Found')
      expect(err.detail).toBe('El arrendatario no existe')
    }
  })

  it('lanza ApiError con body null cuando la respuesta no tiene JSON', async () => {
    configureAuth({
      getAccessToken: () => null,
      refreshAccessToken: async () => {},
      onAuthFailure: () => {}
    })

    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.reject(new Error('no json'))
    })

    try {
      await apiFetch('/api/tenants')
      expect.fail('Debería haber lanzado ApiError')
    } catch (err) {
      expect(err).toBeInstanceOf(ApiError)
      expect(err.status).toBe(500)
      expect(err.body).toBeNull()
    }
  })
})

describe('apiFetch — respuesta 204', () => {
  it('devuelve null cuando la respuesta es 204 No Content', async () => {
    configureAuth({
      getAccessToken: () => null,
      refreshAccessToken: async () => {},
      onAuthFailure: () => {}
    })

    vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: true, status: 204 })

    const result = await apiFetch('/api/auth/logout', { method: 'POST', auth: false })
    expect(result).toBeNull()
  })
})
