<template>
  <div class="space-y-6">
    <!-- En-tête avec indicateurs de qualité -->
    <div class="flex justify-between items-center">
      <div>
        <h2 class="text-2xl font-bold text-gray-900">📦 Commandes</h2>
        <p class="text-gray-600 mt-1">
          Gestion des commandes avec données exactes
          <span v-if="stats.pourcentageQualiteGlobal" class="ml-2">
            ({{ stats.pourcentageQualiteGlobal }}% données complètes)
          </span>
        </p>
      </div>
      <div class="flex space-x-3">
        <!-- Bouton test cohérence -->
        <button
          @click="testerCoherence"
          class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
          :disabled="loading"
        >
          🔧 Test Cohérence
        </button>
        <button
          @click="showCreateModal = true"
          class="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors"
        >
          ➕ Nouvelle Commande
        </button>
      </div>
    </div>

    <!-- Statistiques globales -->
    <div v-if="stats" class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <div class="bg-white p-4 rounded-lg shadow border">
        <div class="text-sm text-gray-600">Total Commandes</div>
        <div class="text-2xl font-bold text-blue-600">{{ stats.totalCommandes }}</div>
      </div>
      <div class="bg-white p-4 rounded-lg shadow border">
        <div class="text-sm text-gray-600">Total Cartes</div>
        <div class="text-2xl font-bold text-green-600">{{ stats.totalCartes }}</div>
        <div class="text-xs text-gray-500">{{ stats.cartesParCommande }} moy/commande</div>
      </div>
      <div class="bg-white p-4 rounded-lg shadow border">
        <div class="text-sm text-gray-600">Cartes avec Nom</div>
        <div class="text-2xl font-bold text-purple-600">{{ stats.cartesAvecNom }}</div>
        <div class="text-xs" :class="stats.pourcentageQualiteGlobal >= 95 ? 'text-green-600' : 'text-orange-600'">
          {{ stats.pourcentageQualiteGlobal }}% qualité
        </div>
      </div>
      <div class="bg-white p-4 rounded-lg shadow border">
        <div class="text-sm text-gray-600">Temps Moyen</div>
        <div class="text-2xl font-bold text-orange-600">{{ stats.tempsParCommande }}min</div>
        <div class="text-xs text-gray-500">{{ stats.tempsTotal }}h total</div>
      </div>
    </div>

    <!-- Filtres -->
    <div class="bg-white p-4 rounded-lg shadow border">
      <div class="flex flex-wrap gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Statut</label>
          <select v-model="filters.statut" class="border border-gray-300 rounded-md px-3 py-2">
            <option value="">Tous</option>
            <option value="EN_ATTENTE">En Attente</option>
            <option value="EN_COURS">En Cours</option>
            <option value="TERMINEE">Terminée</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Priorité</label>
          <select v-model="filters.priorite" class="border border-gray-300 rounded-md px-3 py-2">
            <option value="">Toutes</option>
            <option value="URGENTE">Urgente</option>
            <option value="HAUTE">Haute</option>
            <option value="NORMALE">Normale</option>
            <option value="BASSE">Basse</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Qualité</label>
          <select v-model="filters.qualite" class="border border-gray-300 rounded-md px-3 py-2">
            <option value="">Toutes</option>
            <option value="excellente">Excellente (≥95%)</option>
            <option value="bonne">Bonne (≥80%)</option>
            <option value="moyenne">Moyenne (<80%)</option>
          </select>
        </div>
        <div class="flex-1">
          <label class="block text-sm font-medium text-gray-700 mb-1">Recherche</label>
          <input
            v-model="filters.search"
            type="text"
            placeholder="Numéro de commande..."
            class="w-full border border-gray-300 rounded-md px-3 py-2"
          />
        </div>
      </div>
    </div>

    <!-- Chargement -->
    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
      <span class="text-gray-600 mt-3 block">Chargement des commandes...</span>
    </div>

    <!-- Liste des commandes -->
    <div v-else class="bg-white rounded-lg shadow border overflow-hidden">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Commande
            </th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Cartes
            </th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Priorité
            </th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Statut
            </th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Temps
            </th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Actions
            </th>
          </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
          <tr
            v-for="commande in commandesFiltrees"
            :key="commande.id"
            class="hover:bg-gray-50 transition-colors"
          >
            <!-- Commande -->
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="flex flex-col">
                <div class="text-sm font-medium text-gray-900">
                  {{ commande.numeroCommande || `CMD-${commande.id?.substring(0, 8)}` }}
                </div>
                <div class="text-xs text-gray-500">
                  {{ formatDate(commande.dateCreation) }}
                </div>
              </div>
            </td>

            <!-- Cartes avec qualité -->
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="flex flex-col">
                <div class="text-sm font-medium text-gray-900">
                  {{ commande.nombreCartes || 0 }} cartes
                </div>
                <div class="flex items-center space-x-2">
                  <div class="text-xs text-gray-600">
                    {{ commande.nombreAvecNom || 0 }} avec nom ({{ commande.pourcentageAvecNom || 0 }}%)
                  </div>
                  <div v-if="commande.pourcentageAvecNom !== undefined"
                       :class="commande.pourcentageAvecNom >= 95 ? 'text-xs text-green-600' : 'text-xs text-orange-600'">
                    {{ commande.pourcentageAvecNom >= 95 ? '✅' : '⚠️' }}
                  </div>
                </div>
              </div>
            </td>

            <!-- Priorité -->
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                      :class="getPriorityColor(commande.priorite)">
                  {{ getPriorityLabel(commande.priorite) }}
                </span>
            </td>

            <!-- Statut -->
            <td class="px-6 py-4 whitespace-nowrap">
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                      :class="getStatusColor(commande.statut)">
                  {{ getStatusLabel(commande.statut) }}
                </span>
            </td>

            <!-- Temps -->
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="text-sm text-gray-900">{{ commande.tempsEstimeMinutes || 0 }} min</div>
              <div class="text-xs text-gray-500">{{ Math.round((commande.tempsEstimeMinutes || 0) / 60 * 10) / 10 }}h</div>
            </td>

            <!-- Actions -->
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
              <button
                @click="voirDetails(commande)"
                class="text-blue-600 hover:text-blue-900"
                title="Voir détails"
              >
                👁️
              </button>
              <button
                @click="voirCartes(commande)"
                class="text-green-600 hover:text-green-900"
                title="Voir cartes"
              >
                🃏
              </button>
              <button
                v-if="commande.statut === 'EN_ATTENTE'"
                @click="commencerCommande(commande.id!)"
                class="text-purple-600 hover:text-purple-900"
                title="Commencer"
              >
                ▶️
              </button>
              <button
                v-if="commande.statut === 'EN_COURS'"
                @click="terminerCommande(commande.id!)"
                class="text-orange-600 hover:text-orange-900"
                title="Terminer"
              >
                ✅
              </button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ✅ MODAL CARTES AVEC DÉTAILS COMPLETS -->
    <div v-if="showCartesModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg shadow-xl max-w-6xl w-full max-h-[90vh] overflow-y-auto mx-4">
        <div class="p-6">
          <!-- En-tête de la modal -->
          <div class="flex justify-between items-center mb-6">
            <div>
              <h3 class="text-xl font-semibold text-gray-900">
                🃏 Cartes détaillées - {{ selectedCommande?.numeroCommande }}
              </h3>
              <p class="text-sm text-gray-600 mt-1" v-if="cartesCommande">
                {{ cartesCommande.nombreCartes }} cartes •
                {{ cartesCommande.nombreAvecNom }} avec nom réel ({{ cartesCommande.pourcentageAvecNom }}%)
              </p>
            </div>
            <div class="flex items-center space-x-3">
              <!-- Bouton charger cartes détaillées -->
              <button
                v-if="!cartesDetailleesChargees"
                @click="chargerCartesDetaillees"
                :disabled="loadingCartesDetaillees"
                class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 disabled:bg-gray-400 flex items-center"
              >
                <span v-if="loadingCartesDetaillees" class="mr-2">
                  <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                </span>
                {{ loadingCartesDetaillees ? 'Chargement...' : '📦 Charger détails' }}
              </button>

              <!-- Bouton fermer -->
              <button
                @click="fermerModalCartes"
                class="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </div>
          </div>

          <!-- Chargement initial -->
          <div v-if="loadingCartes" class="text-center py-12">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
            <span class="text-gray-600 mt-3 block">Chargement des informations de base...</span>
          </div>

          <!-- Contenu principal -->
          <div v-else-if="cartesCommande" class="space-y-6">

            <!-- Résumé avec indicateurs de qualité -->
            <div class="bg-gradient-to-r from-blue-50 to-purple-50 p-4 rounded-lg border">
              <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div class="text-center">
                  <div class="text-2xl font-bold text-blue-600">{{ cartesCommande.nombreCartes }}</div>
                  <div class="text-sm text-gray-600">Cartes total</div>
                </div>
                <div class="text-center">
                  <div class="text-2xl font-bold text-green-600">{{ cartesCommande.nombreAvecNom }}</div>
                  <div class="text-sm text-gray-600">Avec nom réel</div>
                </div>
                <div class="text-center">
                  <div class="text-2xl font-bold text-purple-600">{{ cartesCommande.pourcentageAvecNom }}%</div>
                  <div class="text-sm text-gray-600">Qualité noms</div>
                </div>
                <div class="text-center">
                  <div class="text-2xl font-bold" :class="cartesDetailleesChargees ? 'text-green-600' : 'text-orange-600'">
                    {{ cartesDetailleesChargees ? cartesDetaillees.length : 0 }}
                  </div>
                  <div class="text-sm text-gray-600">Détails chargés</div>
                </div>
              </div>
            </div>

            <!-- ✅ AFFICHAGE DES CARTES EN RECTANGLES DÉTAILLÉS -->
            <div v-if="cartesDetailleesChargees && cartesDetaillees.length > 0">
              <div class="flex justify-between items-center mb-4">
                <h4 class="text-lg font-semibold text-gray-900">
                  📋 Cartes détaillées ({{ cartesDetaillees.length }})
                </h4>
                <!-- Filtres de qualité -->
                <div class="flex space-x-2">
                  <button
                    @click="filtreCartes = 'tous'"
                    :class="['px-3 py-1 rounded text-sm', filtreCartes === 'tous' ? 'bg-blue-600 text-white' : 'bg-gray-200']"
                  >
                    Toutes
                  </button>
                  <button
                    @click="filtreCartes = 'avec_nom'"
                    :class="['px-3 py-1 rounded text-sm', filtreCartes === 'avec_nom' ? 'bg-green-600 text-white' : 'bg-gray-200']"
                  >
                    Avec nom
                  </button>
                  <button
                    @click="filtreCartes = 'sans_nom'"
                    :class="['px-3 py-1 rounded text-sm', filtreCartes === 'sans_nom' ? 'bg-orange-600 text-white' : 'bg-gray-200']"
                  >
                    Sans nom
                  </button>
                </div>
              </div>

              <!-- Grille des cartes détaillées -->
              <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                <div
                  v-for="carte in cartesFiltrées"
                  :key="carte.cert_id || carte.id"
                  class="bg-white border border-gray-200 rounded-lg p-4 shadow-sm hover:shadow-md transition-all"
                >
                  <!-- En-tête de la carte -->
                  <div class="flex items-start justify-between mb-3">
                    <div class="flex-1 min-w-0">
                      <h6 class="font-semibold text-gray-900 text-sm truncate mb-1" :title="carte.nom || carte.name">
                        {{ carte.nom || carte.name || 'Carte inconnue' }}
                      </h6>
                      <p class="text-xs text-gray-500 truncate" v-if="carte.label_name">
                        {{ carte.label_name }}
                      </p>
                    </div>
                    <!-- Indicateur de qualité du nom -->
                    <span
                      v-if="carte.nom && !carte.nom.startsWith('Carte-')"
                      class="bg-green-100 text-green-700 text-xs px-2 py-1 rounded-full font-medium"
                      title="Nom réel disponible"
                    >
                      ✅
                    </span>
                    <span
                      v-else
                      class="bg-orange-100 text-orange-700 text-xs px-2 py-1 rounded-full font-medium"
                      title="Nom générique"
                    >
                      ⚠️
                    </span>
                  </div>

                  <!-- Informations de certification -->
                  <div class="space-y-2 text-xs">
                    <div class="flex justify-between items-center">
                      <span class="text-gray-600">📊 Code barre:</span>
                      <span class="font-mono font-medium">{{ carte.code_barre || 'N/A' }}</span>
                    </div>
                    <div class="flex justify-between items-center">
                      <span class="text-gray-600">🌍 Langue:</span>
                      <span class="uppercase font-bold text-blue-600">{{ carte.cert_langue || carte.langue || 'N/A' }}</span>
                    </div>
                    <div class="flex justify-between items-center" v-if="carte.edition">
                      <span class="text-gray-600">📖 Édition:</span>
                      <span class="font-medium">{{ carte.edition }}</span>
                    </div>
                    <div class="flex justify-between items-center" v-if="carte.locale_utilisee">
                      <span class="text-gray-600">🔗 Locale:</span>
                      <span class="font-medium text-purple-600">{{ carte.locale_utilisee }}</span>
                    </div>
                  </div>

                  <!-- Stratégie utilisée pour le nom (debug) -->
                  <div v-if="carte.strategie_nom" class="mt-2 pt-2 border-t border-gray-100">
                    <div class="text-xs text-gray-500 flex items-center justify-between">
                      <span>Stratégie:</span>
                      <span
                        class="px-1.5 py-0.5 rounded text-xs font-medium"
                        :class="getStrategieColor(carte.strategie_nom)"
                      >
                        {{ getStrategieLabel(carte.strategie_nom) }}
                      </span>
                    </div>
                  </div>

                  <!-- ID de référence (pour debug) -->
                  <div class="mt-2 text-xs text-gray-400 truncate" :title="carte.cert_id || carte.id">
                    ID: {{ (carte.cert_id || carte.id || '').toString().substring(0, 8) }}...
                  </div>
                </div>
              </div>

              <!-- Message si pas de cartes après filtrage -->
              <div v-if="cartesFiltrées.length === 0" class="text-center py-8 text-gray-500">
                <div class="text-sm">Aucune carte ne correspond au filtre sélectionné</div>
              </div>
            </div>

            <!-- Message pour charger les détails -->
            <div v-else-if="!cartesDetailleesChargees" class="text-center py-8 bg-gray-50 rounded-lg">
              <div class="text-gray-600 mb-4">
                <div class="text-lg">📦 Détails des cartes non chargés</div>
                <div class="text-sm mt-2">Cliquez sur "📦 Charger détails" pour voir les informations complètes</div>
              </div>
            </div>

            <!-- Chargement des détails -->
            <div v-else-if="loadingCartesDetaillees" class="text-center py-8">
              <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
              <span class="text-gray-600 mt-3 block">Chargement des détails des cartes...</span>
            </div>

            <!-- ✅ ANCIENNE LISTE SIMPLE COMME FALLBACK -->
            <div v-if="cartesCommande.nomsCartes && cartesCommande.nomsCartes.length > 0" class="mt-8">
              <details class="border rounded-lg">
                <summary class="p-4 cursor-pointer hover:bg-gray-50 font-medium">
                  📄 Liste simple des noms ({{ cartesCommande.nomsCartes.length }} cartes)
                </summary>
                <div class="p-4 border-t bg-gray-50">
                  <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-2 text-sm">
                    <div
                      v-for="(carte, index) in cartesCommande.nomsCartes"
                      :key="index"
                      class="bg-white px-3 py-2 rounded border text-center truncate"
                      :title="carte"
                    >
                      {{ carte }}
                    </div>
                  </div>
                </div>
              </details>
            </div>

          </div>

          <!-- Actions de la modal -->
          <div class="flex justify-end mt-6 space-x-3">
            <button
              @click="fermerModalCartes"
              class="bg-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-400 transition-colors"
            >
              Fermer
            </button>
            <button
              v-if="cartesDetailleesChargees && cartesDetaillees.length > 0"
              @click="exporterCartesDetaillees"
              class="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors"
            >
              📥 Exporter détails
            </button>
            <button
              v-else-if="cartesCommande && cartesCommande.nombreCartes > 0"
              @click="exporterCartes"
              class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
            >
              📥 Exporter simple
            </button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, inject, watch } from 'vue'
