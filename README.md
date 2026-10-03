# Equipo de agentes para Claude Code (agnóstico al proyecto)

Equipo: **líder** (la sesión principal) + `architect` + `ui-designer` + `dev1` + `dev2` + `tester`,
con memoria compartida en archivos para que un agente continúe lo que dejó otro.

## Cómo empezar

1. En la carpeta del proyecto, inicia git (recomendado desde el minuto cero):
   ```bash
   git init
   git add -A
   git commit -m "Arnés inicial del equipo de agentes"
   ```
2. Abre Claude Code en esta carpeta:
   ```bash
   claude
   ```
3. Verifica que todo cargó:
   - `/agents` → deben aparecer `architect`, `ui-designer`, `dev1`, `dev2` y `tester`.
   - `/hooks` → deben aparecer los hooks `SubagentStart` y `SubagentStop`.
4. Primer mensaje sugerido:
   > Eres el líder del equipo definido en CLAUDE.md. Quiero construir <describe tu idea>.
   > Empieza entrevistándome para escribir docs/SPEC.md.

## Cómo ver qué está pasando

- En una segunda terminal: `tail -f docs/logs/global.log` (inicio y fin de cada agente, automático).
- Bitácoras por agente: `docs/logs/<agente>.md`.
- Estado de las tareas: `docs/TASKS.md`.
- Por dónde va cada trabajo: `docs/handoff/` (la nota más reciente).
- Historial de cambios: `git log`.

## Cómo evoluciona el proyecto (por ejemplo, de estático a dinámico)

El equipo no cambia. Cambia lo que está en los documentos:
1. Dile al líder el nuevo objetivo. Actualiza `docs/SPEC.md` (con historial de cambios).
2. El `architect` revisa `docs/ARCHITECTURE.md` y registra la decisión en `docs/DECISIONS.md`.
3. El líder recalcula `docs/TASKS.md` y reparte el trabajo.
4. Si hace falta un rol nuevo (backend, devops), usa `docs/templates/agent-template.md`.

## Probar que el traspaso funciona

Pide al líder: "dev1 empieza la tarea T-01 y se detiene a la mitad; después dev2 la continúa leyendo
solo la nota de traspaso". Si dev2 retoma sin que le expliques nada, el arnés funciona.

## Seguridad

- `.claude/settings.json` bloquea `aws ...`, `git push ...` y la lectura de `.env` y `~/.aws`.
  El despliegue lo haces tú con `docs/DEPLOY-AWS-MANUAL.md`.
- Nunca pegues claves de AWS en el chat ni en archivos del repositorio.

## Estructura

```
CLAUDE.md                 <- instrucciones del líder
.claude/agents/           <- architect, ui-designer, dev1, dev2, tester
.claude/hooks/            <- bitácora global automática
.claude/settings.json     <- permisos y hooks
docs/                     <- SPEC, ARCHITECTURE, DESIGN, DECISIONS, TASKS, logs, handoff, templates, manual AWS
src/                      <- el producto
tests/                    <- pruebas del tester
```
