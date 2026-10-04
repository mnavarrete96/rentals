# Bitácora de dev2

Entradas más recientes al final. Formato en `docs/templates/log-entry.md`.

---

## 2026-10-03 12:10 · T-01 · Capa de datos (data.js, store.js, calc.js)
- **Qué hice:** Creé los tres archivos de la capa de datos en `src/js/`. `data.js` expone `window.RentalsData` con 3 arrendatarios que cubren los casos: al día, atrasado 2 meses, y al día en meses pero con cargo único pendiente. `store.js` expone `window.RentalsStore` con todas las funciones de acceso/mutación en memoria. `calc.js` expone `window.RentalsCalc` con las funciones puras `monthsOwed`, `totalOwed` y `status`.
- **Archivos tocados:** `src/js/data.js`, `src/js/store.js`, `src/js/calc.js`, `docs/TASKS.md`
- **Decisiones y motivo:**
  - Los datos mock de t-001 y t-002 usan startDate 2026-06 o 2026-07 (pocos meses atrás) para que los ejemplos sean concisos y fáciles de verificar con la fecha actual (2026-10-03).
  - t-003 tiene todos los meses recurrentes pagados pero un cargo único pendiente, lo que ejercita la rama de `status` que revisa `oneTimeCharges`.
  - En `calc.js`, el parseo de `startDate` se hace con `.split("-")` + `parseInt` en lugar de `new Date("YYYY-MM-DD")` para evitar la ambigüedad UTC que desplaza el día en zonas horarias negativas.
  - `store.js` usa IIFE con `var` (ES5) para ser compatible bajo `file://` sin transpilador; el objeto retornado ya mapea directamente a promesas en v2 sin cambiar la firma.
  - Copia profunda del mock con `JSON.parse(JSON.stringify(...))` para garantizar aislamiento entre tests sucesivos sin recargar la página.
- **Pendiente / siguiente paso:** dev1 puede consumir `RentalsStore` y `RentalsCalc` desde `render.js`. El tester debe ejercitar `calc.js` con fechas fijas (especialmente los bordes de inicio de mes y startDate = mes actual).
- **Nota de traspaso:** docs/handoff/T01-dev2.md

---

## 2026-10-03 14:00 · T-04 · Orquestador (app.js)
- **Qué hice:** Creé `src/js/app.js` con el objeto `window.RentalsApp` que arranca la app e implementa todos los event listeners requeridos: toggle Admin/Arrendatario (RF-06), selección de arrendatario en vista Admin, marcar mes pagado (RF-04), marcar cargo único pagado, selección en vista Tenant y formulario de registro (RF-01).
- **Archivos tocados:** `src/js/app.js`, `docs/TASKS.md`, `docs/logs/dev2.md`, `docs/handoff/T04-dev2.md`
- **Decisiones y motivo:**
  - IIFE + `var` (ES5) para coherencia con el resto del proyecto y compatibilidad `file://`.
  - `_today()` centraliza `new Date()` en un punto único para facilitar mocking en tests futuros.
  - `_refreshAdminView()` y `_refreshTenantView()` evitan repetir la secuencia de re-render en múltiples handlers (DRY).
  - Delegación de eventos en los contenedores `#admin-tenant-list`, `#admin-tenant-detail` y `#tenant-selector` en lugar de listeners en cada elemento individual, lo que soporta HTML generado dinámicamente por render.js.
  - `e.target.closest(...)` para que el click funcione aunque el usuario haga clic en un hijo del elemento con `data-*` (ej. el texto dentro del botón).
  - Auto-invocación de `init()` al final del IIFE: los scripts están al final de `<body>` así que el DOM está listo.
  - Validación básica en el submit (guard `if (!fullName || ...)`) además de los atributos `required` del HTML, para evitar objetos Tenant con datos vacíos.
- **Pendiente / siguiente paso:** T-05 (tester) puede validar la app completa en el navegador abriendo `src/index.html`.
- **Nota de traspaso:** docs/handoff/T04-dev2.md

---

