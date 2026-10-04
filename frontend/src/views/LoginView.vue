<script setup>
/**
 * LoginView.vue — Pantalla de inicio de sesión (F-05).
 *
 * Sin header, sin toggle, sin "Olvidé mi contraseña".
 * Llama auth.login(username, password) al submit.
 * Error genérico: "Usuario o contraseña incorrectos" (nunca revela detalles).
 * Layout centrado vertical y horizontal, tarjeta max-width 400px.
 *
 * Diseño: DESIGN.md §5 "LoginView" y §6 "LoginView (pantalla de inicio de sesión)".
 */
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth.js'

const auth = useAuthStore()

const username = ref('')
const password = ref('')
const showPassword = ref(false)
const loading = ref(false)
const errorMsg = ref(null)

async function handleSubmit() {
  errorMsg.value = null
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    // App.vue reaccionará al cambio de auth.status → 'authenticated' y mostrará AdminView/TenantView.
  } catch {
    errorMsg.value = 'Usuario o contraseña incorrectos'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-view">
    <div class="login-card">
      <!-- Nombre de la app / logo -->
      <h1 class="login-logo">Gestión de Arriendos</h1>

      <form class="login-form" @submit.prevent="handleSubmit">
        <!-- Usuario -->
        <div class="form-group">
          <label for="login-username" class="form-label">Usuario</label>
          <input
            id="login-username"
            v-model="username"
            type="text"
            autocomplete="username"
            class="form-input"
            :disabled="loading"
            required
          />
        </div>

        <!-- Contraseña con toggle -->
        <div class="form-group">
          <label for="login-password" class="form-label">Contraseña</label>
          <div class="password-field">
            <input
              id="login-password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              class="form-input"
              :disabled="loading"
              required
            />
            <button
              type="button"
              class="password-field__toggle"
              :aria-label="showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'"
              @click="showPassword = !showPassword"
            >
              <!-- Ojo abierto -->
              <svg v-if="!showPassword" aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              <!-- Ojo tachado -->
              <svg v-else aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                <line x1="1" y1="1" x2="23" y2="23"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- ErrorBanner: visible solo cuando hay error -->
        <div
          v-if="errorMsg"
          class="error-banner"
          role="alert"
          aria-live="assertive"
          aria-atomic="true"
        >
          {{ errorMsg }}
        </div>

        <!-- Botón Ingresar -->
        <button
          type="submit"
          class="btn-primary"
          :disabled="loading"
          :aria-busy="loading"
        >
          {{ loading ? 'Ingresando…' : 'Ingresar' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
/* Contenedor de pantalla completa centrado */
.login-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  padding: var(--space-md);
}

/* Tarjeta del formulario */
.login-card {
  max-width: 400px;
  width: 100%;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  padding: var(--space-xl);
}

/* Logo / título */
.login-logo {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--color-accent);
  letter-spacing: -0.03em;
  margin: 0 0 var(--space-lg) 0;
  line-height: 1.3;
}

/* Formulario */
.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.form-label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-muted);
  display: block;
}

/* Input base */
.form-input {
  font-family: var(--font-body);
  font-size: 1rem;
  width: 100%;
  min-height: 40px;
  padding: var(--space-sm) var(--space-md);
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text);
  box-sizing: border-box;
  transition: border-color var(--transition), box-shadow var(--transition);
}

.form-input:focus {
  outline: none;
  border-color: var(--color-accent);
  box-shadow: var(--shadow-glow);
}

.form-input:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* Campo de contraseña con toggle */
.password-field {
  position: relative;
}

/* El input de contraseña necesita espacio para el botón */
.password-field .form-input {
  padding-right: 44px;
}

.password-field__toggle {
  position: absolute;
  right: var(--space-md);
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  min-height: 24px;
  padding: 0;
  transition: color var(--transition);
}

.password-field__toggle:hover {
  color: var(--color-text);
}

.password-field__toggle:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
  border-radius: 4px;
}

/* Error banner */
.error-banner {
  background: rgba(192, 57, 43, 0.08);
  border: 1px solid rgba(192, 57, 43, 0.28);
  color: var(--color-danger);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.5;
}

/* Botón principal */
.btn-primary {
  font-family: var(--font-body);
  font-size: 1rem;
  font-weight: 600;
  width: 100%;
  min-height: 40px;
  background: var(--color-accent);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition);
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-accent-hover);
}

.btn-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-primary:focus-visible {
  outline: 2px solid #ffffff;
  outline-offset: 3px;
}
</style>
