# Bitácora de tester

Entradas más recientes al final. Formato en `docs/templates/log-entry.md`.

---

## T-05 · Validación RF-01 a RF-07 · 2026-10-03

### Contexto

Revisión estática del código fuente contra `docs/SPEC.md`. No se dispone de navegador; se razona sobre el
comportamiento a partir de la lectura directa de los archivos.

Archivos leídos: `src/js/data.js`, `src/js/store.js`, `src/js/calc.js`, `src/js/render.js`,
`src/js/app.js`, `src/index.html`, `src/css/styles.css`.

---

### RF-01: Registro de arrendatario

PASA

Evidencia:

- `src/index.html` lineas 89-174: el formulario `#admin-register-form` contiene exactamente los 6 campos
  requeridos: `#reg-full-name`, `#reg-unit`, `#reg-contact`, `#reg-payment-day`, `#reg-start-date`,
  `#reg-monthly-amount`. Todos tienen `required`.
- `src/js/app.js` lineas 216-255: el handler de `submit` lee los 6 campos, construye un objeto `tenant`
  completo (con `oneTimeCharges: []` y `paidMonths: []`) y lo pasa a `RentalsStore.addTenant(tenant)`.
- Tras el submit, se invocan `RentalsRender.renderTenantList(tenants, today, _state.selectedAdminId)` y
  `RentalsRender.renderTenantSelector(tenants, _state.selectedTenantId)` (lineas 250-251).

---

### RF-02: Pagos unicos con descripcion

PASA

Evidencia:

- `src/js/data.js` lineas 44-49: `t-003` tiene dos `oneTimeCharges`, el segundo con
  `description: "Reparación ventana"` y `paid: false`. Confirma que la estructura de cargos
  con descripcion libre esta en los datos mock.
- `src/js/store.js` lineas 99-103: `addOneTimeCharge(tenantId, charge)` localiza al arrendatario y
  hace `push` del objeto cargo (que incluye `description`). La funcion esta expuesta en la API publica
  (linea 112).
- `src/js/render.js` lineas 158-169: el loop de `oneTimeCharges` en `renderTenantDetail` muestra
  `_escapeHtml(charge.description)` y el monto. Los cargos impagos incluyen boton
  `.btn-mark-one-time-paid` y los pagados muestran la etiqueta "Pagado".

---

### RF-03: Calculo de meses adeudados

PASA

Evidencia:

- `src/js/calc.js` lineas 38-69: `monthsOwed(tenant, today)` parsea `startDate` con `.split("-")`,
  itera mes a mes desde `startYear/startMon` hasta `endYear/endMon` (inclusive en ambos extremos) y
  excluye los meses presentes en `tenant.paidMonths`.
- Verificacion del algoritmo con los datos del enunciado:
  - `startDate = "2026-06-01"` => `startYear=2026`, `startMon=6`.
  - `today` = octubre 2026 => `endYear=2026`, `endMon=10`.
  - Rango generado: "2026-06", "2026-07", "2026-08", "2026-09", "2026-10".
  - `paidMonths = ["2026-06","2026-07","2026-08"]` => se eliminan jun-jul-ago.
  - Resultado: `["2026-09","2026-10"]`. Correcto.
- Para el arrendatario `t-002` de `data.js` (mismo `startDate` y `paidMonths`), el resultado coincide.

---

### RF-04: Marcar mes como pagado

PASA

Evidencia:

- `src/js/store.js` lineas 47-53: `markMonthPaid(tenantId, monthKey)` verifica con `indexOf` que el
  mes no este ya en el array antes de hacer `push` (idempotente).
- `src/js/app.js` lineas 162-181: listener delegado en `#admin-tenant-detail` detecta clicks en
  `.btn-mark-paid`, lee `data-tenant-id` y `data-month` del dataset del boton, llama
  `RentalsStore.markMonthPaid(tenantId, month)` y luego `_refreshAdminView()`.
- `render.js` lineas 143-150: los botones `.btn-mark-paid` llevan `data-tenant-id` y `data-month`
  como atributos, permitiendo que el handler los lea correctamente.

---

### RF-05: Vista arrendatario con estado de cuenta

PASA

Evidencia:

- `src/js/render.js` lineas 192-211: `renderTenantSelector` genera botones `.tenant-selector-btn`
  con `data-id` para cada arrendatario. El boton activo lleva `.tenant-selector-btn--active`.
- `src/js/app.js` lineas 200-211: listener delegado en `#tenant-selector` detecta el boton clicado,
  actualiza `_state.selectedTenantId` y llama `_refreshTenantView()`, que a su vez invoca
  `renderTenantAccount(selected, today)`.
- `src/js/render.js` lineas 225-292: `renderTenantAccount` muestra meses adeudados (`.month-tag`),
  cargos pendientes y total USD. El badge usa `_badgeProps(st)` que devuelve "Al dia"/"Atrasado".