## 2026-10-03 · B-01 · Scaffold backend Spring Boot
- **Qué hice:** Creé el scaffold completo del backend en `backend/`. Configuré Gradle wrapper 9.8.0, Spring Boot 4.1.0, Java 25 toolchain. Creé los starters (webmvc, data-jpa, validation, actuator, flyway, postgresql). Implementé `ClockConfig` (bean `Clock` con zona configurable) y `CorsConfig` (WebMvcConfigurer que solo activa CORS si `rentals.cors.allowed-origins` tiene valor). Creé `application.yml` (sin secretos, solo referencias ${ENV_VAR}) y `application-dev.yml`. Creé los directorios `db/migration/` y `db/seed/` con `.gitkeep`. Creé `compose.yaml` en la raíz para PostgreSQL local. Actualicé `.gitignore` con entradas Java/Gradle. Verifiqué que `./gradlew build -x test` termina en verde.
- **Archivos tocados:** `backend/` (todo), `compose.yaml`, `.gitignore`
- **Decisiones y motivo:**
  - Spring Boot 4.1.0: es la versión GA confirmada en Spring Initializr (4.0.x ya existe). Alineado con ARCHITECTURE §4.1.
  - `spring-boot-starter-webmvc` en lugar de `spring-boot-starter-web`: Spring Boot 4.x renombró el starter; verificado con Spring Initializr.
  - `spring-boot-starter-flyway` en lugar de `flyway-core`: Spring Boot 4.x introdujo un starter propio para Flyway; `flyway-database-postgresql` sigue siendo dependencia separada.
  - Gradle 9.8.0: versión GA más reciente (verificado en services.gradle.org/versions/current).
  - `.env.example` NO pudo crearse: la regla de permisos del entorno bloquea escritura de archivos que coincidan con `.env.*`. El usuario debe crearlo manualmente (contenido en la nota de traspaso).
- **Pendiente / siguiente paso:** B-02 (esquema Flyway + entidades JPA + repositorios + DataJpaTest). El usuario debe crear `.env.example` manualmente.
- **Nota de traspaso:** docs/handoff/2026-10-03-dev2-B01.md

---

## 2026-10-03 · B-02 · Esquema Flyway + entidades JPA + repositorios + Testcontainers
- **Qué hice:**
  - Creé `V1__init_schema.sql` siguiendo ARCHITECTURE §5.1 exactamente (3 tablas + índice). No se añadió columna `status` porque el estado es calculado en runtime (ADR-006).
  - Creé dominio: `TenantStatus.java` (enum), `YearMonthConverter.java` (@Converter autoApply), `Tenant.java` (con `@ElementCollection Set<YearMonth>` para paidMonths, per ARCHITECTURE §5.3), `OneTimeCharge.java` (@ManyToOne Tenant).
  - Creé repositorios: `TenantRepository` y `OneTimeChargeRepository` (JpaRepository). No hay `PaidMonthRepository` porque paidMonths es @ElementCollection (sin entidad propia).
  - Añadí Testcontainers al `build.gradle`. Descubrí que en Spring Boot 4.x los artefactos renombraron: `spring-boot-starter-data-jpa-test` (nuevo módulo), `testcontainers-junit-jupiter` y `testcontainers-postgresql` (Testcontainers 2.x), e imports actualizados a `org.springframework.boot.data.jpa.test.autoconfigure` y `org.springframework.boot.jdbc.test.autoconfigure`.
  - Creé `TenantRepositoryTest.java` con 5 tests (guardar/recuperar tenant, deduplicación del Set<YearMonth>, múltiples meses pagados, cargo único con cascada, marcar cargo pagado).
  - Creé `src/test/resources/application.properties` para deshabilitar Flyway en slice tests y usar `ddl-auto=create-drop`.
  - Resultado: `./gradlew build` → BUILD SUCCESSFUL. `TenantRepositoryTest` → 5/5 tests en verde con PostgreSQL 16 en Docker.
