import { create } from 'zustand'
import api from '@/lib/axios'
import type { NotificationResponse } from '@/types'

interface NotificationState {
  notifications: NotificationResponse[]
  unreadCount: number
  isOpen: boolean

  fetchNotifications: () => Promise<void>
  markAsRead: (id: string) => Promise<void>
  togglePanel: () => void
  closePanel: () => void
}

export const useNotificationStore = create<NotificationState>((set, get) => ({
  notifications: [],
  unreadCount: 0,
  isOpen: false,

  fetchNotifications: async () => {
    try {
      const response = await api.get<NotificationResponse[]>('/notifications', {
        params: { page: 1, size: 20 },
      })
      const notifications = response.data
      const unreadCount = notifications.filter((n) => !n.is_read).length
      set({ notifications, unreadCount })
    } catch (error) {
      console.error('Failed to fetch notifications:', error)
    }
  },

  markAsRead: async (id: string) => {
    try {
      await api.put(`/notifications/${id}/read`)
      set((state) => ({
        notifications: state.notifications.map((n) =>
          n.id === id ? { ...n, is_read: true } : n
        ),
        unreadCount: Math.max(0, state.unreadCount - 1),
      }))
    } catch (error) {
      console.error('Failed to mark notification as read:', error)
    }
  },

  togglePanel: () => set((state) => ({ isOpen: !state.isOpen })),
  closePanel: () => set({ isOpen: false }),
}))
