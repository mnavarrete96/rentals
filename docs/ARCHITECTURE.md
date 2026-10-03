# Arquitectura

> **Estado: VIGENTE (v1).** Lo mantiene el `architect`. Cada decisión relevante se registra en `docs/DECISIONS.md`.
> Última actualización: 2026-10-03.

## 1. Contexto

Web **estática** de gestión de arriendos residenciales (ver `docs/SPEC.md`, APROBADO).
Lo que condiciona el diseño:

- Sin backend ni base de datos. Datos **hardcodeados** en un archivo JS (mock).
- HTML + CSS + **JavaScript vanilla**. Prohibido React/Vue/Angular. Sin dependencias de red en producción.
- Dos modos en la misma página: **Administrador** y **Arrendatario**, alternables por un toggle (sin login real).
- Escala esperada: 5 a 20 arrendatarios. Moneda: **USD**.
- Debe abrir con doble clic sobre el `index.html` (protocolo `file://`), sin servidor.
- Diseñada para **escalar a v2 con backend**: la capa de datos debe estar aislada.

## 2. Vista general

Aplicación de una sola página (SPA artesanal, sin framework). Tres capas claras:

```
┌───────────────────────────────────────────────┐
│  index.html  (estructura + contenedores)       │
│                                                 │
│  ┌─────────────┐   ┌──────────────────────┐    │
│  │  Vista Admin │   │  Vista Arrendatario  │    │  <- UI (render.js)
│  └─────────────┘   └──────────────────────┘    │
│          ▲                     ▲                 │
│          └────────┬────────────┘                │
│                   │                              │
│          app.js (orquestador / router de modo)   │  <- Controlador
│                   │                              │
│       ┌───────────┴───────────┐                  │
│       │  calc.js (lógica deuda) │                 │  <- Dominio (puro)
│       └───────────┬───────────┘                  │
│                   │                              │
│          store.js (acceso a datos)               │  <- Capa de datos (abstracción)
│                   │                              │
│          data.js (mock hardcodeado)              │  <- Fuente de datos v1
└───────────────────────────────────────────────┘
```

Regla de dependencias (SOLID / DIP): las flechas apuntan hacia abajo. `calc.js` **no**
conoce el DOM; `render.js` **no** calcula deudas; nadie accede a `data.js` directamente,
siempre a través de `store.js`. Así, en v2, solo `store.js` cambia (de mock a fetch/API).

## 3. Componentes y responsabilidades

| Componente | Responsabilidad única | Archivos / carpeta |
|---|---|---|
| Estructura HTML | Contenedores de ambas vistas, toggle, montaje de scripts | `src/index.html` |
| Estilos | Paleta, tipografía, layout, componentes visuales | `src/css/styles.css` |
| Datos mock | Arrendatarios y pagos precargados (hardcoded) | `src/js/data.js` |
| Capa de datos (store) | Única puerta de acceso/consulta/mutación de datos en memoria | `src/js/store.js` |
| Dominio / cálculo | Lógica pura: meses adeudados, totales, estado al día/atrasado | `src/js/calc.js` |
| Render / UI | Pintar listas, detalle, formularios y estado en el DOM | `src/js/render.js` |
| Orquestador | Arranque, cambio de modo, conexión eventos ↔ store ↔ render | `src/js/app.js` |
| Fuentes | Tipografía embebida localmente (Inter o DM Sans) | `src/assets/fonts/` |

> `calc.js` y `store.js` son **independientes del DOM** y testeables de forma aislada por el `tester`.

## 4. Estructura de carpetas