- **Archivos tocados:**
  - `backend/src/main/resources/db/migration/V1__init_schema.sql` (creado)
  - `backend/src/main/java/com/rentals/domain/TenantStatus.java` (creado)
  - `backend/src/main/java/com/rentals/domain/YearMonthConverter.java` (creado)
  - `backend/src/main/java/com/rentals/domain/Tenant.java` (creado)
  - `backend/src/main/java/com/rentals/domain/OneTimeCharge.java` (creado)
  - `backend/src/main/java/com/rentals/repository/TenantRepository.java` (creado)
  - `backend/src/main/java/com/rentals/repository/OneTimeChargeRepository.java` (creado)
  - `backend/build.gradle` (añadidas dependencias Testcontainers + spring-boot-starter-data-jpa-test)
  - `backend/src/test/java/com/rentals/repository/TenantRepositoryTest.java` (creado)
  - `backend/src/test/resources/application.properties` (creado)
  - `docs/TASKS.md` (B-02 → EN REVISIÓN)
- **Decisiones y motivo:**
  - **paidMonths como @ElementCollection (no entidad PaidMonth):** La ARCHITECTURE §5.3 especifica `@ElementCollection Set<YearMonth>` para `paid_month`. El Set garantiza idempotencia en Java; la PK compuesta en la BD refuerza unicidad. La tarea del líder describía PaidMonth como entidad pero esto contradice ADR-006 (estado/historial calculado) y §5.3. Seguí la ARCHITECTURE.
  - **Sin columna status en tenant:** ADR-006 prohíbe persistir estado calculado. El status se computa en `DebtCalculatorService` (tarea B-03).
  - **SQL sigue ARCHITECTURE §5.1:** `BIGINT GENERATED ALWAYS AS IDENTITY`, `ON DELETE CASCADE`, `year_month CHECK`, `VARCHAR(120/40)`, `contact NOT NULL`, `monthly_amount >= 0`. La tarea describía un esquema diferente (BIGSERIAL, ON DELETE RESTRICT, etc.) que contradeciría la arquitectura.
  - **Spring Boot 4.x:** Los starters y packages de test cambiaron vs 3.x. Documentado en la nota de traspaso para que los siguientes tasks (B-03, B-04, B-06) usen los imports correctos.
- **Pendiente / siguiente paso:** B-03 (DebtCalculatorService + DebtSummary + pruebas de paridad). B-05 (semilla dev). B-06 (seguridad + V2__auth_schema.sql).
- **Nota de traspaso:** docs/handoff/2026-10-03-dev2-B02.md

---

## 2026-10-03 · B-06 · Seguridad backend: JWT, auth endpoints, seed usuarios
- **Qué hice:**
  - Añadí starters `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` y `spring-security-test` a `build.gradle`.
  - Creé `V2__auth_schema.sql` (tablas `app_user` y `refresh_token` siguiendo ARCHITECTURE §5.2 exactamente: constraints de rol↔tenant, lowercase username, ON DELETE RESTRICT/CASCADE).
  - Actualicé `application.yml` con el bloque `rentals.auth.*` (jwt-secret, issuer, ttl, cookie-secure, bootstrap-admin). Actualicé `application-dev.yml` con `refresh-cookie-secure=false`.
  - Nuevas entidades en `domain/`: `Role` (enum), `AppUser` (entidad, relación OneToOne opcional con Tenant), `RefreshToken` (entidad con isExpired/isRevoked/isValid).
  - Nuevos repositorios: `AppUserRepository` (findByUsername, findByTenantId, existsByRole), `RefreshTokenRepository` (findByTokenHash, revokeAllByUser, deleteExpiredByUser).
  - Capa `security/`: `AuthProperties` (ConfigurationProperties record), `JwtConfig` (beans JwtEncoder/JwtDecoder HS256 + JwtAuthenticationConverter con claim "role"), `JwtAccessTokenIssuer` (implementa AccessTokenIssuer), `AppUserDetailsService` (UserDetailsService), `ProblemDetailAuthHandlers` (401/403 como JSON problem), `SecurityConfig` (stateless, reglas por ruta/rol, DaoAuthenticationProvider, AuthenticationManager), `RefreshTokenService` (genera token opaco, SHA-256 hex en BD, rotación, revocación, reuse detection), `AdminBootstrapRunner` (crea ADMIN inicial si no existe).
  - Interface `service/AccessTokenIssuer` y record `service/AuthResult` para que services no dependan de Nimbus.
  - `service/UsernameTakenException`, `service/InvalidRefreshTokenException`.
  - `service/UserAccountService` (crea usuarios ARRENDATARIO/ADMIN, reset de contraseña).
  - `service/AuthService` (login vía AuthenticationManager, refresh con rotación, logout, changePassword).
  - `web/RefreshCookieFactory` (crea/borra cookie HttpOnly SameSite=Strict), `web/AuthController` (4 endpoints A1-A4), `web/GlobalExceptionHandler` (ProblemDetail para BadCredentials, InvalidRefreshToken, UsernameTaken, MethodArgumentNotValid).
  - `db/seed/V1001__dev_seed_users.sql`: 4 usuarios (1 ADMIN `admin`/`admin-dev-2024`, 3 ARRENDATARIO `ana.garcia`, `luis.lopez`, `pedro.vera` / `dev-password-123`). Hashes BCrypt strength 10 generados con el test `HashGenTest`.
  - Tests: `AuthServiceTest` (10 tests, Mockito puro), `AuthControllerTest` (5 tests, @WebMvcTest Spring Boot 4.x + spring-security-test).
  - `./gradlew build` → BUILD SUCCESSFUL, 31 tests en verde.
