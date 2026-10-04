/**
 * http.js — Cliente HTTP central.
 * Envuelve fetch nativo, añade Bearer token, maneja reintento tras refresh y errores.
 *
 * TODO: implementar en F-02
 *   - request(method, path, body, { auth = true } = {})
 *   - ApiError class { status, title, detail, errors }
 *   - configureAuth({ getAccessToken, refreshAccessToken, onAuthFailure })
 *     (DIP: recibe sus dependencias desde main.js para evitar dependencias circulares)
 *   - Manejo de expiración: 401 con auth:true → refreshAccessToken() → reintento único
 *   - 403 no dispara refresh
 *   - /api/auth/* siempre con auth: false
 */

// TODO: implementar en F-02
export function configureAuth(_deps) {
  // placeholder
}

export async function request(_method, _path, _body, _options) {
  // placeholder
  throw new Error('http.js: request() no implementado. Pendiente F-02.')
}
