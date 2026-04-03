import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { TelemetryMessage } from '@/types'

type TelemetryCallback = (data: TelemetryMessage) => void

type RawTelemetryMessage = Partial<TelemetryMessage> & {
  device_id?: string
  metric_type?: string
}

let stompClient: Client | null = null

function normalizeTelemetryMessage(raw: RawTelemetryMessage): TelemetryMessage | null {
  const deviceId = raw.deviceId ?? raw.device_id
  const metricType = raw.metricType ?? raw.metric_type
  const value = typeof raw.value === 'number' ? raw.value : Number(raw.value)
  const timestamp = raw.timestamp

  if (!deviceId || !metricType || Number.isNaN(value) || !timestamp) {
    return null
  }

  return {
    deviceId,
    metricType: metricType as TelemetryMessage['metricType'],
    value,
    timestamp,
  }
}

export function connectToFarm(
  farmId: string,
  onMessage: TelemetryCallback,
  onConnect?: () => void,
  onError?: (error: string) => void,
  onClose?: () => void
): void {
  if (stompClient?.active) {
    stompClient.deactivate()
  }

  const wsUrl = '/ws'
  const token = localStorage.getItem('access_token')

  stompClient = new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      console.log('[WebSocket] Connected to farm:', farmId)
      onConnect?.()

      stompClient?.subscribe(`/topic/farm/${farmId}/telemetry`, (message) => {
        try {
          const parsed = JSON.parse(message.body) as RawTelemetryMessage
          const data = normalizeTelemetryMessage(parsed)
          if (!data) {
            console.warn('[WebSocket] Invalid telemetry payload:', parsed)
            return
          }
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
      onClose?.()
    },
    onWebSocketError: (event) => {
      console.error('[WebSocket] Native socket error:', event)
      onError?.('WebSocket transport error')
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
