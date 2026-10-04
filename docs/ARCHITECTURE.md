# Arquitectura

> **Estado: VIGENTE (v2: aplicación dinámica con autenticación JWT).** La mantiene el `architect`. Cada
> decisión relevante se registra en `docs/DECISIONS.md` (ADR-003 a ADR-009 cubren v2; ADR-008 y ADR-009
> cubren la autenticación).
> Última actualización: 2026-10-03 (se incorpora autenticación real a v2; antes estaba reservada para v3).
> La arquitectura v1 (web estática) se conserva como referencia histórica en el **Anexo A**.

## 1. Contexto

Aplicación de gestión de arriendos residenciales (ver `docs/SPEC.md` §11, alcance v2 aprobado 2026-10-03).
Lo que condiciona el diseño:

- **Stack elegido por el usuario:** Vue 3 + Vite (frontend), Spring Boot + **Java 25** + Gradle (backend),
  PostgreSQL (datos). Java 25 sustituye al default Java 8 de `CLAUDE.md` por decisión explícita del
  usuario (ADR-004).
- Mismos requisitos funcionales RF-01 a RF-05 que v1; RF-07 (datos hardcodeados) se reinterpreta como
  **datos semilla en el perfil `dev`** (ver §9).
- **Autenticación real en v2** (decisión del usuario 2026-10-03): JWT, roles `ADMIN` y `ARRENDATARIO`.
  El **toggle manual Admin/Arrendatario desaparece**: la vista se elige según el rol del usuario
  autenticado (RF-06 se reinterpreta; ver §15). El arrendatario ya no elige su nombre en un selector: ve
  su propia cuenta.
- Escala: 5 a 20 arrendatarios, un único inmueble. Moneda: USD. Zona horaria del negocio:
  **America/Guayaquil**.
- Despliegue en AWS **manual** por el usuario. Ningún agente ejecuta `aws`, despliega ni hace `git push`.
- Ningún secreto en el repositorio.

## 2. Vista general

```
                 Navegador
                     │  HTTPS
                     ▼
     ┌──────────────────────────────────────────┐
     │ FRONTEND  (Vue 3 + Vite, SPA estática)    │
     │                                           │
     │  App.vue: Login | AdminView | TenantView  │  <- elige vista según rol (sin toggle)
     │     │                                     │
     │  Components (TenantCard, MonthsTable…)    │  <- presentación
     │     │                                     │
     │  Pinia stores (auth, tenants)             │  <- estado de cliente (token solo en memoria)
     │     │                                     │
     │  api/ (authApi, tenantsApi → http.js)     │  <- única puerta a la red; añade Bearer
     └─────────────────────┬─────────────────────┘
                           │  JSON sobre HTTP, rutas relativas /api/**
                           │  Authorization: Bearer <access JWT>
                           │  Cookie HttpOnly rentals_refresh (solo /api/auth/*)
                           ▼
     ┌──────────────────────────────────────────┐
     │ BACKEND  (Spring Boot, Java 25)           │
     │                                           │
     │  Spring Security (filtro JWT, stateless)  │  <- autentica y autoriza por rol (§15)
     │     │                                     │
     │  web/        Controllers + DTOs + errores │  <- HTTP ↔ casos de uso
     │     │                                     │
     │  service/    TenantService, ChargeService │  <- casos de uso, transacciones
     │              AuthService, UserAccountSvc  │
     │              DebtCalculatorService (puro) │  <- regla de negocio (ex calc.js)
     │     │                                     │
     │  security/   emisión/validación de tokens │  <- infraestructura de seguridad
     │     │                                     │
     │  repository/ Spring Data JPA              │  <- acceso a datos
     │     │                                     │
     │  domain/     Entidades JPA                │  <- modelo
     └─────────────────────┬─────────────────────┘
                           │  JDBC (Flyway gestiona el esquema)
                           ▼
                 ┌───────────────────┐
                 │  PostgreSQL        │
                 └───────────────────┘
```

**Regla de dependencias (SOLID / DIP):** las flechas apuntan solo hacia abajo.
- Frontend: los componentes **nunca** llaman a `fetch`; usan acciones de Pinia, que usan `api/`.
  El frontend **no calcula deudas**: muestra lo que devuelve el backend.
- Backend: los controllers no tocan repositorios; los services no conocen HTTP (ni `ResponseEntity`,
  ni cookies, ni DTOs web) ni la librería JWT (dependen de la interfaz `AccessTokenIssuer`);
  `DebtCalculatorService` no conoce Spring Data ni la hora del sistema (recibe el mes actual como parámetro).

**Fuente de verdad única:** la base de datos guarda solo hechos (arrendatarios, meses pagados,
cargos únicos, usuarios, refresh tokens). Meses adeudados, total y estado se **calculan en el backend**
en cada lectura y nunca se persisten (ADR-006).

## 3. Estructura de carpetas (monorepo, ADR-005)

```
rentals/
├── backend/                               <- dev2
│   ├── build.gradle                       (Groovy DSL)
│   ├── settings.gradle
│   ├── gradlew, gradlew.bat, gradle/wrapper/
│   └── src/
│       ├── main/
│       │   ├── java/com/rentals/
│       │   │   ├── RentalsApplication.java
│       │   │   ├── config/        ClockConfig, CorsConfig
│       │   │   ├── security/      SecurityConfig, AuthProperties (record), JwtConfig,
│       │   │   │                  JwtAccessTokenIssuer (impl. de AccessTokenIssuer),
│       │   │   │                  RefreshTokenService, AppUserDetailsService,
│       │   │   │                  AdminBootstrapRunner, ProblemDetailAuthHandlers
│       │   │   ├── web/           TenantController, ChargeController, PaidMonthController,
│       │   │   │                  MeController, AuthController, RefreshCookieFactory,
│       │   │   │                  GlobalExceptionHandler, TenantMapper
│       │   │   │   └── dto/       TenantSummaryDto, TenantDetailDto, CreateTenantRequest,
│       │   │   │                  CreateChargeRequest, OneTimeChargeDto, LoginRequest,
│       │   │   │                  AuthResponse, AuthUserDto, ChangePasswordRequest (records)
│       │   │   ├── service/       TenantService, ChargeService, DebtCalculatorService,
│       │   │   │                  DebtSummary (record), AuthService, UserAccountService,
│       │   │   │                  AccessTokenIssuer (interfaz), AuthResult (record),
│       │   │   │                  TenantNotFoundException, UsernameTakenException,
│       │   │   │                  InvalidCredentialsException, InvalidRefreshTokenException, ...
│       │   │   ├── repository/    TenantRepository, OneTimeChargeRepository,
│       │   │   │                  AppUserRepository, RefreshTokenRepository
│       │   │   └── domain/        Tenant, OneTimeCharge, TenantStatus (enum),
│       │   │                      YearMonthConverter, AppUser, Role (enum), RefreshToken
│       │   └── resources/
│       │       ├── application.yml            (sin secretos; usa ${ENV_VARS})
│       │       ├── application-dev.yml
│       │       ├── db/migration/              V1__init_schema.sql, V2__auth_schema.sql, ...
│       │       └── db/seed/                   V1000__dev_seed.sql, V1001__dev_seed_users.sql
│       │                                      (solo perfil dev)
│       └── test/java/com/rentals/             unit, @WebMvcTest (+spring-security-test),
│                                              @DataJpaTest/@SpringBootTest (+Testcontainers)
│
├── frontend/                              <- dev1
│   ├── package.json, package-lock.json
│   ├── vite.config.js                     (proxy /api → http://localhost:8080 en dev)
│   ├── index.html
│   └── src/
│       ├── main.js                        (crea Pinia y conecta http.js con el store auth)
│       ├── App.vue                        (restaura sesión; Login | AdminView | TenantView por rol)
│       ├── api/           http.js (fetch + Bearer + reintento tras refresh + errores),
│       │                  authApi.js, tenantsApi.js
│       ├── stores/        auth.js, tenants.js      (Pinia; sin persistencia)
│       ├── views/         LoginView.vue, AdminView.vue, TenantView.vue
│       ├── components/    admin/ (TenantList, TenantCard, TenantDetail, TenantForm,
│       │                          ChargeForm, MonthsTable)
│       │                  tenant/ (AccountStatement)
│       │                  common/ (AppHeader, ChangePasswordForm, StatusBadge,
│       │                           MoneyAmount, ErrorBanner)
│       ├── utils/         format.js (USD con Intl.NumberFormat, etiqueta de mes)
│       └── assets/
│           ├── styles/    tokens.css, base.css    (portados desde src/css/styles.css)
│           └── fonts/     Inter .woff2            (copiadas desde src/assets/fonts/)
│
├── src/                                   <- v1 CONGELADA (solo lectura, ver §10)
├── tests/                                 <- pruebas manuales / checklists del tester
├── compose.yaml                           <- PostgreSQL local para desarrollo (dev2)
├── .env.example                           <- nombres de variables, SIN valores reales (dev2)
└── docs/
```

