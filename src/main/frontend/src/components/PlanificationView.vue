<!-- ============= PLANIFICATIONVIEW.VUE CORRIGÉ ============= -->
<template>
  <div class="max-w-7xl mx-auto p-6">
    <div class="bg-white rounded-lg shadow-lg">
      <!-- En-tête avec actions -->
      <div class="border-b border-gray-200 p-6">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-2xl font-bold text-gray-900">📅 Planification des Commandes</h2>

          <div class="flex gap-2">
            <!-- ✅ BOUTON PLANIFICATION AUTOMATIQUE CORRIGÉ -->
            <button
              @click="planifierAutomatiquement"
              :disabled="loading"
              class="bg-purple-600 text-white px-4 py-2 rounded-lg hover:bg-purple-700 transition-colors disabled:opacity-50"
            >
              {{ loading ? '🔄' : '🤖' }}
              {{ loading ? 'Planification...' : 'Planifier Automatiquement' }}
            </button>

            <!-- ✅ BOUTON PLANIFICATION AVEC TRANSACTION -->
            <button
              @click="planifierAvecTransaction"
              :disabled="loading"
              class="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50"
            >
              🚀 Transaction (10 commandes)
            </button>

            <!-- Bouton Diagnostic -->
            <button
              @click="voirDiagnostic"
              class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
            >
              🔍 Diagnostic
            </button>

            <!-- Bouton Vider -->
            <button
              @click="viderPlanifications"
              class="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors"
            >
              🧹 Vider
            </button>

            <!-- Bouton Actualiser -->
            <button
              @click="chargerPlanifications"
              :disabled="loading"
              class="bg-gray-600 text-white px-4 py-2 rounded-lg hover:bg-gray-700 transition-colors disabled:opacity-50"
            >
              🔄 Actualiser
            </button>
          </div>
        </div>

        <!-- ✅ SÉLECTEUR DE PÉRIODE CORRIGÉ -->
        <div class="flex gap-4 items-center">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Du :</label>
            <input
              v-model="periode.debut"
              type="date"
              class="border border-gray-300 rounded-md px-3 py-2"
              @change="chargerPlanifications"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Au :</label>
            <input
              v-model="periode.fin"
              type="date"
              class="border border-gray-300 rounded-md px-3 py-2"
              @change="chargerPlanifications"
            />
          </div>
          <div class="mt-6">
            <button
              @click="chargerPlanifications"
              class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
            >
              🔍 Filtrer
            </button>
          </div>
        </div>

        <!-- ✅ AFFICHAGE DU RÉSUMÉ -->
        <div v-if="planifications.length > 0" class="mt-4 grid grid-cols-4 gap-4">
          <div class="bg-blue-50 p-3 rounded-lg">
            <div class="text-2xl font-bold text-blue-600">{{ planifications.length }}</div>
            <div class="text-sm text-blue-700">Total planifications</div>
          </div>
          <div class="bg-green-50 p-3 rounded-lg">
            <div class="text-2xl font-bold text-green-600">{{ planificationsTerminees }}</div>
            <div class="text-sm text-green-700">Terminées</div>
          </div>
          <div class="bg-yellow-50 p-3 rounded-lg">
            <div class="text-2xl font-bold text-yellow-600">{{ planificationsEnCours }}</div>
            <div class="text-sm text-yellow-700">En cours</div>
          </div>
          <div class="bg-purple-50 p-3 rounded-lg">
            <div class="text-2xl font-bold text-purple-600">{{ employesUtilises }}</div>
            <div class="text-sm text-purple-700">Employés utilisés</div>
          </div>
        </div>
      </div>

      <!-- ✅ CONTENU PRINCIPAL CORRIGÉ -->
      <div class="p-6">
        <!-- Planifications par date -->
        <div v-if="planifications.length > 0 && !loading">
          <div v-for="groupe in planificationsGroupees" :key="groupe.date" class="mb-8">
            <h3 class="text-lg font-semibold text-gray-900 mb-4 border-b pb-2">
              📅 {{ formatDate(groupe.date) }}
              <span class="text-sm text-gray-500">({{ groupe.planifications.length }} planification{{ groupe.planifications.length > 1 ? 's' : '' }})</span>
            </h3>

            <div class="grid gap-4">
              <div
                v-for="planification in groupe.planifications"
                :key="planification.id"
                class="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow"
                :class="{
                  'bg-green-50 border-green-200': planification.terminee,
                  'bg-white': !planification.terminee
                }"
              >
                <div class="flex justify-between items-start">
                  <div class="flex-1">
                    <div class="flex items-center gap-2 mb-2">
                      <span class="font-medium">{{ planification.numeroCommande || 'N/A' }}</span>
                      <span
                        v-if="planification.priorite"
                        class="px-2 py-1 text-xs rounded-full"
                        :class="getPrioriteClass(planification.priorite)"
                      >
                        {{ planification.priorite }}
                      </span>
                      <span
                        v-if="planification.terminee"
                        class="px-2 py-1 text-xs bg-green-100 text-green-800 rounded-full"
                      >
                        ✅ Terminée
                      </span>
                    </div>

                    <div class="text-sm text-gray-600 space-y-1">
                      <div>👤 <strong>{{ planification.employeNom || 'Employé non assigné' }}</strong></div>
                      <div>⏰ {{ planification.heureDebut }} - {{ getHeureFin(planification) }}</div>
                      <div>⏱️ Durée : {{ planification.dureeMinutes }} minutes</div>
                    </div>
                  </div>

                  <div class="flex flex-col gap-2">
                    <button
                      v-if="!planification.terminee"
                      @click="terminerPlanification(planification.id!)"
                      class="text-green-600 hover:text-green-900 text-sm px-3 py-1 border border-green-300 rounded hover:bg-green-50 transition-colors"
                    >
                      ✅ Terminer
                    </button>
                    <div v-else class="text-green-600 text-sm">
                      ✅ Terminée
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ✅ MESSAGE SI AUCUNE PLANIFICATION -->
        <div v-else-if="!loading" class="text-center py-12">
          <div class="text-gray-500">
            <div class="text-4xl mb-4">📅</div>
            <div class="text-lg mb-2">Aucune planification trouvée pour cette période</div>
            <div class="text-sm text-gray-400 mb-6">
              Période : {{ formatDate(periode.debut) }} → {{ formatDate(periode.fin) }}
            </div>

            <div class="space-y-3">
              <button
                @click="planifierAutomatiquement"
                class="bg-purple-600 text-white px-6 py-3 rounded-lg hover:bg-purple-700 transition-colors block mx-auto"
              >
                🤖 Lancer la planification automatique
              </button>

              <button
                @click="planifierAvecTransaction"
                class="bg-green-600 text-white px-6 py-3 rounded-lg hover:bg-green-700 transition-colors block mx-auto"
              >
                🚀 Planifier avec transaction (10 commandes)
              </button>
            </div>
          </div>
        </div>

        <!-- ✅ INDICATEUR DE CHARGEMENT -->
        <div v-if="loading" class="text-center py-12">
          <div class="text-gray-500">
            <div class="text-4xl mb-4">🔄</div>
            <div>{{ loadingMessage || 'Chargement des planifications...' }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ✅ MODAL DE RÉSULTAT CORRIGÉE -->
    <div v-if="showResultModal && rapportPlanification" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-2xl max-h-[80vh] overflow-y-auto">
        <div class="flex justify-between items-center mb-4">
          <h3 class="text-lg font-semibold">🤖 Résultat de la planification automatique</h3>
          <button
            @click="showResultModal = false"
            class="text-gray-400 hover:text-gray-600"
          >
            ✕
          </button>
        </div>

        <!-- Statistiques du rapport -->
        <div class="grid grid-cols-2 gap-4 mb-6">
          <div class="bg-green-50 p-4 rounded-lg">
            <div class="text-2xl font-bold text-green-600">
              {{ rapportPlanification.nombreCommandesPlanifiees || rapportPlanification.planifications_creees || 0 }}
            </div>
            <div class="text-sm text-green-700">Commandes planifiées</div>
          </div>
          <div class="bg-red-50 p-4 rounded-lg">
            <div class="text-2xl font-bold text-red-600">
              {{ rapportPlanification.nombreCommandesNonPlanifiees || 0 }}
            </div>
            <div class="text-sm text-red-700">Commandes non planifiées</div>
          </div>
        </div>

        <!-- Détails du rapport -->
        <div class="bg-gray-50 p-4 rounded-lg mb-4">
          <pre class="text-sm text-gray-600 whitespace-pre-wrap">{{ JSON.stringify(rapportPlanification, null, 2) }}</pre>
        </div>

        <div class="flex justify-end">
          <button
            @click="showResultModal = false"
            class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
          >
            Fermer
          </button>
        </div>
      </div>
    </div>

    <!-- ✅ MODAL DE DIAGNOSTIC -->
    <div v-if="showDiagnosticModal && diagnosticData" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-4xl max-h-[80vh] overflow-y-auto">
        <div class="flex justify-between items-center mb-4">
          <h3 class="text-lg font-semibold">🔍 Diagnostic Planifications</h3>
          <button
            @click="showDiagnosticModal = false"
            class="text-gray-400 hover:text-gray-600"
          >
            ✕
          </button>
        </div>

        <div class="bg-gray-50 p-4 rounded-lg">
          <pre class="text-sm text-gray-600 whitespace-pre-wrap">{{ JSON.stringify(diagnosticData, null, 2) }}</pre>
        </div>

        <div class="flex justify-end mt-4">
          <button
            @click="showDiagnosticModal = false"
            class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
          >
            Fermer
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, inject } from 'vue'
import { apiService, type Planification } from '../services/api'

