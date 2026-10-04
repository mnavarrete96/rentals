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

### Toggle Admin / Arrendatario — ELIMINADO EN v2 (`.mode-toggle` > `.btn-mode`)

> **v2 (2026-10-03): este componente desaparece.** La vista (Admin o Arrendatario) la determina el
> rol del JWT al iniciar sesión. No existe ningún control manual que permita cambiar de vista.
> El token CSS y el HTML de v1 se conservan en `src/` (congelada) pero no deben reproducirse en
> `frontend/`.

Referencia histórica (v1, no aplicar en v2):
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

---

### NUEVOS COMPONENTES v2 (autenticación)

### Banner de error (`.error-banner`)

Componente reutilizable en LoginView, ChangePasswordForm y TenantForm.

- Fondo `rgba(192,57,43,0.08)`, borde `1px solid rgba(192,57,43,0.28)`, color `--color-danger`.
- Border-radius `--radius-md` (12px), padding `--space-sm --space-md` (8px 16px).
- Font-size `0.875rem` (14px), font-weight 600, line-height 1.5.
- Visible **solo cuando hay error** (`v-if` en Vue); oculto por defecto.
- **Accesibilidad:** `role="alert"` + `aria-live="assertive"` + `aria-atomic="true"` en el elemento
  contenedor (no en el wrapper de todo el formulario).

### Campo de contraseña con visibilidad controlable (`.password-field`)

Wrapper para cualquier `<input type="password">` que necesite botón mostrar/ocultar.

- Estructura HTML:
  ```
  <div class="password-field">
    <input type="password" id="…" …>
    <button type="button" class="password-field__toggle" aria-label="Mostrar contraseña">
      <!-- icono ojo / ojo tachado según estado -->
    </button>
  </div>
  ```
- Contenedor: `position: relative`.
- Botón: `position: absolute; right: --space-md; top: 50%; transform: translateY(-50%)`.
  Fondo ninguno (`background: none`), borde ninguno, `color: --color-text-muted`.
  **Hover:** `color: --color-text`. Min-width/height 24px (accesible, tamaño táctil razonable
  dentro del campo).
- El input tiene `padding-right: 44px` para que el texto no se tape con el botón.
- Al pulsar el botón: cambia `type="password"` ↔ `type="text"`, cambia el icono y actualiza
  `aria-label` ("Mostrar contraseña" → "Ocultar contraseña").
- **Sin mensaje de texto** junto al icono: el icono solo (ojo / ojo tachado) es suficiente y el
  label aria cubre la accesibilidad.

### AppHeader — cabecera de sesión activa (`.app-header`)

Visible en AdminView y TenantView. **No visible en LoginView.**

- Posición: sticky top 0; `z-index: 100`.
- Fondo `--color-surface`, `border-bottom: 1px solid --color-border`, `box-shadow: --shadow-sm`.
- Altura: 56px en escritorio, 48px en móvil.
- Layout interno: `display: flex; align-items: center; justify-content: space-between; padding: 0 --space-xl`.
  - **Lado izquierdo:** nombre de la app o logo. Mismas reglas que el logo existente: Inter 1.375rem
    700, color `--color-accent`, letter-spacing -0.03em.
  - **Lado derecho:** fila de elementos con `gap: --space-sm` (8px).
    - Nombre de usuario autenticado: Inter 0.8125rem (13px) 500, `color: --color-text-muted`. Etiqueta
      de solo lectura, no es un botón.
    - Botón "Cambiar contraseña": `.btn-header` (ver abajo).
    - Botón "Salir": `.btn-header`.

#### Botón de cabecera (`.btn-header`)
- Padding `6px 12px`, font-size 0.8125rem (13px), font-weight 500.
- Fondo transparente, borde `1px solid --color-border`, radius `--radius-md`, color `--color-text`.
- **Hover:** borde `--color-border-hover`, fondo `--color-surface-alt`.
- **Focus-visible:** outline 2px `--color-accent` offset 2px.
- No usar `.btn-primary` ni `.btn-secondary` en el header: el tamaño y tono diferente es intencional.

