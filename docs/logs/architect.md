# Bitácora de architect

Entradas más recientes al final. Formato en `docs/templates/log-entry.md`.

---

## 2026-10-03 · Arquitectura v1 · Diseño inicial web estática de arriendos
- **Qué hice:** Leí `docs/SPEC.md` (APROBADO) y escribí la arquitectura v1 completa:
  estructura de archivos, modelo de datos JS, flujo de navegación Admin/Arrendatario,
  fronteras dev1/dev2 y riesgos. Registré 2 ADRs (almacenamiento y estrategia CSS).
- **Archivos tocados:** `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/logs/architect.md`.
- **Decisiones y motivo:**
  - Tres capas con dependencias hacia abstracciones (DIP): `data.js` → `store.js` →
    `calc.js`/`render.js` → `app.js`. Aísla la fuente de datos para que v2 (backend) solo
    cambie `store.js`.
  - Meses adeudados y totales **calculados, nunca almacenados** (fuente de verdad única).
    Mes pagado = clave `"YYYY-MM"` en `tenant.paidMonths`.
  - Scripts clásicos con globals namespaced (no ES modules) para funcionar bajo `file://`.
  - CSS propio con custom properties (ADR-002): cumple "sin CDN", sin build, look a medida.
  - Fronteras: dev1 = HTML/CSS/render/fonts; dev2 = data/store/calc/app. Contrato =
    firmas públicas de `store.js`, `calc.js`, `render.js`.
- **Pendiente / siguiente paso:** El líder recalcula `docs/TASKS.md` con estas fronteras.
  Abierto: (1) si `store.js` nace async-ready (lo cierra dev2); (2) paleta concreta y
  contraste AA (lo cierra dev1).
- **Nota de traspaso:** no requerida (tareas abiertas documentadas en ARCHITECTURE §7/§10 y ADRs).

---

## 2026-10-03 · v2 · Arquitectura dinámica (Vue 3 + Spring Boot/Java 25 + PostgreSQL)
- **Qué hice:** Reescribí `docs/ARCHITECTURE.md` para v2 (v1 queda como Anexo A): monorepo
  `frontend/` + `backend/`, capas Spring (web/service/repository/domain), esquema PostgreSQL
  (`tenant`, `paid_month`, `one_time_charge`) con Flyway, API REST de 7 endpoints con DTOs,
  port de `calc.js` a `DebtCalculatorService`, frontend Vue 3 + Pinia + fetch, estrategia de
  migración en 5 fases con `src/` congelada, topología AWS propuesta y riesgos. Registré
  ADR-003 a ADR-007 y marqué ADR-001 como reemplazada por ADR-006 para v2.
- **Archivos tocados:** `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/logs/architect.md`,
  `docs/handoff/2026-10-03-architect-v2.md`.
- **Decisiones y motivo:**
  - Monorepo con carpetas hermanas (ADR-005): fronteras dev1/dev2 obvias y `docs/` compartido.
  - Java 25 prevalece sobre el default Java 8 de CLAUDE.md por elección explícita del usuario (ADR-004).
  - Deuda calculada solo en backend, puro y con `Clock` inyectado (ADR-006): una sola regla, lista para v4.
  - `paidMonths` como `@ElementCollection` con PK compuesta: idempotencia gratis, sin entidad extra.
  - Sin router, sin axios, sin TypeScript, sin Lombok/MapStruct (YAGNI).
  - Mismo origen vía proxy de Vite (dev) y CloudFront `/api/*` (prod): sin CORS.
- **Pendiente / siguiente paso:** Usuario decide D-1 (protección API antes de v3), D-2 (zona horaria),
  D-3 (topología AWS / ADR-007). Líder recalcula TASKS.md y ajusta secciones v1-only del SPEC.
- **Nota de traspaso:** docs/handoff/2026-10-03-architect-v2.md

---

## 2026-10-03 · v2 · Autenticación JWT con roles ADMIN / ARRENDATARIO
- **Qué hice:** Incorporé autenticación real a la arquitectura v2 (adelantada desde v3 por el usuario).
  Añadí ARCHITECTURE §15 y actualicé §1-§14: tablas `app_user` y `refresh_token` (`V2__auth_schema.sql`),
  endpoints `/api/auth/login|refresh|logout|password` y `/api/me/tenant`, matriz de roles, alta de
  credenciales en `POST /api/tenants`, admin inicial por bootstrap, flujo de sesión en Vue (sin toggle ni
  router). Registré ADR-008 y ADR-009; notas en ADR-003 y ADR-007. Corregí zona horaria a America/Guayaquil.
- **Archivos tocados:** `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/logs/architect.md`,
  `docs/handoff/2026-10-03-architect-auth.md`.
- **Decisiones y motivo:**
  - Spring Resource Server + `JwtEncoder` (Nimbus), HS256: validación estándar, sin libs de terceros.
  - Refresh opaco, rotado, hash SHA-256 en BD y en cookie HttpOnly SameSite=Strict; access solo en memoria:
    resiste XSS y permite logout real (ADR-009).
  - `GET /api/me/tenant` para el arrendatario: evita IDOR; autorización solo por ruta en `SecurityConfig`.
  - Admin inicial por `AdminBootstrapRunner` con variables de entorno: sin contraseñas de admin en el repo.
  - Tenant sin borrado en v2; FK `ON DELETE RESTRICT`; baja futura será lógica.
- **Pendiente / siguiente paso:** Líder ajusta TASKS (B-04, F-02..F-04, T-v2 cambian; nuevas U-01, B-06,
  F-05) y SPEC (RF-05, RF-06, RF de auth). Usuario decide D-4, D-5, D-6.
- **Nota de traspaso:** docs/handoff/2026-10-03-architect-auth.md