- Verificacion para `t-002` (Carlos Ramos, `startDate = "2026-06-01"`, `paidMonths` solo
  jun-jul-ago, `today` = oct 2026):
  - `monthsOwed` devuelve `["2026-09","2026-10"]` (2 meses).
  - `status` devuelve "ATRASADO" porque `monthsOwed.length > 0`.
  - Badge: "Atrasado". Total: 2 * 500 = $1000 USD. Correcto segun SPEC.

---

### RF-06: Toggle de modo

PASA

Evidencia:

- `src/js/app.js` lineas 128-144: listeners en `#btn-mode-admin` y `#btn-mode-tenant`. Cada uno
  actualiza `_state.mode`, llama `_updateModeButtons(mode)` y `_updateViews(mode)`.
- `_updateModeButtons` (lineas 48-65): gestiona `.btn-mode--active` y `aria-pressed` en ambos botones.
- `_updateViews` (lineas 72-85): alterna `.view--visible` entre `#view-admin` y `#view-tenant`.
  `display:none` / `display:block` definidos en CSS (lineas 151-156 de styles.css).
- No hay `location.reload()` ni `href` navegacion: el toggle es puro JS.

---

### RF-07: Datos hardcodeados en JS

PASA

Evidencia:

- `src/js/data.js` lineas 1-52: `window.RentalsData` es un array literal con 3 arrendatarios.
  No hay `import`/`export` ni `fetch`.
- `src/index.html` lineas 267-271: scripts cargados en orden correcto
  `data.js -> store.js -> calc.js -> render.js -> app.js`, todos `<script src="...">` sin
  `type="module"`. Funciona bajo `file://`.

---

### Verificaciones adicionales de calidad

#### XSS — uso de _escapeHtml

PASA

Evidencia: en `render.js` todos los strings provenientes de datos de usuario que se insertan en
`innerHTML` (fullName, unit, contact, charge.description) pasan por `_escapeHtml`. Los datos de
sistema como keys de mes ("YYYY-MM") y montos numericos no necesitan escape y se insertan directamente
o via `_formatMonth`/`_formatAmount`.

#### Parseo de fechas — sin new Date("YYYY-MM-DD")

PASA

Evidencia: `calc.js` linea 41 usa `.split("-")` para extraer `startYear` y `startMon`. El objeto
`today` se crea con `new Date()` (sin string), lo que usa hora local y evita el desfase UTC.

#### Clases CSS usadas en render.js contra styles.css

FALLA

Las siguientes clases son generadas por `render.js` pero NO estan definidas en `styles.css`:

| Clase CSS | Usada en render.js | Definida en styles.css |
|---|---|---|
| `.section-label` | lineas 138, 154, 255, 274 | NO |
| `.detail-header` | linea 129 | NO |
| `.account-header` | linea 242 | NO |
| `.charge-paid-label` | linea 166 | NO |
| `.btn-mark-one-time-paid` | linea 164 | NO |

Impacto: los elementos existen y son funcionales (el comportamiento logico no se rompe), pero:
- `.section-label`: las etiquetas "Meses adeudados", "Cargos unicos", "Meses que debes",
  "Cargos adicionales" no tienen estilo diferenciado. La clase `.detail-section-label` SI existe en
  CSS (linea 334) pero no es la clase que render.js aplica; hay una discrepancia de nombre.
- `.detail-header` y `.account-header`: los contenedores de cabecera de detalle y de cuenta
  no tienen estilo de layout (flex, padding, etc.) definido.
- `.charge-paid-label`: la etiqueta "Pagado" en cargos unicos no tiene estilo.
- `.btn-mark-one-time-paid`: el boton hereda de `button` del reset pero no tiene el estilo
  visual de `.btn-mark-paid` que si esta definido. Visualmente inconsistente.

Responsable del arreglo: dev1 (propietario de `src/css/styles.css`).

#### Copia en memoria en store.js

PASA

Evidencia: `store.js` linea 17: `var _data = JSON.parse(JSON.stringify(window.RentalsData))`.
Copia profunda en el arranque. Todas las mutaciones afectan `_data`, no el array original.

---

### Veredicto global

APROBADO CON OBSERVACIONES

Issues criticos: ninguno. La aplicacion es funcional; todos los RF pasan.

Issues menores (no bloquean funcionalidad, pero degradan la presentacion visual):

1. MENOR — dev1 — `src/css/styles.css`: faltan 5 clases CSS usadas por render.js:
   `.section-label`, `.detail-header`, `.account-header`, `.charge-paid-label`,
   `.btn-mark-one-time-paid`. La clase `.detail-section-label` definida en CSS no coincide
   con `.section-label` que usa render.js (nombre divergente).

---

## T-07 · Verificación visual del CSS entregado por ui-designer · 2026-10-03

### Contexto

Revisión estática del código fuente (sin navegador headless) contra el checklist de 6 puntos
entregado por ui-designer en `docs/handoff/2026-10-03-ui-designer.md`. Se leyeron:
`src/css/styles.css`, `src/index.html`, `src/js/render.js`, `docs/DESIGN.md`, `docs/SPEC.md`.
El archivo `src/index.html` fue abierto en el navegador del sistema mediante `open` para
confirmación visual adicional.

---

