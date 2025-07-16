<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <div class="max-w-7xl mx-auto">

      <!-- ✅ EN-TÊTE AVEC BOUTON NOUVEAU -->
      <div class="flex justify-between items-center mb-6">
        <div>
          <h1 class="text-3xl font-bold text-gray-900">
            👥 Employés
          </h1>
          <p class="text-gray-600 mt-1">
            Gestion des employés et de leurs planifications
          </p>
        </div>

        <!-- Actions et sélecteur de date -->
        <div class="flex items-center space-x-3">
          <!-- Bouton Nouvel Employé -->
          <button
            v-if="!employeSelectionne"
            @click="showFormulaire = true"
            class="bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 flex items-center gap-2"
          >
            ➕ Nouvel Employé
          </button>

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

      <!-- ✅ MESSAGES DE FEEDBACK -->
      <div v-if="message.text" :class="[
        'mb-4 p-4 rounded-lg border',
        message.type === 'success' ? 'bg-green-50 border-green-200 text-green-800' : 'bg-red-50 border-red-200 text-red-800'
      ]">
        {{ message.text }}
      </div>

      <!-- ✅ FORMULAIRE DE CRÉATION D'EMPLOYÉ -->
      <div v-if="showFormulaire && !employeSelectionne" class="mb-6 bg-white border border-gray-200 rounded-lg p-6">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-lg font-semibold text-gray-900">➕ Nouvel Employé</h2>
          <button
            @click="annulerFormulaire"
            class="text-gray-500 hover:text-gray-700"
          >
            ❌
          </button>
        </div>

        <div class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">
                Nom *
              </label>
              <input
                v-model="nouvelEmploye.nom"
                type="text"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500',
                  errorsForm.nom ? 'border-red-500' : 'border-gray-300'
                ]"
                placeholder="Nom de famille"
              />
              <p v-if="errorsForm.nom" class="text-red-500 text-sm mt-1">{{ errorsForm.nom }}</p>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">
                Prénom *
              </label>
              <input
                v-model="nouvelEmploye.prenom"
                type="text"
                :class="[
                  'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500',
                  errorsForm.prenom ? 'border-red-500' : 'border-gray-300'
                ]"
                placeholder="Prénom"
              />
              <p v-if="errorsForm.prenom" class="text-red-500 text-sm mt-1">{{ errorsForm.prenom }}</p>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Email
            </label>
            <input
              v-model="nouvelEmploye.email"
              type="email"
              :class="[
                'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500',
                errorsForm.email ? 'border-red-500' : 'border-gray-300'
              ]"
              placeholder="email@exemple.com"
            />
            <p v-if="errorsForm.email" class="text-red-500 text-sm mt-1">{{ errorsForm.email }}</p>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">
              Heures de travail par jour
            </label>
            <input
              v-model.number="nouvelEmploye.heuresTravailParJour"
              type="number"
              min="1"
              max="12"
              :class="[
                'w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500',
                errorsForm.heuresTravailParJour ? 'border-red-500' : 'border-gray-300'
              ]"
            />
            <p v-if="errorsForm.heuresTravailParJour" class="text-red-500 text-sm mt-1">{{ errorsForm.heuresTravailParJour }}</p>
          </div>

          <div class="flex gap-3 pt-4">
            <button
              @click="creerEmploye"
              :disabled="loadingCreation"
              class="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 flex items-center gap-2 disabled:opacity-50"
            >
              <span v-if="loadingCreation" class="animate-spin">⏳</span>
              <span v-else>💾</span>
              {{ loadingCreation ? 'Création...' : 'Créer' }}
            </button>

            <button
              @click="annulerFormulaire"
              class="bg-gray-500 text-white px-4 py-2 rounded-lg hover:bg-gray-600"
            >
              ❌ Annuler
            </button>
          </div>
        </div>
      </div>

      <!-- ✅ LISTE OU DÉTAIL -->
      <div v-if="!employeSelectionne">
        <!-- LISTE DES EMPLOYÉS -->
        <div v-if="loading" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <span class="text-gray-600 mt-3 block">Chargement des employés...</span>
        </div>

        <div v-else-if="employesAvecStats.length === 0" class="text-center py-12">
          <div class="text-gray-500">
            <div class="text-4xl mb-4">👥</div>
            <div class="mb-4">Aucun employé trouvé pour cette date</div>
            <button
              @click="showFormulaire = true"
              class="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700"
            >
              ➕ Créer le premier employé
            </button>
          </div>
        </div>

        <!-- ✅ GRILLE D'EMPLOYÉS CLIQUABLES -->
        <div v-else>
          <!-- Statistiques globales -->
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            <div class="bg-blue-50 p-4 rounded-lg">
              <div class="text-2xl font-bold text-blue-900">{{ employesAvecStats.length }}</div>
              <div class="text-sm text-blue-800">👥 Total employés</div>
            </div>
            <div class="bg-green-50 p-4 rounded-lg">
              <div class="text-2xl font-bold text-green-900">{{ employesDisponibles }}</div>
              <div class="text-sm text-green-800">✅ Disponibles</div>
            </div>
            <div class="bg-yellow-50 p-4 rounded-lg">
              <div class="text-2xl font-bold text-yellow-900">{{ employesCharges }}</div>
              <div class="text-sm text-yellow-800">⚠️ Chargés</div>
            </div>
            <div class="bg-red-50 p-4 rounded-lg">
              <div class="text-2xl font-bold text-red-900">{{ employesSurcharges }}</div>
              <div class="text-sm text-red-800">🚨 Surchargés</div>
            </div>
          </div>

          <!-- Grille des employés -->
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
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
                <button
                  @click.stop="modifierEmploye(employe)"
                  class="flex-1 bg-gray-50 text-gray-600 px-3 py-2 rounded text-sm font-medium hover:bg-gray-100"
                >
                  ✏️ Modifier
                </button>
              </div>
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

