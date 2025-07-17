<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <div class="max-w-7xl mx-auto">

      <!-- ✅ EN-TÊTE UNIFIÉ avec modes de vue -->
      <div class="flex justify-between items-center mb-6">
        <div>
          <h1 class="text-3xl font-bold text-gray-900">
            👥 Employés
          </h1>
          <p class="text-gray-600 mt-1">
            Gestion complète des employés et de leurs planifications
          </p>
        </div>

        <!-- Actions et sélecteur de date -->
        <div class="flex items-center space-x-3">
          <!-- Mode de vue -->
          <div class="flex bg-gray-200 rounded-lg p-1">
            <button
              @click="modeVue = 'gestion'"
              :class="[
                'px-3 py-1 rounded text-sm font-medium transition-colors',
                modeVue === 'gestion' ? 'bg-white text-blue-600 shadow' : 'text-gray-600'
              ]"
            >
              👥 Gestion
            </button>
            <button
              @click="modeVue = 'planning'"
              :class="[
                'px-3 py-1 rounded text-sm font-medium transition-colors',
                modeVue === 'planning' ? 'bg-white text-blue-600 shadow' : 'text-gray-600'
              ]"
            >
              📅 Planning
            </button>
          </div>

          <!-- Bouton Nouvel Employé (mode gestion seulement) -->
          <button
            v-if="modeVue === 'gestion' && !employeSelectionne"
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
            @click="actualiserDonnees"
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

      <!-- ✅ FORMULAIRE DE CRÉATION D'EMPLOYÉ (mode gestion) -->
      <div v-if="showFormulaire && modeVue === 'gestion' && !employeSelectionne" class="mb-6 bg-white border border-gray-200 rounded-lg p-6">
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
              <label class="block text-sm font-medium text-gray-700 mb-1">Nom *</label>
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
              <label class="block text-sm font-medium text-gray-700 mb-1">Prénom *</label>
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
            <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
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
            <label class="block text-sm font-medium text-gray-700 mb-1">Heures de travail par jour</label>
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

      <!-- ✅ CONTENU SELON LE MODE -->
      <div v-if="!employeSelectionne">

        <!-- ========== MODE GESTION ========== -->
        <div v-if="modeVue === 'gestion'">
          <div v-if="loading" class="text-center py-12">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <span class="text-gray-600 mt-3 block">Chargement des employés...</span>
          </div>

          <div v-else-if="employesListe.length === 0" class="text-center py-12">
            <div class="text-gray-500">
              <div class="text-4xl mb-4">👥</div>
              <div class="mb-4">Aucun employé dans l'équipe</div>
              <button
                @click="showFormulaire = true"
                class="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700"
              >
                ➕ Créer le premier employé
              </button>
            </div>
          </div>

          <!-- Liste des employés mode gestion -->
          <div v-else>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              <div
                v-for="employe in employesListe"
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
                      {{ employe.nomComplet }}
                    </h3>
                    <p class="text-sm text-gray-500">{{ employe.email }}</p>
                  </div>
                </div>

                <!-- Informations -->
                <div class="space-y-2 text-sm">
                  <div class="flex justify-between">
                    <span>⏰ Heures/jour:</span>
                    <span class="font-medium">{{ employe.heuresTravailParJour }}h</span>
                  </div>
                  <div class="flex justify-between">
                    <span>📅 Depuis:</span>
                    <span class="font-medium">{{ formatDate(employe.dateCreation) }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span>📊 Statut:</span>
                    <span :class="[
                      'px-2 py-1 rounded text-xs font-medium',
                      employe.actif ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    ]">
                      {{ employe.actif ? 'Actif' : 'Inactif' }}
                    </span>
                  </div>
                </div>

                <!-- Actions -->
                <div class="mt-4 flex space-x-2">
                  <button class="flex-1 bg-blue-50 text-blue-600 px-3 py-2 rounded text-sm font-medium hover:bg-blue-100">
                    📋 Planning
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

        <!-- ========== MODE PLANNING ========== -->
        <div v-else-if="modeVue === 'planning'">
          <div v-if="loading" class="text-center py-12">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <span class="text-gray-600 mt-3 block">Chargement du planning...</span>
          </div>

          <div v-else-if="employesPlanning.length === 0" class="text-center py-12">
            <div class="text-gray-500">
              <div class="text-4xl mb-4">📅</div>
              <div>Aucun planning trouvé pour cette date</div>
            </div>
          </div>

          <!-- Planning des employés -->
          <div v-else>
            <!-- Statistiques globales -->
            <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
              <div class="bg-blue-50 p-4 rounded-lg">
                <div class="text-2xl font-bold text-blue-900">{{ employesPlanning.length }}</div>
                <div class="text-sm text-blue-800">👥 Employés actifs</div>
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

            <!-- Grille des employés avec planning -->
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              <div
                v-for="employe in employesPlanning"
                :key="employe.id"
                @click="voirDetailEmploye(employe.id)"
                :class="[
                  'bg-white rounded-lg shadow-md hover:shadow-lg transition-all cursor-pointer border-l-4 p-6',
                  getStatusBorderColor(employe.status)
                ]"
              >
                <!-- Avatar et nom -->
                <div class="flex items-center mb-4">
                  <div class="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 font-bold text-lg">
                    {{ getInitiales(employe) }}
                  </div>
                  <div class="ml-3">
                    <h3 class="text-lg font-semibold text-gray-900">
                      {{ employe.name }}
                    </h3>
                    <span :class="['px-2 py-1 rounded text-xs font-medium', getStatusClasses(employe.status)]">
                      {{ getStatusLabel(employe.status) }}
                    </span>
                  </div>
                </div>

                <!-- Statistiques planning -->
                <div class="grid grid-cols-2 gap-4 mb-4">
                  <div class="text-center">
                    <div class="text-2xl font-bold text-blue-600">{{ employe.taskCount || 0 }}</div>
                    <div class="text-xs text-gray-500">Tâches</div>
                  </div>
                  <div class="text-center">
                    <div class="text-2xl font-bold text-green-600">{{ formatTime(employe.totalMinutes) }}</div>
                    <div class="text-xs text-gray-500">Durée</div>
                  </div>
                </div>

                <!-- Barre de charge -->
                <div class="mb-4">
                  <div class="flex justify-between text-sm mb-1">
                    <span>Charge de travail</span>
                    <span>{{ Math.round((employe.totalMinutes / employe.maxMinutes) * 100) }}%</span>
                  </div>
                  <div class="w-full bg-gray-200 rounded-full h-2">
                    <div
                      :class="[
                        'h-2 rounded-full',
                        employe.status === 'overloaded' ? 'bg-red-500' :
                        employe.status === 'full' ? 'bg-yellow-500' : 'bg-green-500'
                      ]"
                      :style="{ width: Math.min((employe.totalMinutes / employe.maxMinutes) * 100, 100) + '%' }"
                    ></div>
                  </div>
                </div>

                <!-- Actions planning -->
                <div class="flex space-x-2">
                  <button class="flex-1 bg-blue-50 text-blue-600 px-3 py-2 rounded text-sm font-medium hover:bg-blue-100">
                    👁️ Détail
                  </button>
                  <button class="flex-1 bg-gray-50 text-gray-600 px-3 py-2 rounded text-sm font-medium hover:bg-gray-100">
                    📋 {{ employe.taskCount }} tâches
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ✅ COMPOSANT DE DÉTAIL EMPLOYÉ (unifié pour les deux modes) -->
      <div v-else>
        <EmployeeDetailPage
          :employeeId="employeSelectionne"
          :selectedDate="selectedDate"
          @back="retourListeEmployes"
          @refresh="actualiserDonnees"
        />
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
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
const modeVue = ref<'gestion' | 'planning'>('gestion') // ✅ Mode de vue unifié
const employeSelectionne = ref<string | null>(null)
const loading = ref(false)

