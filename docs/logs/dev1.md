# Bitácora de dev1

Entradas más recientes al final. Formato en `docs/templates/log-entry.md`.

---

## 2026-10-03 · T-02 · Base visual: index.html + styles.css

- **Qué hice:**
  Implementé la base visual completa de la aplicación.
  1. Creé `src/css/styles.css` con todas las custom properties de paleta, todos los componentes
     requeridos (`.card`, `.badge`, `.month-tag`, `.form-group`, `.btn-primary`, `.btn-mode`,
     `.detail-panel`, `.tenant-selector-list`, etc.) y layout responsive (columna única en móvil
     < 640 px, 2 columnas en desktop).
  2. Creé `src/index.html` con la estructura completa: header con toggle de modo, `#view-admin`
     (con `#admin-tenant-list`, `#admin-tenant-detail`, `#admin-register-form` y todos sus campos),
     `#view-tenant` (con `#tenant-selector` y `#tenant-account`), y los 5 scripts en el orden
     correcto al final del `<body>`.
  3. Creé `src/assets/fonts/README.md` documentando la migración CDN → local para producción.

- **Archivos tocados:**
  - `src/index.html` (creado)
  - `src/css/styles.css` (creado)
  - `src/assets/fonts/README.md` (creado — nota de migración, no código)
  - `docs/TASKS.md` (estado T-02 actualizado)

- **Decisiones y motivo:**
  - Fuente via CDN Google Fonts con `@import` en v1 (prototipo sin servidor); documentado en
    comentario al inicio del CSS y en `src/assets/fonts/README.md` para que en v2 se embeba local.
  - Stack de fallback: `'Inter', 'DM Sans', system-ui, sans-serif`.
  - `#view-admin` visible por defecto (`view--visible`); `#view-tenant` oculto. app.js alterna.
  - Scripts clásicos (no ES modules) para compatibilidad bajo `file://` sin servidor.
  - Formulario con `id` únicos por campo (`reg-full-name`, `reg-unit`, `reg-contact`,
    `reg-payment-day`, `reg-start-date`, `reg-monthly-amount`) para que app.js pueda leerlos.
  - `.form-group--full` cubre columnas completas en el grid de 2 columnas del formulario.
  - Paleta exactamente la especificada en la tarea; contraste texto/fondo supera WCAG AA.

- **Pendiente / siguiente paso:**
  - T-03: dev1 debe implementar `render.js` consumiendo los contratos de T-01 y los IDs de T-02.
  - T-04: dev2 implementa `app.js` una vez T-03 esté lista.

- **Nota de traspaso:** docs/handoff/T02-dev1.md

---

## 2026-10-03 · T-03 · Render: render.js

- **Qué hice:**
  Implementé `src/js/render.js` con las 4 funciones públicas del contrato `window.RentalsRender`:
  1. `renderTenantList(tenants, today, selectedId)` — tarjetas `.card` en `#admin-tenant-list`, badge
     de estado via `RentalsCalc.status`, clase `card--active` para el seleccionado.
  2. `renderTenantDetail(tenant, today)` — detalle completo en `#admin-tenant-detail`: cabecera,
     lista de meses adeudados con botones `.btn-mark-paid`, cargos únicos con `.btn-mark-one-time-paid`
     o `.charge-paid-label`, y fila de total. Estado vacío si `tenant` es null/undefined.
  3. `renderTenantSelector(tenants, selectedId)` — botones `.tenant-selector-btn` en `#tenant-selector`,
     con clase `tenant-selector-btn--active` para el seleccionado.
  4. `renderTenantAccount(tenant, today)` — estado de cuenta en `#tenant-account`: muestra mensaje
     positivo si AL_DIA; si ATRASADO muestra meses, cargos pendientes y total. Estado vacío si null.

  Auxiliares internas: `_formatMonth("2026-09") → "Sep 2026"`, `_formatAmount(450) → "$450 USD"`,
  `_badgeProps(status)`, `_escapeHtml(str)` (protección contra XSS en datos de usuario).

  Sin lógica de negocio en render.js: todo cálculo delega en `RentalsCalc.*`.
  Sin `import`/`export`; ES5/ES6 vanilla; script clásico IIFE.

- **Archivos tocados:**
  - `src/js/render.js` (creado)
  - `docs/TASKS.md` (estado T-03 actualizado)

- **Decisiones y motivo:**
  - `_escapeHtml` para evitar XSS al insertar datos de arrendatarios vía `innerHTML`.
  - IIFE para no contaminar el espacio global con las funciones auxiliares.
  - Guarda de `if (!container) return` en cada función para robustez frente a HTML incompleto.
  - Construcción con concatenación de strings en lugar de `createElement` para mantener
    el código conciso y coherente con el estilo de los otros archivos del proyecto.
  - `_formatAmount` centraliza el formato de moneda para que un cambio futuro (p.ej. decimales)
    sea en un solo lugar.

