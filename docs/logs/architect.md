# Bitácora de architect

Entradas más recientes al final. Formato en `docs/templates/log-entry.md`.

---

## 2026-10-03 · Arquitectura v1 · Diseño inicial web estática de arriendos
- **Qué hice:** Leí `docs/SPEC.md` (APROBADO) y escribí la arquitectura v1 completa:
  estructura de archivos, modelo de datos JS, flujo de navegación Admin/Arrendatario,
  fronteras dev1/dev2 y riesgos. Registré 2 ADRs (almacenamiento y estrategia CSS).
- **Archivos tocados:** `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/logs/architect.md`.
- **Decisiones y motivo:**
  - Tres capas con dependencias hacia abstracciones (DIP): `data.js` → `store.js` →
    `calc.js`/`render.js` → `app.js`. Aísla la fuente de datos para que v2 (backend) solo
    cambie `store.js`.
  - Meses adeudados y totales **calculados, nunca almacenados** (fuente de verdad única).
    Mes pagado = clave `"YYYY-MM"` en `tenant.paidMonths`.
  - Scripts clásicos con globals namespaced (no ES modules) para funcionar bajo `file://`.
  - CSS propio con custom properties (ADR-002): cumple "sin CDN", sin build, look a medida.
  - Fronteras: dev1 = HTML/CSS/render/fonts; dev2 = data/store/calc/app. Contrato =
    firmas públicas de `store.js`, `calc.js`, `render.js`.
- **Pendiente / siguiente paso:** El líder recalcula `docs/TASKS.md` con estas fronteras.
  Abierto: (1) si `store.js` nace async-ready (lo cierra dev2); (2) paleta concreta y
  contraste AA (lo cierra dev1).
- **Nota de traspaso:** no requerida (tareas abiertas documentadas en ARCHITECTURE §7/§10 y ADRs).