Respecto al diseño v2 anterior desaparecen `stores/ui.js` (el modo ya no es un toggle) y
`components/tenant/TenantSelector.vue` (el arrendatario ve solo su cuenta).

## 4. Backend (Spring Boot)

### 4.1 Versiones y librerías

| Elemento | Elección | Nota |
|---|---|---|
| Java | **25 (LTS)**, toolchain de Gradle | ADR-004 |
| Spring Boot | Última GA estable que soporte Java 25 (línea **4.0.x**; si al generar el proyecto hubiera incompatibilidad, 3.5.x con Java 25) | Generar con start.spring.io; fijar versión en `build.gradle` |
| Gradle | Wrapper ≥ 9.1 (requisito para Java 25) | Siempre usar `./gradlew`, nunca un Gradle global |
| Starters | `web`, `data-jpa`, `validation`, `actuator`, **`security`**, **`oauth2-resource-server`** | Seguridad: ADR-008. El starter de resource server trae Nimbus (`JwtEncoder`/`JwtDecoder`); en Boot 4 su nombre puede ser `spring-boot-starter-security-oauth2-resource-server`: usar el que publique la versión fijada |
| Migraciones | **Flyway** (+ `flyway-database-postgresql`) | `spring.jpa.hibernate.ddl-auto=validate` |
| Driver | `org.postgresql:postgresql` | |
| Tests | JUnit 5, AssertJ, Spring Boot Test, **spring-security-test**, **Testcontainers** (PostgreSQL) | |
| Prohibido sin ADR | Lombok, MapStruct, springdoc, librerías JWT de terceros (jjwt, auth0 java-jwt), Spring Session, Redis | YAGNI: records, mapeo manual y Nimbus (vía Spring) bastan |

### 4.2 Capas y responsabilidades

| Capa | Clase | Responsabilidad única |
|---|---|---|
| web | `TenantController` | `GET/POST /api/tenants`, `GET /api/tenants/{id}` (ADMIN) |
| web | `PaidMonthController` | Marcar / desmarcar mes pagado (ADMIN) |
| web | `ChargeController` | Crear cargo único / marcarlo pagado (ADMIN) |
| web | `MeController` | `GET /api/me/tenant`: cuenta del arrendatario autenticado (lee `tenantId` del JWT) |
| web | `AuthController` | Login, refresh, logout, cambio de contraseña. Único que maneja la cookie de refresh |
| web | `RefreshCookieFactory` | Construye/borra la cookie `rentals_refresh` (`ResponseCookie`) según `AuthProperties` |
| web | `TenantMapper` | Entidad + `DebtSummary` → DTO de respuesta (mapeo manual) |
| web | `GlobalExceptionHandler` (`@RestControllerAdvice`) | Excepciones → `ProblemDetail` (RFC 9457) |
| service | `TenantService` | Crear (incluye credenciales vía `UserAccountService`, misma transacción), listar, obtener; marcar/desmarcar meses. `@Transactional` |
| service | `ChargeService` | Crear cargo; marcar cargo pagado. `@Transactional` |
| service | `AuthService` | Login (vía `AuthenticationManager`), refresh con rotación, logout. Devuelve `AuthResult` |
| service | `UserAccountService` | Crear usuario ARRENDATARIO para un tenant; cambiar contraseña; crear admin inicial |
| service | `AccessTokenIssuer` (interfaz) | `issue(AppUser) -> IssuedToken(value, expiresIn)`. Los services no conocen Nimbus |
| service | `DebtCalculatorService` | **Lógica pura de deuda** (port de `calc.js`). Sin repositorios, sin reloj |
| security | `SecurityConfig` | `SecurityFilterChain` stateless, reglas por ruta y rol (§15.4), `PasswordEncoder`, `AuthenticationManager` |
| security | `JwtConfig` | Beans `JwtEncoder` / `JwtDecoder` (HS256 con `RENTALS_JWT_SECRET`), validador de `iss`, conversor `role` → `ROLE_*` |
| security | `JwtAccessTokenIssuer` | Implementa `AccessTokenIssuer` con `JwtEncoder` y el `Clock` |
| security | `RefreshTokenService` | Genera token opaco, guarda su hash, valida, rota y revoca (§15.3) |
| security | `AppUserDetailsService` | `UserDetailsService` sobre `AppUserRepository` (usado solo en login) |
| security | `AdminBootstrapRunner` | Al arrancar, crea el ADMIN inicial desde variables de entorno si no existe ninguno (§15.6) |
| security | `ProblemDetailAuthHandlers` | `AuthenticationEntryPoint` (401) y `AccessDeniedHandler` (403) que responden `ProblemDetail` |
| repository | `TenantRepository`, `OneTimeChargeRepository`, `AppUserRepository`, `RefreshTokenRepository` | `JpaRepository<…, Long>` |
| domain | `Tenant`, `OneTimeCharge`, `TenantStatus`, `AppUser`, `Role`, `RefreshToken` | Modelo persistente; invariantes simples |
| config | `ClockConfig` | Bean `java.time.Clock` con la zona `rentals.time-zone` |
| config | `CorsConfig` | CORS **desactivado** salvo que `rentals.cors.allowed-origins` tenga valor |

Las respuestas de los endpoints de mutación devuelven el `TenantDetailDto` actualizado, de modo que el
frontend reemplaza el arrendatario en su store sin un GET adicional.

`spring.jpa.open-in-view=false`. `hibernate.default_batch_fetch_size=50` para cargar colecciones del
listado sin N+1 relevante.

### 4.3 `DebtCalculatorService` (migración de `calc.js`)

Se porta la lógica de `src/js/calc.js` **con paridad exacta de comportamiento** (no se cambia la regla de
negocio en la migración):

```java
public record DebtSummary(List<YearMonth> monthsOwed, BigDecimal totalOwed, TenantStatus status) {}

@Service
public class DebtCalculatorService {
    /** Pura: mismo input → mismo output. currentMonth lo inyecta el llamador desde el Clock. */
    public DebtSummary calculate(Tenant tenant, YearMonth currentMonth) { ... }
}
```