import { apiService, type Commande, type CartesDetail } from '../services/api'

// ========== ÉTAT LOCAL ==========
const commandes = ref<Commande[]>([])
const loading = ref(false)
const showCreateModal = ref(false)
const showDetailsModal = ref(false)
const showCartesModal = ref(false)
const selectedCommande = ref<Commande | null>(null)
const cartesCommande = ref<CartesDetail | null>(null)
const loadingCartes = ref(false)

// Variables pour les cartes détaillées
const cartesDetaillees = ref([])
const loadingCartesDetaillees = ref(false)
const cartesDetailleesChargees = ref(false)
const filtreCartes = ref('tous') // 'tous', 'avec_nom', 'sans_nom'

// Statistiques globales
const stats = ref({
  totalCommandes: 0,
  totalCartes: 0,
  cartesAvecNom: 0,
  pourcentageQualiteGlobal: 0,
  cartesParCommande: 0,
  tempsTotal: 0,
  tempsParCommande: 0
})

// Filtres
const filters = ref({
  statut: '',
  priorite: '',
  search: '',
  qualite: ''
})

// Injection de la fonction de notification
const showNotification = inject('showNotification') as (message: string, type?: 'success' | 'error') => void

// ========== COMPUTED PROPERTIES ==========
const commandesFiltrees = computed(() => {
  let filtered = commandes.value

  // Filtre par statut
  if (filters.value.statut) {
    filtered = filtered.filter(cmd => cmd.statut === filters.value.statut)
  }

  // Filtre par priorité
  if (filters.value.priorite) {
    filtered = filtered.filter(cmd => cmd.priorite === filters.value.priorite)
  }

  // Filtre par qualité
  if (filters.value.qualite) {
    filtered = filtered.filter(cmd => {
      const pourcentage = cmd.pourcentageAvecNom || 0
      switch (filters.value.qualite) {
        case 'excellente': return pourcentage >= 95
        case 'bonne': return pourcentage >= 80 && pourcentage < 95
        case 'moyenne': return pourcentage < 80
        default: return true
      }
    })
  }

  // Filtre par recherche
  if (filters.value.search) {
    const search = filters.value.search.toLowerCase()
    filtered = filtered.filter(cmd =>
      cmd.numeroCommande?.toLowerCase().includes(search) ||
      cmd.id?.toLowerCase().includes(search)
    )
  }

  return filtered
})