// Données employés
const employesListe = ref<any[]>([]) // Mode gestion
const employesPlanning = ref<any[]>([]) // Mode planning

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
  employesPlanning.value.filter(emp => emp.status === 'available').length
)

const employesCharges = computed(() =>
  employesPlanning.value.filter(emp => emp.status === 'full').length
)

const employesSurcharges = computed(() =>
  employesPlanning.value.filter(emp => emp.status === 'overloaded').length
)

// ========== MÉTHODES PRINCIPALES ==========

const actualiserDonnees = async () => {
  if (modeVue.value === 'gestion') {
    await chargerEmployesListe()
  } else {
    await chargerEmployesPlanning()
  }
}

const chargerEmployesListe = async () => {
  loading.value = true
  try {
    console.log('👥 Chargement liste employés...')
    const response = await fetch('http://localhost:8080/api/employes/frontend/liste')
    if (response.ok) {
      employesListe.value = await response.json()
      console.log('✅ Employés liste chargés:', employesListe.value.length)
    } else {
      employesListe.value = []
    }
  } catch (error) {
    console.error('❌ Erreur chargement employés liste:', error)
    employesListe.value = []
  } finally {
    loading.value = false
  }
}

const chargerEmployesPlanning = async () => {
  loading.value = true
  try {
    console.log('📅 Chargement planning employés pour:', selectedDate.value)

    // Essayer l'API planning en premier
    try {
      const planningEmployes = await apiService.getPlanningEmployes(selectedDate.value)
      employesPlanning.value = planningEmployes
      console.log('✅ Planning employés chargé:', employesPlanning.value.length)
    } catch (error) {
      console.log('🔄 API planning indisponible, utilisation liste simple...')
      // Fallback: convertir la liste simple en format planning
      await chargerEmployesListe()
      employesPlanning.value = employesListe.value.map((emp: any) => ({
        id: emp.id,
        name: emp.nomComplet,
        totalMinutes: 0,
        maxMinutes: emp.heuresTravailParJour * 60,
        status: 'available',
        taskCount: 0,
        cardCount: 0
      }))
    }
  } catch (error) {
    console.error('❌ Erreur chargement planning:', error)
    employesPlanning.value = []
  } finally {
    loading.value = false
  }
}

