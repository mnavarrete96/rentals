# Especificación del proyecto

> **Estado: APROBADO** — 2026-10-03 por el usuario.

## 1. Resumen

Aplicación web estática de gestión de arriendo residencial. Permite al administrador registrar arrendatarios, anotar pagos únicos y recurrentes, y marcar meses como pagados. Los arrendatarios pueden consultar su estado de cuenta (meses adeudados y monto total).

## 2. Objetivo

Centralizar el control de cobros de arriendo sin depender de planillas ni papel. La versión inicial es un prototipo estático (datos hardcodeados) que valida la experiencia de uso; está diseñada para escalar a una versión con backend, login real y notificaciones automáticas.

## 3. Usuarios / público

| Rol | Descripción |
|---|---|
| Administrador | Propietario o gestor del inmueble. Registra arrendatarios, pagos y marca cuotas como pagadas. |
| Arrendatario | Inquilino del inmueble. Consulta su estado de cuenta (meses adeudados, total a pagar). |

Cantidad esperada de arrendatarios: **5 a 20**.

## 4. Alcance

**Incluye (v1 estática):**
- Modo Administrador y Modo Arrendatario en la misma página, toggled manualmente (sin login real).
- Registro de arrendatario con: nombre completo, número de unidad, teléfono/correo, día de pago mensual, fecha de inicio del arriendo, monto mensual (recurrente) y garantía (pago único).
- Soporte para múltiples pagos únicos por arrendatario con descripción libre (ej: garantía, daños al departamento, etc.).
- Cálculo automático de meses adeudados desde la fecha de inicio hasta hoy.
- Vista del administrador: lista de arrendatarios, detalle de pagos, acción "marcar mes como pagado".
- Vista del arrendatario: selector de nombre → desglose "Debes los meses X, Y — total $ZZZ USD".
- Diseño sobrio y profesional (definido por el equipo de diseño/dev).
- Moneda: USD en todos los montos.

**No incluye (reservado para versiones futuras):**
- Backend / base de datos persistente.
- Login con credenciales individuales por arrendatario.
- Envío de notificaciones por WhatsApp o correo electrónico.
- Carga de foto de transferencia con interpretación por LLM.
- Panel multipropiedad (múltiples inmuebles).

## 5. Requisitos funcionales

| ID | Requisito | Criterio de aceptación |
|---|---|---|
| RF-01 | El administrador puede registrar un nuevo arrendatario con todos sus datos base. | El formulario acepta: nombre, unidad, teléfono/correo, día de pago, fecha de inicio, monto mensual. Al guardar, el arrendatario aparece en la lista. |
| RF-02 | El administrador puede registrar un pago único (con descripción) para un arrendatario. | Se puede añadir N pagos únicos por arrendatario, cada uno con monto y descripción (ej: "Garantía", "Reparación ventana"). |
| RF-03 | El sistema calcula automáticamente los meses adeudados. | Dado el día de inicio y los meses ya marcados como pagados, el sistema muestra los meses pendientes desde el inicio hasta la fecha actual. |
| RF-04 | El administrador puede marcar un mes como pagado. | Al marcar, ese mes desaparece de la lista de deuda del arrendatario. |
| RF-05 | El arrendatario puede seleccionar su nombre en un selector y ver su estado de cuenta. | Se muestra: meses adeudados listados individualmente, total en USD y estado ("Al día" o "Atrasado"). |
| RF-06 | El modo Admin y el modo Arrendatario son accesibles desde la misma página mediante un toggle visible. | Un botón o tab permite cambiar entre ambas vistas sin recargar la página. |
| RF-07 | Los datos de prueba están hardcodeados en el JS. | Al abrir el HTML en un navegador (sin servidor), la aplicación funciona con datos precargados. |

## 6. Requisitos no funcionales