- **Pendiente / siguiente paso:**
  - T-04: dev2 implementa `app.js` ahora que T-03 está EN REVISIÓN.
  - T-05: tester valida render.js junto con el resto de la app.

- **Nota de traspaso:** docs/handoff/T03-dev1.md

---

## 2026-10-03 · T-05 · Fix CSS: 5 clases faltantes detectadas por tester

- **Qué hice:**
  Agregué los 5 bloques CSS ausentes al final de `src/css/styles.css` (sección marcada
  "Correcciones detectadas por tester (T-05)"):
  1. `.section-label` — subtítulo de sección dentro del panel de detalle y estado de cuenta.
  2. `.detail-header` — flex container para cabecera del panel de detalle (nombre + badge).
  3. `.account-header` — flex container para cabecera de cuenta en vista arrendatario.
  4. `.charge-paid-label` — etiqueta "Pagado" en color verde para cargos únicos saldados.
  5. `.btn-mark-one-time-paid` + `.btn-mark-one-time-paid:hover` — botón para marcar cargo
     único como pagado, misma estética que `.btn-mark-paid` ya existente.

  Todos los bloques usan exclusivamente custom properties ya definidas en `:root`
  (`--color-text-muted`, `--color-success`, `--space-*`, `--radius-sm`) para mantener
  coherencia con la paleta vigente.

- **Archivos tocados:**
  - `src/css/styles.css` (5 bloques añadidos al final)
  - `docs/TASKS.md` (estado T-05 actualizado a LISTO)

- **Decisiones y motivo:**
  - No se renombró `.section-label` a `.detail-section-label` (clase ya existente) porque
    implicaría modificar `render.js`, que no es archivo propio en esta tarea. Se deja como
    clase independiente; un refactor posterior puede unificarlas si el diseño lo permite.
  - Se mantiene la misma paleta rgba de `.btn-mark-paid` para `.btn-mark-one-time-paid`
    a fin de coherencia visual sin introducir nuevas variables de color.

- **Pendiente / siguiente paso:**
  - Ninguno desde el lado de dev1. T-05 queda LISTO.
  - Opcional futuro: unificar `.section-label` y `.detail-section-label` si dev1/dev2
    deciden refactorizar render.js en una tarea posterior.

- **Nota de traspaso:** docs/handoff/2026-10-03-dev1-T05.md

---

## 2026-10-03 · T-06 · Fuente Inter local: reemplazar @import CDN por @font-face

- **Qué hice:**
  Descargué los archivos .woff2 de Inter v20 desde Google Fonts CDN (gstatic.com) y los guardé
  en `src/assets/fonts/`. Inter se sirve como fuente variable, por lo que un solo archivo por
  subconjunto Unicode cubre todos los pesos 400–700:
  - `inter-v20-latin.woff2` (47 KB) — Latin básico, incluye á é í ó ú ñ ü (U+00C0-00FF).
  - `inter-v20-latin-ext.woff2` (83 KB) — Latin extendido (U+0100-02BA y otros).

  Reemplacé en `src/css/styles.css`:
  - Eliminé el `@import url('https://fonts.googleapis.com/...')` y el comentario CDN.
  - Añadí dos declaraciones `@font-face` con `font-weight: 400 700` (rango variable),
    `font-display: swap` y rutas relativas `../assets/fonts/inter-v20-latin*.woff2`.
  - Actualicé el comentario de cabecera del CSS para reflejar la fuente local.

  El `font-family: 'Inter', 'DM Sans', system-ui, sans-serif` en `body` no requirió cambios.

- **Archivos tocados:**
  - `src/assets/fonts/inter-v20-latin.woff2` (creado)
  - `src/assets/fonts/inter-v20-latin-ext.woff2` (creado)
  - `src/css/styles.css` (reemplazado @import por @font-face)
  - `docs/TASKS.md` (estado T-06 → EN REVISIÓN)

- **Decisiones y motivo:**
  - Fuente variable (`font-weight: 400 700`) en lugar de 4 archivos estáticos separados: Google
    Fonts sirve el mismo binario para todos los pesos dentro de cada subconjunto (es Inter como
    variable font), así que descargar un archivo por peso generaría duplicados idénticos. Dos
    archivos (latin + latin-ext) cubren la totalidad del contenido en español.
  - Se mantiene la unicode-range idéntica a la que Google Fonts declara, para que el navegador
    descargue solo el subconjunto necesario según los caracteres de la página (optimización de red).
  - `font-display: swap` preserva el comportamiento anterior (texto visible durante la carga).

- **Pendiente / siguiente paso:**
  - T-07: tester verifica visualmente que la fuente Inter carga correctamente sin conexión.

- **Nota de traspaso:** docs/handoff/2026-10-03-dev1-T06.md

---

## 2026-10-03 · T-07fix · Fix CSS especificidad: borde acento en hover sobre elementos activos