### Punto 1 — Tarjeta activa: borde izquierdo azul de 3px + sombra elevada

FALLA MENOR

Evidencia de implementación:

`styles.css` lineas 247-252:
```css
.card--active {
  border-color: var(--color-accent);
  border-left-width: 3px;
  background-color: var(--color-surface-alt);
  box-shadow: var(--shadow-md);
}
```

El borde izquierdo de 3px y la sombra elevada están definidos correctamente. Sin embargo,
existe un conflicto de especificidad CSS:

- `.card--active` tiene especificidad 0-1-0 (una clase).
- `.card--clickable:hover` tiene especificidad 0-2-0 (clase + pseudoclase).

Cuando el usuario hace clic en una tarjeta y el cursor permanece sobre ella (estado común),
`.card--clickable:hover` gana el conflicto de `border-color` y lo sobreescribe con
`--color-border-hover` (#3d4565). El ancho de 3px se conserva (no hay conflicto en
`border-left-width`), pero el borde izquierdo pierde el azul acento (#4f8ef7) durante el hover.
El indicador azul sólo es visible cuando el cursor sale de la tarjeta activa.

El mismo problema afecta a `.tenant-selector-btn--active` vs `.tenant-selector-btn:hover`
(misma relación de especificidades).

Corrección necesaria (dev1, `src/css/styles.css`):
```css
.card--active.card--clickable:hover {
  border-color: var(--color-accent);
}
.tenant-selector-btn--active:hover {
  border-color: var(--color-accent);
}
```

---

### Punto 2 — Total-row: línea vertical azul + cifra en blanco bold

PASA

Evidencia:

`styles.css` lineas 377-400:
- `border-left: 3px solid var(--color-accent)` → línea vertical azul a la izquierda. ✓
- `.total-amount`: `color: var(--color-text)` (#e8eaf0, blanco casi puro sobre fondo oscuro)
  + `font-weight: 700`. ✓
- `border` shorthand en linea 383 establece 1px en todos los lados; `border-left` en
  linea 384 lo sobreescribe sólo en el lado izquierdo con 3px y color acento. Orden correcto
  en la declaración.

---

### Punto 3 — Month-tag: mes y botón "Marcar pagado" alineados horizontalmente

PASA

Evidencia:

`styles.css` lineas 304-315:
```css
.month-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);  /* 8px */
  ...
}
```

`render.js` lineas 145-149: el texto del mes y el botón `.btn-mark-paid` se insertan dentro
del mismo `<span class="month-tag">`. Con `inline-flex` y `align-items: center`, ambos quedan
en línea horizontal centrados verticalmente, separados por 8px de gap. ✓

---

### Punto 4 — Tenant-list: lista de 3+ arrendatarios no empuja el formulario

PASA

Evidencia:

`styles.css` lineas 632-641:
```css
.tenant-list {
  max-height: 420px;
  overflow-y: auto;
  ...
}
```

Con 3 tarjetas de aproximadamente 70-80px de altura cada una (padding 16px + contenido),
la lista ocupa ~240-260px, muy por debajo del límite de 420px. La lista usa `overflow-y: auto`,
por lo que cualquier desbordamiento activa el scroll interno en lugar de empujar los elementos
siguientes. El `.admin-sidebar` usa `flex-direction: column`, por lo que el `.form-panel`
siempre sigue al contenedor de la lista sin ser desplazado. ✓

---

### Punto 5 — Focus: outline azul visible al tabular

PASA con matiz

Evidencia:

`styles.css` lineas 771-782:
```css
:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}
.btn-primary:focus-visible,
.btn-mode--active:focus-visible {
  outline-color: #ffffff;
  outline-offset: 3px;
}
```

Para botones (`.btn-mode`, `.btn-mark-paid`, `.btn-secondary`, `.btn-primary`): `:focus-visible`
aplica correctamente el outline azul de 2px. ✓

Matiz — inputs dentro de `.form-group`: la regla `.form-group input:focus` (especificidad 0-2-1)
establece `outline: none` y anula `:focus-visible` (especificidad 0-1-0). Sin embargo, la misma
regla añade `border-color: var(--color-accent)` + `box-shadow: var(--shadow-glow)`
(0 0 0 3px rgba(79,142,247,0.2)), que proporciona una indicación visual azul equivalente al
outline. El requisito de "focus visible" queda satisfecho funcionalmente aunque no mediante
`outline` sino mediante borde+sombra.

---

### Punto 6 — Móvil 375px: header en columna, toggle ancho completo, columna única

PASA

Evidencia:

`styles.css` lineas 710-749 (`@media (max-width: 639px)`, que incluye 375px):
- `.header { flex-direction: column; }` → header en columna. ✓
- `.mode-toggle { width: 100%; }` → toggle a ancho completo. ✓
- `.btn-mode { flex: 1; min-height: 40px; }` → botones del toggle llenan el contenedor. ✓
- `.admin-layout, .tenant-layout { grid-template-columns: 1fr; }` → layout de columna única. ✓

---

### Issues de T-05 ya corregidos por ui-designer

Confirmados en `styles.css` versión actual:
- `.section-label` → lineas 802-809. ✓
- `.detail-header` → lineas 812-820. ✓
- `.account-header` → lineas 822-830. ✓
- `.charge-paid-label` → lineas 832-838. ✓
- `.btn-mark-one-time-paid` → lineas 610-627. ✓

---

### Veredicto T-07

APROBADO CON OBSERVACIÓN MENOR

5 de 6 puntos PASAN. 1 falla menor (Punto 1):

| Punto | Resultado | Acción requerida |
|---|---|---|
| 1. Tarjeta activa azul 3px | FALLA MENOR | dev1 debe añadir 2 reglas CSS para preservar border-color en hover sobre elemento activo |
| 2. Total-row acento + blanco bold | PASA | — |
| 3. Month-tag alineación horizontal | PASA | — |
| 4. Tenant-list no empuja formulario | PASA | — |
| 5. Focus outline azul visible | PASA | — |
| 6. Móvil 375px columna única | PASA | — |

Responsable del arreglo: dev1 (`src/css/styles.css`).

---

## F-01, F-02, F-03, F-04, F-05, U-01 · Revisión frontend v2 completo · 2026-10-03

### Contexto

Revisión estática del código fuente del frontend v2 contra `docs/SPEC.md` (§11, alcance v2),
`docs/DESIGN.md` y `docs/ARCHITECTURE.md` §7 y §15.7.

Archivos leídos: `frontend/package.json`, `frontend/vite.config.js`,
`frontend/src/assets/styles/tokens.css`, `frontend/src/assets/styles/base.css`,
`frontend/src/assets/fonts/`, `frontend/src/utils/format.js`,
`frontend/src/api/http.js`, `frontend/src/api/authApi.js`, `frontend/src/api/tenantsApi.js`,
`frontend/src/stores/auth.js`, `frontend/src/stores/tenants.js`,
`frontend/src/views/LoginView.vue`, `frontend/src/views/AdminView.vue`,
`frontend/src/views/TenantView.vue`, `frontend/src/App.vue`,
`frontend/src/components/common/AppHeader.vue`,
`frontend/src/components/common/ChangePasswordForm.vue`,
`frontend/src/components/admin/TenantList.vue`,
`frontend/src/components/admin/TenantDetail.vue`,
`frontend/src/components/admin/TenantForm.vue`,
`frontend/src/components/admin/ChargeForm.vue`,
`frontend/src/components/tenant/AccountStatement.vue`,
`frontend/src/test/http.test.js`, `frontend/src/test/auth.store.test.js`.

Comandos ejecutados:
- `npm run build` → VERDE (109 KB JS, fuentes Inter incluidas, advertencia no bloqueante sobre import dinámico de tenants.js)
- `npm run test` → 21/21 VERDE

---

### F-01: Scaffold frontend

| Verificación | Resultado | Evidencia |
|---|---|---|
| `package.json` versiones: Vue ^3.5.0, Pinia ^2.2.0, Vite ^6.0.0, Vitest ^2.0.0, jsdom ^25.0.0 | OK | Líneas 12-22 de `package.json` |
| `vite.config.js` proxy `/api → http://localhost:8080` | OK | Línea 8 de `vite.config.js` |
| `vite.config.js` test environment jsdom | OK | Línea 12 de `vite.config.js` |
| `tokens.css` paleta warm/light portada desde v1 (--color-bg, --color-accent, --color-success, --color-danger, etc.) | OK | `tokens.css` completo; valores coinciden con `docs/DESIGN.md §2` |
| `base.css` @font-face con rutas `../fonts/` | OK | Líneas 14-37 de `base.css` |
| Archivos .woff2 presentes en `frontend/src/assets/fonts/` | OK | `inter-v20-latin.woff2` e `inter-v20-latin-ext.woff2` presentes |
| `format.js` exporta `formatUSD` y `formatMonth` | OK | Líneas 12-25 de `format.js`; formatUSD usa `Intl.NumberFormat('en-US', {style:'currency', currency:'USD'})`, formatMonth usa `toLocaleDateString('es-EC', {...})` |
| `npm run build` verde | OK | Vite 6.4.3; 109 KB JS; sin errores |

**Resultado F-01: PASA**

---

### F-02: Capa HTTP y stores

| Verificación | Resultado | Evidencia |
|---|---|---|
| `http.js` exporta `apiFetch`, `ApiError`, `configureAuth` | OK | Líneas 23, 37, 59 de `http.js` |
| `apiFetch` añade `Authorization: Bearer` cuando `auth:true` y hay token | OK | Líneas 67-70 de `http.js` |
| Reintento único tras 401 con `auth:true` | OK | Líneas 75-91 de `http.js`; flag `_retry` previene segundo reintento |
| Refresh fallido llama `onAuthFailure` | OK | Línea 87 de `http.js` |
| 204 devuelve `null` | OK | Línea 98 de `http.js` |
| `authApi.js`: login, refresh, logout con `auth:false`; changePassword con `auth:true` | OK | `authApi.js` completo |
| `tenantsApi.js`: listTenants, getTenant, createTenant, addCharge, markChargePaid, markMonthPaid, unmarkMonth, getMyTenant | OK | `tenantsApi.js` completo; 8 funciones presentes |
| `stores/auth.js`: status, user, accessToken (solo memoria), refreshPromise | OK | Líneas 19-22 de `auth.js` |
| `stores/auth.js`: restoreSession, login, refreshAccessToken, logout, changePassword, clearSession | OK | Líneas 36-121 de `auth.js` |
| `stores/auth.js`: isAdmin, isTenant, isAuthenticated | OK | Líneas 25-27 de `auth.js` |
| `stores/auth.js`: configureAuth llamado al inicializar (DIP) | OK | Líneas 135-139 de `auth.js` |
| Token NO escrito en localStorage/sessionStorage | OK | Búsqueda textual en `src/` sin resultados de escritura a localStorage |
| `stores/tenants.js`: summaries, detailsById, myAccount, loading, error | OK | Líneas 15-21 de `tenants.js` |
| `stores/tenants.js`: loadSummaries, loadDetail/loadTenant, createTenant, addCharge, markChargePaid, markMonthPaid, unmarkMonth, loadMyAccount, reset | OK | Acciones completas (nota: método nombrado `loadDetail`+`selectTenant` en lugar de `loadTenant` — funcionalidad equivalente) |
| `npm run test` 21/21 verde | OK | Vitest 2.1.9; 2 archivos de test, 21 tests pasados |

**Resultado F-02: PASA**

---

### F-03: Vista Admin

| Verificación | Resultado | Evidencia |
|---|---|---|
| `AdminView.vue` carga summaries en `onMounted` | OK | Línea 39-41 de `AdminView.vue` |
| Layout 2 columnas `320px 1fr` | OK | Línea 259-262 de `AdminView.vue` |
| Modal TenantForm con manejo de 409 | OK | Líneas 59-76, 188-205 de `AdminView.vue` |
| Modal ChargeForm | OK | Líneas 208-224 de `AdminView.vue` |
| `TenantList.vue` tarjetas con badge AL_DIA/ATRASADO | OK | Líneas 55-67 de `TenantList.vue` |
| Tarjeta activa destacada con `card--active` (borde izquierdo 3px acento) | OK | Líneas 135-140 de `TenantList.vue` |
| `TenantDetail.vue` detalle completo con meses adeudados, cargos, total | OK | Completo con `formatUSD`, `formatMonth`, `btn-mark-paid`, `btn-mark-one-time-paid`, `total-row` |
| `TenantForm.vue` campos username + initialPassword con toggle | OK | Líneas 210-270 de `TenantForm.vue`; `showPassword` ref, SVG toggle inline |
| `TenantForm.vue` error 409 con `role="alert"` | OK | Líneas 103-113 de `TenantForm.vue` |
| `ChargeForm.vue` description + amount con validación | OK | Líneas 27-44 de `ChargeForm.vue` |

**Resultado F-03: PASA**

---

### F-04 + F-05: Vistas arrendatario, auth y orquestador

| Verificación | Resultado | Evidencia |
|---|---|---|
| `AccountStatement.vue` estado de cuenta con badge, meses, cargos, total | OK | Completo; filtra `unpaidCharges`, muestra "Estás al día. No tienes deudas pendientes." si totalOwed = 0 |
| `AccountStatement.vue` arrendatario no puede marcar pagos | OK | Solo lectura; no emite eventos de acción |
| `TenantView.vue` llama `loadMyAccount()` en `onMounted` | OK | Líneas 22-24 de `TenantView.vue` |
| `TenantView.vue` layout max-width 760px + AppHeader + ChangePasswordForm | OK | Líneas 36-57, 70-73 de `TenantView.vue` |
| `AppHeader.vue` logo terracota izquierda | OK | Líneas 31-34, 77-84 de `AppHeader.vue` |
| `AppHeader.vue` username (read-only) + "Cambiar contraseña" + "Salir" | OK | Líneas 37-57 de `AppHeader.vue` |
| `AppHeader.vue` sticky top 0, 56px escritorio / 48px móvil | OK | Líneas 62-70, 130-143 de `AppHeader.vue` |
| `ChangePasswordForm.vue` 3 campos PasswordField con toggle SVG | OK | Líneas 141-238 de `ChangePasswordForm.vue` |
| `ChangePasswordForm.vue` validación frontend: nueva == confirmar | OK | Líneas 88-91 de `ChangePasswordForm.vue` |
| `ChangePasswordForm.vue` error 401 → "Contraseña actual incorrecta" | OK | Línea 109 de `ChangePasswordForm.vue` |
| `ChangePasswordForm.vue` error 400 → detalle del backend | OK | Líneas 110-113 de `ChangePasswordForm.vue` |
| `ChangePasswordForm.vue` éxito → `.modal-success` + cierre en 2s | OK | Líneas 104-106 de `ChangePasswordForm.vue` |
| `ChangePasswordForm.vue` role="dialog" aria-modal, focus trap, Escape cierra | OK | Líneas 131-135, 51-73 de `ChangePasswordForm.vue` |
| `LoginView.vue` campos username/password con toggle | OK | Líneas 46-89 de `LoginView.vue` |
| `LoginView.vue` error genérico "Usuario o contraseña incorrectos" | OK | Línea 30 de `LoginView.vue` |
| `LoginView.vue` loading state (botón deshabilitado + "Ingresando…") | OK | Líneas 104-111 de `LoginView.vue` |
| `LoginView.vue` SIN enlace "Olvidé mi contraseña" | OK | Revisión completa del template; no existe tal enlace |
| `App.vue` `restoreSession()` en `onMounted` | OK | Líneas 25-27 de `App.vue` |
| `App.vue` status 'unknown' → div `.app-loading` con role="status" | OK | Líneas 32-39 de `App.vue` |
| `App.vue` status 'anonymous' → LoginView | OK | Línea 42 de `App.vue` |
| `App.vue` isAdmin → AdminView | OK | Línea 45 de `App.vue` |
| `App.vue` else → TenantView | OK | Línea 48 de `App.vue` |
| AppHeader presente en AdminView y TenantView; NO en LoginView | OK | Importado en `AdminView.vue` línea 14 y `TenantView.vue` línea 14; ausente en `LoginView.vue` |

**Resultado F-04 + F-05: PASA**

---

### U-01: DESIGN.md

| Verificación | Resultado | Evidencia |
|---|---|---|
| Sección LoginView con layout, campos y estados definidos | OK | `DESIGN.md` §5 "LoginView — tarjeta de inicio de sesión" y §6 "LoginView (pantalla de inicio de sesión)" |
| Sección AppHeader con elementos y responsive | OK | `DESIGN.md` §5 "AppHeader — cabecera de sesión activa" con `.app-header`, `.btn-header`, responsive móvil |
| Sección ChangePasswordForm con modal, campos y estados | OK | `DESIGN.md` §5 "ChangePasswordForm — formulario de cambio de contraseña" con overlay, 3 campos, tabla de estados |
| TenantForm con username + initialPassword documentados | OK | `DESIGN.md` §5 "TenantForm — campos de credenciales" con formato, hints y error 409 |
| Toggle Admin/Arrendatario marcado como ELIMINADO en v2 | OK | `DESIGN.md` §5 línea 119: "Toggle Admin / Arrendatario — ELIMINADO EN v2" |

**Resultado U-01: PASA**

---

### Seguridad

| Verificación | Resultado | Evidencia |
|---|---|---|
| Sin toggle v1 (`.mode-toggle`, `.btn-mode`) en `frontend/src/` | OK | `grep` sin resultados |
| Sin enlace "Olvidé mi contraseña" | OK | `grep` sin resultados |
| Sin secretos hardcodeados en frontend | OK | `grep` sin contraseñas reales en código |
| Token solo en memoria (no en localStorage/sessionStorage) | OK | Comentario en `auth.js` línea 5 + búsqueda grep sin escrituras |
| Sin `v-html` con datos del servidor | OK | No se encontró uso de `v-html` en componentes |

---

### Advertencia no bloqueante

La compilación de Vite muestra un aviso (no error) sobre `tenants.js` importado estáticamente por `AdminView.vue` y `TenantView.vue` y dinámicamente en `auth.clearSession()`. Esto no afecta el bundle de producción ni la funcionalidad. Fue documentado por dev1 en `docs/handoff/2026-10-03-dev1-F04F05.md`.

---

### Criterios de aprobación — resultado final

| Criterio | Estado |
|---|---|
| `npm run build` verde | OK |
| `npm run test` verde (21 tests) | OK |
| App.vue orquesta correctamente por status/rol | OK |
| TenantForm incluye username + initialPassword con toggle | OK |
| ChangePasswordForm tiene 3 campos, validación y manejo de errores | OK |
| AppHeader aparece en AdminView y TenantView | OK |
| LoginView sin enlace "Olvidé mi contraseña" | OK |
| Tokens CSS de v1 portados en tokens.css | OK |
| Fuentes .woff2 presentes en frontend/src/assets/fonts/ | OK |
| DESIGN.md tiene secciones de auth (LoginView, AppHeader, ChangePasswordForm) | OK |

**Veredicto global: APROBADO — todas las tareas F-01, F-02, F-03, F-04, F-05, U-01 pasan.**

---

## T-v2 · Validación backend completo v2 (B-01 a B-06, B-04) · 2026-10-04

### Contexto

Revisión estática del código fuente del backend v2 contra `docs/SPEC.md` (§11), `docs/ARCHITECTURE.md`
(§6, §15) y las notas de traspaso `docs/handoff/2026-10-04-dev2-B04.md` y
`docs/handoff/2026-10-03-dev2-B06.md`.

Archivos leídos: todos los `.java` en `backend/src/main/java/`, todos los `.sql` en
`backend/src/main/resources/db/`, `application.yml`, `application-dev.yml`,
`backend/src/test/resources/application.properties`.

Comando ejecutado: `./gradlew test --rerun` → BUILD SUCCESSFUL, **53 tests, 0 fallos, 0 errores, 0 omitidos**.

---

### 1. Estructura de archivos clave

| Archivo | Existe | Nota |
|---|---|---|
| `V1__init_schema.sql` | OK | 3 tablas: tenant, paid_month, one_time_charge |
| `V2__auth_schema.sql` | OK | app_user + refresh_token |
| `V3__auth_add_lockout.sql` | OK | failed_attempts + locked_until sobre app_user |
| `V1000__dev_seed.sql` | OK | 3 arrendatarios (perfil dev) |
| `V1001__dev_seed_users.sql` | OK | admin + 3 arrendatarios (perfil dev) |
| `Tenant.java` | OK | Entidad JPA con paidMonths (@ElementCollection) |
| `OneTimeCharge.java` | OK | Entidad JPA, @ManyToOne LAZY a Tenant |
| `AppUser.java` | OK | Entidad con failedAttempts, lockedUntil, isLocked(Clock), incrementFailedAttempts(Clock) |
| `RefreshToken.java` | OK | isRevoked(), isExpired(Clock), isValid(Clock), revoke(Clock) |
| `Role.java` | OK | enum ADMIN, ARRENDATARIO |
| `TenantStatus.java` | OK | enum AL_DIA, ATRASADO |
| `TenantService.java` | OK | @Transactional a nivel clase; método create() llama UserAccountService |
| `AuthService.java` | OK | login, refresh, logout, changePassword + lockout enforcement |
| `DebtCalculatorService.java` | OK | Puro, sin repositorios ni reloj directo |
| `UserAccountService.java` | OK | createTenantUser, createAdmin |
| `TenantController.java` | OK | 7 endpoints REST (consolida ChargeController y PaidMonthController) |
| `MeController.java` | OK | GET /api/me/tenant extrae tenantId del JWT |
| `AuthController.java` | OK | A1-A4 completos |
| `SecurityConfig.java` | OK | Stateless, CSRF off, reglas por ruta según §15.4 |
| `JwtConfig.java` | OK | Beans JwtEncoder/JwtDecoder HS256, JwtAuthenticationConverter |
| `JwtAccessTokenIssuer.java` | OK | Implementa AccessTokenIssuer |
| `RefreshTokenService.java` | OK | Token opaco, SHA-256 hash, rotación, reuse detection |
| `AdminBootstrapRunner.java` | OK | Lee de AuthProperties (env vars), idempotente |
| `RefreshCookieFactory.java` | OK | httpOnly(true), SameSite=Strict, Path=/api/auth |
| `GlobalExceptionHandler.java` | OK | ProblemDetail: 404, 400, 409, 401, 400 validation |

---

### 2. Requisitos funcionales (RF)

| RF | Verificación | Resultado | Evidencia |
|---|---|---|---|
| RF-01 | POST /api/tenants con username+initialPassword; @Transactional crea tenant+AppUser; 409 si username duplicado | OK | TenantController.create() → TenantService.create() @Transactional + UserAccountService.createTenantUser(); GlobalExceptionHandler mapea UsernameTakenException → 409 |
| RF-02 | POST /api/tenants/{id}/charges crea OneTimeCharge | OK | TenantController.addCharge() → TenantService.addCharge(); retorna 201 con TenantDetailDto |
| RF-04 | PUT /api/tenants/{id}/paid-months/{yearMonth} añade al Set<YearMonth> paidMonths | OK | TenantController.markMonthPaid() → TenantService.markMonthPaid() → tenant.getPaidMonths().add(ym); idempotente vía Set |
| RF-05 | GET /api/me/tenant lee tenantId del JWT (no URL) | OK | MeController: `Long tenantId = jwt.getClaim("tenantId")` → TenantService.getMyTenant(tenantId); sin IDOR |
| RF-06 | Login real: AuthController A1-A4, roles ADMIN y ARRENDATARIO | OK | AuthController + AuthService + JwtConfig + SecurityConfig completos |

---

### 3. Checklist de seguridad (ARCHITECTURE §15.9)

| Punto | Resultado | Evidencia |
|---|---|---|
| Access token solo en cuerpo JSON (no cookie) | OK | AuthController.login() retorna AuthResponse.accessToken en body; nunca Set-Cookie con token JWT |
| Refresh token en cookie HttpOnly | OK | RefreshCookieFactory.create(): `.httpOnly(true).sameSite("Strict").path("/api/auth")` |
| SecurityConfig: /api/tenants/** requiere ADMIN | OK | `requestMatchers("/api/tenants/**").hasRole("ADMIN")` |
| SecurityConfig: /api/me/** requiere ARRENDATARIO | OK | `requestMatchers("/api/me/**").hasRole("ARRENDATARIO")` |
| SecurityConfig: /api/auth/** permitAll (login/refresh/logout) | OK | 3 requestMatchers específicos con permitAll; PUT /api/auth/password requiere ADMIN o ARRENDATARIO |
| MeController extrae tenantId del JWT (sin IDOR) | OK | `jwt.getClaim("tenantId")` — ningún parámetro de URL |
| TenantService.create es @Transactional | OK | @Transactional a nivel clase en TenantService |
| AdminBootstrapRunner lee credenciales de env vars | OK | Lee de AuthProperties (@ConfigurationProperties sobre ${RENTALS_BOOTSTRAP_ADMIN_*}) |
| Sin secretos hardcodeados en código fuente | OK | application.yml: solo ${VAR}; test properties: dummy values con comentario explícito |
| AppUser.isLocked(Clock) + incrementFailedAttempts | OK | AppUser líneas 123-136: isLocked verifica lockedUntil; incrementFailedAttempts bloquea al llegar a 5 intentos por 15 min |
| RefreshToken.isRevoked() | OK | RefreshToken línea 74: `return revokedAt != null` |
| Rotación de refresh token en cada refresh | OK | AuthService.refresh(): revoca el token viejo, emite nuevo via refreshTokenService.createToken() |

---

### 4. Contrato REST (ARCHITECTURE §6)

| Endpoint | Método | Código esperado | Implementado | Clase |
|---|---|---|---|---|
| GET /api/tenants | GET | 200 | OK | TenantController.list() |
| GET /api/tenants/{id} | GET | 200 \| 404 | OK | TenantController.getById() + TenantNotFoundException |
| POST /api/tenants | POST | 201 + Location \| 409 | OK | TenantController.create() con ResponseEntity.created(location) |
| PUT /api/tenants/{id}/paid-months/{yearMonth} | PUT | 200 | OK | TenantController.markMonthPaid() |
| POST /api/tenants/{id}/charges | POST | 201 | OK | TenantController.addCharge() con status(201) |
| PUT /api/tenants/{id}/charges/{chargeId}/paid | PUT | 200 | OK | TenantController.markChargePaid() |
| DELETE /api/tenants/{id}/paid-months/{yearMonth} | DELETE | 200 | OK | TenantController.unmarkMonthPaid() |
| GET /api/me/tenant | GET | 200 | OK | MeController.getMyTenant() |
| POST /api/auth/login | POST | 200 + Set-Cookie | OK | AuthController.login() |
| POST /api/auth/refresh | POST | 200 + Set-Cookie \| 401 | OK | AuthController.refresh() |
| POST /api/auth/logout | POST | 204 + cookie borrada | OK | AuthController.logout() |
| PUT /api/auth/password | PUT | 204 + cookie borrada | OK | AuthController.changePassword() |
| GET /actuator/health | GET | 200 (público) | OK | Spring Boot Actuator; management.endpoints.web.exposure.include=health |

---

### 5. Tests del backend

Comando: `./gradlew test --rerun` desde `backend/`

| Suite | Tests | Fallos | Errores | Omitidos |
|---|---|---|---|---|
| TenantRepositoryTest (@DataJpaTest + Testcontainers) | 5 | 0 | 0 | 0 |
| DebtCalculatorServiceTest (JUnit puro) | 11 | 0 | 0 | 0 |
| TenantControllerTest (@WebMvcTest) | 16 | 0 | 0 | 0 |
| AuthServiceTest (Mockito puro) | 13 | 0 | 0 | 0 |
| AuthControllerTest (@WebMvcTest) | 5 | 0 | 0 | 0 |
| MeControllerTest (@WebMvcTest) | 3 | 0 | 0 | 0 |
| **TOTAL** | **53** | **0** | **0** | **0** |

Docker disponible: sí (TenantRepositoryTest con Testcontainers pasó).
BUILD SUCCESSFUL.

---

### 6. Deuda técnica y problemas menores

| Tipo | Descripción | Bloqueante |
|---|---|---|
| Desviación arquitectural | ARCHITECTURE §4.2 especifica clases separadas `ChargeController`, `PaidMonthController` y `TenantMapper`. La implementación las consolida en `TenantController` (con mapeo inline en `buildDetailDto`). Funcionalmente equivalente y cumple el contrato REST. | NO |
| Dev defaults en application-dev.yml | `DB_PASSWORD: ${DB_PASSWORD:rentals}` pone "rentals" como contraseña por defecto. Es conveniente para desarrollo local pero es un valor visible en el repo. Solo afecta al perfil dev. | NO |
| D-6 sin ADR formal | V3__auth_add_lockout.sql tiene comentario "D-6, approved 2026-10-04" pero no se encontró el ADR correspondiente en DECISIONS.md. La funcionalidad está correctamente implementada. | NO |
| AuthServiceTest creció de 10 a 13 tests | B-06 reportó 10 tests; el XML muestra 13. Los 3 adicionales cubren lockout (D-6). Correcto. | NO |

No hay TODO/FIXME en el código fuente de producción.

---

### Veredicto T-v2

**APROBADO**

Todos los criterios bloqueantes se cumplen:
- RF-01, RF-02, RF-04, RF-05, RF-06 implementados
- SecurityConfig con reglas de rol correctas
- MeController sin IDOR
- TenantService.create transaccional
- Refresh token en cookie HttpOnly
- AdminBootstrapRunner sin secretos hardcodeados
- isLocked + incrementFailedAttempts implementados
- 53/53 tests en verde (incluyendo Testcontainers)
