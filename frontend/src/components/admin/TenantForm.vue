<script setup>
/**
 * TenantForm.vue — Formulario para registrar un nuevo arrendatario.
 *
 * Props:
 *   loading  boolean       — deshabilita el formulario durante el envío
 *   error    string | null — mensaje de error externo (p. ej. "409 username ocupado")
 *
 * Emite:
 *   submit(formData)  — cuando el formulario pasa la validación
 *   cancel            — al pulsar "Cancelar"
 */
import { ref, computed } from 'vue'

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: null
  }
})

const emit = defineEmits(['submit', 'cancel'])

// ── Estado del formulario ────────────────────────────────────────────────────
const fullName = ref('')
const unit = ref('')
const contact = ref('')
const paymentDay = ref('')
const startDate = ref('')
const monthlyAmount = ref('')
const username = ref('')
const initialPassword = ref('')
const showPassword = ref(false)

// ── Validación ────────────────────────────────────────────────────────────────
const usernamePattern = /^[a-z0-9._-]{3,50}$/

const usernameInvalid = computed(() =>
  username.value.length > 0 && !usernamePattern.test(username.value)
)

const passwordInvalid = computed(() =>
  initialPassword.value.length > 0 && initialPassword.value.length < 10
)

const errors = ref({})

function validate() {
  const e = {}
  if (!fullName.value.trim()) e.fullName = 'El nombre es requerido.'
  if (!unit.value.trim()) e.unit = 'La unidad es requerida.'
  if (!contact.value.trim()) e.contact = 'El contacto es requerido.'
  const pd = Number(paymentDay.value)
  if (!paymentDay.value || isNaN(pd) || pd < 1 || pd > 28) {
    e.paymentDay = 'Ingresa un día entre 1 y 28.'
  }
  if (!startDate.value) e.startDate = 'La fecha de inicio es requerida.'
  const ma = Number(monthlyAmount.value)
  if (monthlyAmount.value === '' || isNaN(ma) || ma < 0) {
    e.monthlyAmount = 'Ingresa un monto válido (0 o positivo).'
  }
  if (!username.value.trim()) {
    e.username = 'El nombre de usuario es requerido.'
  } else if (!usernamePattern.test(username.value)) {
    e.username = 'Formato inválido (3–50 chars: letras min., números, punto, guión, guión bajo).'
  }
  if (!initialPassword.value) {
    e.initialPassword = 'La contraseña inicial es requerida.'
  } else if (initialPassword.value.length < 10) {
    e.initialPassword = 'Mínimo 10 caracteres.'
  }
  errors.value = e
  return Object.keys(e).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('submit', {
    fullName: fullName.value.trim(),
    unit: unit.value.trim(),
    contact: contact.value.trim(),
    paymentDay: Number(paymentDay.value),
    startDate: startDate.value,
    monthlyAmount: Number(monthlyAmount.value),
    username: username.value.trim().toLowerCase(),
    initialPassword: initialPassword.value
  })
}

function togglePasswordVisibility() {
  showPassword.value = !showPassword.value
}
</script>

