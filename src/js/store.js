/**
 * store.js — Única puerta de acceso, consulta y mutación de datos en memoria.
 * Expone: window.RentalsStore
 *
 * Lee de window.RentalsData (cargado antes por data.js).
 * Trabaja sobre una copia en memoria para no mutar el array original.
 *
 * Diseñado para que en v2 cada función pueda devolver una Promise sin cambiar
 * los contratos: basta con convertir el return en return Promise.resolve(value).
 *
 * Sin acceso al DOM (no usa document.* ni window.location, etc.).
 */

window.RentalsStore = (function () {
  // Copia profunda en memoria de los datos del mock.
  // Se reconstruye al cargar el script; las mutaciones afectan solo esta copia.
  var _data = JSON.parse(JSON.stringify(window.RentalsData));

  /**
   * Devuelve todos los arrendatarios.
   * @returns {Tenant[]}
   */
  function getAllTenants() {
    return _data;
  }

  /**
   * Devuelve un arrendatario por su id, o undefined si no existe.
   * @param {string} id
   * @returns {Tenant|undefined}
   */
  function getTenantById(id) {
    for (var i = 0; i < _data.length; i++) {
      if (_data[i].id === id) {
        return _data[i];
      }
    }
    return undefined;
  }

  /**
   * Marca un mes como pagado para el arrendatario indicado.
   * Si ya está marcado, no hace nada (idempotente).
   * @param {string} tenantId
   * @param {string} monthKey  Formato "YYYY-MM"
   */
  function markMonthPaid(tenantId, monthKey) {
    var tenant = getTenantById(tenantId);
    if (!tenant) { return; }
    if (tenant.paidMonths.indexOf(monthKey) === -1) {
      tenant.paidMonths.push(monthKey);
    }
  }

  /**
   * Desmarca un mes pagado para el arrendatario indicado.
   * Si no estaba marcado, no hace nada (idempotente).
   * @param {string} tenantId
   * @param {string} monthKey  Formato "YYYY-MM"
   */
  function markMonthUnpaid(tenantId, monthKey) {
    var tenant = getTenantById(tenantId);
    if (!tenant) { return; }
    var idx = tenant.paidMonths.indexOf(monthKey);
    if (idx !== -1) {
      tenant.paidMonths.splice(idx, 1);
    }
  }

  /**
   * Marca un cargo único como pagado.
   * @param {string} tenantId
   * @param {string} chargeId
   */
  function markOneTimePaid(tenantId, chargeId) {
    var tenant = getTenantById(tenantId);
    if (!tenant) { return; }
    for (var i = 0; i < tenant.oneTimeCharges.length; i++) {
      if (tenant.oneTimeCharges[i].id === chargeId) {
        tenant.oneTimeCharges[i].paid = true;
        return;
      }
    }
  }

  /**
   * Agrega un nuevo arrendatario al array en memoria.
   * @param {Tenant} tenantData  Objeto con los campos del modelo Tenant.
   */
  function addTenant(tenantData) {
    _data.push(tenantData);
  }

  /**
   * Agrega un cargo único a un arrendatario existente.
   * @param {string} tenantId
   * @param {{ id: string, description: string, amount: number, paid: boolean }} charge
   */
  function addOneTimeCharge(tenantId, charge) {
    var tenant = getTenantById(tenantId);
    if (!tenant) { return; }
    tenant.oneTimeCharges.push(charge);
  }

  // API pública
  return {
    getAllTenants:    getAllTenants,
    getTenantById:   getTenantById,
    markMonthPaid:   markMonthPaid,
    markMonthUnpaid: markMonthUnpaid,
    markOneTimePaid: markOneTimePaid,
    addTenant:       addTenant,
    addOneTimeCharge: addOneTimeCharge
  };
}());