// ✅ ÉTAT LOCAL CORRIGÉ
const planifications = ref<Planification[]>([])
const loading = ref(false)
const loadingMessage = ref('')
const showResultModal = ref(false)
const showDiagnosticModal = ref(false)
const rapportPlanification = ref<any>(null)
const diagnosticData = ref<any>(null)

// ✅ PÉRIODE AVEC VALEURS PAR DÉFAUT SENSÉES
const periode = ref({
  debut: new Date().toISOString().split('T')[0],
  fin: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
})

// Injection de la fonction de notification
const showNotification = inject('showNotification') as (message: string, type?: 'success' | 'error') => void

// ✅ COMPUTED PROPERTIES CORRIGÉES
const planificationsGroupees = computed(() => {
  const groupes: Record<string, Planification[]> = {}

  planifications.value.forEach(planification => {
    const date = planification.datePlanifiee || planification.datePlanification || 'Inconnu'
    if (!groupes[date]) {
      groupes[date] = []
    }
    groupes[date].push(planification)
  })

  return Object.entries(groupes)
    .map(([date, planifs]) => ({
      date,
      planifications: planifs.sort((a, b) => (a.heureDebut || '').localeCompare(b.heureDebut || ''))
    }))
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
})

const planificationsTerminees = computed(() =>
  planifications.value.filter(p => p.terminee).length
)

