import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useNotificationStore } from '../notificationStore';
import api from '@/lib/axios';

vi.mock('@/lib/axios', () => {
  return {
    default: {
      get: vi.fn(),
      put: vi.fn(),
    },
  };
});

describe('notificationStore', () => {
  const initialStoreState = useNotificationStore.getState();

  beforeEach(() => {
    useNotificationStore.setState(initialStoreState, true);
    vi.clearAllMocks();
  });

  it('should initialize with default state', () => {
    const state = useNotificationStore.getState();
    expect(state.notifications).toEqual([]);
    expect(state.unreadCount).toBe(0);
    expect(state.isOpen).toBe(false);
  });

  it('should fetch unread count', async () => {
    vi.mocked(api.get).mockResolvedValueOnce({ data: { unread_count: 5 } });

    await useNotificationStore.getState().fetchUnreadCount();

    const state = useNotificationStore.getState();
    expect(state.unreadCount).toBe(5);
    expect(api.get).toHaveBeenCalledWith('/notifications/unread-count');
  });

  it('should fetch notifications and reset pagination', async () => {
    const mockNotifications = [
      { id: '1', message: 'Test 1', is_read: false, created_at: '2026-04-27T10:00:00Z' }
    ];
    // First call inside fetchNotifications is fetchUnreadCount
    vi.mocked(api.get).mockResolvedValueOnce({ data: { unread_count: 1 } });
    // Second call is the actual fetch
    vi.mocked(api.get).mockResolvedValueOnce({ data: mockNotifications });

    await useNotificationStore.getState().fetchNotifications();

    const state = useNotificationStore.getState();
    expect(state.notifications).toEqual(mockNotifications);
    expect(state.unreadCount).toBe(1);
    expect(state.currentPage).toBe(1);
    expect(state.hasMore).toBe(false); // Since 1 < pageSize
  });

  it('should toggle panel and close panel', () => {
    useNotificationStore.getState().togglePanel();
    expect(useNotificationStore.getState().isOpen).toBe(true);

    useNotificationStore.getState().togglePanel();
    expect(useNotificationStore.getState().isOpen).toBe(false);

    useNotificationStore.getState().togglePanel();
    useNotificationStore.getState().closePanel();
    expect(useNotificationStore.getState().isOpen).toBe(false);
  });

  it('should mark notification as read', async () => {
    const mockNotifications = [
      { id: '1', message: 'Test 1', is_read: false, created_at: '2026-04-27T10:00:00Z' },
      { id: '2', message: 'Test 2', is_read: true, created_at: '2026-04-27T10:01:00Z' }
    ];
    useNotificationStore.setState({ notifications: mockNotifications, unreadCount: 1 });

    vi.mocked(api.put).mockResolvedValueOnce({});
    vi.mocked(api.get).mockResolvedValueOnce({ data: { unread_count: 0 } });

    await useNotificationStore.getState().markAsRead('1');

    const state = useNotificationStore.getState();
    expect(state.notifications[0].is_read).toBe(true);
    expect(state.unreadCount).toBe(0);
    expect(api.put).toHaveBeenCalledWith('/notifications/1/read');
  });
});
