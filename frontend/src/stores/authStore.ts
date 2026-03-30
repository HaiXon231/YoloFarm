import { create } from 'zustand'
import api from '@/lib/axios'
import type { UserProfile, LoginResponse } from '@/types'

interface AuthState {
  accessToken: string | null
  role: 'FARMER' | 'ADMIN' | null
  user: UserProfile | null
  isAuthenticated: boolean
  isLoading: boolean

  login: (token: string, role: 'FARMER' | 'ADMIN') => void
  logout: () => void
  setUser: (user: UserProfile) => void
  fetchProfile: () => Promise<void>
  loadFromStorage: () => void
}

export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: null,
  role: null,
  user: null,
  isAuthenticated: false,
  isLoading: true,

  login: (token: string, role: 'FARMER' | 'ADMIN') => {
    localStorage.setItem('access_token', token)
    localStorage.setItem('role', role)
    set({
      accessToken: token,
      role,
      isAuthenticated: true,
    })
  },

  logout: () => {
    localStorage.removeItem('access_token')
    localStorage.removeItem('role')
    set({
      accessToken: null,
      role: null,
      user: null,
      isAuthenticated: false,
    })
  },

  setUser: (user: UserProfile) => {
    set({ user, role: user.role })
  },

  fetchProfile: async () => {
    try {
      const response = await api.get<UserProfile>('/auth/me')
      set({ user: response.data, role: response.data.role, isLoading: false })
    } catch {
      get().logout()
      set({ isLoading: false })
    }
  },

  loadFromStorage: () => {
    const token = localStorage.getItem('access_token')
    const role = localStorage.getItem('role') as 'FARMER' | 'ADMIN' | null
    if (token && role) {
      set({
        accessToken: token,
        role,
        isAuthenticated: true,
        isLoading: false,
      })
    } else {
      set({ isLoading: false })
    }
  },
}))