- **Qué hice:**
  Añadí 2 reglas CSS en `src/css/styles.css` para corregir el conflicto de especificidad
  detectado por el tester en T-07:
  1. `.card--active.card--clickable:hover { border-color: var(--color-accent); }` —
     especificidad 0-3-0, supera a `.card--clickable:hover` (0-2-0), preservando el borde
     azul acento cuando el cursor se sitúa sobre una tarjeta activa.
  2. `.tenant-selector-btn--active:hover { border-color: var(--color-accent); }` —
     mismo principio para el selector de arrendatario activo.
  Cada regla se insertó inmediatamente después del bloque que le da origen
  (`.card--active` y `.tenant-selector-btn--active` respectivamente).

- **Archivos tocados:**
  - `src/css/styles.css` (2 reglas añadidas, líneas 281-283 y 498-500)
  - `docs/TASKS.md` (T-06 → LISTO, T-07 → LISTO)
  - `docs/logs/dev1.md` (esta entrada)
  - `docs/handoff/2026-10-03-dev1-T07fix.md` (creado)

- **Decisiones y motivo:**
  - Reglas de override por especificidad en lugar de `!important`: más predecible y más
    fácil de sobreescribir si el diseño cambia.
  - Ubicación junto a la regla de origen (no al final del archivo) para facilitar la
    lectura y el mantenimiento: cada bloque activo tiene su override de hover a continuación.

- **Pendiente / siguiente paso:**
  - Ninguno. T-07 queda LISTO.

- **Nota de traspaso:** docs/handoff/2026-10-03-dev1-T07fix.md

---

## 2026-10-03 · F-01 · Scaffold frontend Vue 3 + Vite

- **Qué hice:**
  Creé el scaffold completo de `frontend/` desde cero:
  1. `package.json` con Vue 3.5, Pinia 2.2, Vite 6, Vitest 2, @vitejs/plugin-vue 5, jsdom 25.
  2. `vite.config.js`: plugin Vue, proxy `/api → http://localhost:8080`, entorno jsdom para tests.
  3. `index.html`: punto de entrada SPA en español, monta `#app`.
  4. `src/main.js`: createApp + createPinia + imports de tokens.css y base.css.
  5. `src/App.vue`: placeholder visible que confirma que el scaffold arranca.
  6. `src/assets/styles/tokens.css`: todas las custom properties de la paleta v2 portadas desde `src/css/styles.css` (variables `--color-*`, `--space-*`, `--radius-*`, `--shadow-*`, `--font-*`, `--transition`).
  7. `src/assets/styles/base.css`: reset, body, @font-face (rutas actualizadas a `../fonts/`) y `.sr-only`.
  8. `src/assets/fonts/`: copiados `inter-v20-latin.woff2` e `inter-v20-latin-ext.woff2` desde `src/assets/fonts/` (sin mover, solo copiar).
  9. `src/utils/format.js`: `formatUSD` (Intl.NumberFormat en-US currency) y `formatMonth` (toLocaleDateString es-EC, month long + year).
  10. `src/api/http.js`, `src/api/authApi.js`, `src/api/tenantsApi.js`: placeholders con estructura mínima y comentarios TODO F-02.
  11. `src/stores/auth.js`, `src/stores/tenants.js`: Pinia stores con estructura state/getters/actions mínimos y comentarios TODO F-02.
  12. `src/views/LoginView.vue`, `AdminView.vue`, `TenantView.vue`: placeholders con comentarios de implementación futura.
  13. `src/components/admin/`, `src/components/tenant/`, `src/components/common/`: directorios vacíos con `.gitkeep`.
  14. Verificación: `npm install && npm run build` termina en verde (63 KB JS + 133 KB fuentes).

- **Archivos tocados:**
  - `frontend/` (todo el directorio, creado desde cero)
  - `docs/TASKS.md` (F-01 → EN REVISIÓN)
  - `docs/logs/dev1.md` (esta entrada)
  - `docs/handoff/2026-10-03-dev1-F01.md` (creado)

- **Decisiones y motivo:**
  - `--font-body` añadida como custom property en tokens.css: permite que los componentes Vue la usen como `var(--font-body)` sin hardcodear el stack de fuentes.
  - `base.css` separado de `tokens.css`: los tokens son valores puros; base.css es el único lugar que aplica estilos globales (fácil de sobreescribir en tests unitarios si fuera necesario).
  - `formatMonth` usa `toLocaleDateString('es-EC', { month: 'long', year: 'numeric' })`: retorna el formato natural en español, p.ej. "enero de 2026". El locale es coherente con la zona horaria `America/Guayaquil` del proyecto.
  - Stores y api con estructura mínima exportada: F-02 puede implementar sin cambiar los import paths.
  - Sin `npm create vite` interactivo: estructura creada manualmente para control total y reproducibilidad.

- **Pendiente / siguiente paso:**
  - F-02: implementar `http.js` (Bearer + reintento), `authApi.js`, `tenantsApi.js`, `stores/auth.js` y `stores/tenants.js` + tests Vitest.
  - B-01: scaffold del backend (en paralelo, mismo sprint).

- **Nota de traspaso:** docs/handoff/2026-10-03-dev1-F01.md
