<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- ✅ EN-TÊTE AVEC RETOUR ET BOUTONS -->
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center">
        <button
          @click="$emit('back')"
          class="mr-4 p-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-full"
        >
          ← Retour
        </button>
        <div>
          <h1 class="text-3xl font-bold text-gray-900">
            👤 {{ employee?.name || 'Chargement...' }}
          </h1>
          <p class="text-gray-600 mt-1">
            {{ formatDate(selectedDate) }}
          </p>
        </div>
      </div>

      <!-- Boutons d'action -->
      <div class="flex space-x-3">
        <!-- Bouton charger toutes les cartes -->
        <button
          @click="chargerToutesLesCartes"
          :disabled="loading || !employee?.tasks?.length || loadingAllCards"
          class="bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 disabled:bg-gray-400 flex items-center"
        >
          <span v-if="loadingAllCards" class="mr-2">
            <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
          </span>
          🃏 {{ loadingAllCards ? 'Chargement...' : 'Charger toutes les cartes' }}
        </button>

        <!-- Bouton actualiser -->
        <button
          @click="refreshEmployeeData"
          class="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
        >
          🔄 Actualiser
        </button>
      </div>
    </div>

    <!-- ✅ CHARGEMENT -->
    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
      <span class="text-gray-600 mt-3 block">Chargement des données employé...</span>
    </div>

    <!-- ✅ CONTENU PRINCIPAL - DÉTAIL EMPLOYÉ -->
    <div v-else-if="employee" class="space-y-6">

      <!-- Carte de profil employé -->
      <div class="bg-white rounded-lg shadow-md p-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center">
            <div class="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 font-bold text-xl">
              {{ getInitiales(employee.name) }}
            </div>
            <div class="ml-4">
              <h2 class="text-2xl font-bold text-gray-900">{{ employee.name }}</h2>
              <span :class="['px-3 py-1 rounded-full text-sm font-medium', getStatusClasses(employee.status)]">
                {{ getStatusText(employee.status) }}
              </span>
            </div>
          </div>

          <!-- Statistiques rapides -->
          <div class="grid grid-cols-4 gap-6 text-center">
            <div>
              <div class="text-2xl font-bold text-blue-600">{{ totalTasks }}</div>
              <div class="text-sm text-gray-500">Commandes</div>
              <div class="text-xs text-gray-400 mt-1">
                {{ completedTasks }}/{{ totalTasks }} terminées
              </div>
            </div>
            <div>
              <div class="text-2xl font-bold text-green-600">{{ totalCards }}</div>
              <div class="text-sm text-gray-500">Cartes total</div>
              <div class="text-xs text-gray-400 mt-1">
                {{ cartesChargees }}/{{ totalCards }} détaillées
              </div>
            </div>
            <div>
              <div class="text-2xl font-bold text-purple-600">{{ formatTime(employee.totalMinutes) }}</div>
              <div class="text-sm text-gray-500">Durée totale</div>
              <div class="text-xs text-gray-400 mt-1">
                / {{ formatTime(employee.maxMinutes) }} max
              </div>
            </div>
            <div v-if="cartesChargees > 0">
              <div class="text-2xl font-bold text-orange-600">{{ statsCartesQualite.pourcentage }}%</div>
              <div class="text-sm text-gray-500">Qualité noms</div>
              <div class="text-xs text-gray-400 mt-1">
                {{ statsCartesQualite.avecNom }}/{{ statsCartesQualite.total }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Filtres des commandes -->
      <div class="bg-white rounded-lg shadow-md p-4">
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-semibold text-gray-900">📋 Commandes assignées</h3>
          <div class="flex space-x-2">
            <button
              v-for="filtre in filtresCommandes"
              :key="filtre.value"
              @click="filtreActif = filtre.value"
              :class="[
                'px-3 py-1 rounded text-sm font-medium transition-colors',
                filtreActif === filtre.value
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              ]"
            >
              {{ filtre.label }}
            </button>
          </div>
        </div>
      </div>

      <!-- Liste des commandes -->
      <div class="space-y-4">
        <div
          v-for="task in commandesFiltrees"
          :key="task.id"
          class="bg-white rounded-lg shadow-md overflow-hidden"
        >
          <div class="p-6">
            <!-- En-tête de la tâche -->
            <div class="flex items-center justify-between mb-4">
              <div>
                <h4 class="text-lg font-semibold text-gray-900">
                  {{ task.numeroCommande || `Commande ${task.id}` }}
                </h4>
                <div class="flex items-center space-x-4 mt-1">
                  <span :class="['px-2 py-1 rounded text-xs font-medium', getPriorityClasses(task.priority)]">
                    {{ task.priority }}
                  </span>
                  <span :class="['px-2 py-1 rounded text-xs font-medium', getStatusClasses(task.status)]">
                    {{ task.status }}
                  </span>
                  <span v-if="task.terminee" class="px-2 py-1 bg-green-100 text-green-800 rounded text-xs font-medium">
                    ✅ Terminée
                  </span>
                </div>
              </div>
              <div class="flex items-center space-x-2">
                <button
                  v-if="task.status !== 'Terminée' && !task.terminee"
                  @click="marquerTerminee(task)"
                  class="bg-green-100 text-green-600 px-3 py-1 rounded text-sm hover:bg-green-200"
                >
                  ✅ Terminer
                </button>
                <button
                  @click="toggleTaskCards(task)"
                  class="bg-blue-100 text-blue-600 px-3 py-1 rounded text-sm hover:bg-blue-200"
                >
                  {{ task.expanded ? '📦 Masquer' : '🎴 Voir cartes' }}
                </button>
              </div>
            </div>

            <!-- Détails de la commande -->
            <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
              <div>
                <div class="text-sm text-gray-500">Heure début</div>
                <div class="font-medium">{{ task.heureDebut || task.startTime }}</div>
              </div>
              <div>
                <div class="text-sm text-gray-500">Heure fin</div>
                <div class="font-medium">{{ task.heureFin || task.endTime }}</div>
              </div>
              <div>
                <div class="text-sm text-gray-500">Durée</div>
                <div class="font-medium">{{ formatTime(task.duration || task.dureeCalculee) }}</div>
              </div>
              <div>
                <div class="text-sm text-gray-500">Cartes</div>
                <div class="font-medium">{{ task.cardCount || task.nombreCartes || 0 }}</div>
              </div>
            </div>

            <!-- Barre de progression de la tâche -->
            <div class="mb-4">
              <div class="w-full bg-gray-200 rounded-full h-2">
                <div
                  class="bg-blue-600 h-2 rounded-full"
                  :style="{ width: getProgressWidth(task) }"
                ></div>
              </div>
            </div>

            <!-- ✅ SECTION DES CARTES DÉTAILLÉES -->
            <div v-if="task.expanded" class="mt-4 p-4 bg-gray-50 rounded-lg">
              <div class="flex justify-between items-center mb-4">
                <h5 class="font-medium text-gray-900">
                  🃏 Cartes à certifier ({{ task.cards?.length || task.cardCount || 0 }})
                </h5>
                <button
                  v-if="!task.cards?.length && (task.cardCount || 0) > 0"
                  @click="chargerCartesCommande(task)"
                  :disabled="task.loadingCards"
                  class="bg-blue-100 text-blue-600 px-3 py-1 rounded text-sm hover:bg-blue-200 disabled:bg-gray-100"
                >
                  {{ task.loadingCards ? '⏳ Chargement...' : '📦 Charger les cartes' }}
                </button>
              </div>

              <!-- État de chargement des cartes -->
              <div v-if="task.loadingCards" class="text-center py-4">
                <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600 mx-auto"></div>
                <span class="text-gray-600 text-sm mt-2 block">Chargement des cartes...</span>
              </div>

              <!-- Liste des cartes en rectangles -->
              <div v-else-if="task.cards?.length" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-3">
                <div
                  v-for="carte in task.cards"
                  :key="carte.cert_id || carte.id"
                  class="bg-white border border-gray-200 rounded-lg p-3 shadow-sm hover:shadow-md transition-shadow"
                >
                  <!-- En-tête de la carte -->
                  <div class="flex items-start justify-between mb-2">
                    <div class="flex-1 min-w-0">
                      <h6 class="font-medium text-gray-900 text-sm truncate" :title="carte.nom || carte.name">
                        {{ carte.nom || carte.name || 'Carte inconnue' }}
                      </h6>
                      <p class="text-xs text-gray-500 truncate" v-if="carte.label_name">
                        {{ carte.label_name }}
                      </p>
                    </div>
                    <!-- Indicateur de qualité -->
                    <span
                      v-if="carte.nom && !carte.nom.startsWith('Carte-')"
                      class="bg-green-100 text-green-600 text-xs px-1.5 py-0.5 rounded"
                      title="Nom de carte disponible"
                    >
                      ✅
                    </span>
                    <span
                      v-else
                      class="bg-orange-100 text-orange-600 text-xs px-1.5 py-0.5 rounded"
                      title="Nom générique"
                    >
                      ⚠️
                    </span>
                  </div>

                  <!-- Informations techniques -->
                  <div class="space-y-1 text-xs text-gray-600">
                    <div class="flex justify-between">
                      <span>📊 Code barre:</span>
                      <span class="font-mono">{{ carte.code_barre || 'N/A' }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span>🌍 Langue:</span>
                      <span class="uppercase font-medium">{{ carte.cert_langue || carte.langue || 'N/A' }}</span>
                    </div>
                    <div class="flex justify-between" v-if="carte.edition">
                      <span>📖 Édition:</span>
                      <span>{{ carte.edition }}</span>
                    </div>
                  </div>

                  <!-- Métriques de temps et prix -->
                  <div class="flex justify-between items-center mt-3 pt-2 border-t border-gray-100">
                    <div class="flex items-center text-xs text-gray-600">
                      <span>⏱️</span>
                      <span class="ml-1">{{ formatTime(carte.duration || Math.floor((task.duration || task.dureeCalculee || 30) / ((task.cardCount || task.nombreCartes) || 1))) }}</span>
                    </div>
                    <div class="flex items-center text-xs text-gray-600">
                      <span>💰</span>
                      <span class="ml-1">{{ (carte.amount || ((task.amount || 0) / ((task.cardCount || task.nombreCartes) || 1))).toFixed(2) }}€</span>
                    </div>
                  </div>

                  <!-- ID de référence (debugging) -->
                  <div class="mt-2 text-xs text-gray-400 truncate" :title="carte.cert_id || carte.id">
                    ID: {{ (carte.cert_id || carte.id || '').toString().substring(0, 8) }}...
                  </div>
                </div>
              </div>

              <!-- Message si pas de cartes chargées -->
              <div v-else-if="(task.cardCount || task.nombreCartes || 0) > 0" class="text-center py-4 text-gray-500">
                <div class="text-sm">
                  Cette commande contient {{ task.cardCount || task.nombreCartes }} cartes à traiter.
                </div>
                <div class="text-xs mt-1">
                  Durée estimée: {{ formatTime(task.duration || task.dureeCalculee || 30) }} •
                  Montant: {{ (task.amount || 0).toFixed(2) }}€
                </div>
              </div>

              <!-- Message si aucune carte -->
              <div v-else class="text-center py-4 text-gray-500">
                <div class="text-sm">Aucune carte associée à cette commande</div>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>

    <!-- ✅ ERREUR -->
    <div v-else class="text-center py-12">
      <div class="text-gray-500">
        <div class="text-4xl mb-4">❌</div>
        <div>Erreur lors du chargement des données employé</div>
        <button
          @click="refreshEmployeeData"
          class="mt-4 bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
        >
          Réessayer
        </button>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { apiService } from '../services/api'

// ========== TYPES ==========
interface CardDetail {
  id: string
  cert_id: string
  card_id: string
  nom: string
  name: string
  label_name?: string
  code_barre: string
  cert_langue: string
  langue: string
  edition?: number
  duration: number
  amount: number
  statut_correspondance?: string
}

interface Task {
  id: string
  numeroCommande: string
  priority: 'Haute' | 'Moyenne' | 'Basse' | 'URGENTE' | 'NORMALE'
  status: 'En cours' | 'Planifiée' | 'Terminée'
  startTime: string
  endTime: string
  heureDebut?: string
  heureFin?: string
  duration: number
  dureeCalculee?: number
  amount: number
  cardCount: number
  nombreCartes?: number
  cards: CardDetail[]
  expanded?: boolean
  terminee?: boolean
  loadingCards?: boolean
}

interface Employee {
  id: string
  name: string
  totalMinutes: number
  maxMinutes: number
  status: 'overloaded' | 'available' | 'full'
  tasks: Task[]
}

// ========== PROPS & EMITS ==========
interface Props {
  employeeId: string
  selectedDate?: string
}

const props = withDefaults(defineProps<Props>(), {
  selectedDate: () => new Date().toISOString().split('T')[0]
})

const emit = defineEmits<{
  back: []
  refresh: []
}>()

// ========== ÉTAT RÉACTIF ==========
const selectedDate = ref(props.selectedDate)
const employee = ref<Employee | null>(null)
const loading = ref(false)
const loadingAllCards = ref(false)
const filtreActif = ref('all')

// ========== FILTRES ==========
const filtresCommandes = [
  { value: 'all', label: 'Toutes' },
  { value: 'planifiees', label: 'Planifiées' },
  { value: 'encours', label: 'En Cours' },
  { value: 'terminees', label: 'Terminées' }
]

// ========== COMPUTED ==========
const totalTasks = computed(() => employee.value?.tasks.length || 0)

const totalCards = computed(() =>
  employee.value?.tasks.reduce((total, task) => total + (task.nombreCartes || task.cardCount || 0), 0) || 0
)

const completedTasks = computed(() =>
  employee.value?.tasks.filter(task => task.status === 'Terminée' || task.terminee).length || 0
)

const cartesChargees = computed(() => {
  if (!employee.value?.tasks) return 0

  return employee.value.tasks.reduce((total, task) => {
    return total + (task.cards?.length || 0)
  }, 0)
})

const statsCartesQualite = computed(() => {
  if (!employee.value?.tasks) return { avecNom: 0, sansNom: 0, total: 0, pourcentage: 0 }

  let avecNom = 0
  let sansNom = 0

  employee.value.tasks.forEach(task => {
    if (task.cards) {
      task.cards.forEach(carte => {
        if (carte.nom && !carte.nom.startsWith('Carte-')) {
          avecNom++
        } else {
          sansNom++
        }
      })
    }
  })

  const total = avecNom + sansNom
  const pourcentage = total > 0 ? Math.round((avecNom / total) * 100) : 0

  return { avecNom, sansNom, total, pourcentage }
})

const commandesFiltrees = computed(() => {
  if (!employee.value?.tasks) return []

  const commandes = employee.value.tasks

  switch (filtreActif.value) {
    case 'planifiees':
      return commandes.filter(cmd => cmd.status === 'Planifiée' && !cmd.terminee)
    case 'encours':
      return commandes.filter(cmd => cmd.status === 'En cours')
    case 'terminees':
      return commandes.filter(cmd => cmd.status === 'Terminée' || cmd.terminee)
    default:
      return commandes
  }
})

// ========== MÉTHODES PRINCIPALES ==========
const loadEmployeeData = async () => {
  if (!props.employeeId) return

  loading.value = true
  try {
    console.log('👤 Chargement employé:', props.employeeId, 'pour date:', selectedDate.value)

    const commandesData = await apiService.getCommandesEmploye(props.employeeId, selectedDate.value)
    console.log('📋 Données reçues de l\'API:', commandesData)

    if (commandesData && commandesData.success) {
      employee.value = {
        id: props.employeeId,
        name: commandesData.employe?.nomComplet || `Employé ${props.employeeId}`,
        totalMinutes: commandesData.dureeeTotaleMinutes || 0,
        maxMinutes: (commandesData.employe?.heuresTravailParJour || 8) * 60,
        status: (commandesData.dureeeTotaleMinutes || 0) > (commandesData.employe?.heuresTravailParJour || 8) * 60 ? 'overloaded' : 'available',
        tasks: (commandesData.commandes || []).map((cmd: any) => ({
          id: cmd.id,
          numeroCommande: cmd.numeroCommande,
          priority: cmd.priorite || 'NORMALE',
          status: cmd.terminee ? 'Terminée' : (cmd.status === 3 ? 'En cours' : 'Planifiée'),
          startTime: cmd.heureDebut || '09:00',
          endTime: calculateEndTime(cmd.heureDebut, cmd.dureeCalculee || cmd.dureeMinutes),
          heureDebut: cmd.heureDebut,
          heureFin: calculateEndTime(cmd.heureDebut, cmd.dureeCalculee || cmd.dureeMinutes),
          duration: cmd.dureeCalculee || cmd.dureeMinutes || 30,
          dureeCalculee: cmd.dureeCalculee || cmd.dureeMinutes || 30,
          amount: 0,
          cardCount: cmd.nombreCartes || 0,
          nombreCartes: cmd.nombreCartes || 0,
          cards: [],
          expanded: false,
          terminee: cmd.terminee || false,
          loadingCards: false
        }))
      }

      console.log('✅ Employé configuré avec', employee.value.tasks.length, 'tâches')
    }

  } catch (error) {
    console.error('❌ Erreur chargement employé:', error)
    employee.value = null
  } finally {
    loading.value = false
  }
}

/**
 * 🃏 CHARGEMENT DES CARTES DÉTAILLÉES POUR UNE COMMANDE - VERSION CORRIGÉE
 * Utilise le bon endpoint frontend pour récupérer les vraies cartes
 */
/**
 * 🔧 CORRECTION DANS EmployeeDetailPage.vue
 * Remplacez la fonction chargerCartesCommande par cette version corrigée
 */

const chargerCartesCommande = async (task: Task) => {
  if (!task.id) {
    console.warn('⚠️ Pas d\'ID de commande pour charger les cartes')
    return
  }

  task.loadingCards = true

  try {
    console.log('🃏 Chargement cartes pour commande:', task.id)

    // ✅ CORRECTION: Utiliser le bon endpoint qui fonctionne
    const response = await fetch(`/api/commandes/frontend/commandes/${task.id}/cartes`)
    if (!response.ok) {
      throw new Error(`Erreur API: ${response.status}`)
    }

    const cartesData = await response.json()
    console.log('✅ Données cartes reçues:', cartesData)

    // ✅ TRAITEMENT DES VRAIES CARTES depuis le backend
    if (cartesData && cartesData.cartes && Array.isArray(cartesData.cartes)) {
      task.cards = cartesData.cartes.map((carte: any) => ({
        id: carte.carteId,
        cert_id: carte.carteId,
        card_id: carte.cardId || carte.carteId,
        nom: carte.nom || carte.labelNom || 'Carte inconnue',
        name: carte.nom || carte.labelNom || 'Carte inconnue',
        label_name: carte.labelNom || carte.nom || 'Carte à certifier',
        code_barre: carte.codeBarre || 'N/A',
        type: carte.type || 'Pokemon',
        annotation: carte.annotation || '',
        cert_langue: 'FR', // Peut être ajouté dans le backend si nécessaire
        langue: 'FR',
        edition: carte.edition || '', // Peut être ajouté dans le backend
        duration: Math.max(3, Math.floor((task.duration || 30) / (task.cardCount || 1))),
        amount: (task.amount || 0) / (task.cardCount || 1),
        statut_correspondance: carte.avecNom ? 'AVEC_NOM' : 'SANS_NOM',
        avecNom: carte.avecNom || false
      }))

      console.log(`✅ ${task.cards.length} vraies cartes chargées pour la commande ${task.numeroCommande}`)

      // ✅ STATISTIQUES des cartes avec nom
      const cartesAvecNom = task.cards.filter(c => c.avecNom)
      const pourcentageAvecNom = task.cards.length > 0 ?
        Math.round((cartesAvecNom.length * 100) / task.cards.length) : 0

      console.log(`📊 Qualité: ${cartesAvecNom.length}/${task.cards.length} cartes avec nom (${pourcentageAvecNom}%)`)

    } else if (cartesData && Array.isArray(cartesData)) {
      // ✅ Format direct (si le backend renvoie directement un tableau)
      task.cards = cartesData.map((carte: any) => ({
        id: carte.carteId || carte.id,
        cert_id: carte.carteId || carte.id,
        card_id: carte.carteId || carte.id,
        nom: carte.nom || carte.name || 'Carte inconnue',
        name: carte.nom || carte.name || 'Carte inconnue',
        label_name: carte.labelNom || carte.label_name || 'Carte à certifier',
        code_barre: carte.codeBarre || carte.code_barre || 'N/A',
        type: carte.type || 'Pokemon',
        annotation: carte.annotation || '',
        cert_langue: 'FR',
        langue: 'FR',
        duration: Math.max(3, Math.floor((task.duration || 30) / (task.cardCount || 1))),
        amount: (task.amount || 0) / (task.cardCount || 1),
        statut_correspondance: carte.avecNom ? 'AVEC_NOM' : 'SANS_NOM',
        avecNom: carte.avecNom || false
      }))

      console.log(`✅ ${task.cards.length} cartes chargées (format direct)`)
    } else {
      console.warn('⚠️ Format de données inattendus:', cartesData)
      task.cards = []
    }

  } catch (error) {
    console.error('❌ Erreur chargement cartes:', error)

    // ✅ FALLBACK: Créer des cartes génériques si l'API échoue
    task.cards = Array.from({ length: task.cardCount || 1 }, (_, index) => ({
      id: `fallback_${task.id}_${index}`,
      cert_id: `fallback_${index}`,
      card_id: `card_${index}`,
      nom: `Carte ${index + 1}`,
      name: `Carte ${index + 1}`,
      label_name: `Carte à certifier ${index + 1}`,
      code_barre: `CODE_${index + 1}`,
      type: 'Pokemon',
      cert_langue: 'FR',
      langue: 'FR',
      annotation: '',
      duration: Math.floor((task.duration || 30) / (task.cardCount || 1)),
      amount: (task.amount || 0) / (task.cardCount || 1),
      statut_correspondance: 'FALLBACK',
      avecNom: false
    }))

    console.log(`🔄 ${task.cards.length} cartes de fallback créées`)
  } finally {
    task.loadingCards = false
  }
}

/**
 * 🔧 CORRECTION ALTERNATIVE: Si vous voulez déboguer l'appel API
 * Ajoutez cette fonction de test dans EmployeeDetailPage.vue
 */
const debugCartesAPI = async (task: Task) => {
  console.log('🔍 DEBUG: Test de l\'API cartes pour commande:', task.id)

  try {
    // Test 1: Ancien endpoint (qui échoue)
    console.log('📡 Test ancien endpoint...')
    const oldResponse = await fetch(`/api/test/commandes/${task.id}/cartes-details`)
    console.log('❌ Ancien endpoint:', oldResponse.status, oldResponse.statusText)

    // Test 2: Nouveau endpoint (qui marche)
    console.log('📡 Test nouveau endpoint...')
    const newResponse = await fetch(`/api/commandes/frontend/commandes/${task.id}/cartes`)
    console.log('✅ Nouveau endpoint:', newResponse.status, newResponse.statusText)

    if (newResponse.ok) {
      const data = await newResponse.json()
      console.log('📊 Données reçues:', data)

      // Appliquer les vraies données
      if (data && data.cartes) {
        task.cards = data.cartes.map((carte: any) => ({
          id: carte.carteId,
          cert_id: carte.carteId,
          nom: carte.nom,
          code_barre: carte.codeBarre,
          type: carte.type,
          avecNom: carte.avecNom
        }))

        console.log('🎯 Cartes appliquées:', task.cards.length)
      }
    }

  } catch (error) {
    console.error('❌ Erreur debug:', error)
  }
}

/**
 * 🔧 VERSION SIMPLE POUR TEST IMMÉDIAT
 * Remplacez temporairement chargerCartesCommande par cette version
 */
const chargerCartesCommandeSimple = async (task: Task) => {
  if (!task.id) return

  task.loadingCards = true

  try {
    console.log('🃏 SIMPLE: Chargement cartes pour:', task.id)

    const response = await fetch(`/api/commandes/frontend/commandes/${task.id}/cartes`)

    if (response.ok) {
      const data = await response.json()
      console.log('✅ SIMPLE: Données reçues:', data)

      if (data.cartes && data.cartes.length > 0) {
        // Utiliser les vraies cartes
        task.cards = data.cartes.map((carte: any, index: number) => ({
          id: carte.carteId,
          cert_id: carte.carteId,
          nom: carte.nom,
          name: carte.nom,
          label_name: carte.nom,
          code_barre: carte.codeBarre,
          type: carte.type,
          cert_langue: 'FR',
          langue: 'FR',
          duration: 20,
          amount: 10.0,
          statut_correspondance: 'VRAIE_CARTE'
        }))

        console.log(`🎯 SIMPLE: ${task.cards.length} vraies cartes appliquées`)
      } else {
        // Aucune carte trouvée
        task.cards = [{
          id: 'no_cards',
          nom: 'Aucune carte trouvée',
          code_barre: 'N/A',
          type: 'N/A',
          cert_langue: 'FR',
          duration: 0,
          amount: 0
        }]
      }
    } else {
      throw new Error(`Erreur ${response.status}`)
    }

  } catch (error) {
    console.error('❌ SIMPLE: Erreur:', error)

    // Fallback simple
    task.cards = [{
      id: 'error',
      nom: 'Erreur de chargement',
      code_barre: 'ERROR',
      type: 'Error',
      cert_langue: 'FR',
      duration: 0,
      amount: 0
    }]
  } finally {
    task.loadingCards = false
  }
}

/**
 * 🎯 CHARGEMENT EN MASSE DES CARTES - VERSION OPTIMISÉE
 */
const chargerToutesLesCartes = async () => {
  if (!employee.value?.tasks || loadingAllCards.value) return

  loadingAllCards.value = true
  let cartesTotales = 0
  let commandesAvecCartes = 0

  try {
    console.log('🎯 Chargement en masse des cartes pour toutes les commandes')

    const commandesACharger = employee.value.tasks.filter(task =>
      task.cardCount && task.cardCount > 0 && (!task.cards || task.cards.length === 0)
    )

    console.log(`📦 ${commandesACharger.length} commandes à traiter`)

    // ✅ CHARGEMENT PARALLÈLE pour de meilleures performances
    const promisesChargement = commandesACharger.map(async (task) => {
      try {
        await chargerCartesCommande(task)
        if (task.cards && task.cards.length > 0) {
          cartesTotales += task.cards.length
          commandesAvecCartes++
        }
      } catch (error) {
        console.error(`❌ Erreur pour commande ${task.id}:`, error)
      }
    })

    await Promise.all(promisesChargement)

    console.log(`✅ Chargement terminé: ${cartesTotales} cartes pour ${commandesAvecCartes} commandes`)

    // ✅ NOTIFICATION de succès
    if (cartesTotales > 0) {
      showNotification?.(`✅ ${cartesTotales} cartes chargées pour ${commandesAvecCartes} commandes`, 'success')
    } else {
      showNotification?.('⚠️ Aucune carte trouvée', 'warning')
    }

  } catch (error) {
    console.error('❌ Erreur chargement en masse:', error)
    showNotification?.('❌ Erreur lors du chargement des cartes', 'error')
  } finally {
    loadingAllCards.value = false
  }
}
const refreshEmployeeData = () => {
  emit('refresh')
  loadEmployeeData()
}

const marquerTerminee = (task: Task) => {
  task.status = 'Terminée'
  task.terminee = true
  console.log('✅ Commande terminée:', task.numeroCommande)
}

const toggleTaskCards = (task: Task) => {
  task.expanded = !task.expanded

  // Si on vient d'ouvrir et qu'on n'a pas encore les cartes, les charger
  if (task.expanded && (!task.cards || task.cards.length === 0) && (task.cardCount || 0) > 0) {
    chargerCartesCommande(task)
  }
}

// ========== MÉTHODES UTILITAIRES ==========
function calculateEndTime(heureDebut: string, dureeMinutes: number): string {
  if (!heureDebut) return '17:00'

  try {
    const [heures, minutes] = heureDebut.split(':').map(Number)
    const totalMinutes = heures * 60 + minutes + (dureeMinutes || 30)
    const nouvellesHeures = Math.floor(totalMinutes / 60) % 24
    const nouvellesMinutes = totalMinutes % 60
    return `${nouvellesHeures.toString().padStart(2, '0')}:${nouvellesMinutes.toString().padStart(2, '0')}`
  } catch (e) {
    return '17:00'
  }
}

const formatTime = (minutes: number): string => {
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return `${hours}h${mins.toString().padStart(2, '0')}`
}

const formatDate = (dateStr: string): string => {
  return new Date(dateStr).toLocaleDateString('fr-FR', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const getInitiales = (nom: string): string => {
  return nom.split(' ').map(part => part.charAt(0)).join('').toUpperCase().substring(0, 2)
}

const getStatusClasses = (status: string) => {
  switch (status) {
    case 'available': case 'Planifiée': return 'bg-green-100 text-green-800'
    case 'full': case 'En cours': return 'bg-orange-100 text-orange-800'
    case 'overloaded': case 'Terminée': return 'bg-red-100 text-red-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'available': return 'Disponible'
    case 'full': return 'Chargé'
    case 'overloaded': return 'Surchargé'
    default: return 'Inconnu'
  }
}

const getPriorityClasses = (priority: string) => {
  switch (priority) {
    case 'Haute': case 'URGENTE': return 'bg-red-100 text-red-800'
    case 'Moyenne': case 'HAUTE': return 'bg-yellow-100 text-yellow-800'
    case 'Basse': case 'NORMALE': return 'bg-green-100 text-green-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

const getProgressWidth = (task: Task): string => {
  if (task.status === 'Terminée' || task.terminee) return '100%'
  if (task.status === 'En cours') return '50%'
  return '10%'
}

// ========== LIFECYCLE ==========
onMounted(() => {
  loadEmployeeData()
})

watch(() => props.employeeId, () => {
  loadEmployeeData()
})

watch(selectedDate, () => {
  loadEmployeeData()
})
</script>
