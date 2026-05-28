<template>
  <div class="flex gap-3 justify-center flex-wrap">
    <button
      v-for="pu in available"
      :key="pu.type"
      @click="$emit('use', pu.type)"
      class="flex flex-col items-center gap-1 bg-white border-2 border-black rounded-xl px-4 py-2 shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] hover:translate-y-0.5 hover:shadow-[1px_1px_0px_0px_rgba(0,0,0,1)] transition-all active:scale-95"
    >
      <span class="text-2xl">{{ pu.emoji }}</span>
      <span class="font-fredoka text-xs">{{ pu.label }}</span>
    </button>
    <div v-if="!available.length" class="font-nunito text-gray-400 text-sm">No power-ups left</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ powerUps: string[] }>()
defineEmits<{ use: [type: string] }>()

const ALL = [
  { type: 'REVENGE', emoji: '⚔️', label: 'Revenge' },
  { type: 'SAFE', emoji: '🛡️', label: 'Safe Card' },
  { type: 'DOUBLE_DOWN', emoji: '🎲', label: 'Double Down' }
]

const available = computed(() => ALL.filter(a => props.powerUps.includes(a.type)))
</script>