#### AppHeader en móvil (< 640px)
- Altura 48px.
- El nombre de usuario se acorta (máx. 14 caracteres + ellipsis: `max-width: 120px; overflow: hidden;
  text-overflow: ellipsis; white-space: nowrap`).
- Los dos botones siguen visibles. Si el ancho es menor de 360px, el texto de los botones puede
  quedar solo con el icono (pero en v2 no se exige iconos: basta con texto).
- `padding: 0 --space-md`.

### LoginView — tarjeta de inicio de sesión (`.login-card`)

La **pantalla completa** es un contenedor centrado; la tarjeta es el formulario en sí.

#### Pantalla contenedora (`.login-view`)
- `min-height: 100vh; display: flex; align-items: center; justify-content: center; background: --color-bg`.
- Padding lateral de seguridad en móvil: `padding: --space-md`.

#### Tarjeta (`.login-card`)
- `max-width: 400px; width: 100%`.
- Fondo `--color-surface`, `border: 1px solid --color-border`, radius `--radius-md` (12px),
  `box-shadow: --shadow-md`, padding `--space-xl` (40px).

#### Contenido de la tarjeta
```
<div class="login-card">
  <h1 class="login-logo">Gestión de Arriendos</h1>     ← o logo de la app
  <form class="login-form">
    <div class="form-group">
      <label for="username">Usuario</label>
      <input id="username" type="text" autocomplete="username" …>
    </div>
    <div class="form-group">
      <label for="password">Contraseña</label>
      <div class="password-field"> … </div>
    </div>
    <div role="alert" aria-live="assertive" aria-atomic="true" class="error-banner">
      Usuario o contraseña incorrectos        ← solo visible en estado error
    </div>
    <button type="submit" class="btn-primary">Ingresar</button>   ← o "Ingresando…" en loading
  </form>
</div>
```

- **Título / logo (`.login-logo`):** mismo estilo que `.logo` del header — Inter 1.375rem 700,
  `color: --color-accent`. `margin-bottom: --space-lg` (24px).
- **`<form>` (`.login-form`):** `display: flex; flex-direction: column; gap: --space-md` (16px).
- **Etiquetas (`.form-group label`):** Inter 0.8125rem 500, `color: --color-text-muted`,
  `margin-bottom: --space-xs` (4px), `display: block`.
- **Inputs:** mismas reglas que en el formulario existente — fondo `--color-bg`, min-height 40px,
  borde `--color-border`, focus `--color-accent` + `--shadow-glow`.
- **ErrorBanner:** se sitúa entre los campos y el botón; oculto por defecto, visible al recibir
  error `401` del backend.
- **Botón "Ingresar" (`.btn-primary`):** ancho 100%, `margin-top: 0` (el gap del flex lo separa).

#### Estados del botón "Ingresar"
| Estado | Visual | Aria |
|---|---|---|
| Normal | Acento terracota, texto blanco | — |
| Loading | `disabled`, opacity 0.7, texto "Ingresando…" | `aria-busy="true"` |
| Error visible | Normal (vuelve a habilitarse) | — |

- **Sin enlace "Olvidé mi contraseña"**: no existe en v2.

#### Responsive
- **Móvil (< 640px):** la tarjeta usa `width: 100%`; con el padding de `.login-view` de `--space-md`
  queda a 16px de cada borde. El formulario se ve exactamente igual que en escritorio (no hay columnas
  que colapsar). Tamaño mínimo 375px: verificar que nada desborde.
- **Tableta / escritorio:** la tarjeta queda centrada con `max-width: 400px`.

### ChangePasswordForm — formulario de cambio de contraseña

Implementado como **modal** para no interrumpir el contexto de la vista activa.

#### Overlay y contenedor (`.modal-overlay`, `.modal`)
- `.modal-overlay`: `position: fixed; inset: 0; background: rgba(0,0,0,0.42);
  display: flex; align-items: center; justify-content: center; z-index: 200; padding: --space-md`.
- `.modal`: `background: --color-surface; border-radius: --radius-md; box-shadow: --shadow-md;
  max-width: 440px; width: 100%; padding: --space-xl`.
