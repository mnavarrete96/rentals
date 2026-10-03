# Bitácora de ui-designer

Entradas más recientes al final. Formato en `docs/templates/log-entry.md`.

---

## 2026-10-03 · T-02/T-05 · Revisión y mejora del diseño visual

- **Qué hice:**
  Leí SPEC.md (§8 brief de diseño), ARCHITECTURE.md, DESIGN.md (borrador) y la nota del tester T-05.
  Revisé src/index.html y src/css/styles.css completos, más render.js para entender las clases dinámicas.

  Evaluación del estado previo:
  - Paleta y estructura: ya correctas (dark navy, acento azul, tipografía Inter).
  - Puntos débiles detectados: ausencia de sombras/elevación (look plano), section-title con opacity
    dimming, total-row sin indicador visual de importancia, month-tag sin gap para el botón interno,
    tenant-list sin límite de altura (rompería con 20 arrendatarios), inline style en el submit del
    formulario, badges sin borde explícito, falta de tokens de transición unificados.
  - Las 5 clases del tester (section-label, detail-header, account-header, charge-paid-label,
    btn-mark-one-time-paid) ya estaban presentes al pie del CSS (añadidas por dev1). Las conservé
    y refiné ligeramente.

  Cambios aplicados en src/css/styles.css:
  1. Nuevos tokens en :root: --color-surface-raised, --color-accent-dim, --color-text-subtle,
     --color-border-hover, --radius-lg, --shadow-sm, --shadow-md, --shadow-glow, --transition.
  2. Header: añadida box-shadow para separación visual del contenido.
  3. Logo: tamaño 1.375rem (era 1.25rem), letter-spacing -0.03em para más carácter.
  4. Cards: box-shadow en reposo; hover con shadow-md + border-hover; activo con borde izquierdo
     de 3px en acento (indicador de selección más robusto que solo border-color).
  5. Badges: borde explícito en ok y late (más definición sobre fondos oscuros).
  6. month-tag: gap: --space-sm para separar el texto del mes del botón btn-mark-paid.
  7. detail-panel: box-shadow + min-height 240px.
  8. total-row: border-left 3px accent (señal de cifra importante), letter-spacing en total-amount.
  9. detail-header, account-header: añadido border-bottom para separar cabecera del cuerpo.
  10. section-title: simplificado (color text-muted sin opacity, letra más pequeña con más tracking).
  11. section-label: weight 600 (era 500) y letter-spacing 0.03em para mejor legibilidad.
  12. form-panel: box-shadow + .form-panel .btn-primary { margin-top } para eliminar inline style del HTML.
  13. inputs: min-height 40px; placeholders con --color-text-subtle; foco con --shadow-glow.
  14. tenant-selector-btn: min-height 44px (target táctil), borde izquierdo 3px en activo.
  15. tenant-list: max-height 420px, overflow-y auto, scrollbar personalizado (4px).
  16. btn-mode: min-height 36px escritorio, 40px en breakpoint móvil con flex: 1.
  17. Botones mark-paid: font-size 0.75rem + hover con glow ring sutil.
  18. :focus-visible: añadido border-radius 2px; override para botones con fondo propio (outline blanco).
  19. Responsive tableta: añadido padding: --space-lg en main-content.
  20. Transiciones unificadas a var(--transition) en todos los elementos interactivos.

  Cambios en src/index.html (mínimos, solo semántica):
  - role="list" en #admin-tenant-list y #tenant-selector.
  - aria-live="polite" aria-atomic="true" en #admin-tenant-detail y #tenant-account.
  - Eliminado style="margin-top: 16px;" del botón submit (cubierto por CSS).

  Completado docs/DESIGN.md con paleta real (valores hex + verificación de contraste), tipografía,
  escala de espaciado, sombras, componentes documentados, estructura de pantallas y checklist de
  accesibilidad.

- **Archivos tocados:**
  - `src/css/styles.css`
  - `src/index.html`
  - `docs/DESIGN.md`
  - `docs/logs/ui-designer.md`
  - `docs/handoff/2026-10-03-ui-designer.md`

