# Registro de decisiones (ADR)

Una entrada por decisión relevante. Las escribe el `architect`. No se borran: si una decisión cambia,
se añade una nueva que reemplaza a la anterior.

## Plantilla

### ADR-000: Título corto
- **Fecha:** AAAA-MM-DD
- **Estado:** Propuesta | Aceptada | Reemplazada por ADR-XXX
- **Contexto:** Qué situación obliga a decidir.
- **Alternativas consideradas:** A, B, C (con pros y contras).
- **Decisión:** Qué se eligió.
- **Consecuencias:** Qué se gana, qué se pierde y qué queda pendiente.

---

### ADR-001: Almacenamiento de datos en mock JS detrás de una capa `store.js`
- **Fecha:** 2026-10-03
- **Estado:** Reemplazada por ADR-006 (en v2). Vigente solo para el código v1 congelado en `src/`.
- **Contexto:** La v1 es estática, sin backend, con datos hardcodeados (RF-07, restricciones SPEC).
  Debe abrir sin servidor (`file://`) y, a la vez, estar preparada para evolucionar a v2 con backend
  y datos persistentes (roadmap SPEC §9). Hay que evitar que la UI quede acoplada a la fuente de datos.
- **Alternativas consideradas:**
  - **A. Datos hardcodeados accedidos directamente desde la UI.** Pro: lo más rápido de escribir.
    Contra: acopla UI al formato del mock; en v2 habría que reescribir la UI. Rechazada por no escalar.
  - **B. LocalStorage como fuente de verdad desde v1.** Pro: persistencia entre recargas.
    Contra: el SPEC dice que LocalStorage NO es requerido en v1; añade complejidad de migración/sync
    y estados sucios en pruebas (YAGNI). Rechazada para v1 (queda como opción contenida en `store.js`).
  - **C. Mock en `data.js` detrás de una capa de acceso `store.js` (abstracción).** Pro: la UI depende
    de una interfaz estable, no del mock; en v2 solo cambia `store.js`. Contra: una capa extra de
    indirección. Elegida: el costo es mínimo y protege la inversión en UI.
- **Decisión:** Datos de prueba en `src/js/data.js`; **todo** acceso (consulta y mutación) pasa por
  `src/js/store.js`. Nadie más lee `data.js` directamente. Los meses adeudados y totales **no se
  almacenan**: se calculan en `calc.js` a partir de `startDate`, `monthlyAmount`, `paidMonths` y
  `oneTimeCharges`. Los meses pagados se guardan como claves `"YYYY-MM"` en `tenant.paidMonths`.
- **Consecuencias:**
  - Se gana: UI desacoplada de la persistencia; migración a v2 aislada a `store.js`; sin datos
    derivados desincronizados; funciona bajo `file://`.
  - Se pierde: una capa de indirección; sin persistencia entre recargas en v1 (aceptado por SPEC).
  - Pendiente: definir si la API de `store.js` nace síncrona (v1) o ya con promesas pensando en v2
    (recomendado async-ready si no complica); esto lo cierra dev2 al implementar.

---

### ADR-002: Estilos con CSS propio (sin framework ni CDN)
- **Fecha:** 2026-10-03
- **Estado:** Aceptada
- **Contexto:** El SPEC pide un diseño sobrio y profesional, paleta oscura/neutra, tipografía moderna
  embebida localmente, responsive y contraste WCAG AA, **sin dependencias de red en producción**
  (RNF SPEC §6). La escala es pequeña (una página, pocos componentes).
- **Alternativas consideradas:**
  - **A. Tailwind vía CDN.** Pro: utilidades rápidas. Contra: viola "sin CDN externo"; traerlo local
    exige un build (PostCSS/purge), lo que contradice "sin build / abre con doble clic". Rechazada.
  - **B. Pico.css (clases mínimas, local).** Pro: estilos base decentes sin mucho esfuerzo, archivo
    único embebible. Contra: imponer su sistema de diseño dificulta lograr el look "dashboard de
    finanzas" específico del brief; terminaríamos sobrescribiéndolo. Rechazada por fricción de
    personalización frente a un alcance pequeño.
  - **C. CSS propio en un solo `styles.css`, con variables CSS para la paleta.** Pro: control total
    del look sobrio pedido; cero dependencias; cero build; fácil de auditar para contraste AA.
    Contra: hay que escribir el layout a mano (flex/grid). Elegida: el proyecto es pequeño y el diseño
    es un requisito explícito, no un extra.
