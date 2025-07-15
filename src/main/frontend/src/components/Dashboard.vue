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

    <!-- Cartes de statistiques -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <StatCard
        title="Commandes Totales"
        :value="stats?.totalOrders || 0"
        icon="📦"
        color="blue"
      />
      <StatCard
        title="En Attente"
        :value="stats?.pendingOrders || 0"
        icon="⏳"
        color="yellow"
      />
      <StatCard
        title="En Cours"
        :value="stats?.scheduledOrders || 0"
        icon="🔄"
        color="indigo"
      />
      <StatCard
        title="Terminées"
        :value="stats?.completedOrders || 0"
        icon="✅"
        color="green"
      />
    </div>

    <!-- Deuxième ligne de statistiques -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <StatCard
        title="En Retard"
        :value="stats?.overdueOrders || 0"
        icon="⚠️"
        color="red"
      />
      <StatCard
        title="Employés Actifs"
        :value="stats?.activeEmployees || 0"
        icon="👥"
        color="purple"
      />
      <StatCard
        title="Temps Moyen (h)"
        :value="stats?.averageProcessingTimeHours?.toFixed(1) || '0.0'"
        icon="⏱️"
        color="gray"
      />
    </div>

    <!-- Graphiques -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Graphique des commandes par jour -->
      <div class="bg-white p-6 rounded-lg shadow">
        <h3 class="text-lg font-semibold mb-4">📈 Commandes par jour (30 derniers jours)</h3>
        <div class="h-64">
          <canvas ref="dailyChart"></canvas>
        </div>
      </div>

      <!-- Graphique des commandes par priorité -->
      <div class="bg-white p-6 rounded-lg shadow">
        <h3 class="text-lg font-semibold mb-4">🎯 Commandes par priorité</h3>
        <div class="h-64">
          <canvas ref="priorityChart"></canvas>
        </div>
      </div>
    </div>

    <!-- Charge de travail des employés -->
    <div class="bg-white p-6 rounded-lg shadow">
      <h3 class="text-lg font-semibold mb-4">👥 Charge de travail des employés</h3>
      <div class="space-y-4">
        <div
          v-for="(workload, employeeName) in stats?.employeeWorkload"
          :key="employeeName"
          class="flex items-center justify-between"
        >
          <span class="font-medium">{{ employeeName }}</span>
          <div class="flex items-center space-x-2 flex-1 max-w-xs">
            <div class="w-full bg-gray-200 rounded-full h-4">
              <div
                :class="[
                  'h-4 rounded-full transition-all',
                  workload > 90 ? 'bg-red-500' :
                  workload > 70 ? 'bg-yellow-500' : 'bg-green-500'
                ]"
                :style="{ width: workload + '%' }"
              ></div>
            </div>
            <span class="text-sm font-medium min-w-[3rem]">{{ workload }}%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Actions rapides -->
    <div class="bg-white p-6 rounded-lg shadow">
      <h3 class="text-lg font-semibold mb-4">⚡ Actions rapides</h3>
      <div class="flex flex-wrap gap-3">
        <button
          @click="$emit('goToTab', 'commandes')"
          class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          📋 Nouvelle Commande
        </button>
        <button
          @click="$emit('goToTab', 'employes')"
          class="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors"
        >
          👤 Ajouter Employé
        </button>
        <button
          @click="planifierAutomatiquement"
          :disabled="loading"
          class="bg-purple-600 text-white px-4 py-2 rounded-lg hover:bg-purple-700 disabled:opacity-50 transition-colors"
        >
          🤖 Planification Auto
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, inject, nextTick } from 'vue'
import { apiService, type DashboardStats } from '../services/api'
import StatCard from './StatCard.vue'

// Props et émissions
defineEmits(['goToTab'])

// État local
const stats = ref<DashboardStats | null>(null)
const loading = ref(false)
const dailyChart = ref<HTMLCanvasElement>()
const priorityChart = ref<HTMLCanvasElement>()

// Injection de la fonction de notification
const showNotification = inject('showNotification') as (message: string, type?: 'success' | 'error') => void

// Chargement des données
const loadData = async () => {
  loading.value = true
  try {
    stats.value = await apiService.getDashboardStats()
    await nextTick()
    drawCharts()
  } catch (error) {
    console.error('Erreur lors du chargement des statistiques:', error)
    showNotification('Erreur lors du chargement des données', 'error')
  } finally {
    loading.value = false
  }
}

// Actualisation des données
const refreshData = () => {
  loadData()
}

