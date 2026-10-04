<script setup>
/**
 * ChangePasswordForm.vue — Modal de cambio de contraseña (F-05).
 *
 * - Campos: contraseña actual, nueva contraseña (min 10 chars), confirmar nueva.
 * - Validación en frontend: nueva == confirmar (sin llamada al backend si no coinciden).
 * - Llama authStore.changePassword(current, new) al submit.
 * - Error 401: "Contraseña actual incorrecta".
 * - Error 400: muestra el mensaje del backend.
 * - Éxito: mensaje y cierra; clearSession() → App.vue muestra LoginView.
 * - Accesibilidad: role="dialog", focus trap, Escape cierra.
 *
 * Emite: close
 *
 * Diseño: DESIGN.md §5 "ChangePasswordForm".
 */
import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useAuthStore } from '../../stores/auth.js'

const auth = useAuthStore()
const emit = defineEmits(['close'])

// ── Estado ──────────────────────────────────────────────────────────────────
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const showCurrent = ref(false)
const showNew = ref(false)
const showConfirm = ref(false)
const loading = ref(false)
const errorMsg = ref(null)
const successMsg = ref(null)

// Refs para focus trap
const modalRef = ref(null)
const firstFieldRef = ref(null)

// ── Ciclo de vida ────────────────────────────────────────────────────────────
onMounted(async () => {
  // Enfoca el primer campo al abrir
  await nextTick()
  firstFieldRef.value?.focus()
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
})

// ── Focus trap + Escape ──────────────────────────────────────────────────────
function handleKeydown(e) {
  if (e.key === 'Escape') {
    close()
    return
  }
  if (e.key === 'Tab' && modalRef.value) {
    const focusable = modalRef.value.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    )
    const first = focusable[0]
    const last = focusable[focusable.length - 1]
    if (e.shiftKey) {
      if (document.activeElement === first) {
        e.preventDefault()
        last.focus()
      }
    } else {
      if (document.activeElement === last) {
        e.preventDefault()
        first.focus()
      }
    }
  }
}

// ── Acciones ─────────────────────────────────────────────────────────────────
function close() {
  if (!loading.value) {
    emit('close')
  }
}

