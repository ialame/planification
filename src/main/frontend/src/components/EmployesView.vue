<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <div class="max-w-7xl mx-auto">

      <!-- ✅ EN-TÊTE SIMPLE -->
      <div class="flex justify-between items-center mb-6">
        <div>
          <h1 class="text-3xl font-bold text-gray-900">
            👥 Employés
          </h1>
          <p class="text-gray-600 mt-1">
            Gestion des employés et de leurs planifications
          </p>
        </div>

        <!-- Sélecteur de date -->
        <div class="flex items-center space-x-3">
          <label class="text-sm font-medium text-gray-700">Date :</label>
          <input
            v-model="selectedDate"
            type="date"
            class="border border-gray-300 rounded-md px-3 py-2"
          />
          <button
            @click="chargerEmployesAvecStats"
            class="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
          >
            🔄 Actualiser
          </button>
        </div>
      </div>

      <!-- ✅ CONDITION: Afficher soit la liste, soit le détail -->
      <div v-if="!employeSelectionne">
        <!-- LISTE DES EMPLOYÉS -->
        <div v-if="loading" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <span class="text-gray-600 mt-3 block">Chargement des employés...</span>
        </div>

        <div v-else-if="employesAvecStats.length === 0" class="text-center py-12">
          <div class="text-gray-500">
            <div class="text-4xl mb-4">👥</div>
            <div>Aucun employé trouvé pour cette date</div>
          </div>
        </div>

        <!-- ✅ GRILLE D'EMPLOYÉS CLIQUABLES -->
        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div
            v-for="employe in employesAvecStats"
            :key="employe.id"
            @click="voirDetailEmploye(employe.id)"
            class="bg-white rounded-lg shadow-md hover:shadow-lg transition-all cursor-pointer border-l-4 border-blue-500 p-6"
          >
            <!-- Avatar et nom -->
            <div class="flex items-center mb-4">
              <div class="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 font-bold text-lg">
                {{ getInitiales(employe) }}
              </div>
              <div class="ml-3">
                <h3 class="text-lg font-semibold text-gray-900">
                  {{ employe.prenom }} {{ employe.nom }}
                </h3>
                <p class="text-sm text-gray-500">{{ employe.email }}</p>
              </div>
            </div>

            <!-- Statistiques -->
            <div class="grid grid-cols-2 gap-4">
              <div class="text-center">
                <div class="text-2xl font-bold text-blue-600">{{ employe.nombreCommandes || 0 }}</div>
                <div class="text-xs text-gray-500">Commandes</div>
              </div>
              <div class="text-center">
                <div class="text-2xl font-bold text-green-600">{{ employe.dureeeTotaleFormatee || '0min' }}</div>
                <div class="text-xs text-gray-500">Durée</div>
              </div>
            </div>

            <!-- Statut et charge -->
            <div class="mt-4 flex justify-between items-center">
              <span :class="['px-2 py-1 rounded text-xs font-medium', getStatutClasses(employe.statut)]">
                {{ getStatutLabel(employe.statut) }}
              </span>
              <span class="text-sm font-medium text-gray-600">
                {{ Math.round(employe.pourcentageCharge || 0) }}%
              </span>
            </div>

            <!-- Actions -->
            <div class="mt-4 flex space-x-2">
              <button class="flex-1 bg-blue-50 text-blue-600 px-3 py-2 rounded text-sm font-medium hover:bg-blue-100">
                📋 Détail
              </button>
              <button class="flex-1 bg-gray-50 text-gray-600 px-3 py-2 rounded text-sm font-medium hover:bg-gray-100">
                ✏️ Modifier
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ✅ COMPOSANT DE DÉTAIL EMPLOYÉ -->
      <div v-else>
        <EmployeeDetailPage
          :employeeId="employeSelectionne"
          :selectedDate="selectedDate"
          @back="retourListeEmployes"
          @refresh="chargerEmployesAvecStats"
        />
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { apiService } from '../services/api'
import EmployeeDetailPage from './EmployeeDetailPage.vue'

// ========== ÉTAT RÉACTIF ==========
const selectedDate = ref(new Date().toISOString().split('T')[0])
const employesAvecStats = ref<any[]>([])
const loading = ref(false)
const employeSelectionne = ref<string | null>(null) // ✅ ÉTAT POUR LA NAVIGATION

// ========== COMPUTED ==========
const employesDisponibles = computed(() =>
  employesAvecStats.value.filter(emp => emp.statut === 'available').length
)

const employesCharges = computed(() =>
  employesAvecStats.value.filter(emp => emp.statut === 'full').length
)

const employesSurcharges = computed(() =>
  employesAvecStats.value.filter(emp => emp.statut === 'overloaded').length
)

// ========== MÉTHODES PRINCIPALES ==========

const chargerEmployesAvecStats = async () => {
  loading.value = true
  try {
    console.log('🔄 Chargement employés avec stats pour:', selectedDate.value)

    const response = await fetch(`/api/employes/avec-stats?date=${selectedDate.value}`)
    employesAvecStats.value = await response.json()

    console.log('✅ Employés chargés:', employesAvecStats.value.length)
  } catch (error) {
    console.error('❌ Erreur chargement employés:', error)
    employesAvecStats.value = []
  } finally {
    loading.value = false
  }
}

// ✅ NAVIGATION DIRECTE vers le détail
const voirDetailEmploye = (employeId: string) => {
  console.log('👤 Navigation vers détail employé:', employeId)
  employeSelectionne.value = employeId
}

// ✅ RETOUR à la liste
const retourListeEmployes = () => {
  employeSelectionne.value = null
  // Optionnel: recharger les stats
  chargerEmployesAvecStats()
}

// ========== MÉTHODES UTILITAIRES ==========

const getInitiales = (employe: any) => {
  return `${employe.prenom?.charAt(0) || ''}${employe.nom?.charAt(0) || ''}`.toUpperCase()
}

const getStatutClasses = (statut: string) => {
  switch (statut) {
    case 'available': return 'bg-green-100 text-green-800'
    case 'full': return 'bg-orange-100 text-orange-800'
    case 'overloaded': return 'bg-red-100 text-red-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

const getStatutLabel = (statut: string) => {
  switch (statut) {
    case 'available': return 'Disponible'
    case 'full': return 'Chargé'
    case 'overloaded': return 'Surchargé'
    default: return 'Inconnu'
  }
}

// ========== LIFECYCLE ==========
onMounted(() => {
  chargerEmployesAvecStats()
})
</script>
