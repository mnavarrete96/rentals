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
