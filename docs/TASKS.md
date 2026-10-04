# Tablero de tareas

Estados: `TODO` · `EN CURSO` · `EN REVISIÓN` · `LISTO` · `BLOQUEADO`

Regla: cada archivo tiene un único responsable a la vez. La columna "Archivos propios" lo hace explícito
para que `dev1` y `dev2` no se pisen.

| ID | Descripción | Responsable | Archivos propios | Depende de | Estado | Nota de traspaso |
|---|---|---|---|---|---|---|
| T-01 | Capa de datos: `data.js` (mock 3 arrendatarios), `store.js` (acceso), `calc.js` (lógica deuda) | dev2 | `src/js/data.js`, `src/js/store.js`, `src/js/calc.js` | — | EN REVISIÓN | docs/handoff/T01-dev2.md |
| T-02 | Base visual: fuentes locales, `index.html` (estructura + contenedores), `styles.css` (paleta, layout, componentes) | dev1 | `src/index.html`, `src/css/styles.css`, `src/assets/fonts/` | — | LISTO | docs/handoff/T02-dev1.md |
| T-03 | Render: `render.js` — pinta lista de arrendatarios, detalle de pagos, vista arrendatario y formulario de registro | dev1 | `src/js/render.js` | T-01, T-02 | EN REVISIÓN | docs/handoff/T03-dev1.md |
| T-04 | Orquestador: `app.js` — arranque, toggle Admin/Arrendatario, eventos (marcar pagado, registrar arrendatario) | dev2 | `src/js/app.js` | T-01, T-03 | EN REVISIÓN | docs/handoff/T04-dev2.md |
| T-05 | Validación completa contra SPEC.md RF-01 a RF-07 | tester | — | T-03, T-04 | LISTO | docs/handoff/2026-10-03-dev1-T05.md |
| T-06 | Descargar fuente Inter localmente y reemplazar @import CDN por @font-face | dev1 | `src/assets/fonts/`, `src/css/styles.css` | T-02 | LISTO | docs/handoff/2026-10-03-dev1-T06.md |
| T-07 | Verificación visual del nuevo CSS entregado por ui-designer (checklist de 6 puntos) | tester | — | T-06 | LISTO | docs/handoff/2026-10-03-dev1-T07fix.md |

## Tareas v2 — Stack dinámico (Vue 3 + Spring Boot + PostgreSQL)

> Fronteras: **dev1 = `frontend/`**, **dev2 = `backend/` + `compose.yaml` + `.env.example`**. Nadie toca `src/` (v1 congelada).

| ID | Descripción | Responsable | Archivos propios | Depende de | Estado | Nota de traspaso |
|---|---|---|---|---|---|---|
| B-01 | Scaffold `backend/`: Gradle wrapper ≥9.1, Java 25 toolchain, Spring Boot, starters (web, data-jpa, validation, actuator), `compose.yaml`, `.env.example` (incluye `RENTALS_JWT_SECRET`, `RENTALS_BOOTSTRAP_ADMIN_*`, `RENTALS_REFRESH_COOKIE_SECURE`), `application.yml` sin secretos, `ClockConfig` (zona `America/Guayaquil`) | dev2 | `backend/`, `compose.yaml`, `.env.example` | — | EN REVISIÓN | docs/handoff/2026-10-03-dev2-B01.md |
| B-02 | Esquema PostgreSQL: `V1__init_schema.sql` (Flyway) + entidades JPA (Tenant, PaidMonth, OneTimeCharge) + `YearMonthConverter` + repositorios + `@DataJpaTest` con Testcontainers | dev2 | `backend/src/` | B-01 | EN REVISIÓN | docs/handoff/2026-10-03-dev2-B02.md |
| B-03 | `DebtCalculatorService` + `DebtSummary` + pruebas de paridad con los casos de `src/js/data.js` | dev2 | `backend/src/` | B-02 | EN REVISIÓN | docs/handoff/2026-10-03-dev2-B03.md |
| B-05 | Semilla dev: `V1000__dev_seed.sql` con los 3 arrendatarios del mock (solo perfil `dev`) | dev2 | `backend/src/` | B-02 | EN REVISIÓN | docs/handoff/2026-10-03-dev2-B03.md |
| B-06 | Seguridad backend: starters security + oauth2-resource-server; `V2__auth_schema.sql`; entidades AppUser/Role/RefreshToken + repos; SecurityConfig, JwtConfig, JwtAccessTokenIssuer, RefreshTokenService, AppUserDetailsService, AdminBootstrapRunner, AuthService, UserAccountService, AuthController + DTOs; bloqueo temporal por intentos fallidos; `V1001__dev_seed_users.sql`; pruebas de auth | dev2 | `backend/src/` | B-02, B-05 | EN CURSO | — |
| B-04 | Services + controllers REST + DTOs + `GlobalExceptionHandler` (ProblemDetail); `MeController`; `CreateTenantRequest` con username+initialPassword; `TenantService.create` crea usuario en misma transacción (409 si duplicado); `@WebMvcTest` con matriz de roles | dev2 | `backend/src/` | B-03, B-06 | TODO | — |
| F-01 | Scaffold `frontend/`: Vite + Vue 3 + Pinia + Vitest, proxy `/api`, portar tokens CSS y fuentes desde `src/` (copiar, no mover) | dev1 | `frontend/` | — | EN REVISIÓN | docs/handoff/2026-10-03-dev1-F01.md |
| F-02 | `api/http.js` (Bearer, reintento tras refresh, onAuthFailure), `api/authApi.js`, `api/tenantsApi.js`, `stores/auth.js`, `stores/tenants.js` + tests Vitest de http.js y stores/auth.js | dev1 | `frontend/src/` | F-01 | EN REVISIÓN | docs/handoff/2026-10-03-dev1-F02.md |
| F-03 | Vista Admin: lista de tarjetas, panel de detalle, marcar mes/cargo pagado, formulario de registro (con campos username + initialPassword, manejo de 409) | dev1 | `frontend/src/` | F-02 | EN REVISIÓN | docs/handoff/2026-10-03-dev1-F03.md |
| F-04 | Vista Arrendatario: muestra cuenta propia con `GET /api/me/tenant` (sin selector ni toggle) | dev1 | `frontend/src/` | F-02 | EN REVISIÓN | docs/handoff/2026-10-03-dev1-F04F05.md |
| U-01 | DESIGN.md: pantalla de login, AppHeader (usuario, "Cambiar contraseña", "Salir"), ChangePasswordForm, campos de credenciales en TenantForm; quitar toggle del diseño | ui-designer | `docs/DESIGN.md` | — | EN REVISIÓN | docs/handoff/2026-10-03-ui-designer-U01.md |
| F-05 | LoginView, App.vue con restoreSession() y vista por rol, AppHeader, ChangePasswordForm (aviso y vuelta al login) | dev1 | `frontend/src/` | F-02, U-01 | EN REVISIÓN | docs/handoff/2026-10-03-dev1-F04F05.md |
| T-v2 | Integración: RF-01 a RF-05 + checklist de autenticación (login, logout, cambio de contraseña, reset por admin, control de roles) sobre v2 completo | tester | — | B-04, B-06, F-03, F-04, F-05 | TODO | — |
