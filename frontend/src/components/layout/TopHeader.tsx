import { useEffect, useRef } from 'react'
import { formatDistanceToNow } from 'date-fns'
import { vi } from 'date-fns/locale'
import { useAuthStore } from '@/stores/authStore'
import { useNotificationStore } from '@/stores/notificationStore'

export default function TopHeader() {
  const { user } = useAuthStore()
  const { notifications, unreadCount, isOpen, fetchNotifications, markAsRead, togglePanel, closePanel } =
    useNotificationStore()
  const panelRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    fetchNotifications()
  }, [fetchNotifications])

  // Close panel on click outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(e.target as Node)) {
        closePanel()
      }
    }
    if (isOpen) document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [isOpen, closePanel])

  const getGreeting = () => {
    const hour = new Date().getHours()
    if (hour < 12) return 'Chào buổi sáng'
    if (hour < 18) return 'Chào buổi chiều'
    return 'Chào buổi tối'
  }

  return (
    <header className="fixed top-0 right-0 w-[calc(100%-16rem)] h-20 bg-background/80 backdrop-blur-md flex items-center justify-between px-8 z-40 border-b border-surface-container-low/50">
      <div className="flex items-center gap-4">
        <h1 className="font-headline text-xl font-bold text-primary">
          {getGreeting()}, {user?.username || '...'}
        </h1>
      </div>

      <div className="flex items-center gap-4">
        {/* Live Indicator */}
        <div className="flex items-center gap-2 px-4 py-2 bg-surface-container-low rounded-full">
          <span className="w-2 h-2 rounded-full bg-accent animate-pulse" />
          <span className="text-xs font-semibold text-on-surface-variant">Live System</span>
        </div>

        {/* Notification Bell */}
        <div className="relative" ref={panelRef}>
          <button
            onClick={togglePanel}
            className="relative p-2.5 hover:bg-surface-container-low rounded-full transition-all duration-200"
            id="notification-bell"
          >
            <span className="material-symbols-outlined text-on-surface-variant">notifications</span>
            {unreadCount > 0 && (
              <span className="absolute top-1.5 right-1.5 w-5 h-5 bg-error text-white text-[10px] font-black flex items-center justify-center rounded-full border-2 border-background">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </button>

          {/* Notification Dropdown */}
          {isOpen && (
            <div className="absolute right-0 top-14 w-96 bg-surface-container-lowest rounded-2xl shadow-modal border border-surface-container-low animate-scale-in overflow-hidden">
              <div className="px-5 py-4 border-b border-surface-container-low">
                <h3 className="font-headline font-bold text-on-surface">Thông báo</h3>
              </div>
              <div className="max-h-80 overflow-y-auto">
                {notifications.length === 0 ? (
                  <div className="py-10 text-center text-sm text-on-surface-variant">
                    Không có thông báo nào
                  </div>
                ) : (
                  notifications.map((notif) => (
                    <button
                      key={notif.id}
                      onClick={() => {
                        if (!notif.is_read) markAsRead(notif.id)
                      }}
                      className={`w-full text-left px-5 py-4 hover:bg-surface-container-low transition-colors border-b border-surface-container-low/50 ${
                        notif.is_read ? 'opacity-60' : ''
                      }`}
                    >
                      <div className="flex items-start gap-3">
                        {!notif.is_read && (
                          <span className="w-2 h-2 mt-1.5 rounded-full bg-primary flex-shrink-0" />
                        )}
                        <div className={notif.is_read ? 'ml-5' : ''}>
                          <p className="text-sm text-on-surface leading-relaxed">{notif.message}</p>
                          <p className="text-xs text-on-surface-variant mt-1">
                            {formatDistanceToNow(new Date(notif.created_at), {
                              addSuffix: true,
                              locale: vi,
                            })}
                          </p>
                        </div>
                      </div>
                    </button>
                  ))
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