// Gestion du formulaire
const creerEmploye = async () => {
  if (!validerFormulaire()) return

  loadingCreation.value = true
  try {
    const response = await fetch('http://localhost:8080/api/employes/frontend/creer', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(nouvelEmploye.value),
    })

    const result = await response.json()
    if (result.success) {
      message.value = { text: result.message, type: 'success' }
      annulerFormulaire()
      await actualiserDonnees()
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
  if (!nouvelEmploye.value.nom.trim()) errors.nom = 'Le nom est obligatoire'
  if (!nouvelEmploye.value.prenom.trim()) errors.prenom = 'Le prénom est obligatoire'
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
  nouvelEmploye.value = { nom: '', prenom: '', email: '', heuresTravailParJour: 8 }
  errorsForm.value = {}
  message.value = { text: '', type: 'success' }
}

// Navigation
const voirDetailEmploye = (employeId: string) => {
  console.log('👤 Navigation vers détail employé:', employeId)
  employeSelectionne.value = employeId
}

const retourListeEmployes = () => {
  employeSelectionne.value = null
  actualiserDonnees()
}

const modifierEmploye = (employe: any) => {
  console.log('✏️ Modifier employé:', employe.nomComplet)
  message.value = { text: 'Modification sera disponible prochainement', type: 'error' }
}

// ========== MÉTHODES UTILITAIRES ==========

const getInitiales = (employe: any) => {
  if (employe.nomComplet) {
    return employe.nomComplet.split(' ').map((n: string) => n.charAt(0)).join('').toUpperCase().substring(0, 2)
  }
  return `${employe.prenom?.charAt(0) || ''}${employe.nom?.charAt(0) || ''}`.toUpperCase()
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('fr-FR')
}

const formatTime = (minutes: number): string => {
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return `${hours}h${mins.toString().padStart(2, '0')}`
}

const getStatusClasses = (status: string) => {
  switch (status) {
    case 'available': return 'bg-green-100 text-green-800'
    case 'full': return 'bg-orange-100 text-orange-800'
    case 'overloaded': return 'bg-red-100 text-red-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

const getStatusLabel = (status: string) => {
  switch (status) {
    case 'available': return 'Disponible'
    case 'full': return 'Chargé'
    case 'overloaded': return 'Surchargé'
    default: return 'Inconnu'
  }
}

const getStatusBorderColor = (status: string) => {
  switch (status) {
    case 'available': return 'border-green-500'
    case 'full': return 'border-yellow-500'
    case 'overloaded': return 'border-red-500'
    default: return 'border-blue-500'
  }
}

// ========== WATCHERS ==========
watch(modeVue, (newMode) => {
  console.log('🔄 Changement mode vue:', newMode)
  actualiserDonnees()
})

watch(selectedDate, () => {
  if (modeVue.value === 'planning') {
    chargerEmployesPlanning()
  }
})

// ========== LIFECYCLE ==========
onMounted(() => {
  actualiserDonnees()
})
</script>
