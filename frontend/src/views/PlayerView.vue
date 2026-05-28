<template>
  <div class="min-h-screen bg-cream flex flex-col p-4 max-w-sm mx-auto">

    <!-- Header -->
    <div class="text-center mb-4">
      <h1 class="font-fredoka text-3xl">🎡 Wheel Unfortunate</h1>
      <p class="font-nunito font-semibold text-gray-600">Hi, {{ store.playerName }}!</p>
    </div>

    <!-- LOBBY: waiting -->
    <div v-if="store.gameState === 'LOBBY'" class="flex-1 flex flex-col items-center justify-center gap-4">
      <div class="bg-white border-4 border-black rounded-2xl p-6 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] text-center w-full">
        <p class="font-fredoka text-2xl mb-2">Waiting for host to start…</p>
        <div class="font-nunito text-gray-500">{{ store.players.length }} player(s) in room</div>
      </div>
      <PowerUpBar :power-ups="myPowerUps" @use="() => {}" />
    </div>

    <!-- ACTIVE: not selected -->
    <div v-else-if="store.gameState === 'ACTIVE' && !isSelectedPlayer" class="flex-1 flex flex-col gap-4">
      <!-- Countdown -->
      <div class="bg-white border-4 border-black rounded-2xl p-5 shadow-[4px_4px_0px_0px_rgba(0,0,0,1)] text-center">
        <p class="font-fredoka text-xl mb-2">Next spin in</p>
        <CountdownBar
          :secondsRemaining="store.secondsUntilSpin"
          :totalSeconds="store.config.spinIntervalMinutes * 60"
        />
      </div>

      <!-- Watching: challenge display + vote -->
      <div v-if="store.currentSpin && !store.isSpinning" class="flex flex-col gap-3">
        <div class="font-fredoka text-xl text-center">
          {{ store.currentSpin.player.name }} got:
        </div>
        <ChallengeCard :challenge="store.currentSpin.challenge" />
        <VotePanel v-if="store.voteWindowOpen" @vote="submitVote" />
        <div v-if="store.voteResult" class="text-center font-fredoka text-xl"
             :class="store.voteResult.completed ? 'text-green-600' : 'text-red-600'">
          {{ store.voteResult.completed ? '✅ Done!' : `❌ Refused! +${store.voteResult.extraDrinks} sips` }}
        </div>
      </div>

      <PowerUpBar :power-ups="myPowerUps" @use="usePowerUp" class="mt-auto" />
    </div>

    <!-- ACTIVE: YOU are selected -->
    <div v-else-if="store.gameState === 'ACTIVE' && isSelectedPlayer"
         class="flex-1 flex flex-col gap-4 animate-pulse-once">
      <div class="bg-wheel-red text-white font-fredoka text-3xl text-center py-4 rounded-2xl border-4 border-black shadow-[4px_4px_0px_0px_rgba(0,0,0,1)]">
        🔥 IT'S YOU! 🔥
      </div>
      <ChallengeCard :challenge="store.currentSpin!.challenge" />
      <VotePanel v-if="store.voteWindowOpen" @vote="submitVote" />
      <PowerUpBar :power-ups="myPowerUps" @use="usePowerUp" class="mt-auto" />
    </div>

    <!-- ENDED -->
    <div v-else-if="store.gameState === 'ENDED'" class="flex-1 flex flex-col items-center justify-center">
      <div class="font-fredoka text-3xl mb-4">Game over!</div>
      <router-link :to="`/stats/${code}`"
                   class="bg-wheel-purple text-white font-fredoka text-xl px-6 py-3 rounded-xl border-2 border-black shadow-[3px_3px_0px_0px_rgba(0,0,0,1)]">
        See Stats
      </router-link>
    </div>

    <!-- Active rules -->
    <ActiveRules :rules="store.activeRules" class="mt-4" />

    <!-- Revenge target picker -->
    <div v-if="showRevengePicker"
         class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white border-4 border-black rounded-2xl p-6 w-full max-w-xs shadow-[6px_6px_0px_0px_rgba(0,0,0,1)]">
        <h3 class="font-fredoka text-2xl mb-4 text-center">Pick a player</h3>
        <div class="flex flex-col gap-2">
          <button v-for="p in otherPlayers" :key="p.id"
                  @click="executeRevenge(p.id)"
                  class="font-nunito font-bold py-3 rounded-xl border-2 border-black bg-cream hover:bg-wheel-orange hover:text-white transition-colors">
            {{ p.name }}
          </button>
        </div>
        <button @click="showRevengePicker = false" class="mt-3 w-full font-nunito text-sm text-gray-500">
          Cancel
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGameStore } from '../stores/gameStore'
import { useWebSocket } from '../composables/useWebSocket'
import CountdownBar from '../components/CountdownBar.vue'
import ChallengeCard from '../components/ChallengeCard.vue'
import VotePanel from '../components/VotePanel.vue'
import PowerUpBar from '../components/PowerUpBar.vue'
import ActiveRules from '../components/ActiveRules.vue'

const route = useRoute()
const router = useRouter()
const store = useGameStore()
const { connect, send } = useWebSocket()

const code = route.params.code as string
const showRevengePicker = ref(false)

const isSelectedPlayer = computed(() =>
  !!store.currentSpin && store.currentSpin.player.id === store.playerId && !store.isSpinning
)

const myPowerUps = computed(() => store.myPlayer?.powerUps ?? [])
const otherPlayers = computed(() => store.players.filter(p => p.id !== store.playerId))

onMounted(async () => {
  store.roomCode = code
  if (!store.playerId) { router.push('/'); return }

  const res = await fetch(`/api/rooms/${code}`)
  if (!res.ok) { router.push('/'); return }
  const data = await res.json()
  store.applyRoomState(data)
  connect(code)
})

function submitVote(vote: string) {
  send(`/app/rooms/${code}/vote`, { vote, playerId: store.playerId })
}

function usePowerUp(type: string) {
  if (type === 'REVENGE') {
    showRevengePicker.value = true
    return
  }
  send(`/app/rooms/${code}/powerup`, { playerId: store.playerId, type })
}

function executeRevenge(targetId: string) {
  showRevengePicker.value = false
  send(`/app/rooms/${code}/powerup`, { playerId: store.playerId, type: 'REVENGE', targetId })
}
</script>
