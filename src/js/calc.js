/**
 * calc.js — Lógica pura de deuda (sin DOM, sin store, sin side effects).
 * Expone: window.RentalsCalc
 *
 * Todas las funciones son puras: reciben los datos y devuelven un valor.
 * `today` siempre se pasa desde fuera (objeto Date) para que sean testeables
 * con fechas fijas sin depender del reloj del sistema.
 *
 * Regla de comparación de meses: se usan claves "YYYY-MM" (strings), nunca
 * comparaciones por días, para evitar bordes de zona horaria.
 */

window.RentalsCalc = (function () {

  /**
   * Auxiliar interna: convierte un Date a clave de mes "YYYY-MM".
   * Usa el año y mes locales (getFullYear / getMonth) para consistencia
   * con las cadenas "YYYY-MM-DD" de startDate.
   * @param {Date} date
   * @returns {string}  Ej: "2026-09"
   */
  function _monthKey(date) {
    var year  = date.getFullYear();
    var month = date.getMonth() + 1;            // getMonth() es 0-based
    var mm    = month < 10 ? "0" + month : "" + month;
    return year + "-" + mm;
  }

  /**
   * Genera la lista de meses adeudados para un arrendatario.
   * Rango: desde el mes de startDate hasta el mes de today (ambos inclusive).
   * Resta los meses que ya están en tenant.paidMonths.
   *
   * @param {Tenant} tenant
   * @param {Date}   today
   * @returns {string[]}  Ej: ["2026-09", "2026-10"]
   */
  function monthsOwed(tenant, today) {
    // Parsear startDate "YYYY-MM-DD" usando partes del string para evitar
    // problemas de UTC vs. local al hacer new Date("YYYY-MM-DD").
    var parts     = tenant.startDate.split("-");
    var startYear = parseInt(parts[0], 10);
    var startMon  = parseInt(parts[1], 10);     // 1-based

    var endYear = today.getFullYear();
    var endMon  = today.getMonth() + 1;         // 1-based

    var owed = [];
    var y = startYear;
    var m = startMon;

    while (y < endYear || (y === endYear && m <= endMon)) {
      var mm  = m < 10 ? "0" + m : "" + m;
      var key = y + "-" + mm;

      if (tenant.paidMonths.indexOf(key) === -1) {
        owed.push(key);
      }

      // Avanzar al mes siguiente
      m++;
      if (m > 12) {
        m = 1;
        y++;
      }
    }

    return owed;
  }

  /**
   * Calcula el total adeudado en USD.
   * Total = (meses pendientes × monto mensual) + suma de cargos únicos impagos.
   *
   * @param {Tenant} tenant
   * @param {Date}   today
   * @returns {number}
   */
  function totalOwed(tenant, today) {
    var unpaidMonths = monthsOwed(tenant, today);
    var recurringDebt = unpaidMonths.length * tenant.monthlyAmount;

    var oneTimeDebt = 0;
    for (var i = 0; i < tenant.oneTimeCharges.length; i++) {
      if (tenant.oneTimeCharges[i].paid === false) {
        oneTimeDebt += tenant.oneTimeCharges[i].amount;
      }
    }

    return recurringDebt + oneTimeDebt;
  }

  /**
   * Devuelve el estado del arrendatario.
   * "AL_DIA"   si monthsOwed está vacío Y no hay cargos únicos impagos.
   * "ATRASADO" en cualquier otro caso.
   *
   * @param {Tenant} tenant
   * @param {Date}   today
   * @returns {"AL_DIA"|"ATRASADO"}
   */
  function status(tenant, today) {
    if (monthsOwed(tenant, today).length > 0) {
      return "ATRASADO";
    }
    for (var i = 0; i < tenant.oneTimeCharges.length; i++) {
      if (tenant.oneTimeCharges[i].paid === false) {
        return "ATRASADO";
      }
    }
    return "AL_DIA";
  }

  // API pública
  return {
    monthsOwed: monthsOwed,
    totalOwed:  totalOwed,
    status:     status
  };
}());
