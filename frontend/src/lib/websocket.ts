import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { TelemetryMessage } from '@/types'

type TelemetryCallback = (data: TelemetryMessage) => void

export type DeviceStatusEvent = {
  deviceId: string
  connectionStatus: 'ONLINE' | 'OFFLINE'
}

type DeviceStatusCallback = (events: DeviceStatusEvent[]) => void
type AdminStatsChangedCallback = () => void
type NotificationUnreadCallback = (unreadCount: number) => void

type RawTelemetryMessage = Partial<TelemetryMessage> & {
  device_id?: string
  metric_type?: string
}

let stompClient: Client | null = null
let adminStompClient: Client | null = null
let notificationStompClient: Client | null = null

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
  onClose?: () => void,
  onDeviceStatus?: DeviceStatusCallback
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

      // Telemetry realtime data
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

      // Device online/offline status events (push từ backend, không cần polling)
      if (onDeviceStatus) {
        stompClient?.subscribe(`/topic/farm/${farmId}/device-status`, (message) => {
          try {
            const events = JSON.parse(message.body) as DeviceStatusEvent[]
            if (Array.isArray(events)) {
              onDeviceStatus(events)
            }
          } catch (err) {
            console.error('[WebSocket] Failed to parse device-status:', err)
          }
        })
      }
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

/** Kết nối WebSocket cho Admin Dashboard để nhận push event thay vì polling 30s */
export function connectAdminStats(
  onStatsChanged: AdminStatsChangedCallback,
  onConnect?: () => void
): void {
  if (adminStompClient?.active) {
    adminStompClient.deactivate()
  }

  const token = localStorage.getItem('access_token')

  adminStompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 10000,
    heartbeatIncoming: 30000,
    heartbeatOutgoing: 30000,
    onConnect: () => {
      console.log('[WebSocket] Admin stats connected')
      onConnect?.()
      adminStompClient?.subscribe('/topic/admin/stats-changed', () => {
        onStatsChanged()
      })
    },
    onStompError: (frame) => {
      console.warn('[WebSocket] Admin STOMP error:', frame.headers.message)
    },
  })

  adminStompClient.activate()
}

export function disconnectAdminStats(): void {
  if (adminStompClient?.active) {
    adminStompClient.deactivate()
    console.log('[WebSocket] Admin stats disconnected')
  }
  adminStompClient = null
}

/** Kết nối WebSocket để nhận unread notification count theo user, thay cho polling 30s. */
export function connectNotificationUnread(onUnreadChanged: NotificationUnreadCallback): void {
  if (notificationStompClient?.active) {
    notificationStompClient.deactivate()
  }

  const token = localStorage.getItem('access_token')

  notificationStompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 10000,
    heartbeatIncoming: 30000,
    heartbeatOutgoing: 30000,
    onConnect: () => {
      console.log('[WebSocket] Notification unread connected')
      notificationStompClient?.subscribe('/user/queue/notifications-unread', (message) => {
        try {
          const payload = JSON.parse(message.body) as { unread_count?: number; unreadCount?: number }
          const unread = payload.unread_count ?? payload.unreadCount
          if (typeof unread === 'number') {
            onUnreadChanged(unread)
          }
        } catch (err) {
          console.error('[WebSocket] Failed to parse notifications-unread:', err)
        }
      })
    },
    onStompError: (frame) => {
      console.warn('[WebSocket] Notification STOMP error:', frame.headers.message)
    },
  })

  notificationStompClient.activate()
}

export function disconnectNotificationUnread(): void {
  if (notificationStompClient?.active) {
    notificationStompClient.deactivate()
    console.log('[WebSocket] Notification unread disconnected')
  }
  notificationStompClient = null
}

export function disconnectWebSocket(): void {
  if (stompClient?.active) {
    stompClient.deactivate()
    console.log('[WebSocket] Disconnected')
  }
  stompClient = null
}