// ========== TYPES ==========
interface NouvelEmploye {
  nom: string
  prenom: string
  email: string
  heuresTravailParJour: number
}

// ========== ÉTAT RÉACTIF ==========
const selectedDate = ref(new Date().toISOString().split('T')[0])
const employesAvecStats = ref<any[]>([])
const loading = ref(false)
const employeSelectionne = ref<string | null>(null)

// Formulaire création employé
const showFormulaire = ref(false)
const loadingCreation = ref(false)
const nouvelEmploye = ref<NouvelEmploye>({
  nom: '',
  prenom: '',
  email: '',
  heuresTravailParJour: 8
})
const errorsForm = ref<{[key: string]: string}>({})
const message = ref<{text: string, type: 'success' | 'error'}>({text: '', type: 'success'})

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

    // Essayer d'abord l'endpoint avec stats
    try {
      const response = await fetch(`http://localhost:8080/api/employes/avec-stats?date=${selectedDate.value}`)
      if (response.ok) {
        employesAvecStats.value = await response.json()
        console.log('✅ Employés avec stats chargés:', employesAvecStats.value.length)
        return
      }
    } catch (error) {
      console.log('🔄 Endpoint avec-stats indisponible, essai liste simple...')
    }

    // Fallback: endpoint liste simple
    const response = await fetch('http://localhost:8080/api/employes/frontend/liste')
    if (response.ok) {
      const employesSimples = await response.json()
      // Convertir au format avec stats
      employesAvecStats.value = employesSimples.map((emp: any) => ({
        ...emp,
        nombreCommandes: 0,
        dureeeTotaleFormatee: '0min',
        statut: 'available',
        pourcentageCharge: 0
      }))
      console.log('✅ Employés simples chargés:', employesAvecStats.value.length)
    } else {
      employesAvecStats.value = []
    }

  } catch (error) {
    console.error('❌ Erreur chargement employés:', error)
    employesAvecStats.value = []
  } finally {
    loading.value = false
  }
}

const creerEmploye = async () => {
  if (!validerFormulaire()) {
    return
  }

  loadingCreation.value = true
  try {
    const response = await fetch('http://localhost:8080/api/employes/frontend/creer', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(nouvelEmploye.value),
    })

    const result = await response.json()

    if (result.success) {
      message.value = { text: result.message, type: 'success' }
      annulerFormulaire()
      await chargerEmployesAvecStats() // Recharger la liste
    } else {
      message.value = { text: result.message, type: 'error' }
    }

  } catch (error) {
    console.error('❌ Erreur création employé:', error)
    message.value = { text: 'Erreur de connexion au serveur', type: 'error' }
  } finally {
    loadingCreation.value = false
  }
}

const validerFormulaire = (): boolean => {
  const errors: {[key: string]: string} = {}

  if (!nouvelEmploye.value.nom.trim()) {
    errors.nom = 'Le nom est obligatoire'
  }

  if (!nouvelEmploye.value.prenom.trim()) {
    errors.prenom = 'Le prénom est obligatoire'
  }

  if (nouvelEmploye.value.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(nouvelEmploye.value.email)) {
    errors.email = 'Email invalide'
  }

  if (nouvelEmploye.value.heuresTravailParJour < 1 || nouvelEmploye.value.heuresTravailParJour > 12) {
    errors.heuresTravailParJour = 'Entre 1 et 12 heures par jour'
  }

  errorsForm.value = errors
  return Object.keys(errors).length === 0
}

const annulerFormulaire = () => {
  showFormulaire.value = false
  nouvelEmploye.value = {
    nom: '',
    prenom: '',
    email: '',
    heuresTravailParJour: 8
  }
  errorsForm.value = {}
  message.value = { text: '', type: 'success' }
}

// ✅ NAVIGATION
const voirDetailEmploye = (employeId: string) => {
  console.log('👤 Navigation vers détail employé:', employeId)
  employeSelectionne.value = employeId
}

const retourListeEmployes = () => {
  employeSelectionne.value = null
  chargerEmployesAvecStats()
}

const modifierEmploye = (employe: any) => {
  console.log('✏️ Modifier employé:', employe.nomComplet)
  // TODO: Implémenter la modification
  message.value = { text: 'Modification sera disponible prochainement', type: 'error' }
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
