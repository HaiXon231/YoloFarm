import { create } from 'zustand'
import api from '@/lib/axios'
import type { NotificationResponse } from '@/types'

type UnreadCountResponse = {
  unread_count: number
}

interface NotificationState {
  notifications: NotificationResponse[]
  unreadCount: number
  isOpen: boolean
  currentPage: number
  pageSize: number
  hasMore: boolean
  isLoading: boolean
  isLoadingMore: boolean
  isMarkingAll: boolean

  fetchNotifications: () => Promise<void>
  fetchUnreadCount: () => Promise<void>
  setUnreadCount: (count: number) => void
  loadMoreNotifications: () => Promise<void>
  markAsRead: (id: string) => Promise<void>
  markAllAsRead: () => Promise<void>
  togglePanel: () => void
  closePanel: () => void
}

export const useNotificationStore = create<NotificationState>((set, get) => ({
  notifications: [],
  unreadCount: 0,
  isOpen: false,
  currentPage: 1,
  pageSize: 10,
  hasMore: false,
  isLoading: false,
  isLoadingMore: false,
  isMarkingAll: false,

  fetchUnreadCount: async () => {
    try {
      const response = await api.get<UnreadCountResponse>('/notifications/unread-count')
      set({ unreadCount: response.data.unread_count })
    } catch (error) {
      console.error('Failed to fetch unread notification count:', error)
    }
  },

  setUnreadCount: (count: number) => {
    set({ unreadCount: Math.max(0, count) })
  },

  fetchNotifications: async () => {
    try {
      const { pageSize, fetchUnreadCount } = get()
      set({ isLoading: true })

      await fetchUnreadCount()

      const response = await api.get<NotificationResponse[]>('/notifications', {
        params: { page: 1, size: pageSize },
      })
      const notifications = response.data
      set({
        notifications,
        currentPage: 1,
        hasMore: notifications.length === pageSize,
      })
    } catch (error) {
      console.error('Failed to fetch notifications:', error)
    } finally {
      set({ isLoading: false })
    }
  },

  loadMoreNotifications: async () => {
    const { currentPage, pageSize, hasMore, isLoadingMore } = get()
    if (!hasMore || isLoadingMore) return

    try {
      set({ isLoadingMore: true })
      const nextPage = currentPage + 1
      const response = await api.get<NotificationResponse[]>('/notifications', {
        params: { page: nextPage, size: pageSize },
      })

      const olderNotifications = response.data
      set((state) => {
        const notifications = [...state.notifications, ...olderNotifications]

        return {
          notifications,
          currentPage: nextPage,
          hasMore: olderNotifications.length === pageSize,
        }
      })
    } catch (error) {
      console.error('Failed to load more notifications:', error)
    } finally {
      set({ isLoadingMore: false })
    }
  },

  markAsRead: async (id: string) => {
    try {
      const current = get().notifications.find((n) => n.id === id)
      await api.put(`/notifications/${id}/read`)
      set((state) => ({
        notifications: state.notifications.map((n) =>
          n.id === id ? { ...n, is_read: true } : n
        ),
        unreadCount: current && !current.is_read ? Math.max(0, state.unreadCount - 1) : state.unreadCount,
      }))

      await get().fetchUnreadCount()
    } catch (error) {
      console.error('Failed to mark notification as read:', error)
    }
  },

  markAllAsRead: async () => {
    const { isMarkingAll } = get()
    if (isMarkingAll) return

    try {
      set({ isMarkingAll: true })

      // Sweep all pages so hidden notifications are also marked as read.
      const unreadIds = new Set<string>()
      const fallbackPageSize = 100
      let page = 1

      while (true) {
        const response = await api.get<NotificationResponse[]>('/notifications', {
          params: { page, size: fallbackPageSize },
        })

        const batch = response.data
        for (const notification of batch) {
          if (!notification.is_read) {
            unreadIds.add(notification.id)
          }
        }

        if (batch.length < fallbackPageSize) break
        page += 1

        if (page > 200) break
      }

      // Use bulk endpoint first when available.
      await api.put('/notifications/read-all')

      // Ensure all unread notifications are marked even if bulk endpoint misses anything.
      await Promise.allSettled([...unreadIds].map((id) => api.put(`/notifications/${id}/read`)))

      set((state) => ({
        notifications: state.notifications.map((n) => ({ ...n, is_read: true })),
        unreadCount: 0,
      }))

      // Refresh first page to keep dropdown state in sync with backend.
      await get().fetchNotifications()
    } catch (error) {
      console.error('Failed to mark all notifications as read:', error)

      // Keep local UI consistent even when backend request fails.
      set((state) => ({
        notifications: state.notifications.map((n) => ({ ...n, is_read: true })),
        unreadCount: 0,
      }))
    } finally {
      await get().fetchUnreadCount()
      set({ isMarkingAll: false })
    }
  },

  togglePanel: () => set((state) => ({ isOpen: !state.isOpen })),
  closePanel: () => set({ isOpen: false }),
}))
