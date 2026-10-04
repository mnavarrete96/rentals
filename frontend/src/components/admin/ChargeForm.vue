<script setup>
/**
 * ChargeForm.vue — Formulario pequeño para añadir un cargo único a un arrendatario.
 *
 * Props:
 *   loading  boolean  — deshabilita el formulario durante el envío
 *
 * Emite:
 *   submit({ description, amount })  — cuando el formulario pasa la validación
 *   cancel                           — al pulsar "Cancelar"
 */
import { ref } from 'vue'

defineProps({
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['submit', 'cancel'])

const description = ref('')
const amount = ref('')
const errors = ref({})

function validate() {
  const e = {}
  if (!description.value.trim()) e.description = 'La descripción es requerida.'
  const a = Number(amount.value)
  if (amount.value === '' || isNaN(a) || a <= 0) {
    e.amount = 'El monto debe ser mayor que 0.'
  }
  errors.value = e
  return Object.keys(e).length === 0
}

function handleSubmit() {
  if (!validate()) return
  emit('submit', {
    description: description.value.trim(),
    amount: Number(amount.value)
  })
}
</script>

<template>
  <form class="charge-form" novalidate @submit.prevent="handleSubmit">
    <h3 class="charge-form-title">Añadir cargo único</h3>

    <!-- Descripción -->
    <div class="form-group">
      <label for="cf-description">Descripción</label>
      <input
        id="cf-description"
        v-model="description"
        type="text"
        :disabled="loading"
        :class="{ 'input--error': errors.description }"
        placeholder="Ej. Garantía"
        maxlength="200"
      />
      <span v-if="errors.description" class="field-error">{{ errors.description }}</span>
    </div>

    <!-- Monto -->
    <div class="form-group">
      <label for="cf-amount">Monto (USD)</label>
      <input
        id="cf-amount"
        v-model="amount"
        type="number"
        min="0.01"
        step="0.01"
        :disabled="loading"
        :class="{ 'input--error': errors.amount }"
        placeholder="0.00"
      />
      <span v-if="errors.amount" class="field-error">{{ errors.amount }}</span>
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
        {{ loading ? 'Guardando…' : 'Guardar cargo' }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.charge-form {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.charge-form-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 var(--space-xs);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
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

.field-error {
  font-size: 0.75rem;
  color: var(--color-danger);
  font-weight: 600;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
  margin-top: var(--space-xs);
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
  transition: background var(--transition);
  min-height: 40px;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-accent-hover);
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
</style>
