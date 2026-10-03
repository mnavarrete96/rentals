/**
 * render.js — Pintado del DOM (sin lógica de negocio).
 * Expone: window.RentalsRender
 *
 * Responsabilidad única: recibir datos ya calculados y generar HTML en los
 * contenedores del DOM. No calcula deudas ni accede a RentalsStore directamente.
 *
 * Depende de:
 *   - window.RentalsCalc (monthsOwed, totalOwed, status) — cargado antes que este script.
 *
 * Orden de carga: data.js → store.js → calc.js → render.js → app.js
 */

window.RentalsRender = (function () {

  // -------------------------------------------------------------------------
  // Auxiliares internas
  // -------------------------------------------------------------------------

  var MONTH_NAMES = [
    "Ene", "Feb", "Mar", "Abr", "May", "Jun",
    "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
  ];

  /**
   * Convierte una clave de mes "YYYY-MM" a texto legible "Sep 2026".
   * @param {string} monthKey  Ej: "2026-09"
   * @returns {string}          Ej: "Sep 2026"
   */
  function _formatMonth(monthKey) {
    var parts = monthKey.split("-");
    var year  = parts[0];
    var mon   = parseInt(parts[1], 10) - 1;   // 0-based para el array
    return MONTH_NAMES[mon] + " " + year;
  }

  /**
   * Devuelve el texto e clase CSS para el badge de estado.
   * @param {"AL_DIA"|"ATRASADO"} statusValue
   * @returns {{ cls: string, label: string }}
   */
  function _badgeProps(statusValue) {
    if (statusValue === "AL_DIA") {
      return { cls: "badge badge--ok", label: "Al día" };
    }
    return { cls: "badge badge--late", label: "Atrasado" };
  }

  /**
   * Formatea un número como precio USD: "$450 USD".
   * @param {number} amount
   * @returns {string}
   */
  function _formatAmount(amount) {
    return "$" + amount + " USD";
  }


  // -------------------------------------------------------------------------
  // 1. renderTenantList
  // -------------------------------------------------------------------------

  /**
   * Pinta las tarjetas de arrendatarios en #admin-tenant-list.
   *
   * @param {Tenant[]} tenants      Array de Tenant (de RentalsStore.getAllTenants())
   * @param {Date}     today        Objeto Date para los cálculos de estado
   * @param {string|null} selectedId  El id del arrendatario cuya tarjeta lleva card--active
   */
  function renderTenantList(tenants, today, selectedId) {
    var container = document.getElementById("admin-tenant-list");
    if (!container) { return; }

    if (!tenants || tenants.length === 0) {
      container.innerHTML = '<span class="empty-state">No hay arrendatarios registrados</span>';
      return;
    }

    var html = "";
    for (var i = 0; i < tenants.length; i++) {
      var t          = tenants[i];
      var st         = RentalsCalc.status(t, today);
      var badge      = _badgeProps(st);
      var activeClass = (t.id === selectedId) ? " card--active" : "";

      html += '<div class="card card--clickable' + activeClass + '" data-id="' + t.id + '">';
      html +=   '<div class="card-header">';
      html +=     '<div>';
      html +=       '<p class="card-name">' + _escapeHtml(t.fullName) + '</p>';
      html +=       '<p class="card-unit">' + _escapeHtml(t.unit) + '</p>';
      html +=     '</div>';
      html +=     '<span class="' + badge.cls + '">' + badge.label + '</span>';
      html +=   '</div>';
      html += '</div>';
    }

    container.innerHTML = html;
  }


  // -------------------------------------------------------------------------
  // 2. renderTenantDetail
  // -------------------------------------------------------------------------

  /**
   * Pinta el detalle de un arrendatario en #admin-tenant-detail.
   * Si tenant es null/undefined, muestra estado vacío.
   *
   * @param {Tenant|null|undefined} tenant
   * @param {Date} today
   */
  function renderTenantDetail(tenant, today) {
    var container = document.getElementById("admin-tenant-detail");
    if (!container) { return; }

    if (!tenant) {
      container.innerHTML = '<span class="empty-state">Selecciona un arrendatario para ver el detalle</span>';
      return;
    }

    var st         = RentalsCalc.status(tenant, today);
    var badge      = _badgeProps(st);
    var months     = RentalsCalc.monthsOwed(tenant, today);
    var total      = RentalsCalc.totalOwed(tenant, today);

    var html = "";

    // Cabecera de detalle
    html += '<div class="detail-header">';
    html +=   '<div>';
    html +=     '<p class="detail-title">' + _escapeHtml(tenant.fullName) + '</p>';
    html +=     '<p class="detail-unit">'  + _escapeHtml(tenant.unit) + ' · ' + _escapeHtml(tenant.contact) + ' · Día de pago: ' + tenant.paymentDay + '</p>';
    html +=   '</div>';
    html +=   '<span class="' + badge.cls + '">' + badge.label + '</span>';
    html += '</div>';

    // Meses adeudados
    html += '<p class="section-label">Meses adeudados</p>';
    if (months.length === 0) {
      html += '<p class="empty-state">Sin meses pendientes</p>';
    } else {
      html += '<div class="months-list">';
      for (var i = 0; i < months.length; i++) {
        var m = months[i];
        html += '<span class="month-tag">';
        html +=   _formatMonth(m);
        html +=   ' <button class="btn-mark-paid" data-tenant-id="' + tenant.id + '" data-month="' + m + '" type="button">✓ Marcar pagado</button>';
        html += '</span>';
      }
      html += '</div>';
    }

    // Cargos unicos
    html += '<p class="section-label">Cargos únicos</p>';
    if (!tenant.oneTimeCharges || tenant.oneTimeCharges.length === 0) {
      html += '<p class="empty-state">Sin cargos adicionales</p>';
    } else {
      for (var j = 0; j < tenant.oneTimeCharges.length; j++) {
        var charge = tenant.oneTimeCharges[j];
        html += '<div class="charge-row">';
        html +=   '<span class="charge-desc">' + _escapeHtml(charge.description) + '</span>';
        html +=   '<span class="charge-amount' + (charge.paid === false ? ' charge-amount--danger' : '') + '">' + _formatAmount(charge.amount) + '</span>';
        if (charge.paid === false) {
          html += '<button class="btn-mark-one-time-paid" data-tenant-id="' + tenant.id + '" data-charge-id="' + charge.id + '" type="button">✓ Marcar pagado</button>';
        } else {
          html += '<span class="charge-paid-label">Pagado</span>';
        }
        html += '</div>';
      }
    }

    // Total adeudado
    html += '<div class="total-row">';
    html +=   '<span class="total-label">Total adeudado</span>';
    html +=   '<span class="total-amount">' + _formatAmount(total) + '</span>';
    html += '</div>';

    container.innerHTML = html;
  }


  // -------------------------------------------------------------------------
  // 3. renderTenantSelector
  // -------------------------------------------------------------------------

  /**
   * Pinta los botones de seleccion de arrendatario en #tenant-selector.
   *
   * @param {Tenant[]}    tenants     Array de Tenant
   * @param {string|null} selectedId  Id del arrendatario activo
   */
  function renderTenantSelector(tenants, selectedId) {
    var container = document.getElementById("tenant-selector");
    if (!container) { return; }

    if (!tenants || tenants.length === 0) {
      container.innerHTML = '<span class="empty-state">No hay arrendatarios registrados</span>';
      return;
    }

    var html = "";
    for (var i = 0; i < tenants.length; i++) {
      var t           = tenants[i];
      var activeClass = (t.id === selectedId) ? " tenant-selector-btn--active" : "";
      html += '<button class="tenant-selector-btn' + activeClass + '" data-id="' + t.id + '" type="button">';
      html +=   _escapeHtml(t.fullName);
      html += '</button>';
    }

    container.innerHTML = html;
  }


  // -------------------------------------------------------------------------
  // 4. renderTenantAccount
  // -------------------------------------------------------------------------

  /**
   * Pinta el estado de cuenta del arrendatario en #tenant-account.
   * Si tenant es null/undefined, muestra estado vacio.
   *
   * @param {Tenant|null|undefined} tenant
   * @param {Date} today
   */
  function renderTenantAccount(tenant, today) {
    var container = document.getElementById("tenant-account");
    if (!container) { return; }

    if (!tenant) {
      container.innerHTML = '<span class="empty-state">Selecciona tu nombre para ver tu estado de cuenta</span>';
      return;
    }

    var st     = RentalsCalc.status(tenant, today);
    var badge  = _badgeProps(st);
    var months = RentalsCalc.monthsOwed(tenant, today);
    var total  = RentalsCalc.totalOwed(tenant, today);

    var html = "";

    // Cabecera de cuenta
    html += '<div class="account-header">';
    html +=   '<div>';
    html +=     '<p class="account-name">' + _escapeHtml(tenant.fullName) + '</p>';
    html +=     '<p class="account-unit">' + _escapeHtml(tenant.unit) + '</p>';
    html +=   '</div>';
    html +=   '<span class="' + badge.cls + '">' + badge.label + '</span>';
    html += '</div>';

    if (st === "AL_DIA") {
      html += '<p class="empty-state">Estás al día. ¡Gracias!</p>';
    } else {
      // Meses adeudados
      if (months.length > 0) {
        html += '<p class="section-label">Meses que debes</p>';
        html += '<div class="months-list">';
        for (var i = 0; i < months.length; i++) {
          html += '<span class="month-tag">' + _formatMonth(months[i]) + '</span>';
        }
        html += '</div>';
      }

      // Cargos unicos pendientes
      var pendingCharges = [];
      if (tenant.oneTimeCharges) {
        for (var j = 0; j < tenant.oneTimeCharges.length; j++) {
          if (tenant.oneTimeCharges[j].paid === false) {
            pendingCharges.push(tenant.oneTimeCharges[j]);
          }
        }
      }

      if (pendingCharges.length > 0) {
        html += '<p class="section-label">Cargos adicionales</p>';
        for (var k = 0; k < pendingCharges.length; k++) {
          var charge = pendingCharges[k];
          html += '<div class="charge-row">';
          html +=   '<span class="charge-desc">' + _escapeHtml(charge.description) + '</span>';
          html +=   '<span class="charge-amount">' + _formatAmount(charge.amount) + '</span>';
          html += '</div>';
        }
      }

      // Total a pagar
      html += '<div class="total-row">';
      html +=   '<span class="total-label">Total a pagar</span>';
      html +=   '<span class="total-amount">' + _formatAmount(total) + '</span>';
      html += '</div>';
    }

    container.innerHTML = html;
  }


  // -------------------------------------------------------------------------
  // Auxiliar de seguridad
  // -------------------------------------------------------------------------

  /**
   * Escapa caracteres HTML especiales para evitar XSS al insertar strings
   * de datos en innerHTML.
   * @param {string} str
   * @returns {string}
   */
  function _escapeHtml(str) {
    if (typeof str !== "string") { return String(str); }
    return str
      .replace(/&/g,  "&amp;")
      .replace(/</g,  "&lt;")
      .replace(/>/g,  "&gt;")
      .replace(/"/g,  "&quot;")
      .replace(/'/g,  "&#039;");
  }


  // -------------------------------------------------------------------------
  // API publica
  // -------------------------------------------------------------------------

  return {
    renderTenantList:     renderTenantList,
    renderTenantDetail:   renderTenantDetail,
    renderTenantSelector: renderTenantSelector,
    renderTenantAccount:  renderTenantAccount
  };

}());
