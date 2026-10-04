<script setup>
/**
 * App.vue — Orquestador principal (F-05).
 *
 * Restaura la sesión al montar con auth.restoreSession().
 * Elige la vista según el estado de autenticación y el rol del usuario:
 *   - status = 'unknown'             → indicador de carga (sin flash de contenido)
 *   - status = 'anonymous'           → LoginView
 *   - status = 'authenticated' + ADMIN        → AdminView
 *   - status = 'authenticated' + ARRENDATARIO → TenantView
 *
 * AppHeader y ChangePasswordForm se incluyen dentro de AdminView y TenantView
 * para mantener la separación de responsabilidades y el contexto de cada vista.
 *
 * §15.7 ARCHITECTURE.md.
 */
import { onMounted } from 'vue'
import { useAuthStore } from './stores/auth.js'
import LoginView from './views/LoginView.vue'
import AdminView from './views/AdminView.vue'
import TenantView from './views/TenantView.vue'

const auth = useAuthStore()

onMounted(async () => {
  await auth.restoreSession()
})
</script>

<template>
  <!-- Estado inicial: restaurando sesión (sin header, sin flash de login) -->
  <div
    v-if="auth.status === 'unknown'"
    class="app-loading"
    role="status"
    aria-label="Cargando aplicación"
  >
    <p class="app-loading__text">Cargando…</p>
  </div>

  <!-- No autenticado: mostrar login -->
  <LoginView v-else-if="auth.status === 'anonymous'" />

  <!-- Autenticado como ADMIN -->
  <AdminView v-else-if="auth.isAdmin" />

  <!-- Autenticado como ARRENDATARIO -->
  <TenantView v-else />
</template>

<style>
/*
 * Estilos globales mínimos.
 * La paleta y los tokens viven en assets/styles/tokens.css (importado en main.js).
 * La base tipográfica vive en assets/styles/base.css (importado en main.js).
 */

/* Reset base */
*,
*::before,
*::after {
  box-sizing: border-box;
}

html,
body {
  margin: 0;
  padding: 0;
  background: var(--color-bg);
  color: var(--color-text);
  font-family: var(--font-body);
  font-size: 16px;
  line-height: 1.6;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* Indicador de carga inicial */
.app-loading {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}

.app-loading__text {
  color: var(--color-text-muted);
  font-size: 0.875rem;
  margin: 0;
}
</style>