- **Decisión:** **CSS propio** en `src/css/styles.css`, usando **custom properties** (`--color-*`,
  `--space-*`, `--radius-*`) para la paleta y el espaciado. Tipografía (Inter o DM Sans) embebida como
  `.woff2` en `src/assets/fonts/` y cargada con `@font-face`. Layout con Flexbox/Grid, responsive con
  media queries. Sin preprocesadores ni build.
- **Consecuencias:**
  - Se gana: cumplimiento estricto de "sin red"; diseño a medida del brief; cambio de tema trivial
    (editar variables); sin toolchain.
  - Se pierde: más CSS escrito a mano que con un framework de utilidades.
  - Pendiente: dev1 fija la paleta concreta (acento único: azul pizarra, verde salvia o ámbar) y
    verifica contraste AA; documentar los tokens al inicio de `styles.css`.
  - Nota v2 (2026-10-03): sigue **vigente** en v2. Los tokens se portan a `frontend/src/assets/styles/`;
    no se adopta librería de componentes (Vuetify, PrimeVue) ni Tailwind sin un nuevo ADR. La regla
    "sin CDN" se mantiene (fuentes locales); el "sin build" deja de aplicar porque Vite ya es un build.

---

### ADR-003: Frontend v2 con Vue 3 (Composition API) + Vite + Pinia, sin router
- **Fecha:** 2026-10-03
- **Estado:** Aceptada
- **Contexto:** El usuario aprobó pasar a v2 dinámica y eligió explícitamente **Vue 3** (SPEC §11).
  El SPEC §9 mencionaba una futura migración a React (v2.5). Hay que fijar framework y piezas
  complementarias (bundler, estado, HTTP, routing, lenguaje).
- **Alternativas consideradas:**
  - **A. React + Vite.** Pro: ecosistema más grande; era la idea del roadmap original. Contra: no es
    la elección del usuario; más decisiones sueltas (estado, formularios) para un proyecto pequeño.
  - **B. Svelte/SvelteKit.** Pro: muy poco código y bundles pequeños. Contra: no elegido por el usuario;
    SvelteKit empuja a SSR/servidor Node que no necesitamos.
  - **C. Vue 3 + Vite.** Pro: elegido por el usuario; plantillas HTML muy cercanas al markup y CSS de v1
    (migración directa de `index.html`/`render.js` a componentes); Pinia y Vite son las piezas oficiales.
    Contra: ecosistema algo menor que React (irrelevante a esta escala).
- **Decisión:** Vue 3 con `<script setup>` (Composition API), Vite, **Pinia** para estado compartido,
  **`fetch` nativo** envuelto en `api/http.js` (sin axios), **sin `vue-router`** en v2 (el toggle
  Admin/Arrendatario es estado en Pinia), **JavaScript** (no TypeScript), CSS propio (ADR-002).
- **Consecuencias:**
  - Se gana: decisión alineada con el usuario; mínimo de dependencias; reutilización directa del CSS
    y de la estructura visual aprobada en `DESIGN.md`.
  - Se pierde: tipado estático del contrato (mitigado con el contrato explícito de ARCHITECTURE §6);
    deep-linking a vistas (no requerido).
  - Pendiente: `vue-router` llega en v3 con el login. La línea "v2.5 React" del SPEC §9 queda sin
    efecto; el líder debe corregir el roadmap.
  - Nota 2026-10-03 (ADR-008): el login se adelanta a v2 y **sigue sin `vue-router`**: `App.vue` elige
    Login / AdminView / TenantView según el estado de sesión y el rol. El toggle y `stores/ui.js` desaparecen.

---

### ADR-004: Backend v2 con Spring Boot + Java 25 (LTS) + Gradle; override del default Java 8
- **Fecha:** 2026-10-03
- **Estado:** Aceptada
- **Contexto:** v2 requiere persistencia y API REST. `CLAUDE.md` y el rol del architect fijan **Java 8**
  como default "si el proyecto incluye Java". El usuario eligió **explícitamente** Spring Boot LTS +
  **Java 25** + Gradle (SPEC §11, aprobado 2026-10-03).
- **Alternativas consideradas:**
  - **A. Java 8 + Spring Boot 2.7.** Pro: respeta el default de `CLAUDE.md`. Contra: Spring Boot 2.x
    está fuera de soporte OSS; Java 8 no tiene records, `java.time` mejorado ni soporte en Boot 3/4;
    contradice la elección explícita del usuario. Rechazada.
  - **B. Java 21 LTS + Spring Boot 3.x.** Pro: combinación muy probada. Contra: no es lo que pidió el
    usuario; Java 25 es la LTS vigente.
  - **C. Java 25 LTS + Spring Boot (última GA compatible, línea 4.0.x) + Gradle wrapper ≥ 9.1.**
    Pro: elección del usuario; LTS con soporte largo; records y `java.time` simplifican DTOs y fechas.
    Contra: stack reciente, posibles incompatibilidades puntuales de plugins.
  - **D. Otro backend (Node, serverless Lambda).** Contra: no elegido por el usuario.