const cartesFiltrées = computed(() => {
  if (!cartesDetaillees.value || cartesDetaillees.value.length === 0) return []

  switch (filtreCartes.value) {
    case 'avec_nom':
      return cartesDetaillees.value.filter(carte =>
        carte.nom && !carte.nom.startsWith('Carte-')
      )
    case 'sans_nom':
      return cartesDetaillees.value.filter(carte =>
        !carte.nom || carte.nom.startsWith('Carte-')
      )
    default:
      return cartesDetaillees.value
  }
})

// ========== MÉTHODES PRINCIPALES ==========
const loadCommandes = async () => {
  loading.value = true
  try {
    console.log('🔄 Chargement des commandes...')
    commandes.value = await apiService.getCommandes()
    calculerStatistiques()
    console.log(`✅ ${commandes.value.length} commandes chargées`)
  } catch (error) {
    console.error('❌ Erreur chargement commandes:', error)
    showNotification?.('Erreur lors du chargement des commandes', 'error')
  } finally {
    loading.value = false
  }
}

const calculerStatistiques = () => {
  const total = commandes.value.length
  const totalCartes = commandes.value.reduce((sum, cmd) => sum + (cmd.nombreCartes || 0), 0)
  const cartesAvecNom = commandes.value.reduce((sum, cmd) => sum + (cmd.nombreAvecNom || 0), 0)
  const tempsTotal = commandes.value.reduce((sum, cmd) => sum + (cmd.tempsEstimeMinutes || 0), 0)

  stats.value = {
    totalCommandes: total,
    totalCartes,
    cartesAvecNom,
    pourcentageQualiteGlobal: totalCartes > 0 ? Math.round((cartesAvecNom / totalCartes) * 100) : 0,
    cartesParCommande: total > 0 ? Math.round(totalCartes / total) : 0,
    tempsTotal: Math.round(tempsTotal / 60),
    tempsParCommande: total > 0 ? Math.round(tempsTotal / total) : 0
  }
}

