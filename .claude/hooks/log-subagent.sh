#!/usr/bin/env bash
# Bitácora global automática: registra el inicio y el fin de cada subagente.
# Uso: log-subagent.sh <inicio|fin>
# Claude Code envía un JSON por stdin; si es posible se extrae el nombre del agente.
# Siempre termina con código 0 para no bloquear el flujo.
set -u

EVENTO="${1:-evento}"
RAIZ="${CLAUDE_PROJECT_DIR:-$(pwd)}"
LOG="$RAIZ/docs/logs/global.log"
mkdir -p "$RAIZ/docs/logs"

ENTRADA="$(cat 2>/dev/null || true)"
AGENTE="desconocido"

if [ -n "$ENTRADA" ]; then
  if command -v jq >/dev/null 2>&1; then
    AGENTE="$(printf '%s' "$ENTRADA" | jq -r '.agent_type // .subagent_type // .agent_name // "desconocido"' 2>/dev/null)"
  elif command -v python3 >/dev/null 2>&1; then
    AGENTE="$(printf '%s' "$ENTRADA" | python3 -c 'import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get("agent_type") or d.get("subagent_type") or d.get("agent_name") or "desconocido")
except Exception:
    print("desconocido")')"
  fi
fi

printf '%s | %-6s | %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$EVENTO" "${AGENTE:-desconocido}" >> "$LOG"
exit 0