- **Decisión:** **C.** Java 25 vía *toolchain* de Gradle (Groovy DSL), Spring Boot última GA que soporte
  Java 25 (4.0.x; fallback 3.5.x si el scaffold lo exige), starters `web`, `data-jpa`, `validation`,
  `actuator`; Flyway para el esquema; Testcontainers para pruebas de integración con PostgreSQL.
  **La elección explícita del usuario prevalece sobre el default Java 8 de `CLAUDE.md`**, solo para este
  proyecto; el default sigue aplicando a proyectos donde el usuario no indique versión.
- **Consecuencias:**
  - Se gana: stack soportado a largo plazo; código más simple (records, inyección por constructor).
  - Se pierde: coherencia literal con `CLAUDE.md` (queda justificado aquí); hace falta JDK 25 local y
    Amazon Corretto 25 en AWS.
  - Pendiente: el líder puede anotar en `CLAUDE.md` que este proyecto usa Java 25 (ver ADR-004) para
    que ningún agente "corrija" a Java 8. Esa edición corresponde al líder/usuario, no al architect.

---

### ADR-005: Monorepo con `frontend/` y `backend/` en la raíz; `src/` v1 congelada
- **Fecha:** 2026-10-03
- **Estado:** Aceptada
- **Contexto:** v2 añade dos aplicaciones con toolchains distintos (npm/Vite y Gradle). Existe código v1
  funcional en `src/` que no debe romperse durante la migración. La memoria del equipo (`docs/`) es única.
- **Alternativas consideradas:**
  - **A. Dos repositorios separados.** Pro: ciclos de vida independientes. Contra: duplica `docs/`,
    `CLAUDE.md` y la memoria compartida; el contrato API y la arquitectura quedarían fuera de sitio;
    exagerado para un equipo y un producto.
  - **B. Monorepo con Gradle sirviendo el frontend (frontend dentro de `backend/src/main/frontend`).**
    Pro: un único artefacto. Contra: mezcla fronteras de dev1 y dev2; acopla builds.
  - **C. Monorepo con carpetas hermanas `frontend/` y `backend/`, cada una con su build autónomo.**
    Pro: fronteras de propiedad obvias (dev1 = `frontend/`, dev2 = `backend/`); `docs/` compartido;
    v1 en `src/` intacta. Contra: dos toolchains en el mismo repo (aceptable).
- **Decisión:** **C.** `frontend/` (Vite) y `backend/` (Gradle) independientes; `compose.yaml` y
  `.env.example` en la raíz para PostgreSQL local. `src/` queda **congelada** como v1 hasta que el usuario
  apruebe archivarla (`legacy/v1-static/`) tras verificar v2 en AWS (ARCHITECTURE §10).
- **Consecuencias:**
  - Se gana: trabajo en paralelo sin conflictos; v1 desplegable en todo momento; migración reversible.
  - Se pierde: durante la transición conviven dos UIs; hay que evitar que alguien edite `src/` por error.
  - Pendiente: fase 4 (archivo de `src/`) requiere decisión explícita del usuario.

---

### ADR-006: La regla de deuda vive solo en el backend (`DebtCalculatorService`)
- **Fecha:** 2026-10-03
- **Estado:** Aceptada (reemplaza a ADR-001 para v2)
- **Contexto:** SPEC §11 deja a criterio del architect reutilizar `calc.js` o portarlo al backend.
- **Alternativas consideradas:**
  - **A. Mantener `calc.js` en el frontend; el backend solo guarda datos.** Pro: cero port. Contra: la
    regla no estaría disponible para v4 (notificaciones automáticas, que corren en el servidor) y habría
    que duplicarla después.
  - **B. Implementarla en ambos lados.** Contra: dos implementaciones que divergen.
  - **C. Portarla a Java como servicio puro y que la API devuelva `monthsOwed`, `totalOwed` y `status`.**
    Pro: una sola fuente de verdad, testeable sin Spring, lista para v4. Contra: requiere pruebas de
    paridad con v1.
- **Decisión:** **C.** `DebtCalculatorService.calculate(Tenant, YearMonth currentMonth) -> DebtSummary`,
  con paridad exacta con `calc.js`; el mes actual se obtiene de un bean `Clock` con zona configurable.
  La BD guarda solo hechos (tenant, `paid_month`, `one_time_charge`); los valores derivados nunca se
  persisten. El frontend no calcula deuda.
