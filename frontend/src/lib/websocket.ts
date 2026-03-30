import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { TelemetryMessage } from '@/types'

type TelemetryCallback = (data: TelemetryMessage) => void

let stompClient: Client | null = null

export function connectToFarm(
  farmId: string,
  onMessage: TelemetryCallback,
  onConnect?: () => void,
  onError?: (error: string) => void
): void {
  if (stompClient?.active) {
    stompClient.deactivate()
  }

  const wsUrl = '/ws'

  stompClient = new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      console.log('[WebSocket] Connected to farm:', farmId)
      onConnect?.()

      stompClient?.subscribe(`/topic/farm/${farmId}/telemetry`, (message) => {
        try {
          const data: TelemetryMessage = JSON.parse(message.body)
          onMessage(data)
        } catch (err) {
          console.error('[WebSocket] Failed to parse message:', err)
        }
      })
    },
    onStompError: (frame) => {
      console.error('[WebSocket] STOMP error:', frame.headers.message)
      onError?.(frame.headers.message || 'WebSocket connection error')
    },
    onWebSocketClose: () => {
      console.log('[WebSocket] Connection closed')
    },
  })

  stompClient.activate()
}

export function disconnectWebSocket(): void {
  if (stompClient?.active) {
    stompClient.deactivate()
    console.log('[WebSocket] Disconnected')
  }
  stompClient = null
}
