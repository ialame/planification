<template>
  <div class="bg-gray-50 p-6 rounded-lg">
    <h3 class="text-lg font-semibold mb-4">🧪 Test de l'API</h3>

    <div class="space-y-4">
      <!-- Test de connexion -->
      <div class="flex items-center space-x-4">
        <button
          @click="testConnection"
          :disabled="loading"
          class="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
        >
          Test Connexion
        </button>
        <span :class="connectionStatus.color">{{ connectionStatus.message }}</span>
      </div>

      <!-- Test Dashboard -->
      <div class="flex items-center space-x-4">
        <button
          @click="testDashboard"
          :disabled="loading"
          class="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 disabled:opacity-50"
        >
          Test Dashboard
        </button>
        <span v-if="dashboardData">{{ dashboardData.totalCommandes }} commandes</span>
      </div>

      <!-- Test Employés -->
      <div class="flex items-center space-x-4">
        <button
          @click="testEmployes"
          :disabled="loading"
          class="bg-purple-600 text-white px-4 py-2 rounded hover:bg-purple-700 disabled:opacity-50"
        >
          Test Employés
        </button>
        <span v-if="employesData">{{ employesData.length }} employés</span>
      </div>

      <!-- Test Commandes -->
      <div class="flex items-center space-x-4">
        <button
          @click="testCommandes"
          :disabled="loading"
          class="bg-orange-600 text-white px-4 py-2 rounded hover:bg-orange-700 disabled:opacity-50"
        >
          Test Commandes
        </button>
        <span v-if="commandesData">{{ commandesData.length }} commandes</span>
      </div>
    </div>

    <!-- Résultats détaillés -->
    <div v-if="lastResult" class="mt-6">
      <h4 class="font-medium mb-2">Dernier résultat :</h4>
      <pre class="bg-white p-3 rounded text-xs overflow-x-auto max-h-40">{{ lastResult }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { apiService } from '../services/api'

const loading = ref(false)
const lastResult = ref<any>(null)
const dashboardData = ref<any>(null)
const employesData = ref<any>(null)
const commandesData = ref<any>(null)

const connectionStatus = ref({
  message: 'Non testé',
  color: 'text-gray-500'
})

const testConnection = async () => {
  loading.value = true
  try {
    const result = await apiService.testConnection()
    connectionStatus.value = {
      message: '✅ Connecté',
      color: 'text-green-600'
    }
    lastResult.value = result
  } catch (error) {
    connectionStatus.value = {
      message: '❌ Erreur de connexion',
      color: 'text-red-600'
    }
    lastResult.value = { error: error.message }
  } finally {
    loading.value = false
  }
}

const testDashboard = async () => {
  loading.value = true
  try {
    const result = await apiService.getDashboardStats()
    dashboardData.value = result
    lastResult.value = result
  } catch (error) {
    lastResult.value = { error: error.message }
  } finally {
    loading.value = false
  }
}

const testEmployes = async () => {
  loading.value = true
  try {
    const result = await apiService.getEmployes()
    employesData.value = result
    lastResult.value = result
  } catch (error) {
    lastResult.value = { error: error.message }
  } finally {
    loading.value = false
  }
}

const testCommandes = async () => {
  loading.value = true
  try {
    const result = await apiService.getCommandes()
    commandesData.value = result
    lastResult.value = result
  } catch (error) {
    lastResult.value = { error: error.message }
  } finally {
    loading.value = false
  }
}
</script>
