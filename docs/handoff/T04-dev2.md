# Traspaso: T-04 · de dev2 · 2026-10-03

## Estado
- [x] Terminada  - [ ] A medias  - [ ] Bloqueada

## Qué quedó hecho
- `src/js/app.js`: objeto `window.RentalsApp` con la función pública `init()`.
  - Estado interno de la UI (`_state`) con `mode`, `selectedAdminId` y `selectedTenantId`.
  - Arranque: render inicial de lista, detalle, selector y estado de cuenta.
  - Toggle Admin/Arrendatario: muestra/oculta `#view-admin` y `#view-tenant` con `.view--visible`, actualiza `.btn-mode--active` y `aria-pressed`.
  - Selección de arrendatario en vista Admin: delegación en `#admin-tenant-list`, lee `data-id`.
  - Marcar mes pagado (RF-04): delegación en `#admin-tenant-detail`, botón `.btn-mark-paid` con `data-tenant-id` + `data-month`.
  - Marcar cargo único pagado: misma delegación, botón `.btn-mark-one-time-paid` con `data-tenant-id` + `data-charge-id`.
  - Selección de arrendatario en vista Tenant: delegación en `#tenant-selector`, botón `.tenant-selector-btn` con `data-id`.
  - Formulario de registro (RF-01): submit en `#admin-register-form`, genera id `"t-" + Date.now()`, llama `RentalsStore.addTenant(...)`, re-renderiza lista y selector, limpia el form.

## Qué falta
- T-05 (tester): validación completa contra SPEC.md RF-01..RF-07 abriendo `src/index.html` en el navegador.

## Dónde continuar (el siguiente agente empieza aquí)
- El tester puede comenzar T-05 directamente abriendo `src/index.html` en el navegador (doble clic o protocolo `file://`). No se requiere servidor.
- Si se detecta algún defecto en render.js o store.js, la nota de traspaso debe dirigirse a dev1 (render) o dev2 (store/calc) respectivamente.

## Archivos tocados
- `src/js/app.js` (creado)
- `docs/TASKS.md` (estado T-04 → EN REVISIÓN)
- `docs/logs/dev2.md` (bitácora)

## Decisiones tomadas y por qué

### Estado de la UI en `_state`
```js
var _state = {
  mode: "admin",           // "admin" | "tenant"
  selectedAdminId: null,   // id del arrendatario seleccionado en vista admin
  selectedTenantId: null   // id del arrendatario seleccionado en vista tenant
};
```
Los dos `selectedId` son independientes para que al volver de la vista Tenant a Admin se recuerde la selección admin, y viceversa.

### `_today()` centralizada
`_today()` devuelve `new Date()`. Si en T-05 el tester quiere inyectar una fecha fija (ej. `new Date(2026, 9, 3)`), basta con hacer un monkey-patch temporal: `RentalsApp._today = function(){ return myFixedDate; }`. La función no está expuesta en la API pública de producción pero puede ser sustituida en tests.

### Delegación de eventos vs listeners directos
render.js genera HTML dinámico con cada llamada a `renderTenantList`, `renderTenantDetail`, etc. Los elementos son nuevos en cada re-render, así que añadir listeners a ellos individualmente requeriría re-registrarlos en cada actualización. La delegación en el contenedor (que persiste en el DOM) evita este problema y es más eficiente.

### `e.target.closest(...)`
El HTML de render.js puede anidar nodos dentro del botón (texto, spans). `closest` sube por el árbol desde el elemento clickeado hasta encontrar el selector, evitando que el evento se pierda si el click cae en un hijo.

### Auto-arranque al final del IIFE
`init()` se llama directamente dentro del IIFE porque los scripts están al final de `<body>`. No se necesita escuchar `DOMContentLoaded`. Si en el futuro los scripts se mueven al `<head>`, hay que envolver `init()` en un listener de `DOMContentLoaded`.

## Problemas conocidos / riesgos
- Si el arrendatario seleccionado en Admin es luego marcado en Tenant (vista Tenant), al volver a Admin el detalle mostrará el estado actualizado automáticamente porque ambas vistas leen de la misma instancia de `RentalsStore`.
- El formulario de registro no valida el formato de `startDate` más allá del input `type="date"` del HTML. Si el navegador no soporta `type="date"`, el usuario puede escribir una cadena con formato incorrecto. En v2, añadir validación explícita de formato "YYYY-MM-DD".
- `Date.now()` como sufijo del id del nuevo arrendatario garantiza unicidad solo si no se registran dos arrendatarios exactamente en el mismo milisegundo (prácticamente imposible en uso manual). En v2 con backend, el id lo asigna el servidor.

## Cómo verificar
1. Abrir `src/index.html` en el navegador (doble clic / `file://`).
2. La lista de arrendatarios debe mostrar 3 tarjetas al cargar.
3. Hacer clic en "Carlos Ramos" (ATRASADO): el detalle debe mostrar "2026-09" y "2026-10" con botones "Marcar pagado".
4. Hacer clic en "✓ Marcar pagado" en "2026-09": el botón desaparece, solo queda "2026-10".
5. Hacer clic en el toggle "Arrendatario": la vista admin desaparece, aparece el selector.
6. Hacer clic en "Carlos Ramos" en el selector: el estado de cuenta muestra el mes restante y el total actualizado.
7. Volver a Admin, rellenar el formulario de registro y hacer clic en "Registrar arrendatario": la lista debe tener 4 tarjetas y el formulario quedar vacío.
8. El arrendatario recién registrado debe aparecer también en el selector de la vista Tenant.