const testerCoherence = async () => {
  loading.value = true
  try {
    const coherence = await apiService.testCoherenceFrontend()

    if (coherence.frontend_pret) {
      showNotification?.('✅ Données cohérentes - Frontend prêt !', 'success')
    } else {
      showNotification?.('⚠️ Problèmes détectés dans les données', 'error')
    }

    console.log('🔧 Test cohérence:', coherence)
  } catch (error) {
    console.error('❌ Erreur test cohérence:', error)
    showNotification?.('Erreur lors du test de cohérence', 'error')
  } finally {
    loading.value = false
  }
}

// ========== MÉTHODES POUR LES CARTES ==========
const voirCartes = async (commande) => {
  if (!commande.id) {
    showNotification?.('ID de commande manquant', 'error')
    return
  }

  // Réinitialiser l'état
  selectedCommande.value = commande
  loadingCartes.value = true
  showCartesModal.value = true
  cartesCommande.value = null
  cartesDetaillees.value = []
  cartesDetailleesChargees.value = false
  filtreCartes.value = 'tous'

  try {
    console.log('🃏 Ouverture modal cartes pour:', commande.numeroCommande)

    // Charger d'abord les informations de base (comme avant)
    cartesCommande.value = await apiService.getCartesCommande(commande.id)
    console.log('✅ Informations de base chargées')

  } catch (error) {
    console.error('❌ Erreur chargement cartes de base:', error)
    showNotification?.('Erreur lors du chargement des cartes', 'error')
  } finally {
    loadingCartes.value = false
  }
}

