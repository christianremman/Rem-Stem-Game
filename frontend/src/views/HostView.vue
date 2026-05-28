<template>
  <div class="min-h-screen bg-cream flex flex-col select-none overflow-hidden"
       :class="{ 'brightness-90': store.isWarning }">

    <!-- Top bar -->
    <div class="flex items-center justify-between px-6 py-2 bg-white border-b-4 border-black">
      <span class="font-fredoka text-2xl">🎡 Wheel Unfortunate</span>
      <div class="flex items-center gap-4">
        <span class="font-fredoka text-xl tracking-widest text-gray-500">{{ code }}</span>
        <button @click="muted = !muted" class="text-xl">{{ muted ? '🔇' : '🔊' }}</button>
        <button @click="showPlayers = !showPlayers" class="font-nunito text-sm border-2 border-black rounded-lg px-3 py-1">
          👥 {{ store.players.length }}
        </button>
        <button v-if="store.gameState === 'LOBBY'" @click="startGame"
                class="bg-wheel-green text-white font-fredoka px-5 py-2 rounded-xl border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
          Start
        </button>
        <button v-else-if="store.gameState === 'ACTIVE'" @click="endGame"
                class="bg-wheel-red text-white font-fredoka px-5 py-2 rounded-xl border-2 border-black shadow-[2px_2px_0px_0px_rgba(0,0,0,1)]">
          End Game
        </button>
      </div>
    </div>

    <!-- Countdown strip (active game only) -->
    <div v-if="store.gameState === 'ACTIVE'" class="bg-white border-b-2 border-black px-6 py-2 flex items-center gap-4">
      <span class="font-fredoka text-lg text-gray-600 shrink-0">Next spin</span>
      <CountdownBar
        :secondsRemaining="store.secondsUntilSpin"
        :totalSeconds="store.config.spinIntervalMinutes * 60"
        class="flex-1"
      />
    </div>

    <!-- Main area -->
    <div class="flex flex-1 overflow-hidden relative">
      <!-- Wheels area -->
      <div class="flex-1 flex items-center justify-around px-8">
        <div class="flex flex-col items-center gap-4">
          <span class="font-fredoka text-4xl tracking-wide">WHO</span>
          <SpinWheel
            :segments="playerSegments"
            :size="wheelSize"
            :spinning="store.isSpinning"
            :target-angle="store.spinAngles.player"
          />
        </div>
        <div class="flex flex-col items-center gap-4">
          <span class="font-fredoka text-4xl tracking-wide">WHAT</span>
          <SpinWheel
            :segments="challengeSegments"
            :size="wheelSize"
            :spinning="store.isSpinning"
            :target-angle="store.spinAngles.challenge"
          />
        </div>
      </div>

      <!-- QR code on lobby -->
      <div v-if="store.gameState === 'LOBBY'" class="absolute bottom-6 right-6">
        <QRCodeDisplay :code="code" :size="120" />
      </div>

      <!-- Player sidebar -->
      <transition name="slide">
        <div v-if="showPlayers"
             class="w-56 bg-white border-l-4 border-black flex flex-col p-4 overflow-y-auto">
          <h3 class="font-fredoka text-xl mb-3">Players</h3>
          <div v-for="player in store.players" :key="player.id"
               class="flex items-center gap-2 mb-2">
            <span class="text-xs">{{ player.connected ? '🟢' : '🟡' }}</span>
            <span class="font-nunito font-semibold flex-1 truncate">{{ player.name }}</span>
          </div>
          <div v-if="!store.players.length" class="font-nunito text-gray-400 text-sm">
            No players yet
          </div>
        </div>
      </transition>
    </div>

    <!-- Rules ticker -->
    <ActiveRules :rules="store.activeRules" class="mx-4 mb-4" />

    <!-- Spin result overlay -->
    <transition name="result-pop">
      <div v-if="store.currentSpin && !store.isSpinning"
           class="absolute inset-0 flex flex-col items-center justify-center pointer-events-none z-30">
        <div class="bg-white border-4 border-black rounded-3xl p-8 shadow-[8px_8px_0px_0px_rgba(0,0,0,1)] text-center max-w-lg pointer-events-auto">
          <div class="font-fredoka text-4xl mb-2">🎯 {{ store.currentSpin.player.name }}!</div>
          <ChallengeCard :challenge="store.currentSpin.challenge" />
          <div v-if="store.voteResult" class="mt-4 font-fredoka text-2xl"
               :class="store.voteResult.completed ? 'text-green-600' : 'text-red-600'">
            {{ store.voteResult.completed ? '✅ Completed!' : `❌ Refused! +${store.voteResult.extraDrinks} sips` }}
          </div>
          <button v-if="store.voteResult" @click="store.currentSpin = null"
                  class="mt-4 font-fredoka px-6 py-2 bg-gray-900 text-white rounded-xl pointer-events-auto">
            Dismiss
          </button>
        </div>
      </div>
    </transition>

    <!-- Spin warning countdown overlay -->
    <transition name="fade">
      <div v-if="store.warningCountdown !== null"
           class="absolute inset-0 bg-black/40 z-20 flex items-center justify-center pointer-events-none">
        <div class="flex flex-col items-center gap-2">
          <div class="font-fredoka text-white drop-shadow-2xl transition-all duration-300"
               :style="{ fontSize: store.warningCountdown === 0 ? '10rem' : '14rem', lineHeight: 1 }">
            {{ store.warningCountdown === 0 ? '🎡' : store.warningCountdown }}
          </div>
          <div class="font-fredoka text-3xl text-white/80 tracking-widest">
            {{ store.warningCountdown === 0 ? 'SPINNING!' : 'GET READY' }}
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGameStore } from '../stores/gameStore'
import { useWebSocket } from '../composables/useWebSocket'
import { useSound } from '../composables/useSound'
import SpinWheel from '../components/SpinWheel.vue'
import CountdownBar from '../components/CountdownBar.vue'
import ActiveRules from '../components/ActiveRules.vue'
import QRCodeDisplay from '../components/QRCodeDisplay.vue'
import ChallengeCard from '../components/ChallengeCard.vue'