```
rentals/
├── src/
│   ├── index.html            <- única página; monta ambas vistas y los scripts
│   ├── css/
│   │   └── styles.css        <- todos los estilos (CSS propio, sin framework)
│   ├── js/
│   │   ├── data.js           <- MOCK: arreglo de arrendatarios hardcodeado
│   │   ├── store.js          <- acceso a datos (abstracción; v2 cambia aquí)
│   │   ├── calc.js           <- lógica pura de deuda (sin DOM)
│   │   ├── render.js         <- pintado del DOM (sin lógica de negocio)
│   │   └── app.js            <- orquestador / toggle de modo / eventos
│   └── assets/
│       └── fonts/            <- tipografía .woff2 embebida localmente
├── tests/                    <- pruebas (las gestiona el tester)
└── docs/                     <- documentación y memoria del equipo
```

Orden de carga de scripts en `index.html` (de dependencia a consumidor):
`data.js` → `store.js` → `calc.js` → `render.js` → `app.js`.
Se cargan como scripts clásicos (no ES modules) para que funcione bajo `file://` sin servidor;
cada archivo expone un único objeto global namespaced (`window.RentalsData`, `RentalsStore`,
`RentalsCalc`, `RentalsRender`, `RentalsApp`).

## 5. Modelo de datos

Fuente única en `data.js`. Un arrendatario:

```js
// Tenant
{
  id: "t-001",                 // string, único y estable
  fullName: "María Pérez",     // string
  unit: "Depto 302",           // string
  contact: "maria@mail.com",   // string (teléfono o correo)
  paymentDay: 5,               // number 1..28 (día de pago mensual)
  startDate: "2025-01-01",     // string ISO "YYYY-MM-DD" (inicio del arriendo)
  monthlyAmount: 450,          // number, USD (pago recurrente mensual)

  // Pagos únicos (N por arrendatario): garantía, daños, etc.
  oneTimeCharges: [
    {
      id: "c-001",             // string, único
      description: "Garantía", // string libre
      amount: 450,             // number, USD
      paid: false              // boolean
    }
  ],

  // Meses de arriendo marcados como pagados. Clave de mes "YYYY-MM".
  paidMonths: ["2025-01", "2025-02"]  // string[] de meses pagados
}
```

Decisiones del modelo:

- **Mes pagado = clave `"YYYY-MM"` dentro de `paidMonths`.** Marcar/desmarcar un mes es
  añadir/quitar de este arreglo. Simple, serializable y directo de migrar a tabla en v2.
- **Meses adeudados = calculados, nunca almacenados.** `calc.js` genera la lista de meses
  esperados desde `startDate` hasta el mes actual y resta `paidMonths`. Evita datos
  derivados desincronizados (fuente de verdad única).
- **Pagos recurrentes** se modelan con `monthlyAmount` + los meses esperados; no se guarda
  una fila por mes, se derivan. **Pagos únicos** sí son objetos explícitos en `oneTimeCharges`
  porque son irregulares y llevan descripción.
- `paymentDay` acotado a 1..28 para evitar meses sin ese día (febrero). Afecta solo a la
  presentación/fecha de vencimiento, no al conteo de meses.

Forma del cálculo que expone `calc.js` (contrato para dev y tester):

```js
RentalsCalc.monthsOwed(tenant, today) -> ["2025-03", "2025-04"]   // meses pendientes
RentalsCalc.totalOwed(tenant, today)  -> 1350                       // USD: meses + cargos únicos impagos
RentalsCalc.status(tenant, today)     -> "AL_DIA" | "ATRASADO"
```

## 6. Flujo de navegación entre modos

Una sola página. El estado de UI tiene un campo `mode` (`"admin"` | `"tenant"`).

```
            [ Toggle Admin / Arrendatario ]
                 │                  │
       mode="admin"           mode="tenant"
                 ▼                  ▼
  ┌────────────────────┐   ┌─────────────────────────┐
  │ VISTA ADMIN        │   │ VISTA ARRENDATARIO      │
  │ - Lista tarjetas   │   │ - Selector de nombre    │
  │ - Detalle pagos    │   │ - Desglose meses        │
  │ - Form registro    │   │   adeudados + total USD │
  │ - "Marcar pagado"  │   │ - Badge Al día/Atrasado │
  └────────────────────┘   └─────────────────────────┘
```

