<script setup>
/**
 * TenantView.vue — Vista principal del arrendatario (F-04).
 *
 * Al montar, carga GET /api/me/tenant mediante tenantsStore.loadMyAccount().
 * Muestra AppHeader + AccountStatement.
 * Sin selector ni toggle: el arrendatario ve solo su propia cuenta.
 *
 * App.vue muestra esta vista cuando auth.status = 'authenticated' && !auth.isAdmin.
 */
import { onMounted, ref } from 'vue'
import { useTenantsStore } from '../stores/tenants.js'

import AppHeader from '../components/common/AppHeader.vue'
import AccountStatement from '../components/tenant/AccountStatement.vue'
import ChangePasswordForm from '../components/common/ChangePasswordForm.vue'

const tenantsStore = useTenantsStore()

const showChangePassword = ref(false)

onMounted(() => {
  tenantsStore.loadMyAccount()
})

function openChangePassword() {
  showChangePassword.value = true
}

function closeChangePassword() {
  showChangePassword.value = false
}
</script>

<template>
  <div class="tenant-page">
    <AppHeader
      @change-password="openChangePassword"
    />

    <main class="main-content">
      <div class="tenant-view">
        <p class="section-title">Mi estado de cuenta</p>

        <AccountStatement
          :tenant="tenantsStore.myAccount"
          :loading="tenantsStore.loading"
          :error="tenantsStore.error"
        />
      </div>
    </main>

    <!-- Modal: Cambiar contraseña -->
    <ChangePasswordForm
      v-if="showChangePassword"
      @close="closeChangePassword"
    />
  </div>
</template>

<style scoped>
.tenant-page {
  min-height: 100vh;
  background: var(--color-bg);
  display: flex;
  flex-direction: column;
}

.main-content {
  max-width: 760px;
  margin: 0 auto;
  padding: var(--space-xl);
  width: 100%;
  flex: 1;
}

.tenant-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.section-title {
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text-muted);
  margin: 0;
}

@media (max-width: 639px) {
  .main-content {
    padding: var(--space-md);
  }
}
</style>