- **Compatibilidad:** Funciona en los últimos 2 años de Chrome, Safari y Firefox. Responsive (desktop + móvil).
- **Rendimiento:** Carga en menos de 2 segundos en conexión normal (es estático, sin peticiones de red).
- **Diseño:** Sobrio y profesional; no minimalista en exceso. Paleta y tipografía definidas por dev1 según brief de diseño.
- **Accesibilidad:** Contraste mínimo WCAG AA en textos principales.
- **Sin dependencias de red en v1:** No debe requerir CDNs externos en producción (incluir librerías localmente si se usan).

## 7. Restricciones

- Tecnología: HTML + CSS + JavaScript vanilla (o con una librería ligera si el architect lo justifica). Sin framework pesado (React/Vue/Angular) en v1.
- Sin backend: los datos viven en el JS. Ningún agente ejecuta comandos `aws` ni hace `git push`.
- Nunca se escriben secretos en el repositorio.
- El despliegue en AWS lo realiza el usuario manualmente (ver `docs/DEPLOY-AWS-MANUAL.md`).

## 8. Notas de diseño (brief para dev)

- Estilo: profesional, sobrio. Inspiración: dashboards de finanzas personales (no corporativo frío, no colorido informal).
- Paleta sugerida: fondos oscuros o neutros cálidos, acentos en un color único (azul pizarra, verde salvia u ámbar), texto con buen contraste.
- Tipografía: sans-serif moderna (Inter, DM Sans o similar desde Google Fonts — incluirla localmente).
- Componentes clave: tarjetas de arrendatario, badge de estado (Al día / Atrasado), tabla de meses con checkmarks, formulario limpio de registro.

## 9. Roadmap de versiones futuras (referencia)

| Versión | Hito |
|---|---|
| v2 | Backend + base de datos; datos persistentes. Migración de `store.js` a API REST/fetch. |
| v2.5 | Migración del frontend a React (reemplaza `render.js`; `calc.js` y el modelo de datos se reutilizan). |
| v3 | Login individual por arrendatario. |
| v4 | Notificaciones automáticas por WhatsApp / correo. |
| v5 | Carga de comprobante de pago + extracción de monto con LLM. |

## 10. Historial de cambios

| Fecha | Cambio | Motivo | Aprobado por |
|---|---|---|---|
| 2026-10-03 | Versión inicial | Entrevista con el usuario (2 rondas de preguntas) | Usuario |
| 2026-10-03 | Cambio de alcance v1→v2: migración a stack dinámico | Usuario decide avanzar al roadmap v2 con backend real y framework frontend | Usuario |

## 11. Alcance v2 (aprobado 2026-10-03)

**Stack elegido por el usuario (decisiones confirmadas 2026-10-03):**
- Frontend: **Vue 3** (Vite, Pinia, fetch nativo, sin TypeScript en v2)
- Backend: **Spring Boot LTS + Java 25 + Gradle** (ver ADR-004 — override explícito del default Java 8)
- Base de datos: **PostgreSQL** (esquema gestionado con Flyway)
- Autenticación: **JWT** con dos roles (`ADMIN` y `ARRENDATARIO`). El admin se crea en seed. Al registrar un arrendatario el admin define su usuario y contraseña inicial. El arrendatario puede cambiarla después.
- Zona horaria del negocio: **America/Guayaquil**
- Despliegue AWS: **CloudFront + S3 (frontend) + EC2 (backend jar) + RDS (PostgreSQL privado)** (ver ADR-007)
- Política de despliegue: AWS manual (sin cambios)

**Qué cambia respecto a v1:**
- `store.js` se reemplaza por llamadas a API REST (fetch/axios desde Vue).
- `data.js` se elimina; los datos viven en PostgreSQL.
- `render.js` e `index.html` vanilla se reemplazan por componentes Vue.
- `calc.js` puede reutilizarse o portarse al backend (a definir por el architect).
- Se agrega un servidor Spring Boot con endpoints REST.
- Se requiere build de frontend (Vite + Vue 3).

**Lo que permanece igual:**
- Modelo de datos conceptual (Tenant, paidMonths, oneTimeCharges).
- Lógica de negocio de `calc.js` (portarse al backend como servicio).
- Política de despliegue manual en AWS.
- Nunca se escriben secretos en el repositorio.