// Planification automatique
const planifierAutomatiquement = async () => {
  loading.value = true
  try {
    const rapport = await apiService.planifierAutomatique()
    showNotification(`Planification terminée: ${rapport.nombreCommandesPlanifiees} commandes planifiées`)
    await loadData() // Actualiser les données
  } catch (error) {
    console.error('Erreur lors de la planification automatique:', error)
    showNotification('Erreur lors de la planification automatique', 'error')
  } finally {
    loading.value = false
  }
}

// Dessiner les graphiques
const drawCharts = () => {
  if (!stats.value) return

  // Graphique des commandes par jour
  if (dailyChart.value) {
    const ctx = dailyChart.value.getContext('2d')
    if (ctx) {
      // Effacer le canvas
      ctx.clearRect(0, 0, dailyChart.value.width, dailyChart.value.height)

      // Données du graphique
      const dates = Object.keys(stats.value.dailyOrdersChart || {})
      const values = Object.values(stats.value.dailyOrdersChart || {})

      if (dates.length > 0) {
        drawLineChart(ctx, dailyChart.value, dates, values, '#3B82F6')
      }
    }
  }

  // Graphique des commandes par priorité
  if (priorityChart.value) {
    const ctx = priorityChart.value.getContext('2d')
    if (ctx) {
      // Effacer le canvas
      ctx.clearRect(0, 0, priorityChart.value.width, priorityChart.value.height)

      // Données du graphique
      const priorities = Object.keys(stats.value.ordersByPriority || {})
      const values = Object.values(stats.value.ordersByPriority || {})

      if (priorities.length > 0) {
        drawBarChart(ctx, priorityChart.value, priorities, values)
      }
    }
  }
}

// Fonction pour dessiner un graphique linéaire simple
const drawLineChart = (ctx: CanvasRenderingContext2D, canvas: HTMLCanvasElement, labels: string[], data: number[], color: string) => {
  const padding = 40
  const width = canvas.width - padding * 2
  const height = canvas.height - padding * 2

  canvas.width = canvas.offsetWidth
  canvas.height = canvas.offsetHeight

  const maxValue = Math.max(...data, 1)
  const stepX = width / (data.length - 1)
  const stepY = height / maxValue

  // Dessiner les axes
  ctx.strokeStyle = '#E5E7EB'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(padding, padding)
  ctx.lineTo(padding, height + padding)
  ctx.lineTo(width + padding, height + padding)
  ctx.stroke()

  // Dessiner la ligne
  ctx.strokeStyle = color
  ctx.lineWidth = 2
  ctx.beginPath()

  data.forEach((value, index) => {
    const x = padding + index * stepX
    const y = height + padding - value * stepY

    if (index === 0) {
      ctx.moveTo(x, y)
    } else {
      ctx.lineTo(x, y)
    }
  })

  ctx.stroke()

  // Dessiner les points
  ctx.fillStyle = color
  data.forEach((value, index) => {
    const x = padding + index * stepX
    const y = height + padding - value * stepY

    ctx.beginPath()
    ctx.arc(x, y, 3, 0, Math.PI * 2)
    ctx.fill()
  })
}

// Fonction pour dessiner un graphique en barres simple
const drawBarChart = (ctx: CanvasRenderingContext2D, canvas: HTMLCanvasElement, labels: string[], data: number[]) => {
  const padding = 40
  const width = canvas.width - padding * 2
  const height = canvas.height - padding * 2

  canvas.width = canvas.offsetWidth
  canvas.height = canvas.offsetHeight

  const maxValue = Math.max(...data, 1)
  const barWidth = width / data.length * 0.8
  const barSpacing = width / data.length * 0.2

  const colors = ['#EF4444', '#F59E0B', '#10B981'] // Rouge, Jaune, Vert

  // Dessiner les barres
  data.forEach((value, index) => {
    const x = padding + index * (barWidth + barSpacing) + barSpacing / 2
    const barHeight = (value / maxValue) * height
    const y = height + padding - barHeight

    ctx.fillStyle = colors[index % colors.length]
    ctx.fillRect(x, y, barWidth, barHeight)

    // Étiquettes
    ctx.fillStyle = '#374151'
    ctx.font = '12px Inter'
    ctx.textAlign = 'center'
    ctx.fillText(labels[index], x + barWidth / 2, height + padding + 20)
    ctx.fillText(value.toString(), x + barWidth / 2, y - 5)
  })
}

// Lifecycle
onMounted(() => {
  loadData()
})
</script>