- **Archivos tocados:**
  - `backend/build.gradle` (starters security + oauth2-resource-server + spring-security-test)
  - `backend/src/main/resources/application.yml` (bloque rentals.auth)
  - `backend/src/main/resources/application-dev.yml` (refresh-cookie-secure=false)
  - `backend/src/main/resources/db/migration/V2__auth_schema.sql` (creado)
  - `backend/src/main/resources/db/seed/V1001__dev_seed_users.sql` (creado)
  - `backend/src/main/java/com/rentals/domain/Role.java` (creado)
  - `backend/src/main/java/com/rentals/domain/AppUser.java` (creado)
  - `backend/src/main/java/com/rentals/domain/RefreshToken.java` (creado)
  - `backend/src/main/java/com/rentals/repository/AppUserRepository.java` (creado)
  - `backend/src/main/java/com/rentals/repository/RefreshTokenRepository.java` (creado)
  - `backend/src/main/java/com/rentals/security/` (7 archivos creados)
  - `backend/src/main/java/com/rentals/service/AccessTokenIssuer.java` (creado)
  - `backend/src/main/java/com/rentals/service/AuthResult.java` (creado)
  - `backend/src/main/java/com/rentals/service/AuthService.java` (creado)
  - `backend/src/main/java/com/rentals/service/UserAccountService.java` (creado)
  - `backend/src/main/java/com/rentals/service/UsernameTakenException.java` (creado)
  - `backend/src/main/java/com/rentals/service/InvalidRefreshTokenException.java` (creado)
  - `backend/src/main/java/com/rentals/web/AuthController.java` (creado)
  - `backend/src/main/java/com/rentals/web/RefreshCookieFactory.java` (creado)
  - `backend/src/main/java/com/rentals/web/GlobalExceptionHandler.java` (creado)
  - `backend/src/main/java/com/rentals/web/dto/LoginRequest.java` (creado)
  - `backend/src/main/java/com/rentals/web/dto/AuthResponse.java` (creado)
  - `backend/src/main/java/com/rentals/web/dto/ChangePasswordRequest.java` (creado)
  - `backend/src/test/java/com/rentals/auth/AuthServiceTest.java` (creado)
  - `backend/src/test/java/com/rentals/auth/AuthControllerTest.java` (creado)
  - `backend/src/test/resources/application.properties` (añadido RENTALS_JWT_SECRET para tests)
  - `docs/TASKS.md` (B-06 → EN REVISIÓN)
  - `docs/logs/dev2.md` (esta entrada)
  - `docs/handoff/2026-10-03-dev2-B06.md` (creado)
