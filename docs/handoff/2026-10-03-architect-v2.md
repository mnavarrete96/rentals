# Traspaso: Arquitectura v2 · de architect · 2026-10-03

## Estado
- [x] Terminada  - [ ] A medias  - [ ] Bloqueada
(Arquitectura completa; quedan 3 decisiones abiertas del usuario que no bloquean el inicio del desarrollo.)

## Qué quedó hecho
- `docs/ARCHITECTURE.md` reescrito para v2 (v1 como Anexo A): estructura monorepo, capas backend,
  esquema PostgreSQL, API REST (§6, contrato dev1↔dev2), frontend Vue 3 + Pinia, reglas,
  estrategia de migración (§10), despliegue AWS propuesto (§11), riesgos y decisiones abiertas (§13).
- `docs/DECISIONS.md`: ADR-003 (Vue 3), ADR-004 (Spring Boot + Java 25, override de Java 8),
  ADR-005 (monorepo), ADR-006 (deuda solo en backend), ADR-007 (topología AWS, **Propuesta**).
  ADR-001 marcada como reemplazada por ADR-006 en v2; ADR-002 sigue vigente con nota v2.

## Qué falta
- Decisiones del usuario: **D-1** protección de la API antes de v3 (recomiendo HTTP Basic de un admin
  para escrituras si v2 va a Internet), **D-2** zona horaria (default `America/Santiago`), **D-3**
  confirmar ADR-007 (EC2 + RDS + CloudFront vs ECS Fargate).
- Líder: actualizar SPEC (RF-07 → "datos semilla en perfil dev"; §1, §4 "No incluye backend", §6 "sin
  peticiones de red", §7 restricciones v1; quitar "v2.5 React" de §9).
- Líder/usuario: opcionalmente anotar en `CLAUDE.md` que este proyecto usa Java 25 (ADR-004).
- Recomendar al usuario crear etiqueta local `v1-static` (sin push) antes de empezar (fase 0).

## Dónde continuar (el siguiente agente empieza aquí)
Líder: recalcular `docs/TASKS.md`. Propuesta de tareas (fronteras: dev1 = `frontend/`, dev2 = `backend/`
+ `compose.yaml` + `.env.example` + líneas Java de `.gitignore`; nadie toca `src/`):

| ID | Resp. | Tarea | Depende |
|---|---|---|---|
| B-01 | dev2 | Scaffold `backend/` (Gradle wrapper ≥9.1, Java 25 toolchain, Spring Boot, starters §4.1), `compose.yaml`, `.env.example`, `application.yml` sin secretos, `ClockConfig` | — |
| B-02 | dev2 | `V1__init_schema.sql` + entidades + `YearMonthConverter` + repositorios; `@DataJpaTest` con Testcontainers | B-01 |
| B-03 | dev2 | `DebtCalculatorService` + `DebtSummary` + pruebas de paridad con casos de `src/js/data.js` | B-02 (solo entidades) |
| B-04 | dev2 | Services + controllers + DTOs + `GlobalExceptionHandler` (ProblemDetail) según §6; `@WebMvcTest` | B-03 |
| B-05 | dev2 | Semilla `db/seed/V1000__dev_seed.sql` (perfil dev) | B-02 |
| F-01 | dev1 | Scaffold `frontend/` (Vite + Vue 3 + Pinia + Vitest), proxy `/api`, portar tokens CSS y fuentes desde `src/` (copiar, no mover) | — |
| F-02 | dev1 | `api/http.js`, `api/tenantsApi.js`, `stores/tenants.js`, `stores/ui.js` + tests de stores con API simulada | F-01 |
| F-03 | dev1 | Vista Admin (lista, detalle, marcar mes/cargo, formulario tenant + garantía, formulario cargo) | F-02 |
| F-04 | dev1 | Vista Arrendatario (selector, desglose, total, badge) + toggle en `App.vue` | F-02 |
| T-v2 | tester | Integración: RF-01 a RF-06 sobre v2 (frontend + backend + PostgreSQL local), checklist de T-05 | B-04, B-05, F-03, F-04 |
| R-v2 | architect / ui-designer | Revisión de entrega grande contra ARCHITECTURE y DESIGN | T-v2 |

B-xx y F-xx pueden ir en paralelo: el contrato §6 está cerrado. ui-designer podría revisar si
`DESIGN.md` necesita mapear componentes Vue (opcional).

## Archivos tocados
- `docs/ARCHITECTURE.md`
- `docs/DECISIONS.md`
- `docs/logs/architect.md`
- `docs/handoff/2026-10-03-architect-v2.md`

## Decisiones tomadas y por qué
- Monorepo `frontend/` + `backend/` hermanos: fronteras claras y memoria compartida (ADR-005).
- Java 25 por elección explícita del usuario sobre el default Java 8 (ADR-004).
- Regla de deuda solo en backend, pura, con `Clock` inyectable (ADR-006).
- IDs `BIGINT` identity; dinero `NUMERIC(10,2)`/`BigDecimal`; meses `CHAR(7)` `YYYY-MM` como
  `@ElementCollection` con PK compuesta (idempotente).
- Endpoints de mutación devuelven el `TenantDetailDto` actualizado (sin GET extra).
- Sin router, axios, TypeScript, Lombok, MapStruct ni Spring Security (hasta D-1): YAGNI.

## Problemas conocidos / riesgos
- API de escritura sin autenticación: **no exponer v2 en Internet hasta resolver D-1.**
- Versiones exactas de Spring Boot/Gradle para Java 25 se fijan en el scaffold (B-01); si hay
  incompatibilidad, usar el fallback de ARCHITECTURE §4.1 y avisar por handoff.
- Testcontainers requiere Docker local.

## Cómo verificar
- Leer `docs/ARCHITECTURE.md` §3, §5, §6 y §10; `docs/DECISIONS.md` ADR-003 a ADR-007.
