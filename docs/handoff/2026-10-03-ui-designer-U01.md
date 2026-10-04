# Traspaso: U-01 · de ui-designer · 2026-10-03

## Estado
- [x] Terminada  - [ ] A medias  - [ ] Bloqueada

## Qué quedó hecho

`docs/DESIGN.md` actualizado con las siguientes secciones nuevas o modificadas:

**Sección 5 — Componentes nuevos v2:**
- Toggle Admin/Arrendatario marcado como ELIMINADO EN v2 (la vista la da el rol del JWT).
- `.error-banner`: componente reutilizable de error con paleta danger, `role="alert"`,
  `aria-live="assertive"`.
- `.password-field`: wrapper para inputs de contraseña con botón mostrar/ocultar posicionado
  absolutamente; `aria-label` dinámico en el botón.
- `.app-header`: sticky 56px (48px móvil), flex space-between; logo izquierda, username + 2 botones
  derecha; nuevo `.btn-header` de tono y tamaño propios.
- `.login-view` / `.login-card`: centrado min-height 100vh, card max-width 400px; estados del
  botón "Ingresar" (normal, loading, error); sin enlace de recuperación; responsive idéntico en
  375px y escritorio.
- `.modal-overlay` / `.modal` (ChangePasswordForm): overlay, max-width 440px, focus trap, Escape
  cierra, `role="dialog"` `aria-modal`; 3 campos con PasswordField; 5 estados documentados
  incluyendo redirección al login tras éxito.
- TenantForm (credenciales): campo "Nombre de usuario" con hint de patrón, campo "Contraseña
  inicial" con PasswordField y aviso de cambio, ErrorBanner 409.

**Sección 6 — Pantallas:**
- LoginView añadida como pantalla nueva (sin header, sin toggle).
- AdminView y TenantView actualizadas para mostrar AppHeader y modal ChangePasswordForm.
- TenantView simplificada: sin selector (el arrendatario ve solo su cuenta).
- Estado de carga inicial documentado.

**Sección 7 — Accesibilidad v2:** autocomplete, aria-label en campos de contraseña, focus trap
del modal, role="status" en el loader.

**Historial de cambios §8:** entrada añadida.

## Qué falta

- No quedan pendientes de diseño para U-01.
- Decisiones abiertas del arquitecto (D-4, D-5, D-6) no afectan al diseño de la interfaz.

## Dónde continuar (el siguiente agente empieza aquí)

**Dev1 (F-05):** implementar LoginView, AppHeader y ChangePasswordForm.
- Leer `docs/DESIGN.md` §5 (sección "NUEVOS COMPONENTES v2") y §6 (LoginView y vistas actualizadas).
- Los tokens CSS de paleta, espaciado y sombras ya están definidos en §2 y §4; reutilizarlos tal
  cual en `frontend/src/assets/styles/tokens.css`.
- `AppHeader` y `ChangePasswordForm` van en `frontend/src/components/common/`.
- `LoginView` va en `frontend/src/views/`.
- El flujo de App.vue ya está descrito en ARCHITECTURE §15.7.

**Dev1 (F-03):** añadir campos "Nombre de usuario" e "Contraseña inicial" al TenantForm
(`frontend/src/components/admin/TenantForm.vue`).
- Ver §5 "TenantForm — campos de credenciales" para el comportamiento exacto del hint y el error 409.

## Archivos tocados

- `docs/DESIGN.md`
- `docs/logs/ui-designer.md`
- `docs/handoff/2026-10-03-ui-designer-U01.md`

## Decisiones tomadas y por qué

- **Modal para ChangePasswordForm:** flujo corto y puntual que no justifica ruta propia; el modal
  permite cambiar la contraseña sin perder el contexto de trabajo.
- **`.btn-header` nuevo:** los botones del header son más pequeños (13px, 6/12px) que los botones
  de formulario para no competir con el contenido de la vista.
- **ErrorBanner como componente reutilizable:** login, modal de contraseña y TenantForm comparten
  el mismo patrón visual y de accesibilidad.
- **Validación de confirmación de contraseña solo en frontend:** el DTO `ChangePasswordRequest` solo
  tiene `currentPassword` y `newPassword`; la confirmación es responsabilidad del cliente.
- **Hint de username como validación inline:** ocupa menos espacio que un mensaje debajo y es más
  inmediato. El hint cambia de `--color-text-subtle` a `--color-danger` cuando el patrón no se cumple.
- **TenantView sin sidebar:** `GET /api/me/tenant` devuelve solo la cuenta propia; no hay selector.

## Problemas conocidos / riesgos

- Los iconos de ojo para el PasswordField no están especificados como SVG concretos; dev1 puede usar
  cualquier icono SVG inline o una librería ligera, siempre con `aria-hidden="true"` en el SVG.
- La trampa de foco del modal requiere lógica JS en Vue; si dev1 usa una librería de componentes
  accesibles (Headless UI, Radix Vue) debe consultarlo con el líder antes (puede cambiar las clases
  CSS a aplicar).

## Cómo verificar

1. Abrir la app en 375px, 768px y 1280px de ancho.
2. En LoginView: verificar centrado, que el ErrorBanner aparece solo con credenciales incorrectas,
   que el botón se deshabilita durante el fetch.
3. En AppHeader: verificar que el nombre de usuario se trunca en móvil, que ambos botones son
   accesibles con Tab.
4. En ChangePasswordForm: abrir el modal, verificar que el foco queda dentro, que Escape cierra,
   que el éxito muestra el mensaje y redirige en 2 segundos.
5. En TenantForm: verificar que el hint de username cambia de color con patrón incorrecto, que el
   error 409 aparece con role="alert".
6. Contraste: verificar con las DevTools de Chrome (Accessibility > Contrast) que los textos de
   banners de error y campos de hint cumplen AA.