- **Consecuencias:**
  - Se gana: regla única y reutilizable; frontend más simple.
  - Se pierde: el frontend depende del backend incluso para mostrar la deuda (aceptable).
  - Pendiente: zona horaria del negocio (D-2 en ARCHITECTURE §13).

---

### ADR-007: Topología de despliegue v2 en AWS (CloudFront + S3 + EC2 + RDS)
- **Fecha:** 2026-10-03
- **Estado:** Aceptada (confirmada por el usuario 2026-10-03)
- **Contexto:** v2 necesita alojar un backend Java y PostgreSQL. El usuario despliega manualmente y ya
  conoce S3 + CloudFront (`DEPLOY-AWS-MANUAL.md`). Carga mínima (5-20 arrendatarios).
- **Alternativas consideradas:**
  - **A. API Gateway + Lambda.** Contra: Spring Boot en Lambda implica arranques en frío y adaptadores;
    complejidad innecesaria.
  - **B. Elastic Beanstalk.** Pro: gestiona la instancia. Contra: depende de que la plataforma ofrezca
    Corretto 25 a tiempo; menos transparente.
  - **C. ECS Fargate (contenedor).** Pro: sin servidor que administrar. Contra: más piezas (ECR, task
    definitions, ALB) para un usuario que despliega a mano.
  - **D. Frontend en S3 + CloudFront (como v1) y backend jar en una EC2 pequeña con Corretto 25, como
    segundo origen de la misma distribución (`/api/*`); RDS PostgreSQL privado.** Pro: reutiliza lo que
    el usuario ya sabe; mismo origen → sin CORS; barato. Contra: hay que mantener el SO de la EC2.
- **Decisión propuesta:** **D**, con C como alternativa. Secretos en SSM Parameter Store; RDS solo
  accesible desde el SG de la EC2; EC2 solo accesible desde el *prefix list* de CloudFront.
- **Consecuencias:** se debe ampliar `docs/DEPLOY-AWS-MANUAL.md` con la sección v2 una vez confirmado.
  D-1 y D-2 confirmadas por el usuario 2026-10-03: HTTP Basic para escrituras; zona horaria `America/Guayaquil`.
- **Nota 2026-10-03:** la elección de HTTP Basic (D-1) queda **reemplazada por ADR-008** (JWT con roles).
  Con autenticación, el tramo CloudFront → EC2 transporta credenciales: se recomienda CloudFront VPC
  Origin (o ALB + HTTPS) en lugar de origen HTTP público (decisión abierta D-4 en ARCHITECTURE §13).

---

### ADR-008: Autenticación con JWT y roles `ADMIN` / `ARRENDATARIO` en v2
- **Fecha:** 2026-10-03
- **Estado:** Aceptada (decisión del usuario 2026-10-03; reemplaza D-1 "HTTP Basic" de ADR-007)
- **Contexto:** El usuario adelanta el login real a v2 (antes reservado para v3). Requisitos: JWT,
  roles `ADMIN` y `ARRENDATARIO`, el admin define usuario/contraseña del arrendatario al registrarlo, el
  arrendatario puede cambiarla, admin inicial por semilla en `dev` y documentado para producción. Escala
  de 5-20 usuarios; despliegue en una sola EC2 detrás de CloudFront con el mismo origen que el frontend.
- **Alternativas consideradas:**
  - **A. Sesión de servidor (cookie `JSESSIONID` / Spring Session).** Pro: revocación inmediata, simple
    para una sola instancia. Contra: el usuario pidió JWT explícitamente; requiere CSRF en toda la API.
  - **B. Proveedor externo (Amazon Cognito / Keycloak).** Pro: gestión de usuarios, MFA, recuperación.
    Contra: piezas y coste extra para 20 usuarios; el despliegue manual se complica. YAGNI.
  - **C. JWT con librería de terceros (jjwt) y filtro propio.** Pro: control total. Contra: código de
    seguridad propio (filtro, validación) que hay que mantener y probar.
  - **D. JWT con Spring Security OAuth2 Resource Server (Nimbus) para validar y `JwtEncoder` para emitir,
    HS256 con secreto en variable de entorno.** Pro: validación estándar mantenida por Spring; mínimo
    código propio; sin dependencias extra. Contra: clave simétrica (aceptable: emisor y validador son la
    misma aplicación).
