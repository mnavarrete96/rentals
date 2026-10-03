# Traspaso: T-02 · de dev1 · 2026-10-03

## Estado
- [x] Terminada  - [ ] A medias  - [ ] Bloqueada

## Qué quedó hecho
- `src/index.html`: estructura completa de la SPA (header, toggle de modo, vista admin, vista arrendatario, scripts en orden).
- `src/css/styles.css`: paleta con custom properties, todos los componentes visuales, layout responsive.
- `src/assets/fonts/README.md`: instrucciones para embeber Inter localmente en producción.

## Qué falta
- `src/js/render.js` (T-03, dev1): pintar los contenedores con datos reales.
- `src/js/app.js` (T-04, dev2): toggle de modo, eventos y orquestación general.

## Dónde continuar (el siguiente agente empieza aquí)
- dev1 (T-03 / render.js): leer esta nota y la nota T01-dev2.md para conocer los contratos de
  `RentalsStore` y `RentalsCalc`. Todos los IDs y clases que render.js necesita están listados
  en la sección "Referencia de IDs y clases" más abajo.
- dev2 (T-04 / app.js): esperar a que T-03 esté EN REVISIÓN, luego leer esta nota para los IDs
  de botones y formularios que app.js debe escuchar.

## Archivos tocados
- `src/index.html` (creado)
- `src/css/styles.css` (creado)
- `src/assets/fonts/README.md` (creado)
- `docs/TASKS.md` (estado T-02 → EN REVISIÓN)
- `docs/logs/dev1.md` (bitácora)

## Referencia de IDs y clases clave para render.js y app.js

### IDs de contenedores (render.js escribe dentro de estos)
| ID | Vista | Qué va adentro |
|---|---|---|
| `#admin-tenant-list` | Admin | Tarjetas `.card` de cada arrendatario. |
| `#admin-tenant-detail` | Admin | Detalle completo del arrendatario seleccionado. |
| `#admin-register-form` | Admin | Formulario (ya en HTML, app.js escucha su `submit`). |
| `#tenant-selector` | Arrendatario | Botones `.tenant-selector-btn` con nombre de cada arrendatario. |
| `#tenant-account` | Arrendatario | Estado de cuenta del arrendatario seleccionado. |

### IDs de botones del toggle (app.js escucha `click`)
| ID | Acción |
|---|---|
| `#btn-mode-admin` | Cambiar a modo admin (añadir `.btn-mode--active`, quitar del otro). |
| `#btn-mode-tenant` | Cambiar a modo arrendatario. |

### IDs de campos del formulario (app.js lee al submit de `#admin-register-form`)
| ID | Campo | Tipo |
|---|---|---|
| `#reg-full-name` | Nombre completo | text |
| `#reg-unit` | Unidad/departamento | text |
| `#reg-contact` | Teléfono o correo | text |
| `#reg-payment-day` | Día de pago (1–28) | number |
| `#reg-start-date` | Fecha de inicio ISO | date |
| `#reg-monthly-amount` | Monto mensual USD | number |

### Clases de componentes generados por render.js
```
Tarjeta de arrendatario (dentro de #admin-tenant-list):
  .card .card--clickable          — tarjeta base
  .card--active                   — tarjeta del arrendatario seleccionado
  .card-header                    — fila nombre + badge
  .card-name                      — nombre completo
  .card-unit                      — unidad
  .badge.badge--ok                — estado "Al día"
  .badge.badge--late              — estado "Atrasado"
  data-id="t-XXX"                 — atributo para identificar el tenant en eventos

Panel de detalle (dentro de #admin-tenant-detail):
  .detail-title                   — nombre del arrendatario
  .detail-unit                    — unidad
  .detail-section                 — sección interna (meses, cargos únicos)
  .detail-section-label           — etiqueta de sección
  .months-list                    — contenedor flex de .month-tag
  .month-tag                      — chip de mes adeudado (texto "2026-09")
  .btn-mark-paid                  — botón "Marcar pagado"
                                    data-tenant-id + data-month requeridos por app.js
  .charge-row                     — fila de cargo único
  .charge-desc                    — descripción del cargo
  .charge-amount.charge-amount--danger — monto del cargo impago
  .total-row                      — fila total
  .total-label                    — "Total adeudado"
  .total-amount                   — monto en USD
  .detail-panel--empty            — estado vacío (ninguno seleccionado)

Selector de arrendatario (dentro de #tenant-selector):
  .tenant-selector-btn            — botón de selección
  .tenant-selector-btn--active    — arrendatario activo
  data-id="t-XXX"                 — atributo para identificar el tenant

Estado de cuenta (dentro de #tenant-account):
  .account-name                   — nombre completo
  .account-unit                   — unidad
  .account-status-row             — fila nombre + badge
  .badge.badge--ok / .badge--late — estado Al día / Atrasado
  .months-list > .month-tag       — chips de meses adeudados
  .charge-row                     — cargos únicos pendientes
  .total-row > .total-amount      — total en USD
  .detail-panel--empty            — sin arrendatario seleccionado
```

### Clases para show/hide de vistas (app.js)
- `.view` — clase base para ambas vistas (display: none por defecto)
- `.view--visible` — clase que activa display (añadida por app.js a la vista activa)
- `#view-admin` empieza con `.view--visible` (admin es el modo por defecto)
- `#view-tenant` empieza sin `.view--visible`

## Decisiones tomadas y por qué
- **Fuente CDN en v1**: el prototipo abre vía `file://`; Google Fonts requiere conexión. Para
  demostración offline se puede abrir sin internet y la fuente cae al fallback `system-ui`.
  En producción se debe embeber localmente (documentado en `src/assets/fonts/README.md` y en
  el comentario inicial de `styles.css`).
- **`#view-admin` visible por defecto**: el modo admin es el caso de uso primario y necesita
  datos ya disponibles al cargar; app.js solo necesita registrar los listeners sin pintar de nuevo
  al arranque.
- **`form-group--full`** en campos de nombre completo y monto mensual para que ocupen el ancho
  total del grid de 2 columnas, mejorando legibilidad.
- **Scripts clásicos al final del `<body>`**: sin ES modules para compatibilidad `file://`.

## Problemas conocidos / riesgos
- La fuente Inter no cargará si el usuario abre el HTML sin conexión a internet (v1 / prototipo).
  El fallback `system-ui` es legible pero el diseño puede variar ligeramente.
- No hay validación visual de errores en el formulario de registro (solo `required` HTML5). En v2
  se puede añadir mensajes de error con clases CSS ya preparadas (`.form-group` es el contenedor
  natural para un `.form-error` futuro).

## Cómo verificar
1. Abrir `src/index.html` directamente en el navegador (doble clic o `file://`).
2. Verificar que se muestra el header con el logo y los dos botones del toggle.
3. Verificar que la vista Admin está visible y la vista Arrendatario está oculta.
4. (Sin JS funcional todavía) Los contenedores `#admin-tenant-list`, `#admin-tenant-detail`,
   `#admin-register-form`, `#tenant-selector`, `#tenant-account` deben existir en el DOM.
5. Verificar responsividad: en ventana < 640px el layout debe pasar a columna única.
