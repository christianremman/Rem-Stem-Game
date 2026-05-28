<template>
  <div class="min-h-screen bg-cream p-6">
    <h1 class="font-fredoka text-5xl text-center mb-2">🏆 End of Night</h1>
    <p class="font-nunito text-center text-gray-500 mb-8">{{ totalSpins }} spins. What a night.</p>

    <div v-if="stats" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5 max-w-5xl mx-auto">
      <div v-for="player in stats.players" :key="player.id"
           class="bg-white border-4 border-black rounded-2xl p-5 shadow-[5px_5px_0px_0px_rgba(0,0,0,1)]">
        <div class="font-fredoka text-3xl mb-1 truncate">{{ player.name }}</div>
        <div class="inline-block bg-wheel-purple text-white font-fredoka text-sm px-3 py-1 rounded-full mb-3 border border-black">
          {{ player.title }}
        </div>
        <div class="grid grid-cols-2 gap-2 font-nunito text-sm">
          <div class="bg-cream rounded-lg p-2 text-center border border-black">
            <div class="font-bold text-xl font-fredoka">{{ player.timesSelected }}</div>
            <div class="text-gray-500">Spun</div>
          </div>
          <div class="bg-cream rounded-lg p-2 text-center border border-black">
            <div class="font-bold text-xl font-fredoka text-wheel-green">{{ player.completed }}</div>
            <div class="text-gray-500">Done</div>
          </div>
          <div class="bg-cream rounded-lg p-2 text-center border border-black">
            <div class="font-bold text-xl font-fredoka text-wheel-red">{{ player.refused }}</div>
            <div class="text-gray-500">Refused</div>
          </div>
          <div class="bg-cream rounded-lg p-2 text-center border border-black">
            <div class="font-bold text-xl font-fredoka text-wheel-orange">🍺 {{ player.estimatedSips }}</div>
            <div class="text-gray-500">Sips</div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="text-center font-nunito text-gray-500 mt-20">Loading stats…</div>

    <div class="text-center mt-10">
      <a href="/"
         class="inline-block bg-wheel-green text-white font-fredoka text-xl px-8 py-3 rounded-xl border-2 border-black shadow-[3px_3px_0px_0px_rgba(0,0,0,1)] hover:translate-y-0.5 transition-transform">
        Play Again 🎡
      </a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useGameStore } from '../stores/gameStore'
import confetti from 'canvas-confetti'

const route = useRoute()
const store = useGameStore()
const code = route.params.code as string

const stats = ref<{ players: any[]; totalSpins: number } | null>(null)
const totalSpins = computed(() => stats.value?.totalSpins ?? 0)

onMounted(async () => {
  if (store.stats) {
    stats.value = store.stats as any
  } else {
    const res = await fetch(`/api/rooms/${code}/end`, { method: 'POST' })
    if (res.ok) stats.value = await res.json()
  }

  confetti({
    particleCount: 150,
    spread: 90,
    origin: { y: 0.5 },
    colors: ['#EF4444','#3B82F6','#22C55E','#F97316','#A855F7','#EC4899']
  })
})
</script>