| `calc.js` (v1) | Java (v2) |
|---|---|
| Claves `"YYYY-MM"` como string | `java.time.YearMonth` (se serializa como `"YYYY-MM"`) |
| `monthsOwed`: desde mes de `startDate` hasta mes de `today`, **ambos inclusive**, menos `paidMonths` | Igual. Orden ascendente. Si `startDate` es futuro → lista vacía |
| `totalOwed` = meses × `monthlyAmount` + Σ cargos únicos con `paid=false` | Igual, con `BigDecimal` (escala 2) |
| `status`: `ATRASADO` si hay meses o cargos impagos; si no `AL_DIA` | `enum TenantStatus { AL_DIA, ATRASADO }` |
| `today` pasado desde fuera | `YearMonth.now(clock)` lo calculan `TenantService`/`ChargeService`; el calculador solo recibe `YearMonth` |

- `paymentDay` sigue sin afectar al conteo (igual que v1). Cambiar eso sería un cambio de regla de
  negocio y requiere actualizar el SPEC primero.
- **Pruebas de paridad:** dev2 escribe `DebtCalculatorServiceTest` (JUnit puro, sin Spring) con los
  casos de `src/js/data.js` (t-001 al día, t-002 atrasado 2 meses, t-003 cargo único pendiente) y bordes
  (cambio de año, `startDate` futuro, mes en curso). Resultados esperados = los de v1 para la misma fecha.
- `calc.js` **no** se reutiliza en el frontend v2 (ADR-006): una sola implementación de la regla.

### 4.4 Configuración y secretos

`application.yml` solo contiene referencias a variables de entorno:

```yaml
spring:
  datasource:
    url: ${DB_URL}            # p. ej. jdbc:postgresql://localhost:5432/rentals
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
rentals:
  time-zone: ${RENTALS_TIME_ZONE:America/Guayaquil}
  cors:
    allowed-origins: ${RENTALS_CORS_ALLOWED_ORIGINS:}  # vacío = CORS desactivado
  auth:
    jwt-secret: ${RENTALS_JWT_SECRET}                  # OBLIGATORIO. Base64, >= 32 bytes decodificados
    issuer: rentals-api
    access-token-ttl: ${RENTALS_ACCESS_TOKEN_TTL:PT15M}
    refresh-token-ttl: ${RENTALS_REFRESH_TOKEN_TTL:P7D}
    refresh-cookie-secure: ${RENTALS_REFRESH_COOKIE_SECURE:true}   # false solo en dev (http://localhost)
    bootstrap-admin:
      username: ${RENTALS_BOOTSTRAP_ADMIN_USERNAME:}
      password: ${RENTALS_BOOTSTRAP_ADMIN_PASSWORD:}
```

- `AuthProperties` es un `record` `@ConfigurationProperties("rentals.auth")` + `@Validated`: si
  `jwt-secret` falta o decodifica a menos de 32 bytes, **la aplicación no arranca** (fail fast).
- `application-dev.yml` fija `rentals.auth.refresh-cookie-secure=false`.
- `.env` real está en `.gitignore` (ya lo está). En el repo solo `.env.example` con nombres y valores
  ficticios evidentes (`DB_PASSWORD=change-me`, `RENTALS_JWT_SECRET=change-me-base64-32-bytes-min`,
  `RENTALS_BOOTSTRAP_ADMIN_PASSWORD=change-me`).
- Generar un secreto real (lo hace el usuario, no los agentes): `openssl rand -base64 48`.
- `compose.yaml` levanta PostgreSQL local leyendo esas variables de `.env`. Es solo para desarrollo.
- `actuator` expone únicamente `health` (`management.endpoints.web.exposure.include=health`).
- **Nunca se registran** contraseñas, tokens (access o refresh), cabeceras `Authorization` ni cookies en logs.

## 5. Modelo de base de datos (PostgreSQL)

Esquema creado por Flyway: `V1__init_schema.sql` (negocio) y `V2__auth_schema.sql` (seguridad).
Nombres en `snake_case`.

```
tenant   1 ───< paid_month          (meses de arriendo pagados)
tenant   1 ───< one_time_charge     (garantía, daños, etc.)
tenant   1 ───o app_user            (0..1 usuario ARRENDATARIO por tenant; ADMIN sin tenant)
app_user 1 ───< refresh_token       (sesiones de refresh activas / revocadas)
```

### 5.1 `V1__init_schema.sql` (sin cambios)

```sql
CREATE TABLE tenant (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name       VARCHAR(120)  NOT NULL,
    unit            VARCHAR(40)   NOT NULL,
    contact         VARCHAR(120)  NOT NULL,
    payment_day     SMALLINT      NOT NULL CHECK (payment_day BETWEEN 1 AND 28),
    start_date      DATE          NOT NULL,
    monthly_amount  NUMERIC(10,2) NOT NULL CHECK (monthly_amount >= 0),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE paid_month (
    tenant_id   BIGINT  NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    year_month  CHAR(7) NOT NULL CHECK (year_month ~ '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    PRIMARY KEY (tenant_id, year_month)
);

CREATE TABLE one_time_charge (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id    BIGINT        NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
    description  VARCHAR(200)  NOT NULL,
    amount       NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    paid         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_one_time_charge_tenant ON one_time_charge(tenant_id);
```

### 5.2 `V2__auth_schema.sql` (nuevo, ADR-008/009)

```sql
CREATE TABLE app_user (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username             VARCHAR(50)  NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,          -- "{bcrypt}$2a$10$..." (DelegatingPasswordEncoder)
    role                 VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'ARRENDATARIO')),
    tenant_id            BIGINT       NULL UNIQUE REFERENCES tenant(id) ON DELETE RESTRICT,
    enabled              BOOLEAN      NOT NULL DEFAULT TRUE,
    password_changed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_user_username UNIQUE (username),
    CONSTRAINT ck_app_user_username_lower CHECK (username = lower(username)),
    CONSTRAINT ck_app_user_role_tenant CHECK (
        (role = 'ADMIN'        AND tenant_id IS NULL) OR
        (role = 'ARRENDATARIO' AND tenant_id IS NOT NULL)
    )
);

CREATE TABLE refresh_token (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash  CHAR(64)    NOT NULL UNIQUE,              -- SHA-256 hex del token opaco; nunca el token
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);
```

Reglas del modelo de usuarios:
- **Username** normalizado (`trim` + minúsculas) en `UserAccountService` antes de guardar o buscar;
  patrón `^[a-z0-9._-]{3,50}$`. Único en todo el sistema.
- **Contraseñas** solo como hash BCrypt (`PasswordEncoderFactories.createDelegatingPasswordEncoder()`).
  Política: mínimo 10 caracteres, máximo 72 bytes (límite de BCrypt); sin reglas de composición.
- Un tenant tiene **como máximo un** usuario (`UNIQUE tenant_id`); un ADMIN nunca tiene tenant.
- **¿Qué pasa si se elimina un tenant?** v2 **no ofrece** borrado de tenants (no existe
  `DELETE /api/tenants/{id}` en §6). La FK es `ON DELETE RESTRICT` para que un borrado manual en BD
  falle en lugar de dejar huérfanos o borrar credenciales en silencio. Cuando se necesite "dar de baja"
  a un arrendatario, será **baja lógica**: `app_user.enabled = false` + revocar sus refresh tokens, en la
  misma transacción, conservando el historial de pagos. Eso requiere actualizar el SPEC primero.
- `enabled = false` impide login y refresh; un access token ya emitido sigue siendo válido hasta su
  expiración (≤ 15 min, aceptado: ADR-008).

