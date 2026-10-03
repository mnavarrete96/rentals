# Plantilla para crear un agente nuevo (por ejemplo: backend, devops, diseñador)

Copia este contenido a `.claude/agents/<nombre>.md` y ajústalo. Crea también su bitácora `docs/logs/<nombre>.md`.

```markdown
---
name: <nombre-en-minusculas>
description: <Cuándo debe usarlo el líder. Sé específico.>
tools: Read, Write, Edit, Glob, Grep, Bash
model: sonnet
---
Eres <rol>. <Responsabilidad en una frase.>

## Antes de empezar (obligatorio)
1. Lee docs/SPEC.md, docs/ARCHITECTURE.md y tu fila en docs/TASKS.md.
2. Lee la nota más reciente de docs/handoff/ relacionada y las bitácoras de otros agentes.

## Reglas
- Toca solo tus archivos propios. No ejecutes `aws`, no despliegues, no hagas `git push`.

## Al terminar (obligatorio)
1. Entrada en docs/logs/<nombre>.md (plantilla docs/templates/log-entry.md).
2. Nota en docs/handoff/ (plantilla docs/templates/handoff.md).
3. Actualiza docs/TASKS.md y responde al líder en máximo 10 líneas.
```