const chargerCartesDetaillees = async () => {
  if (!selectedCommande.value?.id) {
    showNotification?.('ID de commande manquant', 'error')
    return
  }

  loadingCartesDetaillees.value = true
  cartesDetailleesChargees.value = false

  try {
    console.log('🃏 Chargement détails cartes pour:', selectedCommande.value.numeroCommande)

    // Utiliser notre endpoint amélioré
    const response = await fetch(`/api/test/commandes/${selectedCommande.value.id}/cartes-details`)

    if (!response.ok) {
      throw new Error(`Erreur API: ${response.status}`)
    }

    const data = await response.json()
    console.log('✅ Détails cartes reçus:', data)

    if (data.cartes_details_uniques && Array.isArray(data.cartes_details_uniques)) {
      cartesDetaillees.value = data.cartes_details_uniques
      cartesDetailleesChargees.value = true

      // Mettre à jour les statistiques si disponibles
      if (data.statistiques) {
        console.log('📊 Statistiques détaillées:', data.statistiques)
      }

      showNotification?.(`✅ ${cartesDetaillees.value.length} cartes détaillées chargées`, 'success')
    } else {
      throw new Error('Format de données inattendu')
    }

  } catch (error) {
    console.error('❌ Erreur chargement détails cartes:', error)
    showNotification?.('Erreur lors du chargement des détails', 'error')

    // Fallback: créer des cartes basiques depuis les noms existants
    if (cartesCommande.value?.nomsCartes) {
      cartesDetaillees.value = cartesCommande.value.nomsCartes.map((nom, index) => ({
        id: `fallback_${index}`,
        cert_id: `fallback_${index}`,
        nom: nom,
        name: nom,
        code_barre: `FALLBACK_${index}`,
        cert_langue: 'FR',
        strategie_nom: 'FALLBACK_LISTE_SIMPLE'
      }))
      cartesDetailleesChargees.value = true
    }

  } finally {
    loadingCartesDetaillees.value = false
  }
}

