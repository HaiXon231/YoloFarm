import { NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'

const farmerLinks = [
  { to: '/farms', icon: 'yard', label: 'Nông trại' },
  { to: '/profile', icon: 'person', label: 'Hồ sơ' },
]

const adminLinks = [
  { to: '/admin/dashboard', icon: 'dashboard', label: 'Tổng quan' },
  { to: '/admin/device-requests', icon: 'pending_actions', label: 'Yêu cầu thiết bị' },
  { to: '/admin/device-models', icon: 'inventory_2', label: 'Danh mục thiết bị' },
  { to: '/profile', icon: 'person', label: 'Hồ sơ' },
]

export default function Sidebar() {
  const { role, user, logout } = useAuthStore()
  const navigate = useNavigate()
  const links = role === 'ADMIN' ? adminLinks : farmerLinks

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <aside className="h-full w-64 fixed left-0 top-0 bg-sidebar shadow-xl flex flex-col py-8 px-4 z-50">
      {/* Brand */}
      <div className="mb-12 px-4">
        <span className="font-headline text-2xl font-black text-accent">YoloFarm</span>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1">
        {links.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 group ${
                isActive
                  ? 'text-accent font-bold border-l-4 border-accent bg-white/5'
                  : 'text-gray-400 font-medium hover:text-white hover:bg-white/10 border-l-4 border-transparent'
              }`
            }
          >
            <span className="material-symbols-outlined text-xl">{link.icon}</span>
            <span className="text-sm font-semibold tracking-tight">{link.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* User Section */}
      <div className="mt-auto px-2 pb-2">
        <div className="flex items-center gap-3 px-3 py-3 rounded-xl hover:bg-white/5 transition-colors group relative">
          <NavLink to="/profile" className="flex items-center gap-3 flex-1 min-w-0">
            <div className="w-10 h-10 rounded-full bg-primary-container flex items-center justify-center text-on-primary-container font-bold text-sm shrink-0">
              {user?.username?.charAt(0).toUpperCase() || 'U'}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-white text-sm font-bold truncate">{user?.username || '...'}</p>
              <p className="text-gray-500 text-[10px] truncate">{user?.email || ''}</p>
            </div>
          </NavLink>
          <button
            onClick={handleLogout}
            className="p-1.5 rounded-lg hover:bg-white/10 transition-colors opacity-0 group-hover:opacity-100"
            title="Đăng xuất"
            id="sidebar-logout-btn"
          >
            <span className="material-symbols-outlined text-gray-400 text-lg">logout</span>
          </button>
        </div>
      </div>
    </aside>
  )
}
