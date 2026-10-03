/**
 * app.js — Orquestador de la aplicación.
 * Expone: window.RentalsApp
 *
 * Responsabilidad única: arrancar la app, mantener el estado de la UI
 * (modo actual, arrendatario seleccionado) y conectar eventos del DOM
 * con RentalsStore + RentalsRender. No calcula deudas ni pinta HTML directamente.
 *
 * Depende de (cargados antes):
 *   - window.RentalsStore  (store.js)
 *   - window.RentalsRender (render.js)
 *
 * Orden de carga: data.js → store.js → calc.js → render.js → app.js
 * Los scripts están al final de <body>, por lo que el DOM ya está listo
 * cuando este script se ejecuta.
 */

window.RentalsApp = (function () {

  // -------------------------------------------------------------------------
  // Estado interno de la UI
  // -------------------------------------------------------------------------

  var _state = {
    mode: "admin",           // "admin" | "tenant"
    selectedAdminId: null,   // id del arrendatario seleccionado en vista admin
    selectedTenantId: null   // id del arrendatario seleccionado en vista tenant
  };


  // -------------------------------------------------------------------------
  // Auxiliares
  // -------------------------------------------------------------------------

  /**
   * Devuelve la fecha actual como objeto Date.
   * Centralizada aquí para que sea fácil de interceptar en pruebas.
   * @returns {Date}
   */
  function _today() {
    return new Date();
  }

  /**
   * Actualiza los botones del toggle de modo y sus atributos ARIA.
   * @param {"admin"|"tenant"} activeMode
   */
  function _updateModeButtons(activeMode) {
    var btnAdmin  = document.getElementById("btn-mode-admin");
    var btnTenant = document.getElementById("btn-mode-tenant");

    if (!btnAdmin || !btnTenant) { return; }

    if (activeMode === "admin") {
      btnAdmin.classList.add("btn-mode--active");
      btnAdmin.setAttribute("aria-pressed", "true");
      btnTenant.classList.remove("btn-mode--active");
      btnTenant.setAttribute("aria-pressed", "false");
    } else {
      btnTenant.classList.add("btn-mode--active");
      btnTenant.setAttribute("aria-pressed", "true");
      btnAdmin.classList.remove("btn-mode--active");
      btnAdmin.setAttribute("aria-pressed", "false");
    }
  }

  /**
   * Muestra la vista correspondiente al modo activo y oculta la otra.
   * Usa la clase .view--visible.
   * @param {"admin"|"tenant"} activeMode
   */
  function _updateViews(activeMode) {
    var viewAdmin  = document.getElementById("view-admin");
    var viewTenant = document.getElementById("view-tenant");

    if (!viewAdmin || !viewTenant) { return; }

    if (activeMode === "admin") {
      viewAdmin.classList.add("view--visible");
      viewTenant.classList.remove("view--visible");
    } else {
      viewTenant.classList.add("view--visible");
      viewAdmin.classList.remove("view--visible");
    }
  }

  /**
   * Re-renderiza la lista de tarjetas Admin y el detalle con los datos en memoria.
   * Centralizado para no repetir la misma secuencia de llamadas en varios handlers.
   */
  function _refreshAdminView() {
    var today   = _today();
    var tenants = RentalsStore.getAllTenants();
    RentalsRender.renderTenantList(tenants, today, _state.selectedAdminId);
    var selected = _state.selectedAdminId
      ? RentalsStore.getTenantById(_state.selectedAdminId)
      : null;
    RentalsRender.renderTenantDetail(selected, today);
  }

  /**
   * Re-renderiza el selector de arrendatario en la vista Tenant.
   * Centralizado por la misma razón que _refreshAdminView.
   */
  function _refreshTenantView() {
    var today   = _today();
    var tenants = RentalsStore.getAllTenants();
    RentalsRender.renderTenantSelector(tenants, _state.selectedTenantId);
    var selected = _state.selectedTenantId
      ? RentalsStore.getTenantById(_state.selectedTenantId)
      : null;
    RentalsRender.renderTenantAccount(selected, today);
  }


  // -------------------------------------------------------------------------
  // Registro de event listeners
  // -------------------------------------------------------------------------

  /**
   * Registra todos los listeners de la aplicación.
   * Se llama una única vez desde init().
   */
  function _registerListeners() {

    // ── Toggle de modo (RF-06) ──────────────────────────────────────────────

    var btnModeAdmin = document.getElementById("btn-mode-admin");
    if (btnModeAdmin) {
      btnModeAdmin.addEventListener("click", function () {
        _state.mode = "admin";
        _updateModeButtons("admin");
        _updateViews("admin");
      });
    }

    var btnModeTenant = document.getElementById("btn-mode-tenant");
    if (btnModeTenant) {
      btnModeTenant.addEventListener("click", function () {
        _state.mode = "tenant";
        _updateModeButtons("tenant");
        _updateViews("tenant");
      });
    }


    // ── Selección de arrendatario en vista Admin (delegación) ───────────────

    var adminList = document.getElementById("admin-tenant-list");
    if (adminList) {
      adminList.addEventListener("click", function (e) {
        var card = e.target.closest(".card--clickable");
        if (!card) { return; }
        var id = card.dataset.id;
        if (!id) { return; }
        _state.selectedAdminId = id;
        _refreshAdminView();
      });
    }


    // ── Marcar mes como pagado en vista Admin (RF-04, delegación) ───────────

    var adminDetail = document.getElementById("admin-tenant-detail");
    if (adminDetail) {
      adminDetail.addEventListener("click", function (e) {

        // Botón de mes pagado
        var btnPaid = e.target.closest(".btn-mark-paid");
        if (btnPaid) {
          var tenantId = btnPaid.dataset.tenantId;
          var month    = btnPaid.dataset.month;
          if (tenantId && month) {
            RentalsStore.markMonthPaid(tenantId, month);
            _refreshAdminView();
            // Si el arrendatario también está seleccionado en vista Tenant,
            // el cambio se refleja la próxima vez que el usuario cambie de vista
            // (los datos en memoria ya están actualizados).
          }
          return;
        }

        // Botón de cargo único pagado
        var btnOneTime = e.target.closest(".btn-mark-one-time-paid");
        if (btnOneTime) {
          var tenantId2 = btnOneTime.dataset.tenantId;
          var chargeId  = btnOneTime.dataset.chargeId;
          if (tenantId2 && chargeId) {
            RentalsStore.markOneTimePaid(tenantId2, chargeId);
            _refreshAdminView();
          }
          return;
        }

      });
    }


    // ── Selección de arrendatario en vista Tenant (delegación) ─────────────

    var tenantSelector = document.getElementById("tenant-selector");
    if (tenantSelector) {
      tenantSelector.addEventListener("click", function (e) {
        var btn = e.target.closest(".tenant-selector-btn");
        if (!btn) { return; }
        var id = btn.dataset.id;
        if (!id) { return; }
        _state.selectedTenantId = id;
        _refreshTenantView();
      });
    }


    // ── Formulario de registro de nuevo arrendatario (RF-01) ────────────────

    var registerForm = document.getElementById("admin-register-form");
    if (registerForm) {
      registerForm.addEventListener("submit", function (e) {
        e.preventDefault();

        var fullName      = document.getElementById("reg-full-name").value.trim();
        var unit          = document.getElementById("reg-unit").value.trim();
        var contact       = document.getElementById("reg-contact").value.trim();
        var paymentDay    = parseInt(document.getElementById("reg-payment-day").value, 10);
        var startDate     = document.getElementById("reg-start-date").value;
        var monthlyAmount = parseFloat(document.getElementById("reg-monthly-amount").value);

        // Validación básica: todos los campos requeridos
        if (!fullName || !unit || !contact || isNaN(paymentDay) || !startDate || isNaN(monthlyAmount)) {
          return; // El atributo `required` del HTML ya previene la mayoría de los casos vacíos
        }

        var tenant = {
          id:            "t-" + Date.now(),
          fullName:      fullName,
          unit:          unit,
          contact:       contact,
          paymentDay:    paymentDay,
          startDate:     startDate,
          monthlyAmount: monthlyAmount,
          oneTimeCharges: [],
          paidMonths:    []
        };

        RentalsStore.addTenant(tenant);

        // Actualizar lista Admin y selector Tenant para reflejar el nuevo arrendatario
        var today   = _today();
        var tenants = RentalsStore.getAllTenants();
        RentalsRender.renderTenantList(tenants, today, _state.selectedAdminId);
        RentalsRender.renderTenantSelector(tenants, _state.selectedTenantId);

        registerForm.reset();
      });
    }

  } // _registerListeners


  // -------------------------------------------------------------------------
  // Arranque
  // -------------------------------------------------------------------------

  /**
   * Inicializa la aplicación.
   * Renderiza el estado inicial y registra todos los event listeners.
   * Se llama una sola vez al cargar el script.
   */
  function init() {
    var today   = _today();
    var tenants = RentalsStore.getAllTenants();

    // Render inicial — vista Admin
    RentalsRender.renderTenantList(tenants, today, null);
    RentalsRender.renderTenantDetail(null, today);

    // Render inicial — vista Tenant
    RentalsRender.renderTenantSelector(tenants, null);
    RentalsRender.renderTenantAccount(null, today);

    // Asegurar que el estado visual inicial coincide con _state.mode = "admin"
    _updateModeButtons("admin");
    _updateViews("admin");

    // Registrar todos los listeners
    _registerListeners();
  }


  // -------------------------------------------------------------------------
  // API pública
  // -------------------------------------------------------------------------

  var RentalsApp = { init: init };
  window.RentalsApp = RentalsApp;

  // Auto-arranque: los scripts están al final de <body>; el DOM ya está listo.
  init();

  return RentalsApp;

}());