const fermerModalCartes = () => {
  showCartesModal.value = false
  selectedCommande.value = null
  cartesCommande.value = null
  cartesDetaillees.value = []
  cartesDetailleesChargees.value = false
  loadingCartes.value = false
  loadingCartesDetaillees.value = false
  filtreCartes.value = 'tous'
}

const exporterCartesDetaillees = () => {
  if (!cartesDetaillees.value || cartesDetaillees.value.length === 0) {
    showNotification?.('Aucune carte détaillée à exporter', 'error')
    return
  }

  try {
    // Préparer les données pour l'export
    const donneesExport = cartesDetaillees.value.map(carte => ({
      'Nom de la carte': carte.nom || carte.name || 'Inconnu',
      'Code barre': carte.code_barre || '',
      'Langue certification': carte.cert_langue || carte.langue || '',
      'Édition': carte.edition || '',
      'Label': carte.label_name || '',
      'Locale utilisée': carte.locale_utilisee || '',
      'Stratégie nom': carte.strategie_nom || '',
      'ID certification': carte.cert_id || '',
      'ID carte': carte.card_id || ''
    }))

    // Convertir en CSV
    const headers = Object.keys(donneesExport[0])
    const csvContent = [
      headers.join(','),
      ...donneesExport.map(row =>
        headers.map(header => `"${(row[header] || '').toString().replace(/"/g, '""')}"`).join(',')
      )
    ].join('\n')

    // Télécharger le fichier
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `cartes_detaillees_${selectedCommande.value?.numeroCommande || 'commande'}_${new Date().toISOString().split('T')[0]}.csv`
    link.click()

    showNotification?.(`✅ Export réussi: ${donneesExport.length} cartes`, 'success')

  } catch (error) {
    console.error('❌ Erreur export cartes détaillées:', error)
    showNotification?.('Erreur lors de l\'export', 'error')
  }
}

