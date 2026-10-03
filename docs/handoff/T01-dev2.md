# Traspaso: T-01 · de dev2 · 2026-10-03

## Estado
- [x] Terminada  - [ ] A medias  - [ ] Bloqueada

## Qué quedó hecho
- `src/js/data.js`: array `window.RentalsData` con 3 arrendatarios de ejemplo.
- `src/js/store.js`: objeto `window.RentalsStore` con todas las funciones de acceso y mutación en memoria.
- `src/js/calc.js`: objeto `window.RentalsCalc` con las funciones puras `monthsOwed`, `totalOwed` y `status`.

## Qué falta
- `src/js/app.js` (T-04, también de dev2, pendiente de que T-03 esté lista).
- `src/js/render.js` (T-03, de dev1).
- El tester debe validar `calc.js` con fechas fijas.

## Dónde continuar (el siguiente agente empieza aquí)
- dev1 puede implementar T-03 (`render.js`) consumiendo los contratos descritos abajo.
- dev2 retoma T-04 (`app.js`) una vez que T-03 esté en EN REVISIÓN.
- El tester puede ya escribir pruebas unitarias para `calc.js` y `store.js` (no dependen del DOM).

## Archivos tocados
- `src/js/data.js` (creado)
- `src/js/store.js` (creado)
- `src/js/calc.js` (creado)
- `docs/TASKS.md` (estado T-01 actualizado)
- `docs/logs/dev2.md` (bitácora)

## Contratos públicos disponibles para dev1 y tester

### window.RentalsData
Array de objetos Tenant (ver modelo en ARCHITECTURE.md §5).
Tres arrendatarios precargados:
- `t-001` "María Pérez" — AL_DIA (meses 2026-07 a 2026-10 pagados, garantía pagada).
- `t-002` "Carlos Ramos" — ATRASADO 2 meses (2026-09 y 2026-10 sin pagar).
- `t-003` "Ana Torres" — ATRASADO por cargo único pendiente (meses recurrentes al día, "Reparación ventana" $120 impaga).

### window.RentalsStore
```
RentalsStore.getAllTenants()                      -> Tenant[]
RentalsStore.getTenantById(id)                   -> Tenant | undefined
RentalsStore.markMonthPaid(tenantId, monthKey)   -> void   // monthKey = "YYYY-MM"
RentalsStore.markMonthUnpaid(tenantId, monthKey) -> void
RentalsStore.markOneTimePaid(tenantId, chargeId) -> void
RentalsStore.addTenant(tenantData)               -> void
RentalsStore.addOneTimeCharge(tenantId, charge)  -> void
```
Trabaja sobre una copia en memoria. No toca el DOM.

### window.RentalsCalc
```
RentalsCalc.monthsOwed(tenant, today) -> string[]          // ["2026-09","2026-10"]
RentalsCalc.totalOwed(tenant, today)  -> number            // USD
RentalsCalc.status(tenant, today)     -> "AL_DIA" | "ATRASADO"
```
`today` es un objeto Date. Funciones puras: sin DOM, sin store, testables con fechas fijas.

### Orden de carga de scripts en index.html (ya definido en ARCHITECTURE.md §4)
`data.js` → `store.js` → `calc.js` → `render.js` → `app.js`

## Decisiones tomadas y por qué
- Parseo de `startDate` con `.split("-")` + `parseInt` (no `new Date("YYYY-MM-DD")`) para evitar el desplazamiento UTC en zonas horarias negativas.
- `store.js` usa copia profunda vía `JSON.parse(JSON.stringify(...))` para aislar el mock de las mutaciones en memoria.
- ES5 / `var` / IIFE en los tres archivos para compatibilidad bajo `file://` sin transpilador ni servidor.
- Datos mock con fechas 2026-06 a 2026-10 para que los casos sean fácilmente verificables con la fecha del proyecto (2026-10-03).

## Problemas conocidos / riesgos
- Si el tester crea un `new Date("2026-10-03")` sin componente de hora, puede parsearse como UTC medianoche, lo que en zona horaria negativa daría 2026-10-02 local. Recomendación: en los tests construir la fecha con `new Date(2026, 9, 3)` (mes 0-based) para usar hora local.
- `store.js` no persiste entre recargas (es el comportamiento esperado en v1; v2 usará fetch/API).

## Cómo verificar
1. Abrir la consola del navegador con `src/index.html` (o con un HTML mínimo que cargue los tres scripts en orden).
2. Ejecutar:
   ```js
   // Verificar datos mock
   RentalsData.length;                         // 3
   RentalsStore.getAllTenants().length;         // 3

   // Al día: t-001
   var today = new Date(2026, 9, 3);           // 2026-10-03 local
   RentalsCalc.status(RentalsStore.getTenantById("t-001"), today);  // "AL_DIA"
   RentalsCalc.monthsOwed(RentalsStore.getTenantById("t-001"), today); // []

   // Atrasado 2 meses: t-002
   RentalsCalc.status(RentalsStore.getTenantById("t-002"), today);  // "ATRASADO"
   RentalsCalc.monthsOwed(RentalsStore.getTenantById("t-002"), today); // ["2026-09","2026-10"]
   RentalsCalc.totalOwed(RentalsStore.getTenantById("t-002"), today);  // 1000

   // Atrasado por cargo único: t-003
   RentalsCalc.status(RentalsStore.getTenantById("t-003"), today);  // "ATRASADO"
   RentalsCalc.monthsOwed(RentalsStore.getTenantById("t-003"), today); // []
   RentalsCalc.totalOwed(RentalsStore.getTenantById("t-003"), today);  // 120

   // Marcar mes pagado y verificar
   RentalsStore.markMonthPaid("t-002", "2026-09");
   RentalsCalc.monthsOwed(RentalsStore.getTenantById("t-002"), today); // ["2026-10"]
   ```
