# Traspaso: Paleta warm/light + espaciado de paneles · ui-designer · 2026-10-03

## Estado
- [x] Terminada  - [ ] A medias  - [ ] Bloqueada

## Qué quedó hecho

### 1. Paleta warm/light en `src/css/styles.css`

Todos los tokens de color en `:root` reemplazados. Nombres de variables sin cambios
(compatibilidad total con render.js y el HTML existente):

| Token | Antes (dark navy) | Ahora (warm/light) |
|---|---|---|
| `--color-bg` | `#0f1117` | `#faf9f7` |
| `--color-surface` | `#1a1d27` | `#ffffff` |
| `--color-surface-alt` | `#22263a` | `#f2f0ed` |
| `--color-surface-raised` | `#252940` | `#e8e4de` |
| `--color-accent` | `#4f8ef7` | `#a85632` (terracota) |
| `--color-accent-hover` | `#6ba3ff` | `#bf6540` |
| `--color-accent-dim` | `rgba(79,142,247,.12)` | `rgba(168,86,50,.10)` |
| `--color-text` | `#e8eaf0` | `#1c1917` |
| `--color-text-muted` | `#8891b0` | `#57534e` |
| `--color-text-subtle` | `#5e6484` | `#a8a29e` |
| `--color-success` | `#4caf82` | `#2e7d52` |
| `--color-danger` | `#e05c6b` | `#c0392b` |
| `--color-border` | `#2a2f45` | `#e2ddd8` |
| `--color-border-hover` | `#3d4565` | `#c9c2ba` |
| `--shadow-sm` | `rgba(0,0,0,.4)` | `rgba(0,0,0,.08)` |
| `--shadow-md` | `rgba(0,0,0,.4)` | `rgba(0,0,0,.10)` |
| `--shadow-glow` | `rgba(79,142,247,.2)` | `rgba(168,86,50,.18)` |

También actualizados todos los rgba hardcoded dentro de `.badge--ok`, `.badge--late`,
`.month-tag--danger`, `.btn-mark-paid` y `.btn-mark-one-time-paid` para reflejar los nuevos
valores de `--color-success` y `--color-danger`.

El filtro del date-picker cambió de `filter: invert(0.6)` (para fondos oscuros) a
`opacity: 0.5` (el icono nativo ya es negro y visible en fondos claros).

### 2. Panel de detalle Admin y Vista Arrendatario — espaciado ampliado

| Selector | Propiedad | Antes | Ahora |
|---|---|---|---|
| `.detail-panel` | `padding` | 24px | 40px (móvil: 24px) |
| `.detail-header` | `margin-bottom` | 24px | 40px |
| `.detail-header` | `padding-bottom` | 16px | 24px |
| `.account-header` | `margin-bottom` | 24px | 40px |
| `.account-header` | `padding-bottom` | 16px | 24px |
| `.section-label` | `margin-top` | 24px | 40px |
| `.section-label` | `margin-bottom` | 8px | 16px |
| `.months-list` | `gap` | 8px | 16px |
| `.month-tag` | `padding` | 4px 8px | 8px 16px |
| `.charge-row` | `padding` | 8px 0 | 16px 0 |
| `.total-row` | `padding` | 16px | 24px |
| `.total-row` | `margin-top` | 16px | 24px |

### 3. `docs/DESIGN.md` actualizado

- Sección §1: tono actualizado de "dark navy" a "warm/light".
- Sección §2: tabla de paleta y verificación de contraste actualizadas.
- Sección §4: tabla de sombras actualizada.
- Sección §5: documentados nuevos paddings en `.detail-panel`, `.detail-header`,
  `.account-header`, `.section-label`, `.total-row` y `.month-tag`.
- Sección §7: números de contraste actualizados.
- Sección §8: entrada v2 añadida al historial.

## Qué falta / limitaciones conocidas

- **Fila de 2-3 columnas para datos del arrendatario** (RF solicitado): render.js genera
  `.detail-unit` como un único `<p>` con unidad, contacto y día de pago concatenados.
  No es posible aplicar layout de columnas CSS a ese elemento sin que dev1 lo refactorice
  para emitir elementos separados (p. ej. `.detail-meta-item`). Queda como mejora futura.

- **Tester pendiente**: verificar resultado visual con la nueva paleta clara, especialmente:
  1. Contraste de badges (terracota, verde, rojo) sobre fondo blanco.
  2. Estado activo en tarjetas y tenant-selector-btn: borde izquierdo terracota visible.
  3. Panel de detalle: respiro entre cabecera, secciones y total.
  4. Focus visible: outline terracota en inputs/botones.
  5. Móvil (375px): detail-panel con padding reducido, columna única sin desbordamiento.

## Archivos tocados

- `src/css/styles.css`
- `docs/DESIGN.md`
- `docs/logs/ui-designer.md`
- `docs/handoff/2026-10-03-ui-designer-v2.md`

## Decisiones tomadas

- **Acento terracota** en lugar de azul o ámbar: combina con la paleta crema, es sobrio y
  profesional, y supera holgadamente WCAG AA (~5.1:1 sobre blanco).
- **Sombras de baja opacidad** (0.08/0.10): sobre fondos claros las sombras negras fuertes
  quedan como manchas; valores bajos dan ilusión de elevación sin ensuciar la página.
- **Nombres de variables CSS conservados**: garantiza cero impacto en render.js, app.js y
  cualquier selector ya escrito por dev1/dev2.
- **Sin nuevo token de espaciado**: se usa `--space-xl` (40px) directamente donde se necesita
  amplio respiro, evitando confusión en la escala (un token "2xl" menor que "xl" sería confuso).

## Cómo verificar

Abrir `src/index.html` en navegador (file://):
1. Fondo de página: crema suave, paneles blancos, sin dark navy.
2. Logo "Rentals.": el punto debe verse en terracota.
3. Toggle activo: botón con fondo terracota, texto blanco.
4. Tarjeta activa: borde izquierdo terracota de 3px.
5. Panel de detalle con arrendatario seleccionado: cabecera con separador generoso,
   secciones bien separadas, total-row con borde izquierdo terracota.
6. Vista Arrendatario: mismo respiro en cuenta personal.
7. Reducir ventana a 375px: columna única, panel de detalle con padding 24px.
