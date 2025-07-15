<template>
  <div :class="[
    'bg-white p-6 rounded-lg shadow-sm border-l-4 transition-transform hover:scale-105',
    borderColor
  ]">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-sm font-medium text-gray-600">{{ title }}</p>
        <p :class="['text-2xl font-bold', textColor]">{{ formattedValue }}</p>
      </div>
      <div :class="['text-3xl', textColor]">
        {{ icon }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  title: string
  value: string | number
  icon: string
  color: 'blue' | 'green' | 'yellow' | 'red' | 'purple' | 'indigo' | 'gray'
}

const props = defineProps<Props>()

const formattedValue = computed(() => {
  if (typeof props.value === 'number') {
    return props.value.toLocaleString()
  }
  return props.value
})

const borderColor = computed(() => {
  const colors = {
    blue: 'border-blue-500',
    green: 'border-green-500',
    yellow: 'border-yellow-500',
    red: 'border-red-500',
    purple: 'border-purple-500',
    indigo: 'border-indigo-500',
    gray: 'border-gray-500'
  }
  return colors[props.color]
})

const textColor = computed(() => {
  const colors = {
    blue: 'text-blue-600',
    green: 'text-green-600',
    yellow: 'text-yellow-600',
    red: 'text-red-600',
    purple: 'text-purple-600',
    indigo: 'text-indigo-600',
    gray: 'text-gray-600'
  }
  return colors[props.color]
})
</script>