- **Decisión:** **D.** Access token JWT HS256 de **15 min**, stateless, con claims `sub`, `username`,
  `role`, `tenantId` (solo ARRENDATARIO), `iss`. Autorización **solo por ruta** en `SecurityConfig`
  (`/api/tenants/**` → ADMIN, `/api/me/**` → ARRENDATARIO, `PUT /api/auth/password` → ambos, el resto
  `denyAll`). El arrendatario lee su cuenta en `GET /api/me/tenant` (el tenant sale del token; sin IDOR).
  Tabla `app_user` (username único en minúsculas, hash BCrypt, rol, FK opcional y única a `tenant` con
  `ON DELETE RESTRICT`; CHECK rol↔tenant). `POST /api/tenants` crea tenant + usuario en una transacción
  (`409` si el username existe). El admin inicial lo crea `AdminBootstrapRunner` desde
  `RENTALS_BOOTSTRAP_ADMIN_USERNAME/PASSWORD` si no hay ningún ADMIN (mismo mecanismo en dev y prod; sin
  contraseñas de admin en el repo). Los arrendatarios de la semilla dev tienen una contraseña de
  desarrollo pública (datos ficticios, solo perfil `dev`). Frontend sin router: `App.vue` elige la vista
  por rol; desaparecen el toggle y el selector de arrendatario. Detalle en ARCHITECTURE §15.
- **Consecuencias:**
  - Se gana: autenticación real antes de exponer v2 en Internet; cada arrendatario ve solo lo suyo;
    poco código de seguridad propio.
  - Se pierde: un access token no se puede revocar antes de expirar (ventana ≤ 15 min tras logout,
    cambio de contraseña o deshabilitar usuario); RF-05/RF-06 del SPEC cambian (selector y toggle).
  - Pendiente: SPEC (RF-05, RF-06, nuevos RF de auth) lo actualiza el líder; D-4 (tramo CloudFront→EC2),
    D-5 (reset de contraseña por el admin) y D-6 (bloqueo por intentos) los decide el usuario; DESIGN.md
    necesita la pantalla de login y el formulario de cambio de contraseña (ui-designer).

---

### ADR-009: Refresh token opaco, rotado, guardado como hash y enviado en cookie HttpOnly
- **Fecha:** 2026-10-03
- **Estado:** Aceptada
- **Contexto:** ADR-008 usa access tokens cortos; hace falta renovar sin pedir la contraseña cada 15 min,
  permitir logout real (revocación) y decidir dónde guarda los tokens el navegador. El frontend y la API
  comparten origen (proxy de Vite en dev, CloudFront `/api/*` en prod).
- **Alternativas consideradas:**
  - **A. Refresh como JWT stateless + lista negra de `jti` revocados.** Contra: igualmente necesita BD
    para logout; la lista negra crece y hay que purgarla; no detecta reutilización.
  - **B. Refresh opaco en BD (lista blanca) devuelto en el JSON y guardado en `localStorage`.** Pro:
    simple. Contra: cualquier XSS roba un token de 7 días.
  - **C. Refresh opaco en BD (lista blanca, solo su SHA-256), rotado en cada uso, enviado en cookie
    `HttpOnly; Secure; SameSite=Strict; Path=/api/auth`; access token solo en memoria (Pinia).**
    Pro: JS no puede leer el refresh; logout y "cerrar todas las sesiones" triviales; detección de
    reutilización (token revocado presentado → revocar todo). Contra: requiere mismo origen (ya lo
    tenemos) y una llamada de refresh al recargar la página.
- **Decisión:** **C.** Tabla `refresh_token(user_id, token_hash, expires_at, revoked_at)`; 7 días; rotación
  en `POST /api/auth/refresh`; `POST /api/auth/logout` revoca y borra cookie; `PUT /api/auth/password`
  revoca todas las sesiones del usuario. El refresh **no** aparece en el cuerpo de ninguna respuesta. El
  frontend recupera la sesión al cargar llamando a refresh y, ante un `401`, refresca una vez (promesa
  compartida) y reintenta; si falla, vuelve al login. CSRF de la API desactivado (Bearer); los endpoints con
  cookie quedan cubiertos por `SameSite=Strict` y `Path=/api/auth`.
- **Consecuencias:**
  - Se gana: menor superficie ante XSS; revocación real de sesiones; sin estado de sesión por petición.
  - Se pierde: una pequeña tabla y una consulta por refresh (irrelevante a esta escala); en dev hay que
    poner `RENTALS_REFRESH_COOKIE_SECURE=false` si el navegador no acepta `Secure` en `http://localhost`.
  - Pendiente: limpieza de tokens expirados se hace al hacer login (por usuario); si creciera, job programado.
