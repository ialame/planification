// EmployeeDetailPage.vue - Page individuelle par employé

// Types
import {computed, ref} from "vue";

interface Card {
  id: string
  translatable_id: string
  name: string              // depuis card_translation.name
  label_name: string        // depuis card_translation.label_name
  duration: number          // temps de traitement pour cette carte
  amount: number
}

interface Task {
  id: string                // ID de la commande
  priority: 'Haute' | 'Moyenne' | 'Basse'
  status: 'En cours' | 'Planifiée' | 'Terminée'
  startTime: string
  endTime: string
  duration: number          // durée totale de la tâche
  amount: number           // montant total de la commande
  cardCount: number        // nombre de cartes dans cette commande
  cards: Card[]            // liste des cartes de cette commande
  expanded?: boolean       // pour l'affichage accordéon
}

interface Employee {
  id: string
  name: string
  totalMinutes: number
  maxMinutes: number
  status: 'overloaded' | 'available' | 'full'
  tasks: Task[]            // liste des tâches/commandes
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

// Composable pour la gestion d'un employé individuel
export function useEmployeeDetail() {
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
      // TODO: Remplacer par vraie API
      // const response = await fetch(`/api/employees/summary?date=${date}`)
      // const data = await response.json()
      // employeeList.value = data.employees

      // Mock data
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
    } catch (error) {
      console.error('Erreur chargement liste employés:', error)
    } finally {
      loading.value = false
    }
  }

  const fetchEmployeeDetail = async (employeeId: string, date: string): Promise<void> => {
    loading.value = true
    try {
      // TODO: Remplacer par vraie API
      // const response = await fetch(`/api/employees/${employeeId}/detail?date=${date}`)
      // const data = await response.json()
      // employee.value = data.employee

      // Mock data
      const mockEmployee: Employee = {
        id: employeeId,
        name: employeeId === 'sophie-dubois' ? 'Sophie Dubois' : 'Pierre Bernard',
        totalMinutes: employeeId === 'sophie-dubois' ? 387 : 507,
        maxMinutes: employeeId === 'sophie-dubois' ? 360 : 480,
        status: 'overloaded',
        tasks: [
          {
            id: 'CMD001',
            priority: 'Haute',
            status: 'En cours',
            startTime: '08:00',
            endTime: '09:15',
            duration: 75,
            amount: 225.00,
            cardCount: 3,
            expanded: false,
            cards: [
              {
                id: 'CARD001',
                translatable_id: 'TRANS001',
                name: 'Alakazam',
                label_name: 'Alakazam',
                duration: 25,
                amount: 75.00
              },
              {
                id: 'CARD002',
                translatable_id: 'TRANS002',
                name: 'Tortank',
                label_name: 'Tortank',
                duration: 25,
                amount: 75.00
              },
              {
                id: 'CARD003',
                translatable_id: 'TRANS003',
                name: 'Leveinard',
                label_name: 'Leveinard',
                duration: 25,
                amount: 75.00
              }
            ]
          },
          {
            id: 'CMD002',
            priority: 'Moyenne',
            status: 'Planifiée',
            startTime: '09:30',
            endTime: '10:45',
            duration: 75,
            amount: 300.00,
            cardCount: 4,
            expanded: false,
            cards: [
              {
                id: 'CARD004',
                translatable_id: 'TRANS001',
                name: 'Alakazam',
                label_name: 'Alakazam',
                duration: 20,
                amount: 80.00
              },
              {
                id: 'CARD005',
                translatable_id: 'TRANS002',
                name: 'Tortank',
                label_name: 'Tortank',
                duration: 18,
                amount: 70.00
              },
              {
                id: 'CARD006',
                translatable_id: 'TRANS003',
                name: 'Leveinard',
                label_name: 'Leveinard',
                duration: 19,
                amount: 75.00
              },
              {
                id: 'CARD007',
                translatable_id: 'TRANS001',
                name: 'Alakazam',
                label_name: 'Alakazam',
                duration: 18,
                amount: 75.00
              }
            ]
          }
          // ... Générer plus de tâches pour simuler une journée complète
        ]
      }

      // Générer plus de tâches pour remplir la journée
      for (let i = 3; i <= 12; i++) {
        const startHour = 10 + Math.floor(i / 2)
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
    } catch (error) {
      console.error('Erreur chargement détail employé:', error)
    } finally {
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
    try {
      const response = await fetch(`/api/employees/${employeeId}/overtime`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        }
      })
      if (response.ok) {
        console.log('Heures supplémentaires demandées pour:', employeeId)
      }
    } catch (error) {
      console.error('Erreur heures sup:', error)
    }
  }

  return {
    // État
    selectedDate,
    selectedEmployeeId,
    employee,
    employeeList,
    loading,
    showEmployeeList,

    // Computed
    totalTasks,
    totalCards,
    completedTasks,
    inProgressTasks,

    // Méthodes
    formatTime,
    formatDate,
    getEmployeeStatusText,
    getPriorityColor,
    getStatusColor,
    getEmployeeStatusColor,

    // Actions
    fetchEmployeeList,
    fetchEmployeeDetail,
    selectEmployee,
    backToList,
    editTask,
    toggleTaskCards,
    requestOvertime
  }
}
