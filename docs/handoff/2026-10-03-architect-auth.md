# Traspaso: Autenticación JWT en v2 · de architect · 2026-10-03

## Estado
- [x] Terminada  - [ ] A medias  - [ ] Bloqueada
(Diseño completo. Quedan 3 decisiones abiertas del usuario (D-4, D-5, D-6) que no bloquean el inicio.)

## Qué quedó hecho
- `docs/ARCHITECTURE.md`: nueva §15 "Seguridad y autenticación" + actualizaciones en §1-§14
  (estructura de carpetas, librerías, capas, config/secretos, modelo `app_user` + `refresh_token`
  en `V2__auth_schema.sql`, API con columna de rol y endpoints `/api/auth/*` y `/api/me/tenant`,
  frontend sin toggle, semilla, despliegue, riesgos, decisiones). Zona horaria corregida a `America/Guayaquil`.
- `docs/DECISIONS.md`: ADR-008 (JWT + roles, Spring Resource Server/Nimbus) y ADR-009 (refresh opaco
  rotado, hash en BD, cookie HttpOnly; access token solo en memoria). Notas en ADR-003 (sigue sin router)
  y ADR-007 (HTTP Basic reemplazado; tramo CloudFront→EC2 debe ir cifrado o por VPC Origin).

## Decisiones tomadas y por qué
- **Refresh token en cookie HttpOnly, no en el JSON**: se aparta de la letra del pedido ("login devuelve
  accessToken y refreshToken") para que un XSS no pueda robar un token de 7 días. El login devuelve en el
  cuerpo `accessToken`, `expiresIn` y `user {id, username, role, tenantId}`.
- **Admin inicial por `AdminBootstrapRunner` (variables de entorno), no por SQL**: el mismo mecanismo sirve
  en dev y producción y no queda ningún hash/contraseña de admin en el repo. En dev las variables vienen
  del `.env` local. Los 3 arrendatarios de la semilla dev sí tienen usuarios por SQL con la contraseña
  pública de desarrollo `dev-password-123` (datos ficticios, solo perfil `dev`).
- **Arrendatario usa `GET /api/me/tenant`** (tenant tomado del token), no `/api/tenants/{id}`: elimina el
  riesgo de leer cuentas ajenas cambiando el id. Todo `/api/tenants/**` queda solo para ADMIN.
- **Borrado de tenant**: no existe en v2. FK `ON DELETE RESTRICT`; la futura "baja" será lógica
  (`app_user.enabled=false` + revocar sesiones).
- **Sin vue-router**: `App.vue` elige Login / Admin / Arrendatario por estado de sesión y rol.

## Qué falta (propuesta concreta de ajuste a `docs/TASKS.md`, la aplica el líder)

Tareas que **cambian**:

| ID | Cambio |
|---|---|
| B-01 | Sin cambios de alcance. `.env.example` añade también (puede hacerlo B-06): `RENTALS_JWT_SECRET`, `RENTALS_BOOTSTRAP_ADMIN_USERNAME`, `RENTALS_BOOTSTRAP_ADMIN_PASSWORD`, `RENTALS_REFRESH_COOKIE_SECURE`, con valores `change-me` |
| B-04 | **Quitar "HTTP Basic para escrituras".** Añadir: `MeController` (`GET /api/me/tenant`); `CreateTenantRequest` con `username` + `initialPassword`; `TenantService.create` crea el usuario vía `UserAccountService` en la misma transacción (`409` si username duplicado); `TenantDetailDto.username`; `@WebMvcTest` con `spring-security-test` cubriendo la matriz de roles de ARCHITECTURE §15.4. **Depende de B-03 y B-06** |
| B-05 | Sin cambios (solo `V1000__dev_seed.sql` de tenants) |
| F-02 | Añadir `api/authApi.js`, `stores/auth.js`, `configureAuth` en `http.js` (Bearer, reintento único tras refresh, promesa compartida, `onAuthFailure`), `getMyTenant` en `tenantsApi.js`, `tenants.reset()`/`loadMyAccount()`. **Eliminar `stores/ui.js`.** Tests Vitest de `http.js` y `stores/auth.js` |
| F-03 | `TenantForm` con campos `username` + `initialPassword`; mostrar `username` en `TenantDetail`; manejar `409` |
| F-04 | **Sin selector ni toggle.** `TenantView` muestra la cuenta propia con `GET /api/me/tenant`. Quitar "toggle en `App.vue`" (pasa a F-05) |
| T-v2 | Cubre RF-01 a RF-05 + checklist de autenticación de ARCHITECTURE §15.9. Depende además de B-06 y F-05 |

Tareas **nuevas**:

| ID | Resp. | Descripción | Archivos propios | Depende |
|---|---|---|---|---|
| U-01 | ui-designer | `DESIGN.md`: pantalla de login, `AppHeader` (usuario, "Cambiar contraseña", "Salir"), `ChangePasswordForm`, campos de credenciales en `TenantForm`, estados de error/carga; quitar el toggle | `docs/DESIGN.md` | — |
| B-06 | dev2 | Seguridad backend: starters `security` + `oauth2-resource-server` (+ `spring-security-test`); `V2__auth_schema.sql`; `AppUser`, `Role`, `RefreshToken` + repos; `security/` completo (SecurityConfig, JwtConfig, JwtAccessTokenIssuer, RefreshTokenService, AppUserDetailsService, AdminBootstrapRunner, ProblemDetailAuthHandlers, AuthProperties con fail fast); `AuthService`, `UserAccountService`; `AuthController` + `RefreshCookieFactory` + DTOs (A1-A4); `V1001__dev_seed_users.sql`; `.env.example`; pruebas de §15.8 | `backend/`, `.env.example` | B-02 (y B-05 para `V1001`) |
| F-05 | dev1 | `LoginView`, `App.vue` con `restoreSession()` y vista por rol, `AppHeader`, `ChangePasswordForm` (+ aviso y vuelta al login) | `frontend/src/` | F-02, U-01 |

Orden sugerido backend (todo dev2, secuencial): B-01 → B-02 → (B-03, B-05) → **B-06** → B-04.
Frontend (dev1): F-01 → F-02 → (F-03, F-04, F-05). dev1 trabaja contra mocks hasta que B-06/B-04 estén.
Revisión de entrega grande: R-v2 (architect + ui-designer) tras T-v2.

Otras acciones del líder:
- **SPEC:** RF-05 (sin selector; el arrendatario ve su cuenta tras iniciar sesión), RF-06 (vista por rol,
  sin toggle), nuevos RF de auth (login, logout, cambio de contraseña, alta de credenciales por el admin),
  §4 "No incluye" (quitar login), §9 roadmap (v3 ya no es login; quitar v2.5 React). Registrar en historial.
- **Usuario:** decidir D-4 (VPC Origin recomendado vs ALB+HTTPS), D-5 (reset de contraseña de arrendatario
  por el admin; recomendado sí) y D-6 (bloqueo por intentos o WAF rate-based).
- **DEPLOY-AWS-MANUAL.md v2:** generar `RENTALS_JWT_SECRET` (`openssl rand -base64 48`), alta del admin
  inicial con variables de bootstrap y retirarlas tras el primer arranque.

## Dónde continuar (el siguiente agente empieza aquí)
- Líder: aplicar el ajuste de TASKS de arriba y actualizar SPEC; luego U-01 y B-06 pueden empezar.
- dev2 (B-06): leer ARCHITECTURE §4.1, §4.2, §4.4, §5.2, §6.2, §15 y ADR-008/009.
- dev1 (F-02/F-05): leer ARCHITECTURE §6, §7, §15.7 y ADR-009.

## Archivos tocados
- `docs/ARCHITECTURE.md`
- `docs/DECISIONS.md`
- `docs/logs/architect.md`
- `docs/handoff/2026-10-03-architect-auth.md`

## Problemas conocidos / riesgos
- Access token no revocable antes de 15 min (aceptado en ADR-008).
- Sin reset de contraseña por el admin hasta que se decida D-5: un arrendatario que la olvide queda sin acceso.
- Nombre exacto del starter de resource server puede variar en Spring Boot 4 (ver §4.1).
- Cookie `Secure` en `http://localhost`: por eso `refresh-cookie-secure=false` en el perfil dev.
- No exponer v2 en Internet hasta que B-06 esté verificado y D-4 resuelta.

## Cómo verificar
- Leer `docs/ARCHITECTURE.md` §5.2, §6, §15 y `docs/DECISIONS.md` ADR-008/ADR-009.
