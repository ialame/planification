<template>
  <div class="max-w-7xl mx-auto p-6 bg-gray-100 min-h-screen">

    <!-- Liste des employés -->
    <div v-if="showEmployeeList" class="bg-white rounded-lg shadow-lg p-6">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-900 flex items-center gap-2">
          👥 Sélectionner un employé
        </h1>
        <div class="flex items-center gap-4">
          <input
            v-model="selectedDate"
            type="date"
            class="border border-gray-300 rounded-lg px-3 py-2"
          />
          <div class="text-sm text-gray-600">
            📅 {{ formatDate(selectedDate) }}
          </div>
        </div>
      </div>

      <div v-if="loading" class="flex justify-center items-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="emp in employeeList"
          :key="emp.id"
          @click="selectEmployee(emp.id)"
          :class="[
            'border-l-4 bg-white rounded-lg shadow hover:shadow-lg transition-all cursor-pointer p-6',
            getEmployeeStatusColor(emp.status)
          ]"
        >
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-lg font-bold text-gray-900">{{ emp.name }}</h3>
            <span :class="[
              'px-2 py-1 rounded text-xs font-medium',
              emp.status === 'overloaded' ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'
            ]">
              {{ getEmployeeStatusText(emp) }}
            </span>
          </div>

          <div class="space-y-2 text-sm text-gray-600">
            <div class="flex justify-between">
              <span>⏱️ Temps:</span>
              <span>{{ formatTime(emp.totalMinutes) }} / {{ formatTime(emp.maxMinutes) }}</span>
            </div>
            <div class="flex justify-between">
              <span>📋 Tâches:</span>
              <span>{{ emp.taskCount }}</span>
            </div>
            <div class="flex justify-between">
              <span>🃏 Cartes:</span>
              <span>{{ emp.cardCount }}</span>
            </div>
          </div>

          <div v-if="emp.status === 'overloaded'" class="mt-3 text-right">
            <div class="text-red-600 font-semibold text-sm">
              +{{ emp.totalMinutes - emp.maxMinutes }} min de dépassement
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Détail employé -->
    <div v-else-if="employee" class="space-y-6">
      <!-- En-tête employé -->
      <div class="bg-white rounded-lg shadow-lg p-6">
        <div class="flex items-center justify-between mb-6">
          <div class="flex items-center gap-4">
            <button
              @click="backToList"
              class="bg-gray-500 text-white px-4 py-2 rounded hover:bg-gray-600 transition-colors"
            >
              ← Retour
            </button>
            <h1 class="text-2xl font-bold text-gray-900 flex items-center gap-2">
              👤 {{ employee.name }}
            </h1>
            <span :class="[
              'px-3 py-1 rounded',
              employee.status === 'overloaded' ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'
            ]">
              {{ getEmployeeStatusText(employee) }}
            </span>
          </div>

          <div class="text-sm text-gray-600">
            📅 {{ formatDate(selectedDate) }}
          </div>
        </div>

        <!-- Métriques employé -->
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div class="bg-blue-100 border border-blue-200 rounded-lg p-4">
            <div class="flex items-center gap-2 text-blue-800">
              📋 <span class="font-semibold">Tâches totales</span>
            </div>
            <div class="text-2xl font-bold text-blue-900">{{ totalTasks }}</div>
          </div>
          <div class="bg-purple-100 border border-purple-200 rounded-lg p-4">
            <div class="flex items-center gap-2 text-purple-800">
              🃏 <span class="font-semibold">Cartes totales</span>
            </div>
            <div class="text-2xl font-bold text-purple-900">{{ totalCards }}</div>
          </div>
          <div class="bg-green-100 border border-green-200 rounded-lg p-4">
            <div class="flex items-center gap-2 text-green-800">
              ✅ <span class="font-semibold">Terminées</span>
            </div>
            <div class="text-2xl font-bold text-green-900">{{ completedTasks }}</div>
          </div>
          <div class="bg-yellow-100 border border-yellow-200 rounded-lg p-4">
            <div class="flex items-center gap-2 text-yellow-800">
              🔄 <span class="font-semibold">En cours</span>
            </div>
            <div class="text-2xl font-bold text-yellow-900">{{ inProgressTasks }}</div>
          </div>
        </div>

        <!-- Action si surchargé -->
        <div v-if="employee.status === 'overloaded'" class="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg">
          <div class="flex items-center justify-between">
            <span class="text-sm text-red-700 font-medium">
              ⚠️ Employé surchargé de {{ employee.totalMinutes - employee.maxMinutes }} minutes
            </span>
            <button
              @click="requestOvertime(employee.id)"
              class="px-4 py-2 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 transition-colors"
            >
              Demander heures supplémentaires
            </button>
          </div>
        </div>
      </div>

      <!-- Liste des tâches -->
      <div class="bg-white rounded-lg shadow-lg p-6">
        <h2 class="text-xl font-bold text-gray-900 mb-4">📋 Tâches du jour</h2>

        <div class="space-y-3">
          <div
            v-for="task in employee.tasks"
            :key="task.id"
            class="border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
          >
            <!-- En-tête de la tâche -->
            <div class="p-4">
              <div class="flex items-center justify-between">
                <div class="flex items-center gap-4">
                  <div class="flex items-center gap-2">
                    🕐
                    <span class="font-mono text-sm bg-gray-100 px-2 py-1 rounded">
                      {{ task.startTime }} → {{ task.endTime }}
                    </span>
                    <span class="text-sm text-gray-600">
                      ({{ formatTime(task.duration) }})
                    </span>
                  </div>

                  <span :class="[
                    'px-2 py-1 rounded border text-xs font-medium',
                    getPriorityColor(task.priority)
                  ]">
                    {{ task.priority }}
                  </span>

                  <span :class="[
                    'px-2 py-1 rounded text-xs font-medium',
                    getStatusColor(task.status)
                  ]">
                    {{ task.status }}
                  </span>
                </div>

                <div class="flex items-center gap-4 text-sm text-gray-600">
                  <span>🃏 {{ task.cardCount }} cartes</span>
                  <span>💰 {{ task.amount.toFixed(2) }}€</span>
                  <button
                    @click="editTask(task.id)"
                    class="text-blue-600 hover:text-blue-800 font-medium"
                  >
                    ✏️ Modifier
                  </button>
                  <button
                    @click="toggleTaskCards(task.id)"
                    class="text-purple-600 hover:text-purple-800 font-medium"
                  >
                    {{ task.expanded ? '📁 Masquer' : '📂 Voir cartes' }}
                  </button>
                </div>
              </div>

              <div class="mt-2">
                <span class="font-mono text-sm text-gray-700 bg-gray-100 px-2 py-1 rounded">
                  ID: {{ task.id }}
                </span>
              </div>
            </div>

            <!-- Liste des cartes (accordéon) -->
            <div v-if="task.expanded" class="border-t border-gray-200 bg-gray-50">
              <div class="p-4">
                <h4 class="text-sm font-semibold text-gray-700 mb-3">
                  🃏 Cartes de cette commande ({{ task.cards.length }})
                </h4>
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
                  <div
                    v-for="card in task.cards"
                    :key="card.id"
                    class="bg-white border border-gray-200 rounded p-3 text-sm"
                  >
                    <div class="flex items-center justify-between mb-2">
                      <div>
                        <div class="font-medium text-gray-900">{{ card.name }}</div>
                        <div class="text-gray-500 text-xs">{{ card.label_name }}</div>
                      </div>
                    </div>
                    <div class="flex justify-between text-xs text-gray-600">
                      <span>⏱️ {{ formatTime(card.duration) }}</span>
                      <span>💰 {{ card.amount.toFixed(2) }}€</span>
                    </div>
                    <div class="mt-1 text-xs text-gray-400 truncate">
                      {{ card.id }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'

// Types
interface Card {
  id: string
  translatable_id: string
  name: string
  label_name: string
  duration: number
  amount: number
}

interface Task {
  id: string
  priority: 'Haute' | 'Moyenne' | 'Basse'
  status: 'En cours' | 'Planifiée' | 'Terminée'
  startTime: string
  endTime: string
  duration: number
  amount: number
  cardCount: number
  cards: Card[]
  expanded?: boolean
}

interface Employee {
  id: string
  name: string
  totalMinutes: number
  maxMinutes: number
  status: 'overloaded' | 'available' | 'full'
  tasks: Task[]
}

interface EmployeeListItem {
  id: string
  name: string
  totalMinutes: number
  maxMinutes: number
  status: 'overloaded' | 'available' | 'full'
  taskCount: number
  cardCount: number
}

// État réactif
const selectedDate = ref('2025-06-04')
const selectedEmployeeId = ref<string | null>(null)
const employee = ref<Employee | null>(null)
const employeeList = ref<EmployeeListItem[]>([])
const loading = ref(false)
const showEmployeeList = ref(true)

// Computed properties
const totalTasks = computed(() => employee.value?.tasks.length || 0)
const totalCards = computed(() =>
  employee.value?.tasks.reduce((total, task) => total + task.cardCount, 0) || 0
)
const completedTasks = computed(() =>
  employee.value?.tasks.filter(task => task.status === 'Terminée').length || 0
)
const inProgressTasks = computed(() =>
  employee.value?.tasks.filter(task => task.status === 'En cours').length || 0
)

// Méthodes utilitaires
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

const getEmployeeStatusText = (emp: Employee | EmployeeListItem): string => {
  const percentage = ((emp.totalMinutes / emp.maxMinutes) * 100).toFixed(1)
  if (emp.status === 'overloaded') {
    return `⚠️ Surchargé (${percentage}%)`
  }
  if (emp.status === 'available') {
    return `✅ Disponible (${percentage}%)`
  }
  return `📊 Occupé (${percentage}%)`
}

const getPriorityColor = (priority: string): string => {
  switch (priority) {
    case 'Haute': return 'bg-red-100 text-red-800 border-red-200'
    case 'Moyenne': return 'bg-yellow-100 text-yellow-800 border-yellow-200'
    case 'Basse': return 'bg-green-100 text-green-800 border-green-200'
    default: return 'bg-gray-100 text-gray-800 border-gray-200'
  }
}

const getStatusColor = (status: string): string => {
  switch (status) {
    case 'En cours': return 'bg-blue-100 text-blue-800'
    case 'Planifiée': return 'bg-gray-100 text-gray-800'
    case 'Terminée': return 'bg-green-100 text-green-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

const getEmployeeStatusColor = (status: string): string => {
  switch (status) {
    case 'overloaded': return 'border-l-red-500 bg-red-50'
    case 'available': return 'border-l-green-500 bg-green-50'
    case 'full': return 'border-l-yellow-500 bg-yellow-50'
    default: return 'border-l-gray-500 bg-gray-50'
  }
}

// API calls
const fetchEmployeeList = async (date: string): Promise<void> => {
  loading.value = true
  try {
    // Mock data pour test
    setTimeout(() => {
      employeeList.value = [
        {
          id: 'sophie-dubois',
          name: 'Sophie Dubois',
          totalMinutes: 387,
          maxMinutes: 360,
          status: 'overloaded',
          taskCount: 12,
          cardCount: 45
        },
        {
          id: 'pierre-bernard',
          name: 'Pierre Bernard',
          totalMinutes: 507,
          maxMinutes: 480,
          status: 'overloaded',
          taskCount: 15,
          cardCount: 67
        },
        {
          id: 'jean-dupont',
          name: 'Jean Dupont',
          totalMinutes: 507,
          maxMinutes: 480,
          status: 'overloaded',
          taskCount: 18,
          cardCount: 78
        },
        {
          id: 'marie-martin',
          name: 'Marie Martin',
          totalMinutes: 320,
          maxMinutes: 480,
          status: 'available',
          taskCount: 8,
          cardCount: 25
        }
      ]
      loading.value = false
    }, 500)
  } catch (error) {
    console.error('Erreur chargement liste employés:', error)
    loading.value = false
  }
}

const fetchEmployeeDetail = async (employeeId: string, date: string): Promise<void> => {
  loading.value = true
  try {
    // Mock data pour test
    setTimeout(() => {
      const mockEmployee: Employee = {
        id: employeeId,
        name: employeeId === 'sophie-dubois' ? 'Sophie Dubois' :
          employeeId === 'pierre-bernard' ? 'Pierre Bernard' :
            employeeId === 'jean-dupont' ? 'Jean Dupont' : 'Marie Martin',
        totalMinutes: employeeId === 'marie-martin' ? 320 : 507,
        maxMinutes: employeeId === 'sophie-dubois' ? 360 : 480,
        status: employeeId === 'marie-martin' ? 'available' : 'overloaded',
        tasks: []
      }

      // Générer des tâches de test
      for (let i = 1; i <= 12; i++) {
        const startHour = 7 + Math.floor(i / 2)
        const startMin = (i % 2) * 30
        const duration = 30 + Math.random() * 30
        const cardCount = Math.floor(Math.random() * 5) + 2

        mockEmployee.tasks.push({
          id: `CMD${i.toString().padStart(3, '0')}`,
          priority: ['Haute', 'Moyenne', 'Basse'][Math.floor(Math.random() * 3)] as any,
          status: ['Planifiée', 'En cours', 'Terminée'][Math.floor(Math.random() * 3)] as any,
          startTime: `${startHour.toString().padStart(2, '0')}:${startMin.toString().padStart(2, '0')}`,
          endTime: `${(startHour + Math.floor((startMin + duration) / 60)).toString().padStart(2, '0')}:${((startMin + duration) % 60).toString().padStart(2, '0')}`,
          duration: Math.floor(duration),
          amount: cardCount * 25 + Math.random() * 100,
          cardCount,
          expanded: false,
          cards: Array.from({ length: cardCount }, (_, cardIndex) => ({
            id: `CARD${i}_${cardIndex + 1}`,
            translatable_id: `TRANS${(cardIndex % 3) + 1}`,
            name: ['Alakazam', 'Tortank', 'Leveinard'][cardIndex % 3],
            label_name: ['Alakazam', 'Tortank', 'Leveinard'][cardIndex % 3],
            duration: Math.floor(duration / cardCount),
            amount: 20 + Math.random() * 60
          }))
        })
      }

      employee.value = mockEmployee
      loading.value = false
    }, 500)
  } catch (error) {
    console.error('Erreur chargement détail employé:', error)
    loading.value = false
  }
}

// Actions
const selectEmployee = (employeeId: string): void => {
  selectedEmployeeId.value = employeeId
  showEmployeeList.value = false
  fetchEmployeeDetail(employeeId, selectedDate.value)
}

const backToList = (): void => {
  showEmployeeList.value = true
  selectedEmployeeId.value = null
  employee.value = null
}

const editTask = (taskId: string): void => {
  console.log('Éditer commande:', taskId)
}

const toggleTaskCards = (taskId: string): void => {
  if (employee.value) {
    const task = employee.value.tasks.find(t => t.id === taskId)
    if (task) {
      task.expanded = !task.expanded
    }
  }
}

const requestOvertime = async (employeeId: string): Promise<void> => {
  console.log('Heures supplémentaires demandées pour:', employeeId)
}

// Lifecycle
onMounted(() => {
  fetchEmployeeList(selectedDate.value)
})

// Watchers
watch(selectedDate, (newDate) => {
  if (showEmployeeList.value) {
    fetchEmployeeList(newDate)
  } else if (selectedEmployeeId.value) {
    fetchEmployeeDetail(selectedEmployeeId.value, newDate)
  }
})
</script>

<style scoped>
/* Styles personnalisés si nécessaire */
</style>
