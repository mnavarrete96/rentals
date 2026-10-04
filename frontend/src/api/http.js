/**
 * http.js — Cliente HTTP central (§7, §15.7 ARCHITECTURE.md).
 *
 * Envuelve fetch nativo. Añade Bearer token, maneja reintento único tras refresh 401,
 * y convierte respuestas no-2xx en ApiError.
 *
 * DIP: recibe sus dependencias vía configureAuth({ getAccessToken, refreshAccessToken, onAuthFailure })
 * — llamada desde stores/auth.js al inicializarse — para evitar dependencias circulares y
 * permitir testar http.js aislado.
 */

let _getToken = null
let _refreshFn = null
let _onAuthFailure = null

/**
 * Configura las dependencias de autenticación. Llamado por stores/auth.js.
 * @param {object} deps
 * @param {() => string|null} deps.getAccessToken   - Retorna el access token actual (o null).
 * @param {() => Promise}     deps.refreshAccessToken - Llama al store para renovar el token.
 * @param {() => void}        deps.onAuthFailure     - Llamado cuando el refresh falla.
 */
export function configureAuth({ getAccessToken, refreshAccessToken, onAuthFailure }) {
  _getToken = getAccessToken
  _refreshFn = refreshAccessToken
  _onAuthFailure = onAuthFailure
}

/**
 * Error de API. Incluye el status HTTP y el body de la respuesta (ProblemDetail o null).
 * @property {number} status
 * @property {object|null} body  - ProblemDetail: { status, title, detail, errors? }
 * @property {string|null} title
 * @property {string|null} detail
 * @property {Array|null}  errors
 */
export class ApiError extends Error {
  constructor(status, body) {
    super(`HTTP ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.body = body ?? null
    this.title = body?.title ?? null
    this.detail = body?.detail ?? null
    this.errors = body?.errors ?? null
  }
}

/**
 * Petición HTTP base. Rutas relativas ('/api/...').
 *
 * @param {string}  path      - Ruta relativa, p.ej. '/api/tenants'.
 * @param {object}  options   - Opciones de fetch + { auth?: boolean, _retry?: boolean }.
 *   auth    (default true)  - Si true, añade Authorization: Bearer <token>.
 *   _retry  (default false) - Uso interno: marca que ya se reintentó tras un 401.
 * @returns {Promise<any>} - JSON parseado, o null si la respuesta es 204.
 * @throws {ApiError} - Si la respuesta no es 2xx.
 */
export async function apiFetch(path, options = {}) {
  const { auth = true, _retry = false, ...fetchOptions } = options

  const headers = {
    'Content-Type': 'application/json',
    ...fetchOptions.headers
  }

  const token = auth ? _getToken?.() : null
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  let res = await fetch(path, { ...fetchOptions, headers })

  // Si recibimos 401 con auth activo y aún no hemos reintentado: refrescar y reintentar.
  if (res.status === 401 && auth && !_retry) {
    try {
      await _refreshFn?.()
      // El store actualizó accessToken; pedirlo de nuevo.
      const newToken = _getToken?.()
      if (newToken) {
        headers['Authorization'] = `Bearer ${newToken}`
      }
      // Reintentar UNA sola vez. No pasar _retry en las opciones de fetch (no es una opción estándar).
      res = await fetch(path, { ...fetchOptions, headers })
    } catch {
      // El refresh falló: cerrar sesión y lanzar error.
      _onAuthFailure?.()
      const body = await safeJson(res)
      throw new ApiError(401, body)
    }
  }

  if (!res.ok) {
    const body = await safeJson(res)
    throw new ApiError(res.status, body)
  }

  if (res.status === 204) return null
  return res.json()
}

/** Intenta parsear JSON; si falla, devuelve null. */
async function safeJson(res) {
  try {
    return await res.json()
  } catch {
    return null
  }
}