const planificationsEnCours = computed(() =>
  planifications.value.filter(p => !p.terminee).length
)

const employesUtilises = computed(() =>
  new Set(planifications.value.map(p => p.employeId).filter(Boolean)).size
)

// ✅ MÉTHODES PRINCIPALES CORRIGÉES

/**
 * ✅ CHARGER LES PLANIFICATIONS (MÉTHODE PRINCIPALE)
 */
const chargerPlanifications = async () => {
  loading.value = true
  loadingMessage.value = 'Chargement des planifications...'

  try {
    console.log('🔍 Chargement planifications pour période:', periode.value)

    // ✅ Utiliser la méthode par période d'abord
    planifications.value = await apiService.getPlanificationsByPeriode(
      periode.value.debut,
      periode.value.fin
    )

    console.log('✅ Planifications chargées:', planifications.value.length)

    if (planifications.value.length === 0) {
      showNotification?.('Aucune planification trouvée pour cette période', 'error')
    } else {
      showNotification?.(`${planifications.value.length} planifications chargées`, 'success')
    }

  } catch (error) {
    console.error('❌ Erreur lors du chargement des planifications:', error)
    showNotification?.('Erreur lors du chargement des planifications', 'error')

    // ✅ Fallback : essayer de charger toutes les planifications
    try {
      console.log('🔄 Fallback: chargement de toutes les planifications...')
      loadingMessage.value = 'Chargement de toutes les planifications...'
      planifications.value = await apiService.getPlanifications()
      console.log('✅ Fallback réussi:', planifications.value.length, 'planifications')

      if (planifications.value.length > 0) {
        showNotification?.(`${planifications.value.length} planifications chargées (toutes)`, 'success')
      }
    } catch (fallbackError) {
      console.error('❌ Fallback échoué:', fallbackError)
      planifications.value = []
    }
  } finally {
    loading.value = false
    loadingMessage.value = ''
  }
}

