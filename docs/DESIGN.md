# Guía de diseño

> **Estado: APROBADO** — 2026-10-03 por ui-designer.
> Lo mantiene el `ui-designer`. Los devs la leen antes de construir cualquier interfaz.

## 1. Intención y tono

La interfaz es una herramienta de trabajo: el usuario llega con una tarea concreta (registrar un pago, revisar quién está atrasado) y debe poder cumplirla en segundos. El tono es profesional y sobrio, sin ornamentos decorativos. Se inspira en dashboards de finanzas y gestión de documentos — fondo blanco/crema, superficies claras, datos bien organizados con jerarquía visual explícita — y evita tanto el exceso corporativo frío como la informalidad colorida. Cada elemento gana espacio solo si cumple una función.

## 2. Paleta

> **v2 (2026-10-03):** migración de dark navy a warm/light. Los nombres de variables no cambiaron
> para no romper ningún selector existente.

| Rol | Variable CSS | Valor hex / rgba | Uso |
|---|---|---|---|
| Fondo base | `--color-bg` | `#faf9f7` | Fondo de la página (crema muy suave) |
| Superficie | `--color-surface` | `#ffffff` | Tarjetas, paneles, header |
| Superficie alt | `--color-surface-alt` | `#f2f0ed` | Hover / activo de tarjetas y total-row |
| Superficie elevada | `--color-surface-raised` | `#e8e4de` | Capas más altas (dropdown futuro) |
| Acento | `--color-accent` | `#a85632` | CTA, bordes activos, focus, logo — terracota cálido |
| Acento hover | `--color-accent-hover` | `#bf6540` | Hover sobre acento |
| Acento tenue | `--color-accent-dim` | `rgba(168,86,50,0.10)` | Fondo de month-tags |
| Texto principal | `--color-text` | `#1c1917` | Cuerpo y títulos — near-black warm |
| Texto apagado | `--color-text-muted` | `#57534e` | Etiquetas, subtítulos |
| Texto sutil | `--color-text-subtle` | `#a8a29e` | Placeholders, notas de menor importancia |
| Éxito | `--color-success` | `#2e7d52` | Badge "Al día", "Pagado", btn-mark-paid |
| Peligro | `--color-danger` | `#c0392b` | Badge "Atrasado", cargos pendientes |
| Borde | `--color-border` | `#e2ddd8` | Bordes en reposo |
| Borde hover | `--color-border-hover` | `#c9c2ba` | Bordes al pasar el cursor |

