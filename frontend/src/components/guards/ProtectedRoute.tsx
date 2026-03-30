import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

interface ProtectedRouteProps {
  children: React.ReactNode
  requiredRole?: 'FARMER' | 'ADMIN'
}

export default function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const { isAuthenticated, role, isLoading } = useAuthStore()

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-background">
        <div className="flex flex-col items-center gap-4">
          <div className="w-12 h-12 border-4 border-primary/20 border-t-primary rounded-full animate-spin" />
          <span className="text-on-surface-variant font-label text-sm">Đang tải...</span>
        </div>
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  // Role-based access control
  if (requiredRole && role !== requiredRole) {
    if (role === 'FARMER') {
      return <Navigate to="/farms" replace />
    }
    if (role === 'ADMIN') {
      return <Navigate to="/admin/device-requests" replace />
    }
  }

  return <>{children}</>
}