- **Decisiones y motivo:**
  - **Sin bloqueo por intentos (failedAttempts):** La tarea describía un contador en AppUser, pero ARCHITECTURE §13 lo clasifica como "D-6: abierta, YAGNI si se activa WAF rate-based". AppUser NO tiene `failedAttempts`/`lockedUntil`. AuthService delega en AuthenticationManager que ya maneja DisabledException. Si el usuario decide activar D-6, será un ADR y una migración V3. Se reporta al líder.
  - **GlobalExceptionHandler en B-06:** La tarea original lo asignaba a B-04, pero es necesario para que `BadCredentialsException` del AuthController retorne 401 ProblemDetail (sin él, Spring lanza ServletException). Se creó un handler básico en B-06; B-04 puede extenderlo.
  - **Spring Boot 4.x (Jackson 3.x):** Jackson 3.x usa `tools.jackson.databind` (no `com.fasterxml.jackson.databind`). Los tests no deben importar ObjectMapper de com.fasterxml.
  - **Spring Boot 4.x (@MockitoBean vs @MockBean):** En Spring Framework 7.x, @MockBean fue reemplazado por `@MockitoBean` (`org.springframework.test.context.bean.override.mockito.MockitoBean`). Los tests usan la nueva anotación.
  - **Spring Boot 4.x (@WebMvcTest):** El import correcto es `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`.
  - **DaoAuthenticationProvider constructor:** En Spring Security 7.x, el constructor acepta `UserDetailsService` (no PasswordEncoder). Se usa `new DaoAuthenticationProvider(userDetailsService)` + `setPasswordEncoder(...)`.
- **Pendiente / siguiente paso:** B-04 (TenantService, ChargeService, TenantController, ChargeController, PaidMonthController, MeController, TenantMapper, CreateTenantRequest con username+initialPassword, tests @WebMvcTest con matriz de roles).
- **Nota de traspaso:** docs/handoff/2026-10-03-dev2-B06.md

---

## 2026-10-04 · B-06 (fix) · Bloqueo temporal por intentos fallidos de login (brute-force lockout)
- **Qué hice:**
  - Verifiqué que `V2__auth_schema.sql` NO tenía las columnas `failed_attempts` / `locked_until` en `app_user` (omitidas en la entrega anterior de B-06 por falta de ADR aprobado; ahora el líder lo aprueba explícitamente como D-6).
  - Creé `V3__auth_add_lockout.sql`: `ALTER TABLE app_user ADD COLUMN failed_attempts SMALLINT NOT NULL DEFAULT 0, ADD COLUMN locked_until TIMESTAMPTZ`.
  - Añadí a `AppUser.java`: campos `failedAttempts` (int, default 0) y `lockedUntil` (OffsetDateTime, nullable); métodos `isLocked(Clock)`, `incrementFailedAttempts(Clock)` (5 fallos → bloqueo 15 min, reset contador), `resetFailedAttempts()`.
  - Refactoricé `AuthService.login()`: carga el usuario ANTES de llamar al `authenticationManager` (para verificar el bloqueo). Si el usuario no existe o está bloqueado → lanza `BadCredentialsException` genérica (sin enumerar usuarios). Si el `authenticationManager` falla → incrementa intentos + `save()` → lanza `BadCredentialsException`. Si tiene éxito → `resetFailedAttempts()` + `save()` + emite tokens.
  - Actualicé `AuthServiceTest.java`: corregí los tests `login_withWrongPassword_throwsBadCredentials` y `login_withDisabledUser_throwsBadCredentials` (ahora también stubban `findByUsername` porque la búsqueda es previa al authManager). Añadí 3 tests nuevos de bloqueo: cuenta bloqueada, 5 fallos consecutivos bloquean, login exitoso tras fallos resetea el contador.
  - `./gradlew build` → BUILD SUCCESSFUL, **34 tests en verde** (antes 31).
- **Archivos tocados:**
  - `backend/src/main/resources/db/migration/V3__auth_add_lockout.sql` (creado)
  - `backend/src/main/java/com/rentals/domain/AppUser.java` (campos + métodos de bloqueo)
  - `backend/src/main/java/com/rentals/service/AuthService.java` (lógica de bloqueo en login)
  - `backend/src/test/java/com/rentals/auth/AuthServiceTest.java` (3 tests nuevos + 2 tests ajustados)
  - `docs/TASKS.md` (B-06 → EN REVISIÓN)
  - `docs/logs/dev2.md` (esta entrada)
