/**
 * format.js — Utilidades de formateo de datos para la UI.
 * Únicas funciones autorizadas para formatear USD y etiquetas de mes en el frontend.
 * Los componentes no formatean directamente; usan estas funciones.
 */

/**
 * Formatea un número como moneda USD usando Intl.NumberFormat.
 * @param {number} amount
 * @returns {string} p.ej. "$1,200.00"
 */
export function formatUSD(amount) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)
}

/**
 * Formatea una cadena "YYYY-MM" como etiqueta legible en español (Ecuador).
 * @param {string} yearMonth  Formato "YYYY-MM", p.ej. "2026-01"
 * @returns {string} p.ej. "enero de 2026"
 */
export function formatMonth(yearMonth) {
  const [year, month] = yearMonth.split('-')
  const date = new Date(Number(year), Number(month) - 1, 1)
  return date.toLocaleDateString('es-EC', { month: 'long', year: 'numeric' })
}
