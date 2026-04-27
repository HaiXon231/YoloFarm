import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useAuthStore } from '../authStore';
import api from '@/lib/axios';

// Mock axios
vi.mock('@/lib/axios', () => {
  return {
    default: {
      get: vi.fn(),
    },
  };
});

describe('authStore', () => {
  const initialStoreState = useAuthStore.getState();

  beforeEach(() => {
    // Reset zustand store before each test
    useAuthStore.setState(initialStoreState, true);
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('should initialize with default state', () => {
    const state = useAuthStore.getState();
    expect(state.accessToken).toBeNull();
    expect(state.role).toBeNull();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.isLoading).toBe(true);
  });

  it('should login and set tokens in localStorage', () => {
    useAuthStore.getState().login('test-token', 'FARMER');

    const state = useAuthStore.getState();
    expect(state.accessToken).toBe('test-token');
    expect(state.role).toBe('FARMER');
    expect(state.isAuthenticated).toBe(true);
    expect(localStorage.getItem('access_token')).toBe('test-token');
    expect(localStorage.getItem('role')).toBe('FARMER');
  });

  it('should logout and clear state and localStorage', () => {
    useAuthStore.getState().login('test-token', 'FARMER');
    useAuthStore.getState().logout();

    const state = useAuthStore.getState();
    expect(state.accessToken).toBeNull();
    expect(state.role).toBeNull();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('role')).toBeNull();
  });

  it('should load token from storage if available', () => {
    localStorage.setItem('access_token', 'stored-token');
    localStorage.setItem('role', 'ADMIN');

    useAuthStore.getState().loadFromStorage();

    const state = useAuthStore.getState();
    expect(state.accessToken).toBe('stored-token');
    expect(state.role).toBe('ADMIN');
    expect(state.isAuthenticated).toBe(true);
    expect(state.isLoading).toBe(false);
  });

  it('should handle fetchProfile success', async () => {
    const mockUser = { id: '1', username: 'testuser', role: 'FARMER', email: 'test@example.com' };
    vi.mocked(api.get).mockResolvedValueOnce({ data: mockUser });

    await useAuthStore.getState().fetchProfile();

    const state = useAuthStore.getState();
    expect(state.user).toEqual(mockUser);
    expect(state.role).toBe('FARMER');
    expect(state.isLoading).toBe(false);
  });

  it('should handle fetchProfile failure by logging out', async () => {
    // Giả lập đang login
    useAuthStore.getState().login('test-token', 'FARMER');
    
    vi.mocked(api.get).mockRejectedValueOnce(new Error('Unauthorized'));

    await useAuthStore.getState().fetchProfile();

    const state = useAuthStore.getState();
    expect(state.user).toBeNull();
    expect(state.accessToken).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.isLoading).toBe(false);
  });
});
