<template>
  <div class="w-full h-4 bg-gray-200 rounded-full border-2 border-black overflow-hidden">
    <div class="h-full transition-all duration-1000"
         :class="barColor"
         :style="{ width: pct + '%' }">
    </div>
  </div>
  <div class="text-center font-fredoka text-2xl mt-1 tabular-nums" :class="textColor">
    {{ displayTime }}
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  secondsRemaining: number | null
  totalSeconds: number
}>()

const pct = computed(() => {
  if (props.secondsRemaining === null) return 100
  return Math.max(0, (props.secondsRemaining / props.totalSeconds) * 100)
})

const barColor = computed(() => {
  if (pct.value > 50) return 'bg-wheel-green'
  if (pct.value > 20) return 'bg-wheel-orange'
  return 'bg-wheel-red'
})

const textColor = computed(() => {
  if (pct.value > 50) return 'text-green-700'
  if (pct.value > 20) return 'text-orange-600'
  return 'text-red-600'
})

const displayTime = computed(() => {
  if (props.secondsRemaining === null) return '--:--'
  const s = Math.max(0, props.secondsRemaining)
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
})
</script>
