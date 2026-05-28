<template>
  <div class="min-h-screen bg-cream flex flex-col items-center justify-center p-4">
    <h1 class="text-6xl font-fredoka text-gray-900 mb-2 drop-shadow-md">🎡 Wheel Unfortunate</h1>
    <p class="font-nunito text-gray-600 mb-10 text-lg">A party drinking game. Spin responsibly.</p>

    <div class="flex gap-6 w-full max-w-xl">
      <!-- Create -->
      <div class="flex-1 bg-white border-4 border-black rounded-2xl p-6 shadow-[6px_6px_0px_0px_rgba(0,0,0,1)]">
        <h2 class="font-fredoka text-2xl mb-4">Host a Game</h2>
        <div class="mb-3">
          <label class="block font-nunito font-bold text-sm mb-1">Spin interval</label>
          <select v-model="createConfig.spinIntervalMinutes"
                  class="w-full border-2 border-black rounded-lg p-2 font-nunito">
            <option :value="15">15 minutes</option>
            <option :value="30">30 minutes</option>
          </select>
        </div>
        <div class="mb-4">
          <label class="block font-nunito font-bold text-sm mb-1">Intensity</label>
          <select v-model="createConfig.intensity"
                  class="w-full border-2 border-black rounded-lg p-2 font-nunito">
            <option value="CHILL">😌 Chill</option>
            <option value="NORMAL">🎲 Normal</option>
            <option value="SAVAGE">🔥 Savage</option>
          </select>
        </div>
        <button @click="createRoom"
                class="w-full bg-wheel-green text-white font-fredoka text-xl py-3 rounded-xl border-2 border-black shadow-[3px_3px_0px_0px_rgba(0,0,0,1)] hover:translate-y-0.5 hover:shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] transition-all">
          Create Room
        </button>
      </div>

      <!-- Join -->
      <div class="flex-1 bg-white border-4 border-black rounded-2xl p-6 shadow-[6px_6px_0px_0px_rgba(0,0,0,1)]">
        <h2 class="font-fredoka text-2xl mb-4">Join a Game</h2>
        <input v-model="joinCode" placeholder="Room code"
               class="w-full border-2 border-black rounded-lg p-2 font-nunito mb-3 uppercase tracking-widest text-center text-xl"
               maxlength="6" />
        <input v-model="joinName" placeholder="Your name"
               class="w-full border-2 border-black rounded-lg p-2 font-nunito mb-4"
               maxlength="20" />
        <button @click="joinRoom" :disabled="!joinCode || !joinName"
                class="w-full bg-wheel-blue text-white font-fredoka text-xl py-3 rounded-xl border-2 border-black shadow-[3px_3px_0px_0px_rgba(0,0,0,1)] hover:translate-y-0.5 hover:shadow-[2px_2px_0px_0px_rgba(0,0,0,1)] transition-all disabled:opacity-40 disabled:cursor-not-allowed">
          Join Game
        </button>
      </div>
    </div>

    <p v-if="error" class="mt-4 text-red-600 font-nunito font-bold">{{ error }}</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '../stores/gameStore'

const router = useRouter()
const store = useGameStore()

const createConfig = ref({ spinIntervalMinutes: 15, intensity: 'NORMAL' })
const joinCode = ref('')
const joinName = ref('')
const error = ref('')

async function createRoom() {
  error.value = ''
  const res = await fetch('/api/rooms', { method: 'POST' })
  if (!res.ok) { error.value = 'Failed to create room.'; return }
  const data = await res.json()
  store.roomCode = data.roomCode
  store.hostToken = data.hostToken
  store.isHost = true

  await fetch(`/api/rooms/${data.roomCode}/config`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(createConfig.value)
  })

  router.push(`/host/${data.roomCode}`)
}

async function joinRoom() {
  error.value = ''
  const code = joinCode.value.toUpperCase().trim()
  const res = await fetch(`/api/rooms/${code}/players`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: joinName.value.trim() })
  })
  if (!res.ok) { error.value = 'Room not found.'; return }
  const data = await res.json()
  store.roomCode = code
  store.playerId = data.playerId
  store.playerName = data.playerName
  store.isHost = false
  router.push(`/play/${code}`)
}
</script>