- **Accesibilidad:** `role="dialog"`, `aria-modal="true"`, `aria-labelledby="modal-title"`.
  Trampa de foco (`focus trap`) al abrir: el foco pasa al primer campo; Tab cicla dentro del modal;
  Escape cierra el modal (llama a cancelar).

#### Contenido del modal
```
<div class="modal" role="dialog" aria-modal="true" aria-labelledby="change-pw-title">
  <h2 id="change-pw-title">Cambiar contraseña</h2>
  <form class="change-pw-form">
    <div class="form-group"> Contraseña actual (PasswordField) </div>
    <div class="form-group">
      Nueva contraseña (PasswordField)
      <span class="field-hint">Mínimo 10 caracteres</span>
    </div>
    <div class="form-group"> Confirmar nueva contraseña (PasswordField) </div>
    <div role="alert" aria-live="assertive" class="error-banner"> … </div>
    <div class="modal-success" aria-live="polite"> … </div>
    <div class="modal-actions">
      <button type="button" class="btn-secondary">Cancelar</button>
      <button type="submit" class="btn-primary">Cambiar</button>
    </div>
  </form>
</div>
```

- **Título (h2):** Inter 1.125rem (18px) 600, `color: --color-text`, `margin-bottom: --space-lg`.
- **`.field-hint`:** Inter 0.75rem (12px) 400, `color: --color-text-subtle`, `display: block`,
  `margin-top: --space-xs` (4px). Solo aparece en el campo "Nueva contraseña".
- **`.modal-actions`:** `display: flex; justify-content: flex-end; gap: --space-sm; margin-top: --space-lg`.
  "Cancelar" a la izquierda del par, "Cambiar" a la derecha.
- **Validación en frontend:** antes de enviar, verificar que "Nueva contraseña" == "Confirmar nueva
  contraseña"; si no coinciden, mostrar en el ErrorBanner "Las contraseñas no coinciden" (no hace
  petición al backend). El backend no tiene endpoint de confirmación.

#### Estados del modal
| Estado | Visual |
|---|---|
| Normal | Todos los campos vacíos, botones habilitados |
| Loading | Botón "Cambiar" `disabled` + `aria-busy="true"` + texto "Cambiando…", campos `readonly` |
| Error (contraseña actual incorrecta) | ErrorBanner visible: "Contraseña actual incorrecta". Campos se habilitan. Foco vuelve al campo "Contraseña actual" |
| Error (confirmación no coincide) | ErrorBanner visible: "Las contraseñas no coinciden". Validación en frontend, sin llamada al servidor |
| Éxito | ErrorBanner oculto. `.modal-success` muestra: "Contraseña cambiada correctamente. Inicia sesión de nuevo." en `color: --color-success` (Inter 0.875rem 500). El botón "Cambiar" se deshabilita. Tras 2 segundos, `App.vue` llama a `clearSession()` y muestra LoginView |

- **`.modal-success`:** `color: --color-success`, `font-size: 0.875rem`, `font-weight: 500`,
  `margin-top: --space-sm`. `aria-live="polite"` para anunciarlo a lectores de pantalla.
- En móvil el modal toma `width: 100%` con el padding del overlay; sin scroll horizontal.

### TenantForm — campos de credenciales (`.form-panel` actualización)

El formulario de registro de arrendatario ya tenía los campos de datos base (RF-01).
En v2 se añaden **al final del formulario**, antes del botón de submit, dos campos nuevos:

#### Campo "Nombre de usuario" (username)
- Etiqueta: "Nombre de usuario"
- Input `type="text"`, `autocomplete="username"`.
- **Texto de ayuda / hint** (`.field-hint` debajo del input): "Formato sugerido: nombre.apellido —
  solo letras minúsculas, números, puntos, guiones y guion bajo (3–50 caracteres)".
- **Validación en frontend:** patrón `^[a-z0-9._-]{3,50}$`. Si no cumple el patrón, mostrar el
  hint en `color: --color-danger` en lugar del gris habitual (cambio de estado del hint, no un
  banner separado). El backend también valida con `@Pattern`.
