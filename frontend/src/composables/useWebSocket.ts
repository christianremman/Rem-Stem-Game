import { ref, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useGameStore } from '../stores/gameStore'

export function useWebSocket() {
  const store = useGameStore()
  const connected = ref(false)
  let client: Client | null = null

  function connect(roomCode: string) {
    client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 3000,
      onConnect: () => {
        connected.value = true
        client!.subscribe(`/topic/rooms/${roomCode}`, msg => {
          const event = JSON.parse(msg.body)
          store.handleEvent(event)
        })
        client!.subscribe('/user/queue/private', msg => {
          const event = JSON.parse(msg.body)
          store.handleEvent(event)
        })
        if (store.playerId) {
          client!.publish({
            destination: `/app/rooms/${roomCode}/register`,
            body: JSON.stringify({ playerId: store.playerId, sessionId: client!.clientId })
          })
        }
      },
      onDisconnect: () => {
        connected.value = false
      }
    })
    client.activate()
  }

  function send(destination: string, body: object) {
    client?.publish({ destination, body: JSON.stringify(body) })
  }

  function disconnect() {
    client?.deactivate()
    connected.value = false
  }

  onUnmounted(disconnect)

  return { connected, connect, send, disconnect }
}
