import { ref } from 'vue'

export function useSound() {
  const muted = ref(false)
  const sounds: Record<string, HTMLAudioElement> = {}

  function preload() {
    ;['drumroll', 'airhorn', 'tick'].forEach(name => {
      const audio = new Audio(`/sounds/${name}.mp3`)
      audio.preload = 'auto'
      sounds[name] = audio
    })
  }

  function play(name: string) {
    if (muted.value) return
    const audio = sounds[name]
    if (!audio) return
    audio.currentTime = 0
    audio.play().catch(() => {})
  }

  function stop(name: string) {
    sounds[name]?.pause()
  }

  return { muted, preload, play, stop }
}
