# Tablero de tareas

Estados: `TODO` · `EN CURSO` · `EN REVISIÓN` · `LISTO` · `BLOQUEADO`

Regla: cada archivo tiene un único responsable a la vez. La columna "Archivos propios" lo hace explícito
para que `dev1` y `dev2` no se pisen.

| ID | Descripción | Responsable | Archivos propios | Depende de | Estado | Nota de traspaso |
|---|---|---|---|---|---|---|
| T-01 | Capa de datos: `data.js` (mock 3 arrendatarios), `store.js` (acceso), `calc.js` (lógica deuda) | dev2 | `src/js/data.js`, `src/js/store.js`, `src/js/calc.js` | — | EN REVISIÓN | docs/handoff/T01-dev2.md |
| T-02 | Base visual: fuentes locales, `index.html` (estructura + contenedores), `styles.css` (paleta, layout, componentes) | dev1 | `src/index.html`, `src/css/styles.css`, `src/assets/fonts/` | — | LISTO | docs/handoff/T02-dev1.md |
| T-03 | Render: `render.js` — pinta lista de arrendatarios, detalle de pagos, vista arrendatario y formulario de registro | dev1 | `src/js/render.js` | T-01, T-02 | EN REVISIÓN | docs/handoff/T03-dev1.md |
| T-04 | Orquestador: `app.js` — arranque, toggle Admin/Arrendatario, eventos (marcar pagado, registrar arrendatario) | dev2 | `src/js/app.js` | T-01, T-03 | EN REVISIÓN | docs/handoff/T04-dev2.md |
| T-05 | Validación completa contra SPEC.md RF-01 a RF-07 | tester | — | T-03, T-04 | LISTO | docs/handoff/2026-10-03-dev1-T05.md |
| T-06 | Descargar fuente Inter localmente y reemplazar @import CDN por @font-face | dev1 | `src/assets/fonts/`, `src/css/styles.css` | T-02 | EN REVISIÓN | docs/handoff/2026-10-03-dev1-T06.md |
| T-07 | Verificación visual del nuevo CSS entregado por ui-designer (checklist de 6 puntos) | tester | — | T-06 | EN CURSO | — |