<template>
  <form class="form-panel" novalidate @submit.prevent="handleSubmit">
    <h3 class="form-title">Registrar arrendatario</h3>

    <!-- Error 409 u otro error externo -->
    <div
      v-if="error"
      class="error-banner"
      role="alert"
      aria-live="assertive"
      aria-atomic="true"
    >
      {{ error }}
    </div>

    <!-- Nombre completo -->
    <div class="form-group form-group--full">
      <label for="tf-fullName">Nombre completo</label>
      <input
        id="tf-fullName"
        v-model="fullName"
        type="text"
        autocomplete="name"
        :disabled="loading"
        :class="{ 'input--error': errors.fullName }"
        placeholder="Ej. María Pérez"
      />
      <span v-if="errors.fullName" class="field-error">{{ errors.fullName }}</span>
    </div>

    <!-- Grid 2 cols -->
    <div class="form-row">
      <!-- Unidad -->
      <div class="form-group">
        <label for="tf-unit">Unidad</label>
        <input
          id="tf-unit"
          v-model="unit"
          type="text"
          :disabled="loading"
          :class="{ 'input--error': errors.unit }"
          placeholder="Ej. Depto 301"
        />
        <span v-if="errors.unit" class="field-error">{{ errors.unit }}</span>
      </div>

      <!-- Día de pago -->
      <div class="form-group">
        <label for="tf-paymentDay">Día de pago (1–28)</label>
        <input
          id="tf-paymentDay"
          v-model="paymentDay"
          type="number"
          min="1"
          max="28"
          :disabled="loading"
          :class="{ 'input--error': errors.paymentDay }"
          placeholder="1–28"
        />
        <span v-if="errors.paymentDay" class="field-error">{{ errors.paymentDay }}</span>
      </div>
    </div>

    <!-- Contacto -->
    <div class="form-group form-group--full">
      <label for="tf-contact">Contacto</label>
      <input
        id="tf-contact"
        v-model="contact"
        type="text"
        :disabled="loading"
        :class="{ 'input--error': errors.contact }"
        placeholder="Correo o teléfono"
      />
      <span v-if="errors.contact" class="field-error">{{ errors.contact }}</span>
    </div>

    <div class="form-row">
      <!-- Fecha de inicio -->
      <div class="form-group">
        <label for="tf-startDate">Fecha de inicio</label>
        <input
          id="tf-startDate"
          v-model="startDate"
          type="date"
          :disabled="loading"
          :class="{ 'input--error': errors.startDate }"
        />
        <span v-if="errors.startDate" class="field-error">{{ errors.startDate }}</span>
      </div>

      <!-- Monto mensual -->
      <div class="form-group">
        <label for="tf-monthlyAmount">Mensualidad (USD)</label>
        <input
          id="tf-monthlyAmount"
          v-model="monthlyAmount"
          type="number"
          min="0"
          step="0.01"
          :disabled="loading"
          :class="{ 'input--error': errors.monthlyAmount }"
          placeholder="450.00"
        />
        <span v-if="errors.monthlyAmount" class="field-error">{{ errors.monthlyAmount }}</span>
      </div>
    </div>

    <!-- Divider de credenciales -->
    <div class="form-divider">Credenciales de acceso</div>

    <!-- Nombre de usuario -->
    <div class="form-group form-group--full">
      <label for="tf-username">Nombre de usuario</label>
      <input
        id="tf-username"
        v-model="username"
        type="text"
        autocomplete="username"
        :disabled="loading"
        :class="{ 'input--error': errors.username }"
        placeholder="nombre.apellido"
      />
      <span
        class="field-hint"
        :class="{ 'field-hint--danger': usernameInvalid || errors.username }"
      >
        Formato sugerido: nombre.apellido — solo letras minúsculas, números, puntos, guiones y
        guion bajo (3–50 caracteres).
      </span>
      <span v-if="errors.username" class="field-error">{{ errors.username }}</span>
    </div>

    <!-- Contraseña inicial -->
    <div class="form-group form-group--full">
      <label for="tf-initialPassword">Contraseña inicial</label>
      <div class="password-field">
        <input
          id="tf-initialPassword"
          v-model="initialPassword"
          :type="showPassword ? 'text' : 'password'"
          autocomplete="new-password"
          :disabled="loading"
          :class="{ 'input--error': errors.initialPassword }"
          placeholder="Mínimo 10 caracteres"
        />
        <button
          type="button"
          class="password-field__toggle"
          :aria-label="showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'"
          @click="togglePasswordVisibility"
        >
          <!-- Ojo abierto -->
          <svg v-if="!showPassword" aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
            <circle cx="12" cy="12" r="3"/>
          </svg>
          <!-- Ojo tachado -->
          <svg v-else aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94"/>
            <path d="M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19"/>
            <line x1="1" y1="1" x2="23" y2="23"/>
          </svg>
        </button>
      </div>
      <span
        class="field-hint"
        :class="{ 'field-hint--danger': passwordInvalid || errors.initialPassword }"
      >
        Mínimo 10 caracteres. El arrendatario deberá cambiarla al ingresar por primera vez.
      </span>
      <span v-if="errors.initialPassword" class="field-error">{{ errors.initialPassword }}</span>
    </div>

    <!-- Acciones -->
    <div class="form-actions">
      <button
        type="button"
        class="btn-secondary"
        :disabled="loading"
        @click="emit('cancel')"
      >
        Cancelar
      </button>
      <button
        type="submit"
        class="btn-primary"
        :disabled="loading"
        :aria-busy="loading"
      >
        {{ loading ? 'Registrando…' : 'Registrar' }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.form-panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.form-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 var(--space-xs);
  line-height: 1.4;
}

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

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-md);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.form-group--full {
  grid-column: 1 / -1;
}

.form-group label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--color-text-muted);
  display: block;
}

.form-group input {
  font-family: var(--font-body);
  font-size: 1rem;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 0 var(--space-md);
  min-height: 40px;
  color: var(--color-text);
  transition: border-color var(--transition), box-shadow var(--transition);
  width: 100%;
  box-sizing: border-box;
}

.form-group input:focus {
  outline: none;
  border-color: var(--color-accent);
  box-shadow: var(--shadow-glow);
}

.form-group input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.input--error {
  border-color: var(--color-danger) !important;
}

.field-hint {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
  line-height: 1.4;
}

.field-hint--danger {
  color: var(--color-danger);
}

.field-error {
  font-size: 0.75rem;
  color: var(--color-danger);
  font-weight: 600;
}

/* Password field */
.password-field {
  position: relative;
}

.password-field input {
  padding-right: 44px;
}

.password-field__toggle {
  position: absolute;
  right: var(--space-md);
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
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

/* Divider */
.form-divider {
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  padding-top: var(--space-sm);
  border-top: 1px solid var(--color-border);
  margin-top: var(--space-xs);
}

/* Actions */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
  margin-top: var(--space-sm);
}

.btn-primary {
  font-family: var(--font-body);
  font-size: 0.9375rem;
  font-weight: 600;
  background: var(--color-accent);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-lg);
  cursor: pointer;
  transition: background var(--transition), box-shadow var(--transition);
  min-height: 40px;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-accent-hover);
  box-shadow: 0 2px 8px rgba(168, 86, 50, 0.22);
}

.btn-primary:focus-visible {
  outline: 2px solid #fff;
  outline-offset: 3px;
}

.btn-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-secondary {
  font-family: var(--font-body);
  font-size: 0.9375rem;
  font-weight: 500;
  background: transparent;
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-lg);
  cursor: pointer;
  transition: border-color var(--transition), color var(--transition);
  min-height: 40px;
}

.btn-secondary:hover:not(:disabled) {
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.btn-secondary:focus-visible {
  outline: 2px solid var(--color-accent);
  outline-offset: 2px;
}

.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 639px) {
  .form-row {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .btn-primary,
  .btn-secondary {
    width: 100%;
  }
}
</style>