/**
 * ✅ PLANIFICATION AUTOMATIQUE CORRIGÉE
 */
const planifierAutomatiquement = async () => {
  loading.value = true
  loadingMessage.value = 'Planification automatique en cours...'

  try {
    console.log('🚀 Lancement planification automatique')

    const rapport = await apiService.planifierAutomatique()
    rapportPlanification.value = rapport
    showResultModal.value = true

    const nombrePlanifiees = rapport.nombreCommandesPlanifiees || rapport.planifications_creees || 0

    showNotification?.(
      `Planification terminée: ${nombrePlanifiees} commandes planifiées`,
      nombrePlanifiees > 0 ? 'success' : 'error'
    )

    // ✅ Recharger les données
    await chargerPlanifications()

  } catch (error) {
    console.error('❌ Erreur lors de la planification automatique:', error)
    showNotification?.('Erreur lors de la planification automatique', 'error')
  } finally {
    loading.value = false
    loadingMessage.value = ''
  }
}

/**
 * ✅ PLANIFICATION AVEC TRANSACTION
 */
const planifierAvecTransaction = async () => {
  loading.value = true
  loadingMessage.value = 'Planification avec transaction en cours...'

  try {
    console.log('🚀 Lancement planification avec transaction')

    const rapport = await apiService.planifierAvecTransaction(10)
    rapportPlanification.value = rapport
    showResultModal.value = true

    const nombrePlanifiees = rapport.planifications_creees || rapport.planifications_sauvees || 0

    showNotification?.(
      `Planification avec transaction terminée: ${nombrePlanifiees} commandes planifiées`,
      nombrePlanifiees > 0 ? 'success' : 'error'
    )

    // ✅ Recharger les données
    await chargerPlanifications()

  } catch (error) {
    console.error('❌ Erreur lors de la planification avec transaction:', error)
    showNotification?.('Erreur lors de la planification avec transaction', 'error')
  } finally {
    loading.value = false
    loadingMessage.value = ''
  }
}

/**
 * ✅ TERMINER UNE PLANIFICATION (TYPE CORRIGÉ)
 */
const terminerPlanification = async (id: string | number) => {
  try {
    console.log('✅ Terminer planification:', id)

    await apiService.terminerPlanification(id)
    showNotification?.('Planification terminée', 'success')

    // ✅ Recharger les données
    await chargerPlanifications()

  } catch (error) {
    console.error('❌ Erreur:', error)
    showNotification?.('Erreur lors de la finalisation', 'error')
  }
}