const exporterCartes = () => {
  showNotification?.('Export des cartes en cours...', 'success')
}

// ========== MÉTHODES UTILITAIRES ==========
const formatDate = (dateString?: string) => {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleDateString('fr-FR')
}

const getPriorityLabel = (priorite?: string) => {
  const labels = {
    'URGENTE': 'Urgente',
    'HAUTE': 'Haute',
    'NORMALE': 'Normale',
    'BASSE': 'Basse'
  }
  return labels[priorite as keyof typeof labels] || priorite || 'N/A'
}

const getPriorityColor = (priorite?: string) => {
  const colors = {
    'URGENTE': 'bg-red-100 text-red-800',
    'HAUTE': 'bg-orange-100 text-orange-800',
    'NORMALE': 'bg-blue-100 text-blue-800',
    'BASSE': 'bg-gray-100 text-gray-800'
  }
  return colors[priorite as keyof typeof colors] || 'bg-gray-100 text-gray-800'
}

const getStatusLabel = (statut?: string) => {
  const labels = {
    'EN_ATTENTE': 'En Attente',
    'PLANIFIEE': 'Planifiée',
    'EN_COURS': 'En Cours',
    'TERMINEE': 'Terminée',
    'ANNULEE': 'Annulée'
  }
  return labels[statut as keyof typeof labels] || statut || 'N/A'
}

const getStatusColor = (statut?: string) => {
  const colors = {
    'EN_ATTENTE': 'bg-yellow-100 text-yellow-800',
    'PLANIFIEE': 'bg-blue-100 text-blue-800',
    'EN_COURS': 'bg-indigo-100 text-indigo-800',
    'TERMINEE': 'bg-green-100 text-green-800',
    'ANNULEE': 'bg-gray-100 text-gray-800'
  }
  return colors[statut as keyof typeof colors] || 'bg-gray-100 text-gray-800'
}

const getStrategieColor = (strategie) => {
  switch (strategie) {
    case 'CORRESPONDANCE_EXACTE':
      return 'bg-green-100 text-green-700'
    case 'FR_EN_PRIORITAIRE':
      return 'bg-blue-100 text-blue-700'
    case 'NIMPORTE_QUELLE':
      return 'bg-yellow-100 text-yellow-700'
    case 'FALLBACK_CODE_BARRE':
    case 'FALLBACK_LISTE_SIMPLE':
      return 'bg-red-100 text-red-700'
    default:
      return 'bg-gray-100 text-gray-700'
  }
}

const getStrategieLabel = (strategie) => {
  switch (strategie) {
    case 'CORRESPONDANCE_EXACTE':
      return 'Exacte'
    case 'FR_EN_PRIORITAIRE':
      return 'FR/EN'
    case 'NIMPORTE_QUELLE':
      return 'Autre'
    case 'FALLBACK_CODE_BARRE':
      return 'Fallback'
    case 'FALLBACK_LISTE_SIMPLE':
      return 'Simple'
    default:
      return 'Inconnue'
  }
}

// Actions sur les commandes
const voirDetails = (commande: Commande) => {
  selectedCommande.value = commande
  showDetailsModal.value = true
}

const commencerCommande = async (id: string) => {
  try {
    await apiService.commencerCommande(id)
    showNotification?.('Commande commencée')
    await loadCommandes()
  } catch (error) {
    console.error('Erreur:', error)
    showNotification?.('Erreur lors du démarrage de la commande', 'error')
  }
}

const terminerCommande = async (id: string) => {
  try {
    await apiService.terminerCommande(id)
    showNotification?.('Commande terminée')
    await loadCommandes()
  } catch (error) {
    console.error('Erreur:', error)
    showNotification?.('Erreur lors de la finalisation de la commande', 'error')
  }
}

// ========== WATCHERS ==========
watch(selectedCommande, () => {
  filtreCartes.value = 'tous'
})

// ========== LIFECYCLE ==========
onMounted(() => {
  loadCommandes()
})
</script>