- **Decisiones y motivo:**
  - **BadCredentialsException en vez de LockedException:** `LockedException` no está mapeada en `GlobalExceptionHandler` (que es de B-06, fuera de mis archivos propios en esta tarea). Para mantener el mismo 401 genérico sin tocar GlobalExceptionHandler, el servicio lanza `BadCredentialsException` tanto para contraseña incorrecta como para cuenta bloqueada. El mensaje es siempre "Usuario o contraseña incorrectos" (sin revelar el motivo real). Si en el futuro se quiere diferenciar el código de error para el cliente, GlobalExceptionHandler puede añadir `@ExceptionHandler(LockedException.class)` de forma independiente.
  - **Búsqueda de usuario previa al authManager:** Necesaria para verificar `isLocked()` antes de la verificación de contraseña (que haría BCrypt). El usuario no encontrado devuelve el mismo error genérico. Diferencia de timing mínima, aceptable a esta escala (5-20 usuarios).
  - **save() en login exitoso:** Se guarda siempre (aunque `failedAttempts` ya sea 0) para simplificar. El overhead es mínimo.
- **Pendiente / siguiente paso:** B-04 (TenantService, ChargeService, TenantController, etc.).
- **Nota de traspaso:** docs/handoff/2026-10-03-dev2-B06.md (existente; se actualiza in-situ al ser un fix del mismo alcance)

---

## 2026-10-03 · B-03 + B-05 · DebtCalculatorService + semilla dev SQL
- **Qué hice:**
  - B-03: Creé `DebtSummary.java` (record inmutable: `List<YearMonth> monthsOwed`, `BigDecimal totalOwed`, `TenantStatus status`) en `service/`, siguiendo ARCHITECTURE §4.3 y la firma `calculate(Tenant, YearMonth)`.
  - B-03: Creé `DebtCalculatorService.java` (@Service, lógica pura sin repositorios ni reloj): genera la lista de meses desde startDate hasta currentMonth, descuenta paidMonths, suma cargos únicos no pagados y calcula totalOwed y status.
  - B-03: Creé `DebtCalculatorServiceTest.java` con 11 tests JUnit 5 puros (sin Spring, sin BD): los 3 casos de paridad de data.js (t-001, t-002, t-003) + 8 bordes. Todos en verde.
  - B-05: Creé `V1000__dev_seed.sql` con los 3 arrendatarios de data.js usando fechas absolutas (deterministas), DO $$ ... RETURNING id para evitar hardcodear IDs secuenciales, paid_months y one_time_charges incluidos.
  - `./gradlew build` → BUILD SUCCESSFUL (11 nuevos tests DebtCalculatorService + 5 TenantRepository = 16 en verde).
- **Archivos tocados:**
  - `backend/src/main/java/com/rentals/service/DebtSummary.java` (creado)
  - `backend/src/main/java/com/rentals/service/DebtCalculatorService.java` (creado)
  - `backend/src/test/java/com/rentals/service/DebtCalculatorServiceTest.java` (creado)
  - `backend/src/main/resources/db/seed/V1000__dev_seed.sql` (creado)
  - `docs/TASKS.md` (B-03, B-05 → EN REVISIÓN)
  - `docs/logs/dev2.md` (esta entrada)
  - `docs/handoff/2026-10-03-dev2-B03.md` (creado)
- **Decisiones y motivo:**
  - Firma `calculate(Tenant, YearMonth)` (no LocalDate + paymentDay check): ARCHITECTURE §4.3 explicita que `paymentDay` NO afecta al conteo y especifica `YearMonth currentMonth`. La tarea del líder describía un comportamiento diferente; se siguió la ARCHITECTURE como documento autoritativo y se reporta en la nota de traspaso.
  - `DebtSummary` en `service/` (no en `service/dto/`): ARCHITECTURE §3 lista `DebtSummary (record)` directamente bajo `service/`. No existe ninguna mención a `service/dto/` en la arquitectura.
  - Seed con `DO $$ ... RETURNING id`: evita hardcodear IDs y es robusto ante cualquier valor inicial de la secuencia. Fechas absolutas (no `now()`) para determinismo en tests de paridad.
- **Pendiente / siguiente paso:** B-06 (seguridad JWT, V2__auth_schema.sql, entidades AppUser/Role/RefreshToken, V1001__dev_seed_users.sql).
- **Nota de traspaso:** docs/handoff/2026-10-03-dev2-B03.md