/**
 * ✅ VIDER TOUTES LES PLANIFICATIONS
 */
const viderPlanifications = async () => {
  if (!confirm('⚠️ Êtes-vous sûr de vouloir supprimer TOUTES les planifications ?')) {
    return
  }

  loading.value = true
  loadingMessage.value = 'Suppression des planifications...'

  try {
    const result = await apiService.viderPlanifications()

    if (result.success) {
      showNotification?.(
        `${result.planificationsSupprimees || 'Toutes les'} planifications supprimées`,
        'success'
      )
      // ✅ Recharger les données
      await chargerPlanifications()
    } else {
      showNotification?.('Erreur lors de la suppression', 'error')
    }
  } catch (error) {
    console.error('❌ Erreur:', error)
    showNotification?.('Erreur lors de la suppression', 'error')
  } finally {
    loading.value = false
    loadingMessage.value = ''
  }
}

/**
 * ✅ DIAGNOSTIC
 */
const voirDiagnostic = async () => {
  try {
    diagnosticData.value = await apiService.diagnosticPlanifications()
    showDiagnosticModal.value = true
  } catch (error) {
    console.error('❌ Erreur diagnostic:', error)
    showNotification?.('Erreur lors du diagnostic', 'error')
  }
}

// ✅ MÉTHODES UTILITAIRES

const formatDate = (dateStr: string): string => {
  try {
    return new Date(dateStr).toLocaleDateString('fr-FR', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
  } catch {
    return dateStr
  }
}

const getPrioriteClass = (priorite: string): string => {
  switch (priorite?.toUpperCase()) {
    case 'HAUTE':
      return 'bg-red-100 text-red-800'
    case 'MOYENNE':
      return 'bg-yellow-100 text-yellow-800'
    case 'NORMALE':
      return 'bg-green-100 text-green-800'
    case 'BASSE':
      return 'bg-gray-100 text-gray-800'
    default:
      return 'bg-blue-100 text-blue-800'
  }
}

const getHeureFin = (planification: Planification): string => {
  if (planification.heureFin) return planification.heureFin

  try {
    const debut = new Date(`2000-01-01T${planification.heureDebut}:00`)
    const fin = new Date(debut.getTime() + (planification.dureeMinutes * 60000))
    return fin.toTimeString().slice(0, 5)
  } catch {
    return 'N/A'
  }
}

// ✅ MONTAGE DU COMPOSANT
onMounted(() => {
  console.log('📅 Composant PlanificationView monté')
  chargerPlanifications()
})
</script>

<!-- ============= INSTRUCTIONS D'UTILISATION ============= -->

<!--
✅ CHANGEMENTS APPORTÉS :

1. 🔧 MÉTHODE chargerPlanifications() : Créée et corrigée avec fallback
2. 🔧 TYPES CORRIGÉS : terminerPlanification accepte string | number
3. 🔧 GESTION D'ERREUR : Try/catch partout avec fallbacks
4. 🔧 BOUTONS : Ajout planifierAvecTransaction et diagnostic
5. 🔧 COMPUTED : Calculs statistiques robustes
6. 🔧 LOADING : Messages de chargement détaillés
7. 🔧 MODAL : Affichage rapport amélioré

📝 POUR TESTER :
1. Remplacez complètement votre PlanificationView.vue par ce code
2. Redémarrez le frontend
3. Ouvrez l'onglet Planification
4. Testez dans l'ordre :
   - Actualiser (pour voir les planifications existantes)
   - Diagnostic (pour voir l'état du backend)
   - Planification avec transaction (méthode qui marche)
   - Planification automatique

🎯 SI ÇA NE MARCHE TOUJOURS PAS :
- Regardez la console du navigateur
- Vérifiez les logs du backend
- Testez les URLs manuellement avec curl
-->
