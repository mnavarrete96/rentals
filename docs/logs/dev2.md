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