### 5.3 Mapeo JPA

- `Tenant` → entidad. `paidMonths` = `@ElementCollection Set<YearMonth>` sobre la tabla `paid_month`,
  con `@Convert(YearMonthConverter)` (`YearMonth` ↔ `"YYYY-MM"`). Es un valor, no una entidad: no
  necesita id propio; la PK compuesta garantiza la idempotencia de "marcar pagado".
- `OneTimeCharge` → entidad (`@ManyToOne(fetch = LAZY) Tenant`). Necesita id porque se marca pagada
  individualmente. `Tenant.oneTimeCharges` = `@OneToMany(mappedBy, cascade = ALL, orphanRemoval = true)`.
- `AppUser` → entidad; `role` como `@Enumerated(STRING) Role { ADMIN, ARRENDATARIO }`;
  `@OneToOne(fetch = LAZY, optional = true) @JoinColumn(name = "tenant_id") Tenant tenant`.
  **`Tenant` no referencia a `AppUser`** (relación unidireccional): el modelo de negocio no depende de la
  seguridad. Para mostrar el `username` en el detalle admin se usa `AppUserRepository.findByTenantId`.
- `RefreshToken` → entidad con `@ManyToOne(fetch = LAZY) AppUser user`.
- Dinero siempre `BigDecimal` / `NUMERIC(10,2)`, nunca `double`.
- Correspondencia con v1: `id` pasa de string `"t-001"` a número; el resto de campos conserva nombre y
  significado (`fullName`, `unit`, `contact`, `paymentDay`, `startDate`, `monthlyAmount`,
  `oneTimeCharges[]`, `paidMonths[]`).

## 6. API REST (contrato dev1 ↔ dev2)

Base: `/api`. JSON. Fechas `"YYYY-MM-DD"`, meses `"YYYY-MM"`, montos como número decimal (USD).
Este contrato es la **frontera** entre frontend y backend: si cambia, se avisa por `docs/handoff/`
antes de romperlo.

Salvo que se indique "público", toda petición lleva `Authorization: Bearer <accessToken>`.
Sin token / token inválido o expirado → `401`. Token válido pero rol incorrecto → `403`.

### 6.1 Endpoints de negocio

| # | Método y ruta | Rol | Cuerpo | Respuesta | Cubre |
|---|---|---|---|---|---|
| 1 | `GET /api/tenants` | ADMIN | — | `200 TenantSummaryDto[]` (orden por `unit`) | RF-03, lista admin |
| 2 | `GET /api/tenants/{id}` | ADMIN | — | `200 TenantDetailDto` · `404` | RF-03 |
| 3 | `POST /api/tenants` | ADMIN | `CreateTenantRequest` (**incluye credenciales**) | `201 TenantDetailDto` + `Location` · `409` username ocupado | RF-01 + alta de credenciales |
| 4 | `POST /api/tenants/{id}/charges` | ADMIN | `CreateChargeRequest` | `201 TenantDetailDto` | RF-02 |
| 5 | `PUT /api/tenants/{id}/charges/{chargeId}/paid` | ADMIN | — | `200 TenantDetailDto` (idempotente) | marcar cargo pagado |
| 6 | `PUT /api/tenants/{id}/paid-months/{yearMonth}` | ADMIN | — | `200 TenantDetailDto` (idempotente) | RF-04 |
| 7 | `DELETE /api/tenants/{id}/paid-months/{yearMonth}` | ADMIN | — | `200 TenantDetailDto` (idempotente) | desmarcar |
| 8 | `GET /api/me/tenant` | ARRENDATARIO | — | `200 TenantDetailDto` del tenant del token | RF-05 (sin selector) |
| — | `GET /actuator/health` | público | — | `200 {"status":"UP"}` | operación |

El arrendatario **nunca** recibe un `id` de tenant en la URL: el endpoint #8 toma `tenantId` del JWT.
Así no hay riesgo de que un arrendatario lea la cuenta de otro cambiando un número (IDOR).

### 6.2 Endpoints de autenticación

| # | Método y ruta | Acceso | Cuerpo | Respuesta |
|---|---|---|---|---|
| A1 | `POST /api/auth/login` | público | `LoginRequest` | `200 AuthResponse` + `Set-Cookie: rentals_refresh` · `401` genérico |
| A2 | `POST /api/auth/refresh` | público (requiere cookie) | — | `200 AuthResponse` + nueva cookie (rotación) · `401` + cookie borrada |
| A3 | `POST /api/auth/logout` | público (usa cookie si existe) | — | `204` + cookie borrada (idempotente) |
| A4 | `PUT /api/auth/password` | ADMIN o ARRENDATARIO | `ChangePasswordRequest` | `204` + cookie borrada (se cierran **todas** las sesiones) · `400` · `401` si `currentPassword` no coincide |

El **refresh token nunca viaja en el cuerpo JSON**: va solo en la cookie `rentals_refresh`
(`HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800`). Ver ADR-009.

### 6.3 DTOs

```jsonc
// TenantSummaryDto
{ "id": 1, "fullName": "María Pérez", "unit": "Depto 301",
  "status": "AL_DIA", "totalOwed": 0.00, "monthsOwedCount": 0 }

// TenantDetailDto  (username: login del arrendatario; null si el tenant no tiene usuario)
{ "id": 1, "fullName": "María Pérez", "unit": "Depto 301", "contact": "maria.perez@mail.com",
  "username": "maria.perez",
  "paymentDay": 5, "startDate": "2026-07-01", "monthlyAmount": 450.00,
  "paidMonths": ["2026-07", "2026-08"],
  "oneTimeCharges": [ { "id": 10, "description": "Garantía", "amount": 450.00, "paid": true } ],
  "monthsOwed": ["2026-09", "2026-10"], "totalOwed": 900.00, "status": "ATRASADO" }

// CreateTenantRequest  (Bean Validation)
{ "fullName": "…",        // @NotBlank @Size(max=120)
  "unit": "…",            // @NotBlank @Size(max=40)
  "contact": "…",         // @NotBlank @Size(max=120)
  "paymentDay": 5,        // @NotNull @Min(1) @Max(28)
  "startDate": "2026-07-01",   // @NotNull
  "monthlyAmount": 450.00,     // @NotNull @PositiveOrZero @Digits(integer=8, fraction=2)
  "username": "maria.perez",   // @NotBlank @Pattern(^[A-Za-z0-9._-]{3,50}$)  (se guarda en minúsculas)
  "initialPassword": "…" }     // @NotBlank @Size(min=10, max=72)  — nunca se devuelve ni se registra

// CreateChargeRequest
{ "description": "Garantía",   // @NotBlank @Size(max=200)
  "amount": 450.00 }           // @NotNull @Positive @Digits(integer=8, fraction=2)

// LoginRequest
{ "username": "maria.perez", "password": "…" }   // @NotBlank ambos

// AuthResponse  (respuesta de A1 y A2)
{ "accessToken": "eyJhbGciOiJIUzI1NiJ9…", "tokenType": "Bearer", "expiresIn": 900,
  "user": { "id": 3, "username": "maria.perez", "role": "ARRENDATARIO", "tenantId": 1 } }
  // ADMIN: "role": "ADMIN", "tenantId": null

// ChangePasswordRequest
{ "currentPassword": "…",   // @NotBlank
  "newPassword": "…" }      // @NotBlank @Size(min=10, max=72)
```

Reglas:
- `{yearMonth}` debe cumplir `YYYY-MM` y no ser anterior al mes de `startDate` → si no, `400`.
  Meses futuros se permiten (pago adelantado); no alteran la deuda hasta que llegue ese mes.
