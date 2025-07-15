<template>
  <div id="app" class="min-h-screen bg-gray-100">
    <!-- Navigation -->
    <nav class="bg-blue-600 text-white shadow-lg">
      <div class="max-w-7xl mx-auto px-4">
        <div class="flex justify-between items-center h-16">
          <div class="flex items-center">
            <h1 class="text-xl font-bold">📦 Gestion Commandes Pokemon</h1>
          </div>
          <div class="flex space-x-4">
            <button
              v-for="tab in tabs"
              :key="tab.id"
              @click="changeTab(tab.id)"
              :class="[
                'px-4 py-2 rounded-md transition-colors',
                activeTab === tab.id
                  ? 'bg-blue-700 text-white'
                  : 'text-blue-100 hover:text-white hover:bg-blue-500'
              ]"
            >
              {{ tab.label }}
            </button>
          </div>
        </div>
      </div>
    </nav>

    <!-- Contenu principal -->
    <main class="max-w-7xl mx-auto px-4 py-6">
      <!-- Dashboard -->
      <DashboardView v-if="activeTab === 'dashboard'" @go-to-tab="changeTab" />

      <!-- Commandes -->
      <CommandesView v-if="activeTab === 'commandes'" />

      <!-- Employés -->
      <EmployesView v-if="activeTab === 'employes'" />

      <!-- Planification -->
      <PlanificationView v-if="activeTab === 'planification'" />
    </main>

    <!-- Notifications -->
    <div
      v-if="notification.show"
      :class="[
        'fixed top-4 right-4 p-4 rounded-lg shadow-lg transition-all z-50',
        notification.type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'
      ]"
    >
      {{ notification.message }}
    </div>

    <div v-if="showPlanning">
      <EmployeeScheduleDashboard />
    </div>

    <!-- Bouton pour afficher/masquer -->
    <button
      @click="showPlanning = !showPlanning"
      class="bg-blue-600 text-white px-4 py-2 rounded"
    >
      {{ showPlanning ? 'Masquer' : 'Afficher' }} Planning
    </button>
  </div>



</template>

<script setup lang="ts">
import { ref, provide } from 'vue'
import DashboardView from './components/DashboardView.vue'
import CommandesView from './components/CommandesView.vue'
import EmployesView from './components/EmployesView.vue'
import PlanificationView from './components/PlanificationView.vue'
import EmployeeDetailPage from './components/EmployeeDetailPage.vue'
// État global
const activeTab = ref('dashboard')
const notification = ref({
  show: false,
  message: '',
  type: 'success'
})

const tabs = [
  { id: 'dashboard', label: '📊 Dashboard' },
  { id: 'commandes', label: '📋 Commandes' },
  { id: 'employes', label: '👥 Employés' },
  { id: 'planification', label: '📅 Planification' }
]

// Fonction pour changer d'onglet
const changeTab = (tabId: string) => {
  console.log('Changement vers onglet:', tabId) // Debug
  activeTab.value = tabId
}

// Fonction pour afficher les notifications
const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
  notification.value = { show: true, message, type }
  setTimeout(() => {
    notification.value.show = false
  }, 3000)
}

// Provide pour les composants enfants
provide('showNotification', showNotification)
provide('changeTab', changeTab) // Ajout pour les composants enfants

//import { ref } from 'vue'
import EmployeeScheduleDashboard from './components/EmployeeScheduleDashboard.vue'

const showPlanning = ref(false)

</script>

<style>
#app {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>
