<template>
  <div class="space-y-6">
    <!-- En-tête -->
    <div class="flex justify-between items-center">
      <h2 class="text-2xl font-bold text-gray-900">📊 Dashboard</h2>
      <button
        @click="refreshData"
        :disabled="loading"
        class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
      >
        {{ loading ? '🔄 Chargement...' : '🔄 Actualiser' }}
      </button>
    </div>

    <!-- Message de bienvenue -->
    <div class="bg-blue-50 border border-blue-200 rounded-lg p-6">
      <h3 class="text-lg font-semibold text-blue-900 mb-2">
        🎉 Bienvenue dans votre application de gestion Pokemon !
      </h3>
      <p class="text-blue-700">
        Cette application vous permet de gérer vos commandes de cartes Pokemon avec une planification automatique intelligente.
      </p>
    </div>

    <!-- Statistiques de base -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6">
      <div class="bg-white p-6 rounded-lg shadow border-l-4 border-blue-500">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600">Commandes Totales</p>
            <p class="text-2xl font-bold text-blue-600">{{ stats.totalCommandes || 0 }}</p>
          </div>
          <div class="text-3xl text-blue-600">📦</div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-lg shadow border-l-4 border-yellow-500">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600">En Attente</p>
            <p class="text-2xl font-bold text-yellow-600">{{ stats.commandesEnAttente || 0 }}</p>
          </div>
          <div class="text-3xl text-yellow-600">⏳</div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-lg shadow border-l-4 border-green-500">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600">Terminées</p>
            <p class="text-2xl font-bold text-green-600">{{ stats.commandesTerminees || 0 }}</p>
          </div>
          <div class="text-3xl text-green-600">✅</div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-lg shadow border-l-4 border-purple-500">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600">Employés Actifs</p>
            <p class="text-2xl font-bold text-purple-600">{{ stats.employesActifs || 0 }}</p>
          </div>
          <div class="text-3xl text-purple-600">👥</div>
        </div>
      </div>
    </div>

    <!-- Actions rapides -->
    <div class="bg-white p-6 rounded-lg shadow">
      <h3 class="text-lg font-semibold mb-4">⚡ Actions rapides</h3>
      <div class="flex flex-wrap gap-3">
        <button
          @click="goToCommandes"
          class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          📋 Voir les Commandes
        </button>
        <button
          @click="goToEmployes"
          class="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors"
        >
          👤 Gérer les Employés
        </button>
        <button
          @click="goToPlanification"
          class="bg-purple-600 text-white px-4 py-2 rounded-lg hover:bg-purple-700 transition-colors"
        >
          📅 Voir la Planification
        </button>
        <button
          @click="planifierAutomatiquement"
          :disabled="loading"
          class="bg-orange-600 text-white px-4 py-2 rounded-lg hover:bg-orange-700 disabled:opacity-50 transition-colors"
        >
          🤖 Planification Auto
        </button>
        <button
          @click="testDebugCartes"
          class="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors"
        >
          🔍 DEBUG Cartes
        </button>
      </div>
    </div>

    <!-- Status de connexion API -->
    <div class="bg-white p-6 rounded-lg shadow">
      <h3 class="text-lg font-semibold mb-4">🔗 État de la connexion</h3>
      <div class="flex items-center space-x-2">
        <div :class="[
          'w-3 h-3 rounded-full',
          apiConnected ? 'bg-green-500' : 'bg-red-500'
        ]"></div>
        <span :class="[
          'text-sm font-medium',
          apiConnected ? 'text-green-700' : 'text-red-700'
        ]">
          {{ apiConnected ? 'API Backend connectée' : 'API Backend déconnectée' }}
        </span>
      </div>
      <p class="text-sm text-gray-600 mt-2">
        Backend: <code class="bg-gray-100 px-2 py-1 rounded">http://localhost:8080</code>
      </p>

      <!-- Détails de la dernière synchronisation -->
      <div v-if="lastSync" class="mt-3 text-xs text-gray-500">
        Dernière sync: {{ new Date(lastSync).toLocaleTimeString() }}
      </div>
    </div>

    <!-- Données en temps réel -->
    <div v-if="apiConnected && rawApiData" class="bg-white p-6 rounded-lg shadow">
      <h3 class="text-lg font-semibold mb-4">📡 Données API brutes (Debug)</h3>
      <pre class="bg-gray-50 p-3 rounded text-xs overflow-x-auto">{{ JSON.stringify(rawApiData, null, 2) }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, inject } from 'vue'
import { apiService, type DashboardStats } from '../services/api'

// État local
const loading = ref(false)
const apiConnected = ref(false)
const lastSync = ref<string | null>(null)
const rawApiData = ref<any>(null)

// Stats corrigées selon votre backend
const stats = ref<DashboardStats>({
  totalCommandes: 0,
  commandesEnAttente: 0,
  commandesEnCours: 0,
  commandesTerminees: 0,
  employesActifs: 0,
  status: 'disconnected'
})

// Injection des fonctions partagées
const showNotification = inject('showNotification') as (message: string, type?: 'success' | 'error') => void
const changeTab = inject('changeTab') as (tabId: string) => void

// Fonctions de navigation
const goToCommandes = () => {
  console.log('Navigation vers commandes')
  changeTab('commandes')
}

const goToEmployes = () => {
  console.log('Navigation vers employes')
  changeTab('employes')
}

const goToPlanification = () => {
  console.log('Navigation vers planification')
  changeTab('planification')
}