- Ocupa **ancho completo** (clase `.--full` o equivalente en el grid de 2 columnas).

#### Campo "Contraseña inicial" (initialPassword)
- Etiqueta: "Contraseña inicial"
- Implementado con el componente `PasswordField` (botón mostrar/ocultar).
- **Texto de ayuda / hint** debajo: "Mínimo 10 caracteres. El arrendatario deberá cambiarla al
  ingresar por primera vez." (recomendación, no enforced en v2).
- **Validación en frontend:** longitud ≥ 10; si no cumple, mostrar hint en `color: --color-danger`.
- Ocupa **ancho completo**.

#### Error 409 — username duplicado
- Si el backend responde `409`, mostrar un **ErrorBanner** visible en la parte superior del
  formulario (antes del primer campo o dentro de la sección de credenciales): "El nombre de usuario
  ya está en uso. Elige otro."
- Los demás campos conservan su valor para que el admin solo corrija el username.
- **Accesibilidad:** el banner debe tener `role="alert"` + `aria-live="assertive"` para que los
  lectores de pantalla lo anuncien automáticamente.

---

## 6. Pantallas / secciones

### LoginView (pantalla de inicio de sesión) — v2

Pantalla exclusiva para la autenticación. Visible cuando `auth.status === 'anonymous'`.
**No existe header ni toggle en esta pantalla.**

```
<body>
  <div class="login-view">              ← min-height: 100vh, flex centrado
    <div class="login-card">            ← max-width 400px, padding --space-xl
      <h1 class="login-logo">…</h1>    ← nombre / logo de la app (terracota)
      <form class="login-form">
        <div class="form-group">        ← label + input "Usuario"
        <div class="form-group">        ← label + PasswordField "Contraseña"
        <div role="alert" …>            ← ErrorBanner (oculto por defecto)
        <button class="btn-primary">   ← "Ingresar" / "Ingresando…"
      </form>
    </div>
  </div>
</body>
```

- Responsive: en 375px el card toma ancho total menos 32px de padding lateral.
- No hay sidebar, no hay header, no hay toggle.
- Error genérico siempre: "Usuario o contraseña incorrectos" (nunca revela si el usuario existe).

### Vista Administrador — v2 (`AdminView.vue`)

```
<AppHeader>                                ← sticky; nombre usuario + btns
<main class="main-content">
  <div class="admin-layout">              ← 2 cols: 320px | 1fr
    <aside class="admin-sidebar">
      ├── section-title "Arrendatarios"
      ├── tenant-list (scroll) → cards clickables
      └── form-panel "Registrar arrendatario"
          ├── … campos base (RF-01) …
          ├── form-group "Nombre de usuario"  ← NUEVO v2
          └── form-group "Contraseña inicial" ← NUEVO v2 (PasswordField)
              [ErrorBanner 409 si username duplicado]
    </aside>
    <section class="admin-main">
      └── detail-panel
          ├── detail-header (nombre + badge)
          ├── section-label "Meses adeudados"
          ├── months-list → month-tags + btn-mark-paid
          ├── section-label "Cargos únicos"
          ├── charge-rows
          └── total-row
    </section>
  </div>
  <!-- ChangePasswordForm modal: se monta sobre el layout cuando el usuario lo solicita -->
</main>
```

En móvil (< 640px): columna única; sidebar encima, main abajo.
AppHeader en móvil: compacto (48px), nombre de usuario truncado a 14 car., botones `.btn-header`.

### Vista Arrendatario — v2 (`TenantView.vue`)

El arrendatario ve **solo su propia cuenta**. No hay selector de nombre.

```
<AppHeader>                                ← sticky; nombre usuario + btns
<main class="main-content">
  <section class="tenant-view">
    ├── section-title "Mi estado de cuenta"
    └── detail-panel
        ├── account-header (nombre + badge)
        ├── section-label "Meses que debes"
        ├── months-list → month-tags (sin btn-mark-paid: el arrendatario no marca pagos)
        ├── section-label "Cargos adicionales"
        ├── charge-rows
        └── total-row
  </section>
  <!-- ChangePasswordForm modal: disponible desde AppHeader -->
</main>
```

