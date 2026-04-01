import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import { useEffect } from 'react'
import { useAuthStore } from '@/stores/authStore'

import PublicRoute from '@/components/guards/PublicRoute'
import ProtectedRoute from '@/components/guards/ProtectedRoute'
import MainLayout from '@/components/layout/MainLayout'

import LoginPage from '@/pages/auth/LoginPage'
import RegisterPage from '@/pages/auth/RegisterPage'
import FarmsPage from '@/pages/farmer/FarmsPage'
import FarmDetailPage from '@/pages/farmer/FarmDetailPage'
import DeviceRequestsPage from '@/pages/admin/DeviceRequestsPage'
import DeviceModelsPage from '@/pages/admin/DeviceModelsPage'
import AdminDashboardPage from '@/pages/admin/AdminDashboardPage'
import ProfilePage from '@/pages/ProfilePage'

export default function App() {
  const { loadFromStorage, fetchProfile, isAuthenticated } = useAuthStore()

  useEffect(() => {
    loadFromStorage()
  }, [loadFromStorage])

  useEffect(() => {
    if (isAuthenticated) {
      fetchProfile()
    }
  }, [isAuthenticated, fetchProfile])

  return (
    <BrowserRouter>
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 3000,
          style: {
            background: '#fff',
            color: '#2c2f31',
            borderRadius: '16px',
            boxShadow: '0 20px 40px rgba(0,0,0,0.1)',
            padding: '12px 20px',
            fontSize: '14px',
            fontFamily: 'Manrope, sans-serif',
            fontWeight: 500,
          },
          success: {
            iconTheme: { primary: '#006947', secondary: '#fff' },
          },
          error: {
            iconTheme: { primary: '#b31b25', secondary: '#fff' },
          },
        }}
      />
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<PublicRoute><LoginPage /></PublicRoute>} />
        <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} />

        {/* Protected Routes with Layout */}
        <Route element={<ProtectedRoute><MainLayout /></ProtectedRoute>}>
          {/* Farmer Routes */}
          <Route path="/farms" element={<ProtectedRoute requiredRole="FARMER"><FarmsPage /></ProtectedRoute>} />
          <Route path="/farms/:farmId" element={<ProtectedRoute requiredRole="FARMER"><FarmDetailPage /></ProtectedRoute>} />

          {/* Admin Routes */}
          <Route path="/admin/dashboard" element={<ProtectedRoute requiredRole="ADMIN"><AdminDashboardPage /></ProtectedRoute>} />
          <Route path="/admin/device-requests" element={<ProtectedRoute requiredRole="ADMIN"><DeviceRequestsPage /></ProtectedRoute>} />
          <Route path="/admin/device-models" element={<ProtectedRoute requiredRole="ADMIN"><DeviceModelsPage /></ProtectedRoute>} />
          
          {/* Global Profiles */}
          <Route path="/profile" element={<ProfilePage />} />
        </Route>

        {/* Redirects */}
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