const route = useRoute()
const router = useRouter()
const store = useGameStore()
const { connect } = useWebSocket()
const sound = useSound()

const code = route.params.code as string
const muted = ref(false)
const showPlayers = ref(false)

watch(muted, v => { sound.muted.value = v })
watch(() => store.isWarning, v => { if (v) sound.play('drumroll') })
watch(() => store.isSpinning, v => { if (!v && store.currentSpin) { sound.stop('drumroll'); sound.play('airhorn') } })

const CHALLENGE_COLORS = ['#EF4444','#3B82F6','#22C55E','#F97316','#A855F7','#EC4899','#FBBF24','#14B8A6']
const CHALLENGE_LABELS = ['DRINK','SOCIAL','GIVE TAKE','TRUTH','DARE','CHALLENGE','HOT SEAT','MOST LIKELY']
const PLAYER_COLORS = ['#EF4444','#3B82F6','#22C55E','#F97316','#A855F7','#EC4899','#FBBF24','#14B8A6','#F43F5E','#8B5CF6','#10B981','#F59E0B']

const windowWidth = ref(window.innerWidth)
const onResize = () => { windowWidth.value = window.innerWidth }
const wheelSize = computed(() => Math.min(windowWidth.value / 2.2, 520))

const playerSegments = computed(() =>
  store.players.length
    ? store.players.map((p, i) => ({ label: p.name, color: PLAYER_COLORS[i % PLAYER_COLORS.length] }))
    : [{ label: 'Waiting...', color: '#9CA3AF' }]
)

const challengeSegments = computed(() =>
  CHALLENGE_LABELS.map((l, i) => ({ label: l, color: CHALLENGE_COLORS[i] }))
)

let pingInterval: ReturnType<typeof setInterval>

onMounted(async () => {
  sound.preload()
  window.addEventListener('resize', onResize)
  store.roomCode = code
  const res = await fetch(`/api/rooms/${code}`)
  if (!res.ok) { router.push('/'); return }
  const data = await res.json()
  store.applyRoomState(data)
  connect(code, async () => {
    const r = await fetch(`/api/rooms/${code}`)
    if (r.ok) store.applyRoomState(await r.json())
  })

  pingInterval = setInterval(async () => {
    const r = await fetch(`/api/rooms/${code}`)
    if (!r.ok) router.push('/')
  }, 10 * 60 * 1000)
})

onUnmounted(() => {
  clearInterval(pingInterval)
  window.removeEventListener('resize', onResize)
})

async function startGame() {
  await fetch(`/api/rooms/${code}/start`, {
    method: 'POST',
    headers: { 'X-Host-Token': store.hostToken }
  })
}

async function endGame() {
  if (!confirm('End the game and show stats?')) return
  await fetch(`/api/rooms/${code}/end`, {
    method: 'POST',
    headers: { 'X-Host-Token': store.hostToken }
  })
  router.push(`/stats/${code}`)
}
</script>

<style scoped>
.slide-enter-active, .slide-leave-active { transition: transform 0.2s; }
.slide-enter-from, .slide-leave-to { transform: translateX(100%); }

.result-pop-enter-active { animation: pop-in 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.result-pop-leave-active { animation: pop-out 0.2s ease-in; }
@keyframes pop-in { from { transform: scale(0.5); opacity: 0; } to { transform: scale(1); opacity: 1; } }
@keyframes pop-out { from { transform: scale(1); opacity: 1; } to { transform: scale(0.5); opacity: 0; } }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