En móvil: columna única, sin sidebar (no hay selector). Diseño más simple que la vista admin.
Layout de `tenant-view`: `max-width: 760px; margin: 0 auto` para que el detalle no se extienda
a todo el ancho en pantallas grandes (mejor legibilidad).

### Estado de carga inicial (`status === 'unknown'`)

Mientras `App.vue` espera la respuesta de `restoreSession()` (el intento de refresh):

- Mostrar un indicador de carga centrado: spinner o texto "Cargando…" con `color: --color-text-muted`.
- Sin header, sin login: evita el parpadeo de pantalla (flash of unauthenticated content).
- No es necesario un componente elaborado; basta con un `<div>` centrado en `min-height: 100vh`.

## 7. Accesibilidad

### Requisitos generales (v1 y v2)
- [x] HTML semántico: `<header>`, `<main>`, `<section>`, `<aside>`, `<nav>`, `<form>`, `<label for>`.
- [x] Contraste AA: texto principal ~17:1 (AAA), texto muted ~7.2:1 (AAA), acento sobre blanco ~5.1:1 (AA), estados ~4.9–5.5:1 (AA).
- [x] Foco visible: `:focus-visible` con outline 2px `--color-accent` (terracota) offset 2px; botones primarios con outline blanco.
- [x] Textos alternativos: no hay imágenes. Iconos decorativos con `aria-hidden="true"`.
- [x] Tamaños táctiles: inputs min-height 40px; tenant-selector-btn min-height 44px.
- [x] `aria-live="polite" aria-atomic="true"` en paneles de detalle para anunciar cambios a lectores de pantalla.
- [x] `role="list"` en listas de arrendatarios.

### Requisitos adicionales v2 (autenticación)
- [x] **LoginView:** `<label for>` vinculado a cada input; `autocomplete="username"` y
  `autocomplete="current-password"` en los campos de login para compatibilidad con gestores de
  contraseñas.
- [x] **ErrorBanner:** `role="alert"` + `aria-live="assertive"` + `aria-atomic="true"` para que los
  lectores de pantalla anuncien errores de login y de formulario sin que el usuario mueva el foco.
- [x] **PasswordField:** botón con `aria-label` dinámico ("Mostrar contraseña" / "Ocultar
  contraseña"); el icono tiene `aria-hidden="true"`. El botón recibe foco por teclado normalmente.
- [x] **AppHeader:** el nombre de usuario es texto puro (no botón). Botones "Cambiar contraseña" y
  "Salir" son `<button>` nativos con texto descriptivo (no solo iconos).
- [x] **ChangePasswordForm (modal):** `role="dialog"`, `aria-modal="true"`,
  `aria-labelledby="change-pw-title"`. Trampa de foco activa mientras el modal está abierto. Escape
  cierra el modal. `.modal-success` con `aria-live="polite"` para anunciar éxito.
- [x] **TenantForm (credenciales):** `autocomplete="new-password"` en el campo de contraseña
  inicial. El ErrorBanner 409 con `role="alert"` + `aria-live="assertive"`.
- [ ] `aria-pressed` en el toggle Admin/Arrendatario: **ya no aplica** (componente eliminado en v2).
- [x] **Estado de carga:** el indicador de carga inicial tiene `role="status"` +
  `aria-label="Cargando aplicación"` para informar a lectores de pantalla.

## 8. Historial de cambios

| Fecha | Cambio | Motivo |
|---|---|---|
| 2026-10-03 | Versión inicial completada | Review del código implementado por dev1/dev2; doc creada a partir del brief SPEC.md §8 |
| 2026-10-03 | v2: paleta warm/light + espaciado ampliado en paneles de detalle | Pedido del usuario: look más cálido/claro, paneles Admin y Arrendatario con más respiro |
| 2026-10-03 | Incorporación de autenticación JWT en v2: LoginView, AppHeader, ChangePasswordForm, credenciales en TenantForm, eliminación del toggle Admin/Arrendatario | v2 introduce autenticación real con JWT; la vista se determina por el rol del token; nuevas pantallas y componentes documentados para dev1 (tarea U-01) |
