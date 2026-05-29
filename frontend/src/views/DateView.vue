<template>
  <div class="date-root min-h-screen flex flex-col items-center select-none overflow-hidden">

    <!-- Top bar -->
    <div class="date-topbar w-full flex items-center justify-between px-5 py-3">
      <span class="font-fredoka text-xl date-accent">🌹 Date Night</span>
      <span class="font-fredoka text-lg tracking-widest date-muted">{{ code }}</span>
      <button @click="endGame" class="font-nunito text-sm date-muted hover:date-accent transition-colors px-3 py-1 rounded-lg border border-current">
        End
      </button>
    </div>

    <!-- Turn indicator -->
    <div class="mt-6 mb-2 min-h-[2rem] flex items-center justify-center">
      <span v-if="turnName" class="font-fredoka text-2xl date-turn">{{ turnName }}'s turn</span>
    </div>

    <!-- Countdown strip (active only) -->
    <div v-if="store.gameState === 'ACTIVE'" class="w-full max-w-sm px-4 mb-4">
      <CountdownBar
        :secondsRemaining="store.secondsUntilSpin"
        :totalSeconds="store.config.spinIntervalMinutes * 60"
      />
    </div>

    <!-- Wheel -->
    <div class="flex flex-col items-center my-2">
      <SpinWheel
        :segments="romanticSegments"
        :size="wheelSize"
        :spinning="store.isSpinning"
        :target-angle="store.spinAngles.challenge"
      />
    </div>

    <!-- Challenge card -->
    <transition name="card-pop">
      <div v-if="store.currentSpin && !store.isSpinning" class="w-full max-w-sm px-4 mt-6">
        <div class="date-card rounded-2xl p-5 text-center">
          <div class="font-fredoka text-4xl mb-1">{{ challengeEmoji }}</div>
          <div class="font-fredoka text-2xl mb-3 date-card-type">
            {{ store.currentSpin.challenge.type.replace(/_/g, ' ') }}
          </div>
          <p class="font-nunito font-semibold text-lg leading-snug date-card-text">
            {{ store.currentSpin.challenge.text }}
          </p>
        </div>
      </div>
    </transition>

    <!-- Start button (lobby) -->
    <div v-if="store.gameState === 'LOBBY'" class="mt-8 flex flex-col items-center gap-3">
      <p class="font-nunito date-muted text-sm">Share the room code so both players can join, then start.</p>
      <button @click="startGame"
              class="font-fredoka text-xl px-8 py-3 rounded-xl border-2 border-current date-start-btn transition-all hover:scale-105">
        Start Date Night
      </button>
    </div>

    <!-- Spin warning overlay -->
    <transition name="fade">
      <div v-if="store.warningCountdown !== null"
           class="absolute inset-0 flex items-center justify-center pointer-events-none z-20"
           style="background: rgba(26,10,10,0.6)">
        <div class="flex flex-col items-center gap-2">
          <div class="font-fredoka drop-shadow-2xl transition-all duration-300 date-warning-num"
               :style="{ fontSize: store.warningCountdown === 0 ? '8rem' : '12rem', lineHeight: 1 }">
            {{ store.warningCountdown === 0 ? '🌹' : store.warningCountdown }}
          </div>
          <div class="font-fredoka text-2xl date-warning-label tracking-widest">
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
import SpinWheel from '../components/SpinWheel.vue'
import CountdownBar from '../components/CountdownBar.vue'

const ROMANTIC_COLORS = ['#DC143C', '#FFB6C1', '#C4977A', '#FFF0EC']
const ROMANTIC_LABELS = ['QUESTION', 'TOUCH', 'DARE', 'DRINK']

const CHALLENGE_EMOJI: Record<string, string> = {
  PERSONAL_QUESTION: '💬',
  PHYSICAL_TOUCH: '💋',
  ROMANTIC_DARE: '🌹',
  DRINK: '🍺'
}

const romanticSegments = ROMANTIC_LABELS.map((label, i) => ({ label, color: ROMANTIC_COLORS[i] }))

const route = useRoute()
const router = useRouter()
const store = useGameStore()
const { connect } = useWebSocket()

const code = route.params.code as string

const windowWidth = ref(window.innerWidth)
const onResize = () => { windowWidth.value = window.innerWidth }
const wheelSize = computed(() => Math.min(windowWidth.value * 0.85, 380))

const turnName = ref('')

const challengeEmoji = computed(() =>
  store.currentSpin ? (CHALLENGE_EMOJI[store.currentSpin.challenge.type] ?? '🎡') : ''
)

watch(() => store.currentSpin, (spin) => {
  if (!spin) return
  const players = store.players
  if (players.length < 2) return
  const justSpun = spin.player.id
  const next = players.find(p => p.id !== justSpun)
  if (next) turnName.value = next.name
})

watch(() => store.players, (players) => {
  if (players.length > 0 && !turnName.value) {
    turnName.value = players[0].name
  }
}, { immediate: true })

let pingInterval: ReturnType<typeof setInterval>

onMounted(async () => {
  window.addEventListener('resize', onResize)
  store.roomCode = code
  const res = await fetch(`/api/rooms/${code}`)
  if (!res.ok) { router.push('/'); return }
  store.applyRoomState(await res.json())

  if (store.players.length > 0 && !turnName.value) {
    turnName.value = store.players[0].name
  }

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

watch(() => store.gameState, (state) => {
  if (state === 'ENDED') router.push(`/stats/${code}`)
})

async function startGame() {
  await fetch(`/api/rooms/${code}/start`, {
    method: 'POST',
    headers: { 'X-Host-Token': store.hostToken }
  })
}

async function endGame() {
  if (!confirm('End the date night and show stats?')) return
  await fetch(`/api/rooms/${code}/end`, {
    method: 'POST',
    headers: { 'X-Host-Token': store.hostToken }
  })
  router.push(`/stats/${code}`)
}
</script>

<style scoped>
.date-root {
  background: #1A0A0A;
  color: #FFE4E1;
  position: relative;
}

.date-topbar {
  background: rgba(45, 12, 30, 0.8);
  border-bottom: 1px solid #3d1a2a;
}

.date-accent { color: #FF85A1; }
.date-muted  { color: #b07080; }
.date-turn   { color: #FF85A1; }

.date-card {
  background: #2D0C1E;
  border: 2px solid #5c1e3a;
}
.date-card-type { color: #FF85A1; }
.date-card-text { color: #FFE4E1; }

.date-start-btn { color: #FF85A1; border-color: #FF85A1; }

.date-warning-num   { color: #FFE4E1; }
.date-warning-label { color: rgba(255,228,225,0.75); }

.card-pop-enter-active { animation: pop-in 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.card-pop-leave-active { animation: pop-out 0.2s ease-in; }
@keyframes pop-in  { from { transform: scale(0.5); opacity: 0; } to { transform: scale(1); opacity: 1; } }
@keyframes pop-out { from { transform: scale(1); opacity: 1; } to { transform: scale(0.5); opacity: 0; } }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from,   .fade-leave-to     { opacity: 0; }
</style>