- El toggle **no recarga** la página (RF-06): `app.js` cambia `mode`, muestra/oculta el
  contenedor correspondiente y pide a `render.js` repintar.
- Ambas vistas leen del **mismo** `store.js`. Una acción del admin (marcar mes pagado)
  se refleja al instante en la vista del arrendatario, porque comparten los datos en memoria.
- Sin router de URL en v1 (YAGNI). Si en v2 se requiere deep-linking, se añade hash routing
  sin tocar la lógica de dominio.

## 7. Reglas para los desarrolladores

- **Fronteras estrictas** (para que dev1 y dev2 no se pisen):
  - **dev1 (UI/presentación):** `src/index.html`, `src/css/styles.css`, `src/js/render.js`,
    `src/assets/fonts/`.
  - **dev2 (datos/lógica):** `src/js/data.js`, `src/js/store.js`, `src/js/calc.js`, `src/js/app.js`.
  - El contrato entre ambos son las funciones públicas de `store.js`, `calc.js` y `render.js`
    (sección 5). Si cambia una firma, se avisa por `docs/handoff/` antes de romperla.
- **Sin frameworks ni CDNs externos.** Librerías (si las hubiera) siempre embebidas en `src/assets/`.
- **`calc.js` y `store.js` sin DOM**: solo funciones y datos. Nada de `document.*` ahí.
- **`render.js` sin lógica de negocio**: recibe datos ya calculados y los pinta.
- Nombres: funciones en `camelCase`, objeto global por archivo en `PascalCase` con prefijo `Rentals`.
- Fechas siempre en ISO `"YYYY-MM-DD"` (y meses `"YYYY-MM"`). Sin librerías de fechas en v1.
- Nada de secretos en el repositorio. Nadie ejecuta `aws` ni `git push`.

## 8. Despliegue

Estático. El usuario sube el contenido de `src/` a su hosting (S3 + CloudFront u otro) de
forma **manual** siguiendo `docs/DEPLOY-AWS-MANUAL.md`. No se requiere build: los archivos
se sirven tal cual. Como todo es relativo y sin ES modules, también funciona abriendo
`src/index.html` localmente con `file://`.

## 9. Riesgos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Cálculo de meses mal hecho (bordes de mes, zona horaria) | Deuda incorrecta mostrada | `calc.js` puro + pruebas del `tester` con fechas fijas; usar comparaciones por `"YYYY-MM"`, no por días. |
| dev1 y dev2 tocan el mismo archivo | Conflictos / retrabajo | Fronteras de la sección 7; contrato de funciones estable. |
| Datos derivados almacenados y desincronizados | Inconsistencias | Meses adeudados y totales **siempre calculados**, nunca persistidos. |
| ES modules fallan bajo `file://` (CORS) | App no abre con doble clic | Scripts clásicos con globals namespaced (sección 4). |
| Acople de UI a estructura de datos | v2 (backend) obliga a reescribir UI | Acceso solo vía `store.js`; UI consume contratos, no el mock directo. |
| Tipografía por CDN rompe "sin red" | Incumple RNF | Fuentes `.woff2` embebidas en `src/assets/fonts/`. |

## 10. Evolución prevista

- **v2 (backend + persistencia):** cambia **solo** `store.js` (de leer `data.js` a `fetch`/API
  asíncrono). `calc.js`, `render.js` e `index.html` permanecen. Para absorber la asincronía,
  `store.js` puede exponer su API ya pensada para devolver promesas si conviene.
- **v3 (login por arrendatario):** se sustituye el toggle manual por autenticación; la vista
  Arrendatario deja de usar el selector de nombre y recibe el tenant de la sesión.
- **v4/v5 (notificaciones, OCR de comprobantes):** son servicios externos; no afectan al núcleo
  de dominio, se enganchan en la capa de datos/controlador.
- **LocalStorage (opcional, no requerido v1):** si se quisiera persistir en el navegador antes
  del backend, es un cambio contenido en `store.js` sin tocar el resto.