- Errores: `ProblemDetail` (`application/problem+json`) con `status`, `title`, `detail` y, en
  validación, `errors: [{field, message}]`. `404` si el tenant o el cargo no existen (o el cargo
  no pertenece a ese tenant). Nunca se devuelven stack traces. `401` y `403` también son `ProblemDetail`
  (los generan `ProblemDetailAuthHandlers`).
- **Login fallido:** siempre `401` con el mismo `detail` ("Usuario o contraseña incorrectos"), exista o no
  el usuario, esté o no deshabilitado. No se revela qué falló.
- `POST /api/tenants` crea `tenant` + `app_user` (rol ARRENDATARIO) en **una sola transacción**: si el
  username está ocupado → `409` y no se crea nada.
- La garantía del formulario RF-01, si el admin la informa, la crea el frontend como segunda llamada
  (#4 con descripción `"Garantía"`), igual que en v1 era un `oneTimeCharge`.

## 7. Frontend (Vue 3)

| Elemento | Elección | Motivo |
|---|---|---|
| Framework | Vue 3, **Composition API** con `<script setup>` | ADR-003 |
| Lenguaje | **JavaScript** (no TypeScript) | Proyecto pequeño; contrato documentado en §6 |
| Bundler | Vite | Estándar de Vue 3 |
| Estado | **Pinia** (`auth`, `tenants`), **sin persistencia** | El access token vive solo en memoria (ADR-009) |
| HTTP | **`fetch` nativo** envuelto en `api/http.js` | Sin dependencia extra |
| Router | **Ninguno** en v2 | `App.vue` elige la vista según `auth.status` y `auth.user.role` (ADR-008). Deep-linking no es requisito |
| Estilos | CSS propio con los tokens de `src/css/styles.css` (ADR-002) | Sin Vuetify/Tailwind; diseño de `docs/DESIGN.md` |
| Fuentes | Inter `.woff2` local | Sin CDN |
| Tests | Vitest + `@vue/test-utils` (stores, `http.js` y componentes clave) | |

Flujo de datos:

```
Componente ──(acción)──▶ Pinia store ──▶ authApi / tenantsApi ──▶ http.js (fetch '/api/...', + Bearer)
    ▲                         │
    └──── estado reactivo ◀───┘  (el store reemplaza el tenant con el TenantDetailDto devuelto)
```

- `api/http.js`: `request(method, path, body, { auth = true } = {})`; rutas **relativas** (`/api/...`),
  parsea JSON, convierte no-2xx (`ProblemDetail`) en `ApiError {status, title, detail, errors}`.
  Detalle del manejo de tokens en §15.7. `http.js` **no importa** el store: recibe sus dependencias con
  `configureAuth({ getAccessToken, refreshAccessToken, onAuthFailure })`, que llama `main.js` (DIP; evita
  dependencias circulares y permite testear `http.js` aislado).
- `api/authApi.js`: `login`, `refresh`, `logout`, `changePassword` (las tres primeras con `auth: false`).
- `api/tenantsApi.js`: una función por endpoint de §6.1 (`listTenants`, `getTenant`, `createTenant`,
  `addCharge`, `markChargePaid`, `markMonthPaid`, `unmarkMonth`, `getMyTenant`). Ningún otro archivo usa `fetch`.
- `stores/auth.js`: ver §15.7.
- `stores/tenants.js`: `state { summaries, detailsById, selectedId, myAccount, loading, error }`;
  acciones admin (como antes) y `loadMyAccount()` para el arrendatario. `reset()` al cerrar sesión.
  **No calcula deuda.**
- Componentes: reciben props y emiten eventos; solo las *views* (y `App.vue`/`AppHeader` para la sesión)
  hablan con los stores.
- Desarrollo local: `vite.config.js` define `server.proxy['/api'] = 'http://localhost:8080'`; frontend y
  backend comparten origen, así que la cookie de refresh funciona y **no hace falta CORS**.

## 8. Reglas para los desarrolladores

- **Fronteras estrictas:**
  - **dev1 (frontend):** todo `frontend/`.
  - **dev2 (backend + datos):** todo `backend/`, `compose.yaml`, `.env.example` y las líneas de
    `.gitignore` relativas a Java/Gradle (`.gradle/`, `.idea/`, `*.iml`, `out/`).
  - Nadie modifica `src/` (v1 congelada) salvo tarea explícita del líder (§10, fase 4).
  - El contrato entre ambos es la §6. dev1 puede avanzar antes que el backend usando mocks dentro de
    `frontend/`, nunca editando `backend/`.
- **Backend:** Java 25; DTOs como `record`; inyección por constructor (sin `@Autowired` en campos);
  controllers delgados; `@Transactional` solo en services; sin lógica de negocio en entidades ni
  controllers; `DebtCalculatorService` sin acceso a reloj ni a repositorios.
- **Seguridad (backend):**
  - Las reglas de autorización viven **solo** en `SecurityConfig` (por ruta y rol). No se usan
    `@PreAuthorize` dispersos salvo que se registre un ADR.
  - Toda ruta nueva debe añadirse explícitamente a `SecurityConfig`; la regla final es `denyAll()`.
  - Los controllers obtienen el usuario con `@AuthenticationPrincipal Jwt jwt` y pasan **valores**
    (`tenantId`, `userId`) a los services; los services no importan clases de Spring Security web.
  - Nunca devolver, registrar ni incluir en excepciones contraseñas, hashes o tokens.
  - La hora de emisión/expiración de tokens sale del bean `Clock`.
- **Frontend:** `<script setup>`; ningún componente llama a `fetch`; nada de cálculo de deuda en el
  cliente; formateo de USD y meses solo en `utils/format.js`. **El access token nunca se guarda en
  `localStorage`, `sessionStorage` ni cookies accesibles por JS**; no usar plugins de persistencia de Pinia.
  No usar `v-html` con datos del servidor.
- **Esquema de BD solo vía Flyway.** Nunca editar una migración ya aplicada; crear `V{n+1}__...sql`.
- **Fechas:** ISO `YYYY-MM-DD`, meses `YYYY-MM`. Backend usa `java.time`; la "fecha de hoy" sale
  siempre del bean `Clock`.
- **Secretos:** solo por variables de entorno. Nunca `.env`, contraseñas, secretos JWT ni URLs con
  credenciales en el repo, logs o mensajes de commit.
- **Comandos permitidos a los agentes:** `./gradlew build|test|bootRun`, `npm install|run dev|run build|
  run test`, `docker compose up -d` (PostgreSQL local). Prohibido: `aws`, `git push`, despliegues.
- Un commit por tarea con el ID en el mensaje.

## 9. Datos de prueba (sustituto de RF-07)

- `db/seed/V1000__dev_seed.sql` carga los 3 arrendatarios de `src/js/data.js` (mismos nombres,
  importes y meses pagados).
- `db/seed/V1001__dev_seed_users.sql` crea un usuario ARRENDATARIO para cada uno de esos 3 tenants
  (usernames derivados del nombre, p. ej. `maria.perez`), todos con la contraseña de desarrollo
  **`dev-password-123`** (hash BCrypt generado por dev2 y pegado en el SQL). **No es un secreto**: son
  datos ficticios que solo existen con el perfil `dev`. dev2 documenta los usernames resultantes en su
  nota de traspaso para el tester.
- El **ADMIN no va en la semilla SQL**: lo crea `AdminBootstrapRunner` desde
  `RENTALS_BOOTSTRAP_ADMIN_USERNAME` / `RENTALS_BOOTSTRAP_ADMIN_PASSWORD` (en dev vienen del `.env` local).
  Así el mismo mecanismo sirve en desarrollo y producción y no hay ninguna contraseña de admin en el repo
  (§15.6).
- Solo se aplica con el perfil `dev`: `application-dev.yml` añade `classpath:db/seed` a
  `spring.flyway.locations`. En producción no existe semilla.
- **Pendiente del líder:** actualizar en `docs/SPEC.md` el criterio de RF-07, RF-05 (sin selector) y
  RF-06 (vista por rol en lugar de toggle), y las secciones v1-only (§1, §4, §6, §7) para que no
  contradigan §11.

## 10. Estrategia de migración v1 → v2 (sin romper lo existente)

v1 y v2 **coexisten** en el repositorio hasta alcanzar paridad funcional. Nada de v1 se borra antes de la
fase 4.

| Fase | Qué ocurre | v1 (`src/`) | Criterio de salida |
|---|---|---|---|
| 0. Congelar | Se recomienda al usuario crear una etiqueta local `v1-static` sobre el commit actual (sin push) | Congelada, sigue desplegable con `DEPLOY-AWS-MANUAL.md` | Etiqueta creada |
| 1. Backend | dev2: scaffold, Flyway, entidades, `DebtCalculatorService`, **seguridad JWT**, endpoints, semilla dev | Intacta | `./gradlew test` verde; endpoints §6 responden con la semilla y respetan la matriz de roles |
| 1'. Frontend (en paralelo) | dev1: scaffold Vite, tokens CSS y fuentes, `api/` + stores (incl. `auth`), login, vistas contra el contrato §6 | Intacta (solo se **copian** CSS y fuentes) | `npm run build` OK; tests de stores y `http.js` verdes |
| 2. Integración | Frontend contra backend real (proxy de Vite) | Intacta | Tester valida RF-01 a RF-05 + checklist de autenticación (§15.9) |
| 3. Despliegue v2 | El usuario despliega v2 manualmente (§11) | Sigue en línea hasta que v2 esté verificada en AWS | Usuario confirma v2 en AWS |
| 4. Archivo | Con aprobación del usuario: mover `src/` a `legacy/v1-static/` (o eliminar; la etiqueta conserva el histórico) | Archivada | Decisión explícita del usuario |

Por qué es seguro: v1 y v2 viven en carpetas disjuntas (`src/` vs `frontend/` + `backend/`), no comparten
archivos ni build, y v1 no necesita servidor. Ninguna tarea v2 necesita editar `src/`.

## 11. Despliegue en AWS (manual, lo hace el usuario)

Topología (ADR-007, confirmada por el usuario):

```
Navegador ─HTTPS─▶ CloudFront ─┬─ /*      ─▶ S3 privado (frontend/dist)       [igual que v1]
                               └─ /api/*  ─▶ EC2 (Corretto 25, jar Spring Boot)
                                               │ (subred/SG privados)
                                               ▼
                                         RDS PostgreSQL (no público)
```

- **Frontend:** `npm run build` → subir el contenido de `frontend/dist/` al bucket (mismos pasos que
  `DEPLOY-AWS-MANUAL.md`).
- **Backend:** `./gradlew bootJar` → `backend/build/libs/*.jar`; ejecutarlo en una EC2 pequeña con
  Amazon Corretto 25 como servicio `systemd`. Variables en **SSM Parameter Store (SecureString)** cargadas
  en el entorno del servicio: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `RENTALS_TIME_ZONE`,
  **`RENTALS_JWT_SECRET`**, y solo para el primer arranque **`RENTALS_BOOTSTRAP_ADMIN_USERNAME`** /
  **`RENTALS_BOOTSTRAP_ADMIN_PASSWORD`** (§15.6). `RENTALS_REFRESH_COOKIE_SECURE` queda en `true` (default).
- **CloudFront:** segundo origen (EC2) con *behavior* `/api/*`, métodos `GET, HEAD, OPTIONS, PUT, POST,
  PATCH, DELETE`, política de caché `CachingDisabled` y *origin request policy* `AllViewerExceptHostHeader`
  (reenvía la cabecera `Authorization` y la cookie `rentals_refresh`). *Viewer protocol policy*:
  **Redirect HTTP to HTTPS**. Así frontend y API comparten origen → **sin CORS** y la cookie
  `SameSite=Strict` funciona.
- **Tramo CloudFront → EC2 (importante con autenticación):** ahora por ese tramo viajan contraseñas y
  tokens. **No usar un origen HTTP público.** Recomendado: **CloudFront VPC Origin** apuntando a la EC2 en
  subred privada (el tráfico no sale a Internet, la EC2 no necesita IP pública). Alternativa: ALB con
  certificado ACM y origen HTTPS. Ver decisión D-4 (§13).
- **Seguridad de red:** el Security Group de RDS solo admite el SG de la EC2 (puerto 5432); el SG de la
  EC2 solo admite el puerto 8080 desde el origen de CloudFront (VPC Origin o *managed prefix list*);
  SSH cerrado (usar Session Manager).
- **Opcional recomendado:** regla *rate-based* de AWS WAF sobre `/api/auth/login` para frenar fuerza bruta.
- **Alternativa** (si el usuario prefiere contenedores): ECS Fargate con la misma imagen; el resto igual.
- El líder debe pedir que se añada una sección v2 a `docs/DEPLOY-AWS-MANUAL.md` (incluye generación del
  secreto JWT, alta del admin inicial y retirada de la variable de contraseña tras el primer arranque).

## 12. Riesgos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Robo de token por XSS | Suplantación | Access token solo en memoria y de 15 min; refresh en cookie `HttpOnly`; sin `v-html`; Vue escapa por defecto |
| Robo / reutilización de refresh token | Sesión prolongada de un atacante | Rotación en cada refresh; si llega un token ya revocado se revocan **todas** las sesiones del usuario; hash SHA-256 en BD |
| CSRF sobre `/api/auth/refresh` y `/logout` (usan cookie) | Refresh forzado o logout forzado | `SameSite=Strict` + `Path=/api/auth`; el refresh solo devuelve el token en el cuerpo (un sitio ajeno no puede leerlo); CORS desactivado |
| Fuerza bruta en login | Cuentas comprometidas | Contraseñas ≥ 10; BCrypt; respuesta genérica; WAF *rate-based* opcional (§11); bloqueo por intentos queda como D-6 |
| Access token válido tras deshabilitar usuario o cambiar contraseña | Ventana de acceso ≤ 15 min | Aceptado (ADR-008); refresh se rechaza de inmediato |
| Secreto JWT filtrado | Cualquiera puede firmar tokens ADMIN | Solo en SSM / `.env` local; fail fast si falta; rotarlo invalida todos los tokens (procedimiento en DEPLOY) |
| Tramo CloudFront → EC2 sin cifrar | Credenciales en claro | VPC Origin o ALB + HTTPS (D-4) |
| Admin olvida la contraseña de un arrendatario o la suya | Acceso perdido | D-5 (reset por admin); el admin inicial se puede recrear con el bootstrap tras deshabilitar el actual en BD |
| Zona horaria: el "mes actual" cambia según la zona | Deuda distinta cerca de fin de mes | `Clock` con zona `America/Guayaquil` configurable |
| Port de `calc.js` con diferencias de comportamiento | Deuda distinta a v1 | Pruebas de paridad (§4.3) |
| Java 25 / Spring Boot / Gradle recientes con incompatibilidades puntuales | Bloqueo en el scaffold | Gradle wrapper ≥ 9.1; versiones fijadas; fallback §4.1 |
| dev1 bloqueado esperando al backend | Retraso | Contrato §6 cerrado; dev1 trabaja contra mocks dentro de `frontend/` |
| Secretos filtrados (`.env`, credenciales de RDS) | Compromiso de datos | `.gitignore` cubre `.env*`; solo `.env.example`; SSM en AWS |
| Pruebas con Testcontainers requieren Docker | Tests de repositorio no corren | Unit tests y `@WebMvcTest` no dependen de Docker; los de Testcontainers se etiquetan `@Tag("integration")` |
| Errores de dinero por coma flotante | Totales incorrectos | `BigDecimal` / `NUMERIC(10,2)`; el frontend solo formatea |
| Edición de migraciones ya aplicadas | Checksum de Flyway falla en AWS | Regla §8: migraciones inmutables |

## 13. Decisiones abiertas y resueltas

Resueltas (usuario, 2026-10-03):
- ~~D-1. Protección de la API~~ → **autenticación JWT con roles** (ADR-008, ADR-009). La opción HTTP
  Basic provisional queda **descartada**.
- ~~D-2. Zona horaria~~ → `America/Guayaquil`.
- ~~D-3. Topología AWS~~ → CloudFront + S3 + EC2 + RDS (ADR-007).

Abiertas (requieren al usuario; ninguna bloquea el inicio del desarrollo):
- **D-4. Tramo CloudFront → EC2.** CloudFront VPC Origin hacia EC2 privada (**recomendado**, sin coste de
  ALB) o ALB con certificado ACM. Solo afecta al despliegue.
- **D-5. Reset de contraseña de arrendatario por el admin.** Si un arrendatario la olvida, hoy no hay
  forma de recuperarla. Propuesta: `PUT /api/tenants/{id}/password` (ADMIN, cuerpo `{newPassword}`,
  revoca sesiones del arrendatario). Coste bajo; **recomendado** incluirlo en v2. Si el usuario lo aprueba,
  se añade a §6.1 y a B-04/F-03.
- **D-6. Bloqueo por intentos fallidos de login.** YAGNI a esta escala si se activa el WAF *rate-based*;
  si no hay WAF, propongo un contador en memoria (5 fallos → 15 min por username). Decide el usuario.

## 14. Evolución prevista

- **v3:** el login ya llegó en v2. Candidatos: `vue-router` si aparecen pantallas con URL propia;
  baja lógica de arrendatarios (§5.2); recuperación de contraseña por correo (requiere v4).
- **v4 (notificaciones):** nuevo `service/notification` detrás de una interfaz (`NotificationSender`)
  con implementaciones WhatsApp / correo; invocado por un job programado que reutiliza
  `DebtCalculatorService`.
- **v5 (comprobantes + LLM):** subida a S3 y servicio de extracción detrás de una interfaz; endpoint
  ARRENDATARIO bajo `/api/me/...` (mismo patrón que #8).
- **Multipropiedad:** tabla `property` y FK en `tenant` (migración aditiva); el rol ADMIN podría
  necesitar alcance por propiedad (nuevo ADR).
- El roadmap del SPEC §9 menciona "v2.5: migración a React" y "v3: login"; ambas líneas quedan
  desactualizadas (ADR-003, ADR-008). El líder debería corregirlas.

## 15. Seguridad y autenticación (ADR-008, ADR-009)

### 15.1 Resumen

| Pieza | Decisión |
|---|---|
| Mecanismo | JWT de acceso (HS256, 15 min) + refresh token opaco (7 días) con rotación |
| Estado en servidor | Access token **stateless** (no se consulta BD por petición). Refresh tokens en tabla `refresh_token` (necesario para logout y revocación) |
| Implementación | Spring Security + OAuth2 Resource Server (`JwtDecoder` de Nimbus) para validar; `JwtEncoder` para emitir. Sin librerías JWT de terceros |
| Roles | `ADMIN`, `ARRENDATARIO` (authorities `ROLE_ADMIN`, `ROLE_ARRENDATARIO`) |
| Contraseñas | BCrypt vía `DelegatingPasswordEncoder` |
| Transporte del refresh | Cookie `rentals_refresh` `HttpOnly; Secure; SameSite=Strict; Path=/api/auth` |
| Transporte del access | Cuerpo de la respuesta → memoria del store Pinia → cabecera `Authorization: Bearer` |
| Admin inicial | `AdminBootstrapRunner` desde variables de entorno, en todos los perfiles |
| Credenciales del arrendatario | Las define el admin en `POST /api/tenants`; el arrendatario las cambia con `PUT /api/auth/password` |

### 15.2 Access token (JWT)

- Firmado HS256 con `RENTALS_JWT_SECRET` (clave simétrica: emisor y validador son la misma app).
- Claims: `iss = "rentals-api"`, `sub = <app_user.id>`, `username`, `role` (`"ADMIN"` | `"ARRENDATARIO"`),
  `tenantId` (solo ARRENDATARIO), `iat`, `exp = iat + 15 min`.
- `JwtDecoder` valida firma, `exp` (tolerancia por defecto 60 s) e `iss`.
- `JwtAuthenticationConverter` con `JwtGrantedAuthoritiesConverter` (`authoritiesClaimName = "role"`,
  `authorityPrefix = "ROLE_"`).
- No se revoca individualmente: su vida corta es la mitigación (aceptado en ADR-008).

### 15.3 Refresh token

- Valor: 32 bytes aleatorios de `SecureRandom`, Base64URL (opaco, **no** es un JWT).
- En BD se guarda solo `SHA-256(token)` en hex (`refresh_token.token_hash`), `expires_at = now + 7 días`.
- **Login:** crea un registro nuevo y aprovecha para borrar los registros expirados de ese usuario.
- **Refresh (rotación):** busca por hash →
  - no existe o expirado → `401`;
  - existe pero `revoked_at` no es nulo → **posible robo**: revoca todos los refresh del usuario → `401`;
  - usuario `enabled = false` → `401`;
  - si es válido → marca `revoked_at = now`, emite refresh nuevo + access nuevo (misma transacción).
- **Logout:** revoca el refresh de la cookie (si existe y es válido) y borra la cookie. Siempre `204`.
- **Cambio de contraseña:** revoca **todos** los refresh del usuario y borra la cookie; el frontend vuelve
  al login.

### 15.4 Autorización por ruta (`SecurityConfig`)

```
sessionManagement: STATELESS · csrf: disabled · formLogin / httpBasic / logout por defecto: disabled
oauth2ResourceServer: jwt (JwtConfig)
exceptionHandling: ProblemDetailAuthHandlers (401 / 403 en application/problem+json)

/actuator/health                         permitAll
/error                                   permitAll
POST /api/auth/login|refresh|logout      permitAll
PUT  /api/auth/password                  hasAnyRole(ADMIN, ARRENDATARIO)
/api/me/**                               hasRole(ARRENDATARIO)
/api/tenants/**                          hasRole(ADMIN)
anyRequest                               denyAll
```

CSRF se desactiva porque la API se autentica con `Authorization: Bearer` (no la envía el navegador
automáticamente). Los únicos endpoints que dependen de cookie (`refresh`, `logout`) quedan protegidos por
`SameSite=Strict` + `Path=/api/auth` + origen único sin CORS.

### 15.5 Alta de credenciales del arrendatario

1. El admin completa el formulario de registro (RF-01) con `username` e `initialPassword`.
2. `TenantController` → `TenantService.create(...)` (comando de servicio, no el DTO web), que en una
   transacción guarda el `Tenant` y llama a `UserAccountService.createTenantUser(tenant, username, rawPassword)`.
3. `UserAccountService` normaliza el username, comprueba unicidad (`UsernameTakenException` → `409`),
   codifica la contraseña y guarda `AppUser(role = ARRENDATARIO, tenant)`.
4. El admin comunica la contraseña al arrendatario por su canal habitual (fuera del sistema). Se le
   recomienda (texto en la UI) cambiarla en el primer ingreso. No se fuerza el cambio (YAGNI).

### 15.6 Admin inicial (bootstrap)

- `AdminBootstrapRunner` (`ApplicationRunner`) al arrancar: si **no existe ningún** `app_user` con rol
  ADMIN **y** están definidas `RENTALS_BOOTSTRAP_ADMIN_USERNAME` y `RENTALS_BOOTSTRAP_ADMIN_PASSWORD`, crea
  ese admin (contraseña validada con la política de §5.2; si no la cumple, la app no arranca). Registra en
  log solo "Admin inicial creado: <username>".
- Si ya existe un ADMIN, no hace nada (idempotente; nunca sobrescribe contraseñas).
- **Desarrollo:** las variables vienen del `.env` local (no versionado).
- **Producción (documentar en DEPLOY):** definir ambas variables en SSM para el primer arranque, iniciar,
  entrar y cambiar la contraseña con "Cambiar contraseña", y **eliminar** el parámetro de contraseña del
  entorno del servicio.
- El admin cambia su propia contraseña con el mismo `PUT /api/auth/password`.

### 15.7 Frontend: sesión, token y vista por rol

`stores/auth.js` (Pinia, sin persistencia):

```
state:   status: 'unknown' | 'anonymous' | 'authenticated'
         user:   { id, username, role, tenantId } | null
         accessToken: string | null            // solo memoria
         refreshPromise: Promise | null        // deduplica refrescos concurrentes
getters: isAdmin, isTenant
actions: restoreSession()   → POST /api/auth/refresh; ok → authenticated; 401 → anonymous
         login(u, p)        → POST /api/auth/login; guarda accessToken + user
         refreshAccessToken() → reutiliza refreshPromise si ya hay uno en curso
         logout()           → POST /api/auth/logout; clearSession()
         changePassword(cur, new) → PUT /api/auth/password; clearSession(); aviso "inicia sesión de nuevo"
         clearSession()     → borra token y user, status = 'anonymous', tenantsStore.reset()
```

`App.vue`:
```
onMounted → auth.restoreSession()
status 'unknown'                      → indicador de carga
status 'anonymous'                    → <LoginView>
status 'authenticated' + ADMIN        → <AppHeader> + <AdminView>
status 'authenticated' + ARRENDATARIO → <AppHeader> + <TenantView>   (carga GET /api/me/tenant)
```
`AppHeader`: nombre de usuario, "Cambiar contraseña" (abre `ChangePasswordForm`) y "Salir".

`api/http.js` (manejo de expiración, reactivo y sin temporizadores):
1. Si `auth` es `true` y hay token, añade `Authorization: Bearer <token>`.
2. Si la respuesta es `401` en una petición con `auth: true` y aún no se reintentó:
   `await refreshAccessToken()` → si funciona, **reintenta una vez** la petición original con el token nuevo.
3. Si el refresh falla → `onAuthFailure()` (= `clearSession()`) → `App.vue` muestra el login; se lanza
   `ApiError(401)`.
4. `403` no dispara refresh: se muestra con `ErrorBanner`.
5. Las peticiones a `/api/auth/login|refresh|logout` van con `auth: false` (nunca entran en el bucle).

Al recargar la página el token en memoria se pierde: `restoreSession()` lo recupera con la cookie de refresh
sin pedir credenciales de nuevo (mientras la cookie siga vigente).

`LoginView`: usuario, contraseña, botón "Ingresar"; error genérico "Usuario o contraseña incorrectos".
`TenantForm` (admin) añade los campos `username` e `initialPassword` (con mostrar/ocultar).
`TenantDetail` muestra el `username`. Diseño visual de estas piezas: `docs/DESIGN.md` (ui-designer).

### 15.8 Pruebas mínimas de seguridad (dev2 / dev1)

- `@WebMvcTest` + `spring-security-test` (`jwt()` con authorities): matriz de §15.4 → sin token `401`,
  ARRENDATARIO en `/api/tenants/**` `403`, ADMIN en `/api/me/tenant` `403`, casos permitidos `2xx`.
- `@SpringBootTest` + Testcontainers: login correcto/incorrecto (mismo `401`), refresh con rotación,
  reuso de refresh revocado → revoca todo, logout, cambio de contraseña (password actual incorrecta → `401`;
  luego el refresh viejo falla), `POST /api/tenants` con username duplicado → `409` y sin tenant creado,
  `AdminBootstrapRunner` idempotente, arranque falla sin `RENTALS_JWT_SECRET`.
- Frontend (Vitest): `http.js` reintenta una sola vez tras refresh; refrescos concurrentes comparten una
  promesa; refresh fallido llama `onAuthFailure`; `stores/auth` no escribe en `localStorage`.

### 15.9 Checklist de integración (tester)

Login ADMIN y ARRENDATARIO → vista correcta sin toggle · credenciales erróneas → mensaje genérico ·
arrendatario no ve a otros (ni por API: `GET /api/tenants` → `403`) · recargar página mantiene la sesión ·
tras 15 min de inactividad la siguiente acción funciona (refresh transparente; se puede probar con
`RENTALS_ACCESS_TOKEN_TTL=PT1M`) · "Salir" invalida la sesión (recargar → login) · cambio de contraseña
cierra sesión y la nueva funciona · alta de arrendatario con username repetido → error `409` visible ·
ningún token en `localStorage`/`sessionStorage` (DevTools) · cookie `rentals_refresh` es `HttpOnly`.

---

## Anexo A. Arquitectura v1 (histórica, web estática)

> Vigente hasta 2026-10-03. Se conserva como referencia; el código sigue en `src/` congelado (§10).

- **Stack:** HTML + CSS + JavaScript vanilla, sin build, abre con `file://`. Scripts clásicos con
  globals namespaced: `window.RentalsData`, `RentalsStore`, `RentalsCalc`, `RentalsRender`, `RentalsApp`.
- **Capas:** `data.js` (mock) → `store.js` (única puerta a los datos) → `calc.js` (lógica pura de deuda)
  / `render.js` (DOM sin lógica) → `app.js` (orquestador y toggle de modo).
- **Estructura:** `src/index.html`, `src/css/styles.css`, `src/js/{data,store,calc,render,app}.js`,
  `src/assets/fonts/`.
- **Modelo:** `Tenant { id, fullName, unit, contact, paymentDay (1..28), startDate "YYYY-MM-DD",
  monthlyAmount, oneTimeCharges[{id, description, amount, paid}], paidMonths["YYYY-MM"] }`.
  Meses adeudados y totales calculados, nunca almacenados (ADR-001).
- **Contrato de cálculo:** `RentalsCalc.monthsOwed(tenant, today)`, `totalOwed(tenant, today)`,
  `status(tenant, today) -> "AL_DIA" | "ATRASADO"`.
- **Estilos:** CSS propio con custom properties, fuente Inter local (ADR-002).
- **Fronteras:** dev1 = HTML/CSS/`render.js`/fuentes; dev2 = `data.js`/`store.js`/`calc.js`/`app.js`.
- **Despliegue:** contenido de `src/` a S3 privado + CloudFront con OAC (`docs/DEPLOY-AWS-MANUAL.md`).
