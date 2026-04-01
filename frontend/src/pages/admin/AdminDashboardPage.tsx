import { useState, useEffect } from 'react'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { AdminStatsResponse } from '@/types'
import LoadingSpinner from '@/components/ui/LoadingSpinner'
import toast from 'react-hot-toast'
import AdminDetailDrawer from '@/components/admin/AdminDetailDrawer'

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<AdminStatsResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  
  // Drawer state
  const [detailType, setDetailType] = useState<'farmers' | 'farms' | 'devices' | 'active_devices' | null>(null)
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)

  const fetchStats = async () => {
    try {
      const res = await api.get<AdminStatsResponse>('/admin/stats')
      setStats(res.data)
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchStats()
  }, [])

  const handleCardClick = (type: 'farmers' | 'farms' | 'devices' | 'active_devices') => {
    setDetailType(type)
    setIsDrawerOpen(true)
  }

  if (isLoading) return <LoadingSpinner size="lg" text="Đang tải dữ liệu hệ thống..." />

  const cards = [
    {
      type: 'farmers' as const,
      label: 'Tổng số nông dân',
      value: stats?.total_farmers || 0,
      icon: 'person',
      gradient: 'from-emerald-500 to-teal-700',
      shadow: 'shadow-emerald-500/20'
    },
    {
      type: 'farms' as const,
      label: 'Tổng số nông trại',
      value: stats?.total_farms || 0,
      icon: 'yard',
      gradient: 'from-blue-500 to-indigo-700',
      shadow: 'shadow-blue-500/20'
    },
    {
      type: 'devices' as const,
      label: 'Tổng số thiết bị',
      value: stats?.total_devices || 0,
      icon: 'devices',
      gradient: 'from-purple-500 to-fuchsia-700',
      shadow: 'shadow-purple-500/20'
    },
    {
      type: 'pending_requests' as const, // For link to existing page
      label: 'Yêu cầu chờ duyệt',
      value: stats?.pending_requests || 0,
      icon: 'pending_actions',
      gradient: 'from-orange-500 to-rose-700',
      shadow: 'shadow-orange-500/20',
      link: '/admin/device-requests'
    },
    {
      type: 'active_devices' as const,
      label: 'Thiết bị đang hoạt động',
      value: stats?.active_devices || 0,
      icon: 'sensors',
      gradient: 'from-cyan-500 to-sky-700',
      shadow: 'shadow-cyan-500/20'
    }
  ]

  return (
    <div className="animate-fade-in pb-12">
      {/* Header */}
      <div className="mb-10">
        <div className="flex items-center gap-3 mb-2">
          <div className="w-8 h-8 rounded-lg bg-primary/20 flex items-center justify-center">
             <span className="material-symbols-outlined text-primary text-xl">admin_panel_settings</span>
          </div>
          <span className="text-xs font-bold text-primary uppercase tracking-[0.2em] font-label">Platform Metrics</span>
        </div>
        <h1 className="font-headline text-3xl font-black text-on-surface">Tổng quan hệ thống</h1>
        <p className="text-sm text-on-surface-variant max-w-2xl mt-2 font-medium leading-relaxed">
          Phân tích dữ liệu thời gian thực và quản lý tài nguyên trên toàn bộ hệ sinh thái YoloFarm. 
          Nhấp vào các thẻ để xem chi tiết danh sách.
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-6 mb-10">
        {cards.map((card, index) => (
          <div 
            key={card.label} 
            onClick={() => card.link ? window.location.href = card.link : handleCardClick(card.type as any)}
            className={`group relative overflow-hidden bg-surface-container-lowest p-6 rounded-[2rem] border border-surface-container-low shadow-sm hover:shadow-xl hover:-translate-y-2 transition-all duration-500 cursor-pointer animate-slide-up ${card.shadow}`}
            style={{ animationDelay: `${index * 0.1}s` }}
          >
            {/* Background Decoration */}
            <div className={`absolute -right-4 -top-4 w-24 h-24 bg-gradient-to-br ${card.gradient} opacity-5 group-hover:opacity-10 rounded-full transition-opacity duration-500`} />
            
            <div className={`w-12 h-12 rounded-2xl bg-gradient-to-br ${card.gradient} flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform duration-500 mb-6`}>
              <span className="material-symbols-outlined text-white text-xl">{card.icon}</span>
            </div>
            
            <div className="space-y-1">
              <p className="text-[11px] font-black text-on-surface-variant uppercase tracking-widest opacity-60 group-hover:opacity-100 transition-opacity">
                {card.label}
              </p>
              <h2 className="text-4xl font-black text-on-surface tracking-tight group-hover:text-primary transition-colors">
                {card.value}
              </h2>
            </div>

            {/* Click Indicator */}
            <div className="mt-4 flex items-center gap-1.5 text-[10px] font-bold text-primary opacity-0 group-hover:opacity-100 transition-opacity">
              <span>Xem chi tiết</span>
              <span className="material-symbols-outlined text-[12px] animate-bounce-x">arrow_forward</span>
            </div>
          </div>
        ))}
      </div>

      {/* Main Content Area */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Server Status */}
        <div className="lg:col-span-2 bg-surface-container-lowest rounded-[2.5rem] p-8 border border-surface-container-low shadow-sm animate-slide-up" style={{ animationDelay: '0.6s' }}>
          <div className="flex items-center justify-between mb-8">
            <h3 className="font-headline text-xl font-black text-on-surface flex items-center gap-3">
              <div className="w-10 h-10 rounded-2xl bg-primary-container/30 flex items-center justify-center">
                <span className="material-symbols-outlined text-primary">storage</span>
              </div>
              Trạng thái máy chủ
            </h3>
            <span className="flex items-center gap-1.5 px-3 py-1 bg-primary/10 rounded-full">
              <span className="w-2 h-2 rounded-full bg-primary animate-pulse" />
              <span className="text-[10px] font-black text-primary uppercase">Mọi thứ ổn định</span>
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[
              { name: 'API Gateway', status: 'Hoạt động', icon: 'api', delay: '0.7s' },
              { name: 'MQTT Broker', status: 'Hoạt động', icon: 'hub', delay: '0.8s' },
              { name: 'Database DB', status: 'Hoạt động', icon: 'database', delay: '0.9s' },
            ].map((s) => (
              <div key={s.name} className="p-5 rounded-3xl bg-surface-container-low border border-surface-container-high hover:border-primary/30 transition-colors animate-slide-up" style={{ animationDelay: s.delay }}>
                <span className="material-symbols-outlined text-primary mb-3 block">{s.icon}</span>
                <span className="text-xs font-bold text-on-surface-variant block mb-1 uppercase tracking-wider">{s.name}</span>
                <span className="text-sm font-black text-primary">{s.status}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Action / Notification */}
        <div className="card-primary-gradient p-8 rounded-[2.5rem] shadow-xl shadow-primary/10 flex flex-col justify-between animate-slide-up" style={{ animationDelay: '0.8s' }}>
          <div>
             <div className="w-12 h-12 bg-white/20 rounded-2xl flex items-center justify-center mb-6">
               <span className="material-symbols-outlined text-white text-2xl">notifications_active</span>
             </div>
             <h3 className="text-2xl font-black text-white mb-3">Thông báo mới</h3>
             <p className="text-white/80 text-sm leading-relaxed mb-6">
               Hệ thống hiện có <strong>{stats?.pending_requests || 0}</strong> yêu cầu phê duyệt thiết bị mới đang chờ xử lý từ các nông dân.
             </p>
          </div>
          
          <a 
            href="/admin/device-requests" 
            className="w-full bg-white text-primary font-black py-4 rounded-2xl flex items-center justify-center gap-2 hover:bg-surface-container-lowest transition-all hover:scale-[1.02]"
          >
            ĐI TỚI PHÊ DUYỆT
            <span className="material-symbols-outlined text-sm">arrow_forward</span>
          </a>
        </div>
      </div>

      {/* Details Drawer */}
      <AdminDetailDrawer 
        isOpen={isDrawerOpen} 
        onClose={() => setIsDrawerOpen(false)} 
        type={detailType}
        title={cards.find(c => c.type === detailType)?.label || 'Chi tiết'}
      />
    </div>
  )
}