**Verificación de contraste (WCAG AA):**
- `--color-text` (#1c1917) sobre `--color-bg` (#faf9f7): ~17:1 — PASA AAA
- `--color-text-muted` (#57534e) sobre `--color-bg` (#faf9f7): ~7.2:1 — PASA AAA
- `#ffffff` sobre `--color-accent` (#a85632): ~5.1:1 — PASA AA
- `--color-success` (#2e7d52) sobre `--color-surface` (#ffffff): ~4.9:1 — PASA AA
- `--color-danger` (#c0392b) sobre `--color-surface` (#ffffff): ~5.5:1 — PASA AA

## 3. Tipografía

| Uso | Fuente | Tamaño | Peso | Interlineado |
|---|---|---|---|---|
| Logo | Inter | 1.375rem (22px) | 700 | — |
| Títulos de detalle / cuenta | Inter | 1.125rem (18px) | 600 | 1.4 |
| Nombres de arrendatario en tarjeta / cuenta | Inter | 1.25rem (20px) | 700 | 1.3 |
| Cuerpo base | Inter | 1rem (16px) | 400 | 1.6 |
| Etiquetas de campo y subtítulos | Inter | 0.8125rem (13px) | 500 | 1.5 |
| Section title (header de bloque) | Inter | 0.6875rem (11px) | 600 | — |
| Badges y chips | Inter | 0.75rem (12px) | 600 | — |
| Monto total | Inter | 1.25rem (20px) | 700 | — |

Fallback stack: `'Inter', 'DM Sans', system-ui, sans-serif`

> Nota v1: Inter se carga desde Google Fonts CDN. En v2 usar `@font-face` local desde `src/assets/fonts/`.

## 4. Espaciado y layout

### Escala de espaciado (base 4px)
| Variable | Valor | Uso típico |
|---|---|---|
| `--space-xs` | 4px | Gaps internos pequeños (icono+texto, chip+botón) |
| `--space-sm` | 8px | Padding de botones pequeños, gap entre tarjetas |
| `--space-md` | 16px | Padding de inputs, gap en grids de formulario |
| `--space-lg` | 24px | Padding de paneles, gap entre secciones |
| `--space-xl` | 40px | Padding del `main-content`, separaciones mayores |

### Ancho máximo del contenido
`max-width: 1200px`, centrado con `margin: 0 auto`.

### Rejillas
- Vista admin: `grid-template-columns: 320px 1fr` — sidebar fija + panel de detalle fluido.
- Vista arrendatario: `grid-template-columns: 260px 1fr`.
- Formulario: `grid-template-columns: 1fr 1fr` (campos de media anchura).

### Breakpoints
| Nombre | Rango | Comportamiento |
|---|---|---|
| Móvil | `< 640px` | Columna única; header en columna; toggle de modo a ancho completo |
| Tableta | `640px – 1023px` | Layout de 2 cols con sidebar más estrecha (280px / 220px); padding reducido |
| Escritorio | `>= 1024px` | Layout completo (320px / 260px sidebar) |

### Elevación (sombras)
| Variable | Valor | Uso |
|---|---|---|
| `--shadow-sm` | `0 1px 3px rgba(0,0,0,.08), 0 1px 2px rgba(0,0,0,.04)` | Tarjetas en reposo, paneles |
| `--shadow-md` | `0 4px 16px rgba(0,0,0,.10), 0 2px 6px rgba(0,0,0,.06)` | Tarjeta al hacer hover, tarjeta activa |
| `--shadow-glow` | `0 0 0 3px rgba(168,86,50,.18)` | Focus de inputs (acento terracota) |

## 5. Componentes

### Tarjeta de arrendatario (`.card`)
- Superficie: `--color-surface` con `--shadow-sm`.
- Border-radius: `--radius-md` (12px).
- **Normal:** fondo `--color-surface`, borde `--color-border`.
- **Hover:** fondo `--color-surface-alt`, borde `--color-border-hover`, sombra `--shadow-md`.
- **Activo (`.card--active`):** borde izquierdo de 3px en `--color-accent`, fondo `--color-surface-alt`, sombra `--shadow-md`.
- Estructura interna: `.card-header` (flex row, nombre + badge), `.card-name` (600 weight), `.card-unit` (text-muted).

### Badge de estado (`.badge`)
- Píldora (border-radius 999px), padding 3px 10px, font-size 0.75rem weight 600.
- **Al día (`.badge--ok`):** fondo `rgba(76,175,130,.12)`, color `--color-success`, borde `rgba(76,175,130,.3)`.
- **Atrasado (`.badge--late`):** fondo `rgba(224,92,107,.12)`, color `--color-danger`, borde `rgba(224,92,107,.3)`.

### Botón principal (`.btn-primary`)
- Fondo `--color-accent`, texto blanco, ancho 100% (contexto formulario).
- **Hover:** fondo `--color-accent-hover`, sombra sutil de acento.
- **Active:** fondo `--color-accent`, sin sombra.
- **Focus:** outline blanco de 2px con offset 3px.

### Botón secundario (`.btn-secondary`)
- Fondo transparente, borde `--color-border`.
- **Hover:** borde `--color-accent`, texto `--color-accent`.

### Botón "Marcar pagado" (`.btn-mark-paid`, `.btn-mark-one-time-paid`)
- Variante pequeña: fondo success tenue, borde success tenue, texto `--color-success`.
- Font-size 0.75rem. **Hover:** fondo más sólido + glow ring sutil.
- Estos dos botones son visualmente idénticos; sus nombres distintos reflejan el evento que disparan.

### Toggle Admin / Arrendatario (`.mode-toggle` > `.btn-mode`)
- Contenedor: fondo `--color-bg`, borde `--color-border`, radius `--radius-md`.
- Inactivo: texto `--color-text-muted`; hover: fondo `--color-surface-alt`.
- Activo (`.btn-mode--active`): fondo `--color-accent`, texto blanco.
- Min-height 36px en escritorio, 40px en móvil; `flex: 1` en móvil para llenar el contenedor.

### Chip de mes adeudado (`.month-tag`)
- Fondo `--color-accent-dim`, borde `rgba(168,86,50,0.22)`, color `--color-accent`.
- `display: inline-flex; align-items: center; gap: --space-sm` para contener el texto del mes + botón de marcar pagado.
- **Padding: `--space-sm --space-md` (8px 16px)** para mayor tamaño táctil y respiro visual.
- Lista (`.months-list`): **`gap: --space-md` (16px)** para separar chips con claridad.
- Variante peligro (`.month-tag--danger`): paleta danger (rgba 192,57,43).

### Panel de detalle (`.detail-panel`)
- Fondo `--color-surface`, borde `--color-border`, radius `--radius-md`, sombra `--shadow-sm`.
- **Padding: `--space-xl` (40px)** para dar amplio respiro interno.
- Vacío (`.detail-panel--empty`): centrado vertical, min-height 240px, texto `--color-text-muted`.
- En móvil: padding reducido a `--space-lg` (24px).

### Cabecera de detalle / cuenta (`.detail-header`, `.account-header`)
- Flex row: nombre+metadatos a la izquierda, badge a la derecha.
- **`margin-bottom: --space-xl` (40px)** y `padding-bottom: --space-lg` (24px) con separador `border-bottom`.

### Etiqueta de sección interna (`.section-label`)
- **`margin-top: --space-xl` (40px)** para separar secciones con claridad.
- `margin-bottom: --space-md` (16px) para dar respiro al contenido que sigue.

### Fila total deuda (`.total-row`)
- Fondo `--color-surface-alt`, borde left de 3px en `--color-accent` (indicador visual de cifra importante).
- **Padding: `--space-lg` (24px)** y `margin-top: --space-lg` (24px) para prominencia visual.
- Flex row: label izquierda (muted), amount derecha (text, 1.25rem weight 700).

### Formulario de registro (`.form-panel`)
- Superficie con `--shadow-sm`. Grid 2 columnas; campos `--full` ocupan el ancho completo.
- Input: fondo `--color-bg`, min-height 40px. Focus: borde `--color-accent` + `--shadow-glow`.
- Botón de submit: `.btn-primary` (100% ancho) con margin-top gestionado por `.form-panel .btn-primary`.

### Selector de arrendatario (`.tenant-selector-btn`)
- Min-height 44px para targets táctiles. Borde izquierdo de 3px en activo (igual que `.card--active`).

### Lista de arrendatarios (`.tenant-list`)
- Max-height 420px con `overflow-y: auto` y scrollbar personalizado (4px, color borde).
- En móvil: max-height 260px.

## 6. Pantallas / secciones

### Vista Administrador (`#view-admin`)
```
Header (sticky)
└── main-content
    └── admin-layout (2 cols: 320px | 1fr)
        ├── admin-sidebar
        │   ├── section-title "Arrendatarios"
        │   ├── tenant-list (scroll) → cards clickables
        │   └── form-panel "Registrar arrendatario"
        └── admin-main
            └── detail-panel (vacío o con detalle del arrendatario seleccionado)
                ├── detail-header (nombre + badge)
                ├── section-label "Meses adeudados"
                ├── months-list → month-tags + btn-mark-paid
                ├── section-label "Cargos únicos"
                ├── charge-rows
                └── total-row
```
En móvil: columna única; sidebar encima, main abajo.

### Vista Arrendatario (`#view-tenant`)
```
Header (sticky)
└── main-content
    └── tenant-layout (2 cols: 260px | 1fr)
        ├── aside
        │   ├── section-title "Seleccionar arrendatario"
        │   └── tenant-selector-list → tenant-selector-btns
        └── section
            ├── section-title "Estado de cuenta"
            └── detail-panel (vacío o con cuenta del arrendatario)
                ├── account-header (nombre + badge)
                ├── section-label "Meses que debes"
                ├── months-list → month-tags
                ├── section-label "Cargos adicionales"
                ├── charge-rows
                └── total-row
```
En móvil: columna única; selector encima, cuenta abajo.

## 7. Accesibilidad

- [x] HTML semántico: `<header>`, `<main>`, `<section>`, `<aside>`, `<nav>`, `<form>`, `<label for>`.
- [x] Contraste AA: texto principal ~17:1 (AAA), texto muted ~7.2:1 (AAA), acento sobre blanco ~5.1:1 (AA), estados ~4.9–5.5:1 (AA).
- [x] Foco visible: `:focus-visible` con outline 2px `--color-accent` (terracota) offset 2px; botones primarios con outline blanco.
- [x] Teclado: toggle Admin/Arrendatario con `<button>` nativos; `aria-pressed` en cada opción.
- [x] Textos alternativos: no hay imágenes en v1.
- [x] Tamaños táctiles: inputs min-height 40px; tenant-selector-btn min-height 44px; btn-mode min-height 36px (40px móvil).
- [x] `aria-label` en `<nav class="mode-toggle">` ("Cambiar modo") y en las dos `<section>` de vistas.
- [x] `aria-live="polite" aria-atomic="true"` en paneles de detalle para anunciar cambios a lectores de pantalla.
- [x] `role="list"` en `#admin-tenant-list` y `#tenant-selector`.

## 8. Historial de cambios

| Fecha | Cambio | Motivo |
|---|---|---|
| 2026-10-03 | Versión inicial completada | Review del código implementado por dev1/dev2; doc creada a partir del brief SPEC.md §8 |
| 2026-10-03 | v2: paleta warm/light + espaciado ampliado en paneles de detalle | Pedido del usuario: look más cálido/claro, paneles Admin y Arrendatario con más respiro |
