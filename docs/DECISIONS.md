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
- **Estado:** Aceptada
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
