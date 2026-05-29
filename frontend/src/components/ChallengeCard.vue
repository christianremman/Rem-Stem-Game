<template>
  <div class="bg-challenge border-4 border-black rounded-2xl p-6 shadow-[6px_6px_0px_0px_rgba(0,0,0,1)] text-center max-w-md mx-auto">
    <div class="font-fredoka text-4xl mb-1">{{ typeEmoji }}</div>
    <div class="font-fredoka text-3xl mb-3">{{ challenge.type.replace('_', ' ') }}</div>
    <p class="font-nunito font-semibold text-gray-900 text-lg leading-snug">{{ challenge.text }}</p>
    <div v-if="challenge.sips" class="mt-3 font-fredoka text-2xl text-wheel-red">
      🍺 {{ challenge.sips }} sip{{ challenge.sips > 1 ? 's' : '' }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ChallengeData } from '../stores/gameStore'

const props = defineProps<{ challenge: ChallengeData }>()

const typeEmoji = computed(() => ({
  DRINK: '🍺',
  SOCIAL: '🌍',
  GIVE_TAKE: '🔄',
  TRUTH: '💬',
  DARE: '😈',
  CHALLENGE: '🎯',
  HOT_SEAT: '🔥',
  MOST_LIKELY_TO: '👉',
  PERSONAL_QUESTION: '💬',
  PHYSICAL_TOUCH: '💋',
  ROMANTIC_DARE: '🌹'
}[props.challenge.type] ?? '🎡'))
</script>