- **Decisiones y motivo:**
  - Mantuve la paleta dark navy + acento azul (#4f8ef7): ya era buena y coherente con el brief.
  - Elegí sombras de bajo contraste (negras con opacity alta) porque sobre fondos oscuros las sombras
    coloridas se ven artificiales.
  - Borde izquierdo de 3px para activos (cards, tenant-selector-btn) en lugar de cambiar solo el
    border-color completo: es un patrón reconocible de dashboards y no requiere JS adicional.
  - max-height en tenant-list: con 20 arrendatarios (tope del SPEC) la lista sin scroll empujaría el
    formulario fuera del viewport en pantallas normales.
  - No consolidé .btn-mark-paid y .btn-mark-one-time-paid en una sola clase: render.js usa ambas y
    render.js no es archivo del ui-designer.

- **Pendiente / siguiente paso:**
  - En v2, cuando Inter sea local, mover el @import a @font-face en src/assets/fonts/.
  - Cuando render.js genere los items de lista, añadir role="listitem" a cada .card y a cada
    .tenant-selector-btn para completar el patrón list/listitem.
  - El tester debería revisar el resultado visual final.

- **Nota de traspaso:** docs/handoff/2026-10-03-ui-designer.md

---

## 2026-10-03 · Iteración v2 · Paleta warm/light + espaciado de paneles

- **Qué hice:**
  Leí el CSS completo, index.html, render.js (solo lectura), DESIGN.md y el handoff anterior.
  Apliqué tres cambios pedidos por el usuario:

  **1. Paleta warm/light** — Migré todos los tokens de color en `:root` de dark navy a tonos
  cálidos y claros. Fondo `#faf9f7` (crema suave), superficies `#ffffff`, hover `#f2f0ed`.
  Acento: terracota `#a85632` (contraste ~5.1:1 sobre blanco, WCAG AA). Texto `#1c1917`
  (near-black warm, ~17:1 AAA). Éxito y peligro ajustados a versiones más oscuras que funcionan
  sobre fondos claros: `#2e7d52` y `#c0392b`. Sombras reducidas a opacidades bajas (0.08/0.10)
  apropiadas para fondos blancos. Todos los rgba hardcoded en badges, month-tags y botones
  mark-paid actualizados a los nuevos valores de success/danger. Filtro del date-picker cambiado
  de `invert(0.6)` (para fondos oscuros) a `opacity: 0.5` (el icono nativo es negro y ya es
  visible en fondo claro).

  **2. Vista Admin — detalle de arrendatario** — Aumenté el padding del `.detail-panel` de 24px
  a 40px. Aumenté `margin-bottom` y `padding-bottom` del `.detail-header` a 40px/24px.
  Aumenté `margin-top` de `.section-label` de 24px a 40px y `margin-bottom` de 8px a 16px.
  Aumenté `gap` en `.months-list` de 8px a 16px. Aumenté padding en `.month-tag` de 4px/8px
  a 8px/16px. Aumenté padding en `.charge-row` de 8px a 16px. Aumenté padding y margin-top
  del `.total-row` a 24px. En móvil: detail-panel vuelve a 24px de padding.

  **3. Vista Arrendatario — estado de cuenta** — Los mismos cambios en `.account-header`
  (margin-bottom 40px, padding-bottom 24px), `.section-label`, `.months-list`, `.charge-row`
  y `.total-row` se aplican también a la vista arrendatario porque comparte las mismas clases.

  Todos los nombres de variables CSS se mantuvieron intactos; solo cambiaron sus valores.

- **Archivos tocados:**
  - `src/css/styles.css`
  - `docs/DESIGN.md`
  - `docs/logs/ui-designer.md`
  - `docs/handoff/2026-10-03-ui-designer-v2.md`

- **Decisiones y motivo:**
  - Acento terracota (`#a85632`) en lugar de azul o ámbar: encaja con paleta crema, es sobrio
    y profesional, contrasta bien sin verse "de plástico" en fondos blancos.
  - Sombras con opacidad baja (0.08/0.10): sobre fondos claros las sombras negras fuertes se
    ven como manchas; opacidades bajas dan la ilusión de elevación sin ensuciar.
  - Decidí NO agregar un token `--space-2xl` (habría interrumpido el orden xl > 2xl): en cambio
    uso `--space-xl` (40px) directamente en el panel, que es el valor correcto para un respiro
    generoso.
  - La limitación de la fila de 2-3 columnas para datos del arrendatario (`.detail-unit`) no es
    implementable solo con CSS porque render.js genera un único `<p>` con todo el contenido
    concatenado. Queda documentado para que dev1 lo refactorice si se desea.

- **Pendiente / siguiente paso:**
  - Si dev1 refactoriza render.js para emitir `.detail-meta` con elementos separados, el
    ui-designer puede añadir un layout de columnas CSS sobre esos elementos.
  - Tester: verificar resultado visual con la nueva paleta clara (especialmente contraste de
    badges sobre fondos blancos y acento terracota en estado activo).

- **Nota de traspaso:** docs/handoff/2026-10-03-ui-designer-v2.md
