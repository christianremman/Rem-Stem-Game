import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface PlayerData {
  id: string
  name: string
  connected: boolean
  powerUps: string[]
}

export interface ChallengeData {
  type: string
  text: string
  sips?: number
  subtype?: string
  wheelIndex: number
}

export interface SpinResultData {
  player: PlayerData
  challenge: ChallengeData
  playerWheelAngle: number
  challengeWheelAngle: number
  spinNumber: number
}

export interface GameConfigData {
  spinIntervalMinutes: number
  intensity: 'CHILL' | 'NORMAL' | 'SAVAGE'
}

export const useGameStore = defineStore('game', () => {
  const roomCode = ref<string>('')
  const hostToken = ref<string>('')
  const playerId = ref<string>('')
  const playerName = ref<string>('')
  const isHost = ref(false)

  const players = ref<PlayerData[]>([])
  const activeRules = ref<string[]>([])
  const config = ref<GameConfigData>({ spinIntervalMinutes: 15, intensity: 'NORMAL' })
  const gameState = ref<'LOBBY' | 'ACTIVE' | 'PAUSED' | 'ENDED'>('LOBBY')
  const spinCount = ref(0)

  const currentSpin = ref<SpinResultData | null>(null)
  const spinAngles = ref<{ player: number; challenge: number }>({ player: 0, challenge: 0 })
  const secondsUntilSpin = ref<number | null>(null)
  const isSpinning = ref(false)
  const isWarning = ref(false)
  const warningCountdown = ref<number | null>(null)
  const voteWindowOpen = ref(false)
  const voteResult = ref<{ completed: boolean; extraDrinks: number } | null>(null)

  const stats = ref<{ players: any[]; totalSpins: number } | null>(null)

  const myPlayer = computed(() => players.value.find(p => p.id === playerId.value) ?? null)

  function applyRoomState(data: any) {
    gameState.value = data.state
    config.value = data.config
    players.value = data.players
    activeRules.value = data.activeRules ?? []
    spinCount.value = data.spinCount ?? 0
  }

  let countdownTimer: ReturnType<typeof setInterval> | null = null
  let warningTimer: ReturnType<typeof setInterval> | null = null

  function startWarningCountdown(seconds: number) {
    if (warningTimer !== null) { clearInterval(warningTimer); warningTimer = null }
    warningCountdown.value = seconds
    warningTimer = setInterval(() => {
      if (warningCountdown.value !== null && warningCountdown.value > 0) {
        warningCountdown.value--
      } else {
        clearInterval(warningTimer!)
        warningTimer = null
      }
    }, 1000)
  }

  function stopWarningCountdown() {
    if (warningTimer !== null) { clearInterval(warningTimer); warningTimer = null }
    warningCountdown.value = null
  }

  function startCountdown(seconds: number) {
    stopCountdown()
    secondsUntilSpin.value = seconds
    countdownTimer = setInterval(() => {
      if (secondsUntilSpin.value !== null && secondsUntilSpin.value > 0) {
        secondsUntilSpin.value--
      } else {
        stopCountdown()
      }
    }, 1000)
  }

  function stopCountdown() {
    if (countdownTimer !== null) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }

  function handleEvent(event: { type: string; payload: any }) {
    const p = event.payload
    switch (event.type) {
      case 'PLAYER_JOINED':
        if (!players.value.find(x => x.id === p.player.id)) {
          players.value.push(p.player)
        }
        break
      case 'PLAYER_DISCONNECTED':
        players.value = players.value.map(pl =>
          pl.id === p.playerId ? { ...pl, connected: false } : pl
        )
        break
      case 'GAME_STARTED':
        gameState.value = 'ACTIVE'
        config.value = p.config
        startCountdown(5)
        break
      case 'COUNTDOWN':
        secondsUntilSpin.value = p.secondsUntilSpin
        break
      case 'SPIN_WARNING':
        isWarning.value = true
        startWarningCountdown(p?.secondsUntilSpin ?? 5)
        break
      case 'SPIN_STARTING':
        isSpinning.value = true
        isWarning.value = false
        stopWarningCountdown()
        voteWindowOpen.value = false
        voteResult.value = null
        stopCountdown()
        if (p) {
          spinAngles.value = { player: p.playerWheelAngle ?? 0, challenge: p.challengeWheelAngle ?? 0 }
        }
        break
      case 'SPIN_RESULT':
        currentSpin.value = p
        isSpinning.value = false
        startCountdown(config.value.spinIntervalMinutes * 60)
        break
      case 'VOTE_WINDOW_OPEN':
        voteWindowOpen.value = true
        break
      case 'VOTE_RESULT':
        voteWindowOpen.value = false
        voteResult.value = p
        break
      case 'POWERUP_USED':
        players.value = players.value.map(pl =>
          pl.id === p.playerId
            ? { ...pl, powerUps: pl.powerUps.filter(pu => pu !== p.type) }
            : pl
        )
        break
      case 'RULE_ADDED':
        activeRules.value.push(p.rule)
        break
      case 'GAME_ENDED':
        gameState.value = 'ENDED'
        stats.value = p
        stopCountdown()
        break
    }
  }

  return {
    roomCode, hostToken, playerId, playerName, isHost,
    players, activeRules, config, gameState, spinCount,
    currentSpin, spinAngles, secondsUntilSpin, isSpinning, isWarning, warningCountdown,
    voteWindowOpen, voteResult, stats, myPlayer,
    applyRoomState, handleEvent
  }
})