// Planification automatique
const planifierAutomatiquement = async () => {
  loading.value = true
  try {
    const rapport = await apiService.planifierAutomatique()
    showNotification?.(
      `Planification terminée: ${rapport.nombreCommandesPlanifiees || 0} commandes planifiées`,
      'success'
    )
    await loadData() // Actualiser les données
  } catch (error) {
    console.error('Erreur planification automatique:', error)
    showNotification?.('Erreur lors de la planification automatique', 'error')
  } finally {
    loading.value = false
  }
}

// Fonction de test pour déboguer les cartes
/// ✅ Remplacez temporairement votre fonction debugCartes dans DashboardView.vue
// ✅ Remplacez votre fonction debugCartes dans DashboardView.vue
const testDebugCartes = async () => {
  try {
    console.log('🔍 Test des endpoints existants...')

    // ✅ Test 1: Dashboard stats (on sait que ça marche)
    console.log('📊 Test dashboard...')
    const dashResponse = await fetch('http://localhost:8080/api/dashboard/stats', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      }
    })

    if (dashResponse.ok) {
      const dashResult = await dashResponse.json()
      console.log('✅ Dashboard OK:', dashResult)
    } else {
      console.log('❌ Dashboard failed:', dashResponse.status)
    }

    // ✅ Test 2: Employés
    console.log('👥 Test employés...')
    const empResponse = await fetch('http://localhost:8080/api/employes', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      }
    })

    if (empResponse.ok) {
      const employes = await empResponse.json()
      console.log('✅ Employés OK:', employes.length, 'employés trouvés')
    } else {
      console.log('❌ Employés failed:', empResponse.status)
    }

    // ✅ Test 3: Commandes
    console.log('📦 Test commandes...')
    const cmdResponse = await fetch('http://localhost:8080/api/commandes', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      }
    })

    if (cmdResponse.ok) {
      const commandes = await cmdResponse.json()
      console.log('✅ Commandes OK:', commandes.length, 'commandes trouvées')

      // ✅ Test 4: Cartes (si on a des commandes)
      if (commandes.length > 0) {
        const commande = commandes[0]
        console.log('🎴 Test cartes pour commande:', commande.id)

        const cartesResponse = await fetch(`http://localhost:8080/api/commandes/${commande.id}/cartes`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
          }
        })

        if (cartesResponse.ok) {
          const cartes = await cartesResponse.json()
          console.log('✅ Cartes OK:', cartes)
          showNotification?.(`Cartes trouvées: ${cartes.nombreCartes}`, 'success')
        } else {
          const errorText = await cartesResponse.text()
          console.log('❌ Cartes failed:', cartesResponse.status, errorText)
          showNotification?.('Erreur cartes: ' + cartesResponse.status, 'error')
        }
      }
    } else {
      console.log('❌ Commandes failed:', cmdResponse.status)
    }

  } catch (error) {
    console.error('❌ Erreur test:', error)
    showNotification?.(`Erreur: ${error.message}`, 'error')
  }
}
// Test de connexion API
const testApiConnection = async () => {
  try {
    console.log('🔍 Test de connexion à l\'API...')

    // Test simple avec l'endpoint de test
    await apiService.testConnection()

    apiConnected.value = true
    lastSync.value = new Date().toISOString()
    console.log('✅ API connectée avec succès!')

    return true
  } catch (error) {
    apiConnected.value = false
    console.log('❌ API non disponible:', error)

    return false
  }
}

// Chargement des données RÉELLES
const loadData = async () => {
  loading.value = true
  try {
    console.log('📊 Chargement des statistiques du dashboard...')

    if (apiConnected.value) {
      // Récupérer les vraies données de votre API
      const dashboardStats = await apiService.getDashboardStats()

      console.log('📈 Données reçues:', dashboardStats)
      rawApiData.value = dashboardStats // Pour debug

      // Mapper les données selon votre structure backend
      stats.value = {
        totalCommandes: dashboardStats.totalCommandes || 0,
        commandesEnAttente: dashboardStats.commandesEnAttente || 0,
        commandesEnCours: dashboardStats.commandesEnCours || 0,
        commandesTerminees: dashboardStats.commandesTerminees || 0,
        employesActifs: dashboardStats.employesActifs || 0,
        status: dashboardStats.status || 'success'
      }

      lastSync.value = new Date().toISOString()

      showNotification?.(`✅ Dashboard mis à jour: ${stats.value.totalCommandes} commandes`)

    } else {
      // Fallback : données de démonstration
      console.log('⚠️  Mode démo - utilisation de données fictives')
      stats.value = {
        totalCommandes: 15,
        commandesEnAttente: 8,
        commandesEnCours: 3,
        commandesTerminees: 4,
        employesActifs: 3,
        status: 'demo'
      }
    }
  } catch (error) {
    console.error('❌ Erreur lors du chargement:', error)
    showNotification?.('Erreur lors du chargement des données', 'error')

    // En cas d'erreur, utiliser des données par défaut
    stats.value = {
      totalCommandes: 0,
      commandesEnAttente: 0,
      commandesEnCours: 0,
      commandesTerminees: 0,
      employesActifs: 0,
      status: 'error'
    }
  } finally {
    loading.value = false
  }
}

// Actualisation des données
const refreshData = async () => {
  console.log('🔄 Actualisation du dashboard...')

  // Re-tester la connexion puis charger les données
  const connected = await testApiConnection()
  if (connected) {
    await loadData()
  } else {
    // Charger en mode démo
    await loadData()
  }
}

// Lifecycle
onMounted(async () => {
  console.log('🚀 Initialisation du dashboard...')

  await testApiConnection()
  await loadData()

  // Auto-refresh toutes les 30 secondes si connecté
  setInterval(() => {
    if (apiConnected.value && !loading.value) {
      loadData()
    }
  }, 30000)
})
</script>
