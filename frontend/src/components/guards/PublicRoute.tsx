import { Navigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

interface PublicRouteProps {
  children: React.ReactNode
}

export default function PublicRoute({ children }: PublicRouteProps) {
  const { isAuthenticated, role } = useAuthStore()

  if (isAuthenticated) {
    // Redirect to appropriate dashboard based on role
    if (role === 'ADMIN') {
      return <Navigate to="/admin/device-requests" replace />
    }
    return <Navigate to="/farms" replace />
  }

  return <>{children}</>
}
