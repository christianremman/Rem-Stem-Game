import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useGameStore } from './gameStore'

describe('gameStore countdown', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('GAME_STARTED initializes secondsUntilSpin from initial spin delay', () => {
    const store = useGameStore()
    store.handleEvent({
      type: 'GAME_STARTED',
      payload: { config: { spinIntervalMinutes: 15, intensity: 'NORMAL' } }
    })
    expect(store.secondsUntilSpin).toBe(5)
  })

  it('secondsUntilSpin counts down every second after GAME_STARTED', () => {
    const store = useGameStore()
    store.handleEvent({
      type: 'GAME_STARTED',
      payload: { config: { spinIntervalMinutes: 1, intensity: 'NORMAL' } }
    })
    vi.advanceTimersByTime(5000)
    expect(store.secondsUntilSpin).toBe(0)
  })

  it('COUNTDOWN syncs secondsUntilSpin from server', () => {
    const store = useGameStore()
    store.handleEvent({
      type: 'GAME_STARTED',
      payload: { config: { spinIntervalMinutes: 15, intensity: 'NORMAL' } }
    })
    vi.advanceTimersByTime(3000)
    store.handleEvent({ type: 'COUNTDOWN', payload: { secondsUntilSpin: 887 } })
    expect(store.secondsUntilSpin).toBe(887)
  })

  it('SPIN_STARTING stops countdown', () => {
    const store = useGameStore()
    store.handleEvent({
      type: 'GAME_STARTED',
      payload: { config: { spinIntervalMinutes: 1, intensity: 'NORMAL' } }
    })
    store.handleEvent({ type: 'SPIN_STARTING', payload: { playerWheelAngle: 90, challengeWheelAngle: 45 } })
    const valueAtStop = store.secondsUntilSpin
    vi.advanceTimersByTime(5000)
    expect(store.secondsUntilSpin).toBe(valueAtStop)
  })

  it('SPIN_RESULT restarts countdown from full interval', () => {
    const store = useGameStore()
    store.handleEvent({
      type: 'GAME_STARTED',
      payload: { config: { spinIntervalMinutes: 1, intensity: 'NORMAL' } }
    })
    store.handleEvent({ type: 'SPIN_STARTING', payload: { playerWheelAngle: 90, challengeWheelAngle: 45 } })
    store.handleEvent({
      type: 'SPIN_RESULT',
      payload: {
        player: { id: 'p1', name: 'Alice', connected: true, powerUps: [] },
        challenge: { type: 'DRINK', text: 'Drink 2 sips.', sips: 2, wheelIndex: 0 },
        playerWheelAngle: 90,
        challengeWheelAngle: 45,
        spinNumber: 1
      }
    })
    expect(store.secondsUntilSpin).toBe(60)
    vi.advanceTimersByTime(3000)
    expect(store.secondsUntilSpin).toBe(57)
  })

  it('SPIN_RESULT supports 30-second testing interval', () => {
    const store = useGameStore()
    store.handleEvent({
      type: 'GAME_STARTED',
      payload: { config: { spinIntervalMinutes: 0.5, intensity: 'NORMAL' } }
    })
    store.handleEvent({ type: 'SPIN_STARTING', payload: { playerWheelAngle: 90, challengeWheelAngle: 45 } })
    store.handleEvent({
      type: 'SPIN_RESULT',
      payload: {
        player: { id: 'p1', name: 'Alice', connected: true, powerUps: [] },
        challenge: { type: 'DRINK', text: 'Drink 2 sips.', sips: 2, wheelIndex: 0 },
        playerWheelAngle: 90,
        challengeWheelAngle: 45,
        spinNumber: 1
      }
    })
    expect(store.secondsUntilSpin).toBe(30)
  })

  it('GAME_ENDED stops countdown', () => {
    const store = useGameStore()
    store.handleEvent({
      type: 'GAME_STARTED',
      payload: { config: { spinIntervalMinutes: 1, intensity: 'NORMAL' } }
    })
    store.handleEvent({ type: 'GAME_ENDED', payload: { players: [], totalSpins: 0 } })
    const valueAtEnd = store.secondsUntilSpin
    vi.advanceTimersByTime(5000)
    expect(store.secondsUntilSpin).toBe(valueAtEnd)
  })
})

describe('gameStore warning countdown', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('SPIN_WARNING sets warningCountdown from payload', () => {
    const store = useGameStore()
    store.handleEvent({ type: 'SPIN_WARNING', payload: { secondsUntilSpin: 5 } })
    expect(store.warningCountdown).toBe(5)
  })

  it('warningCountdown counts down every second', () => {
    const store = useGameStore()
    store.handleEvent({ type: 'SPIN_WARNING', payload: { secondsUntilSpin: 5 } })
    vi.advanceTimersByTime(3000)
    expect(store.warningCountdown).toBe(2)
  })

  it('warningCountdown stops at 0', () => {
    const store = useGameStore()
    store.handleEvent({ type: 'SPIN_WARNING', payload: { secondsUntilSpin: 3 } })
    vi.advanceTimersByTime(10000)
    expect(store.warningCountdown).toBe(0)
  })

  it('SPIN_STARTING clears warningCountdown to null', () => {
    const store = useGameStore()
    store.handleEvent({ type: 'SPIN_WARNING', payload: { secondsUntilSpin: 5 } })
    store.handleEvent({ type: 'SPIN_STARTING', payload: { playerWheelAngle: 90, challengeWheelAngle: 45 } })
    expect(store.warningCountdown).toBeNull()
  })

  it('timer stops after SPIN_STARTING', () => {
    const store = useGameStore()
    store.handleEvent({ type: 'SPIN_WARNING', payload: { secondsUntilSpin: 5 } })
    store.handleEvent({ type: 'SPIN_STARTING', payload: { playerWheelAngle: 90, challengeWheelAngle: 45 } })
    vi.advanceTimersByTime(5000)
    expect(store.warningCountdown).toBeNull()
  })

  it('defaults to 5 when payload missing secondsUntilSpin', () => {
    const store = useGameStore()
    store.handleEvent({ type: 'SPIN_WARNING', payload: {} })
    expect(store.warningCountdown).toBe(5)
  })
})

describe('gameStore PLAYER_JOINED', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('adds player to list on PLAYER_JOINED', () => {
    const store = useGameStore()
    store.handleEvent({
      type: 'PLAYER_JOINED',
      payload: { player: { id: 'p1', name: 'Alice', connected: true, powerUps: [] } }
    })
    expect(store.players).toHaveLength(1)
    expect(store.players[0].name).toBe('Alice')
  })

  it('does not add duplicate player on repeated PLAYER_JOINED', () => {
    const store = useGameStore()
    const event = {
      type: 'PLAYER_JOINED',
      payload: { player: { id: 'p1', name: 'Alice', connected: true, powerUps: [] } }
    }
    store.handleEvent(event)
    store.handleEvent(event)
    expect(store.players).toHaveLength(1)
  })
})
