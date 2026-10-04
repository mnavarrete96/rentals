<script setup>
/**
 * AppHeader.vue — Cabecera sticky de sesión activa (F-05).
 *
 * Visible en AdminView y TenantView. No visible en LoginView.
 * Muestra el nombre de la app (izquierda) y, a la derecha:
 * - Nombre de usuario (solo lectura)
 * - Botón "Cambiar contraseña" → emite changePassword()
 * - Botón "Salir" → llama auth.logout()
 *
 * Diseño: DESIGN.md §5 "AppHeader — cabecera de sesión activa".
 */
import { useAuthStore } from '../../stores/auth.js'

const auth = useAuthStore()

const emit = defineEmits(['changePassword'])

async function handleLogout() {
  await auth.logout()
  // App.vue reaccionará al cambio de auth.status a 'anonymous' y mostrará LoginView.
}

function handleChangePassword() {
  emit('changePassword')
}
</script>

<template>
  <header class="app-header">
    <!-- Izquierda: nombre de la app -->
    <div class="app-header__logo">
      Gestión de Arriendos
    </div>

    <!-- Derecha: usuario + botones -->
    <div class="app-header__actions">
      <span class="app-header__username" :title="auth.user?.username">
        {{ auth.user?.username }}
      </span>

      <button
        type="button"
        class="btn-header"
        @click="handleChangePassword"
      >
        Cambiar contraseña
      </button>

      <button
        type="button"
        class="btn-header"
        @click="handleLogout"
      >
        Salir
      </button>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 56px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-xl);
}

/* Logo / nombre de la app */
.app-header__logo {
  font-family: var(--font-body);
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--color-accent);
  letter-spacing: -0.03em;
  white-space: nowrap;
}

/* Lado derecho */
.app-header__actions {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

/* Nombre de usuario (solo lectura) */
.app-header__username {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

/* Botones del header */
.btn-header {
  font-family: var(--font-body);
  font-size: 0.8125rem;
  font-weight: 500;
  padding: 6px 12px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text);
  cursor: pointer;
  transition: background var(--transition), border-color var(--transition);
  white-space: nowrap;
}

.btn-header:hover {
  background: var(--color-surface-alt);
  border-color: var(--color-border-hover);
}

.btn-header:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

/* Responsive: móvil < 640px */
@media (max-width: 639px) {
  .app-header {
    height: 48px;
    padding: 0 var(--space-md);
  }

  .app-header__logo {
    font-size: 1.125rem;
  }

  .app-header__username {
    max-width: 120px;
  }
}
</style>
