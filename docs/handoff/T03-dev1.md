# Traspaso: T-03 · de dev1 · 2026-10-03

## Estado
- [x] Terminada  - [ ] A medias  - [ ] Bloqueada

## Qué quedó hecho
- `src/js/render.js`: objeto `window.RentalsRender` con las 4 funciones públicas del contrato.
  - `renderTenantList(tenants, today, selectedId)` — escribe en `#admin-tenant-list`
  - `renderTenantDetail(tenant, today)` — escribe en `#admin-tenant-detail`
  - `renderTenantSelector(tenants, selectedId)` — escribe en `#tenant-selector`
  - `renderTenantAccount(tenant, today)` — escribe en `#tenant-account`

## Qué falta
- `src/js/app.js` (T-04, dev2): orquestador que invoca estas funciones y escucha los eventos.
- T-05 (tester): validación completa contra SPEC.md RF-01..RF-07.

## Dónde continuar (el siguiente agente empieza aquí)
- **dev2 (T-04 / app.js)**: puede comenzar ahora. Leer esta nota para conocer los `data-*`
  atributos que render.js emite y que app.js debe escuchar con delegación de eventos.
- **tester (T-05)**: una vez T-04 esté EN REVISIÓN, abrir `src/index.html` en el navegador
  y ejecutar los pasos de verificación al final de esta nota.

## Archivos tocados
- `src/js/render.js` (creado)
- `docs/TASKS.md` (estado T-03 → EN REVISIÓN)
- `docs/logs/dev1.md` (bitácora)

## Contratos públicos de window.RentalsRender

### renderTenantList(tenants, today, selectedId)
- **Contenedor destino:** `#admin-tenant-list`
- **Parámetros:**
  - `tenants`: `Tenant[]` — resultado de `RentalsStore.getAllTenants()`
  - `today`: `Date`
  - `selectedId`: `string | null`
- **HTML generado por tarjeta:**
  ```html
  <div class="card card--clickable [card--active]" data-id="t-XXX">
    <div class="card-header">
      <div>
        <p class="card-name">Nombre completo</p>
        <p class="card-unit">Unidad</p>
      </div>
      <span class="badge [badge--ok|badge--late]">Al día | Atrasado</span>
    </div>
  </div>
  ```
- **data-* que emite:** `data-id="t-XXX"` en cada `.card`.
  app.js debe escuchar `click` en `#admin-tenant-list` y leer `event.target.closest(".card").dataset.id`.

### renderTenantDetail(tenant, today)
- **Contenedor destino:** `#admin-tenant-detail`
- **data-* que emite:**
  - `.btn-mark-paid`: `data-tenant-id="t-XXX"` y `data-month="YYYY-MM"`.
    app.js llama `RentalsStore.markMonthPaid(tenantId, month)` y re-renderiza.
  - `.btn-mark-one-time-paid`: `data-tenant-id="t-XXX"` y `data-charge-id="c-XXX"`.
    app.js llama `RentalsStore.markOneTimePaid(tenantId, chargeId)` y re-renderiza.
- **Estado vacío** (tenant === null/undefined):
  ```html
  <span class="empty-state">Selecciona un arrendatario para ver el detalle</span>
  ```

### renderTenantSelector(tenants, selectedId)
- **Contenedor destino:** `#tenant-selector`
- **HTML generado por botón:**
  ```html
  <button class="tenant-selector-btn [tenant-selector-btn--active]" data-id="t-XXX" type="button">
    Nombre completo
  </button>
  ```
- **data-* que emite:** `data-id="t-XXX"` en cada `.tenant-selector-btn`.
  app.js escucha `click` en `#tenant-selector`, lee `event.target.closest(".tenant-selector-btn").dataset.id`
  y re-renderiza con el nuevo `selectedId`.

### renderTenantAccount(tenant, today)
- **Contenedor destino:** `#tenant-account`
- **No emite botones de acción** (vista de solo lectura para el arrendatario).
- **Estado vacío** (tenant === null/undefined):
  ```html
  <span class="empty-state">Selecciona tu nombre para ver tu estado de cuenta</span>
  ```

## Decisiones tomadas y por qué
- **IIFE + `window.RentalsRender`**: igual que el resto de archivos del proyecto. Sin ES modules
  para compatibilidad `file://`.
- **`_escapeHtml`**: los datos mock son conocidos, pero la función ya existe para cuando en v2
  los datos vengan de input de usuario (formulario de registro).
- **`innerHTML` con concatenación de strings**: coherente con el estilo del equipo; mantiene el
  código compacto y legible. No se usó `createElement` para evitar código más verboso sin
  beneficio funcional en v1.
- **Delegación de eventos al llamador (app.js)**: render.js no registra ningún listener. Solo
  genera el HTML con los `data-*` correctos. app.js decide cuándo y cómo reaccionar. Esto
  respeta la separación de responsabilidades definida en ARCHITECTURE.md §3.
- **`_formatAmount`**: centraliza el formato de moneda (`"$450 USD"`) en un punto único. Si en
  v2 se añaden decimales o se cambia la moneda, solo cambia esta función.
- **Guardas `if (!container) return`**: cada función verifica que el contenedor exista antes de
  escribir. Robustez frente a HTML incompleto o cambios de estructura futuros.

## Problemas conocidos / riesgos
- render.js depende de `window.RentalsCalc` en tiempo de ejecución. Si los scripts no se cargan
  en el orden correcto (`calc.js` antes que `render.js`), fallará con "RentalsCalc is not defined".
  Este orden ya está garantizado en `src/index.html` (T-02).
- El formato de moneda no incluye decimales (`$450 USD`, no `$450.00 USD`). Según el modelo de
  datos, `amount` y `monthlyAmount` son enteros. Si en v2 se admiten decimales, actualizar
  `_formatAmount` en este archivo.

## Cómo verificar
1. Abrir `src/index.html` en el navegador (doble clic o `file://`). T-04 (app.js) debe estar
   implementada para que el render se dispare automáticamente al cargar.
2. Sin app.js, se puede verificar manualmente desde la consola:
   ```js
   var today = new Date(2026, 9, 3);   // 2026-10-03 local
   var tenants = RentalsStore.getAllTenants();

   // Lista de tarjetas en vista admin
   RentalsRender.renderTenantList(tenants, today, null);
   // → #admin-tenant-list debe contener 3 tarjetas .card

   // Seleccionar t-002 (ATRASADO)
   RentalsRender.renderTenantList(tenants, today, "t-002");
   // → tarjeta t-002 lleva .card--active

   // Detalle de t-002 (2 meses adeudados)
   RentalsRender.renderTenantDetail(RentalsStore.getTenantById("t-002"), today);
   // → #admin-tenant-detail muestra "2026-09" y "2026-10" con botones .btn-mark-paid

   // Detalle vacío
   RentalsRender.renderTenantDetail(null, today);
   // → muestra "Selecciona un arrendatario para ver el detalle"

   // Selector de arrendatario
   RentalsRender.renderTenantSelector(tenants, "t-001");
   // → #tenant-selector tiene 3 botones; t-001 lleva .tenant-selector-btn--active

   // Estado de cuenta t-001 (AL_DIA)
   RentalsRender.renderTenantAccount(RentalsStore.getTenantById("t-001"), today);
   // → #tenant-account muestra "Estás al día. ¡Gracias!"

   // Estado de cuenta t-002 (ATRASADO)
   RentalsRender.renderTenantAccount(RentalsStore.getTenantById("t-002"), today);
   // → muestra meses y total $1000 USD
   ```