async function handleSubmit() {
  errorMsg.value = null
  successMsg.value = null

  // Validación en frontend: contraseñas deben coincidir
  if (newPassword.value !== confirmPassword.value) {
    errorMsg.value = 'Las contraseñas no coinciden'
    return
  }

  // Validación: nueva contraseña mínimo 10 caracteres
  if (newPassword.value.length < 10) {
    errorMsg.value = 'La nueva contraseña debe tener mínimo 10 caracteres'
    return
  }

  loading.value = true
  try {
    await auth.changePassword(currentPassword.value, newPassword.value)
    // clearSession() fue llamado por changePassword(); auth.status → 'anonymous'
    // App.vue mostrará LoginView automáticamente.
    successMsg.value = 'Contraseña cambiada correctamente. Inicia sesión de nuevo.'
    // Cerrar modal tras 2 segundos (App.vue ya habrá cambiado a LoginView)
    setTimeout(() => emit('close'), 2000)
  } catch (err) {
    if (err?.status === 401) {
      errorMsg.value = 'Contraseña actual incorrecta'
      await nextTick()
      firstFieldRef.value?.focus()
    } else if (err?.status === 400) {
      errorMsg.value = err?.detail ?? err?.message ?? 'Error de validación'
    } else {
      errorMsg.value = err?.detail ?? err?.message ?? 'Error al cambiar la contraseña'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <!-- Overlay del modal -->
  <div
    class="modal-overlay"
    @click.self="close"
  >
    <!-- Modal -->
    <div
      ref="modalRef"
      class="modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="change-pw-title"
    >
      <h2 id="change-pw-title" class="modal-title">Cambiar contraseña</h2>

      <form class="change-pw-form" @submit.prevent="handleSubmit">
        <!-- Contraseña actual -->
        <div class="form-group">
          <label for="current-password" class="form-label">Contraseña actual</label>
          <div class="password-field">
            <input
              id="current-password"
              ref="firstFieldRef"
              v-model="currentPassword"
              :type="showCurrent ? 'text' : 'password'"
              autocomplete="current-password"
              class="form-input"
              :readonly="loading"
              required
            />
            <button
              type="button"
              class="password-field__toggle"
              :aria-label="showCurrent ? 'Ocultar contraseña' : 'Mostrar contraseña'"
              @click="showCurrent = !showCurrent"
            >
              <!-- Ojo abierto -->
              <svg v-if="!showCurrent" aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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

        <!-- Nueva contraseña -->
        <div class="form-group">
          <label for="new-password" class="form-label">Nueva contraseña</label>
          <div class="password-field">
            <input
              id="new-password"
              v-model="newPassword"
              :type="showNew ? 'text' : 'password'"
              autocomplete="new-password"
              class="form-input"
              :readonly="loading"
              required
            />
            <button
              type="button"
              class="password-field__toggle"
              :aria-label="showNew ? 'Ocultar contraseña' : 'Mostrar contraseña'"
              @click="showNew = !showNew"
            >
              <svg v-if="!showNew" aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              <svg v-else aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                <line x1="1" y1="1" x2="23" y2="23"/>
              </svg>
            </button>
          </div>
          <span class="field-hint">Mínimo 10 caracteres</span>
        </div>

        <!-- Confirmar nueva contraseña -->
        <div class="form-group">
          <label for="confirm-password" class="form-label">Confirmar nueva contraseña</label>
          <div class="password-field">
            <input
              id="confirm-password"
              v-model="confirmPassword"
              :type="showConfirm ? 'text' : 'password'"
              autocomplete="new-password"
              class="form-input"
              :readonly="loading"
              required
            />
            <button
              type="button"
              class="password-field__toggle"
              :aria-label="showConfirm ? 'Ocultar contraseña' : 'Mostrar contraseña'"
              @click="showConfirm = !showConfirm"
            >
              <svg v-if="!showConfirm" aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
              <svg v-else aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                <line x1="1" y1="1" x2="23" y2="23"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Error banner -->
        <div
          v-if="errorMsg"
          class="error-banner"
          role="alert"
          aria-live="assertive"
          aria-atomic="true"
        >
          {{ errorMsg }}
        </div>

        <!-- Éxito -->
        <div
          v-if="successMsg"
          class="modal-success"
          aria-live="polite"
        >
          {{ successMsg }}
        </div>

        <!-- Acciones -->
        <div class="modal-actions">
          <button
            type="button"
            class="btn-secondary"
            :disabled="loading"
            @click="close"
          >
            Cancelar
          </button>
          <button
            type="submit"
            class="btn-primary"
            :disabled="loading || !!successMsg"
            :aria-busy="loading"
          >
            {{ loading ? 'Cambiando…' : 'Cambiar' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
/* Overlay */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.42);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
  padding: var(--space-md);
}

/* Modal */
.modal {
  background: var(--color-surface);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  max-width: 440px;
  width: 100%;
  padding: var(--space-xl);
  max-height: 90vh;
  overflow-y: auto;
}

.modal-title {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 var(--space-lg) 0;
}

/* Formulario */
.change-pw-form {
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

/* Campo de contraseña */
.password-field {
  position: relative;
}

.form-input {
  font-family: var(--font-body);
  font-size: 1rem;
  width: 100%;
  min-height: 40px;
  padding: var(--space-sm) 44px var(--space-sm) var(--space-md);
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

.form-input[readonly] {
  opacity: 0.7;
  cursor: not-allowed;
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

/* Hint de campo */
.field-hint {
  font-size: 0.75rem;
  font-weight: 400;
  color: var(--color-text-subtle);
  display: block;
  margin-top: var(--space-xs);
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

/* Mensaje de éxito */
.modal-success {
  color: var(--color-success);
  font-size: 0.875rem;
  font-weight: 500;
  margin-top: var(--space-sm);
}

/* Acciones del modal */
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
  margin-top: var(--space-lg);
}

/* Botón principal */
.btn-primary {
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 600;
  padding: var(--space-sm) var(--space-lg);
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

/* Botón secundario */
.btn-secondary {
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  padding: var(--space-sm) var(--space-lg);
  background: transparent;
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: border-color var(--transition), color var(--transition);
}

.btn-secondary:hover:not(:disabled) {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.btn-secondary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-secondary:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}
</style>
