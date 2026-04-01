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
      gradient: 'from-emerald-600 to-teal-800',
      accent: 'card-accent-emerald',
      iconBg: 'bg-emerald-100 text-emerald-600'
    },
    {
      type: 'farms' as const,
      label: 'Tổng số nông trại',
      value: stats?.total_farms || 0,
      icon: 'yard',
      gradient: 'from-blue-600 to-indigo-800',
      accent: 'card-accent-blue',
      iconBg: 'bg-blue-100 text-blue-600'
    },
    {
      type: 'devices' as const,
      label: 'Tổng số thiết bị',
      value: stats?.total_devices || 0,
      icon: 'devices',
      gradient: 'from-purple-600 to-fuchsia-800',
      accent: 'card-accent-purple',
      iconBg: 'bg-purple-100 text-purple-600'
    },
    {
      type: 'pending_requests' as const,
      label: 'Yêu cầu chờ duyệt',
      value: stats?.pending_requests || 0,
      icon: 'pending_actions',
      gradient: 'from-orange-600 to-rose-800',
      accent: 'card-accent-orange',
      iconBg: 'bg-orange-100 text-orange-600',
      link: '/admin/device-requests'
    },
    {
      type: 'active_devices' as const,
      label: 'Thiết bị đang hoạt động',
      value: stats?.active_devices || 0,
      icon: 'sensors',
      gradient: 'from-cyan-600 to-sky-800',
      accent: 'card-accent-cyan',
      iconBg: 'bg-cyan-100 text-cyan-600'
    }
  ]

  return (
    <div className="animate-fade-in pb-12 relative">
      {/* Background Blobs for that "Vibrant" feel */}
      <div className="fixed top-20 right-20 w-96 h-96 bg-primary/10 rounded-full blur-[120px] -z-10 animate-blob" />
      <div className="fixed bottom-20 left-20 w-80 h-80 bg-secondary/10 rounded-full blur-[100px] -z-10 animate-blob [animation-delay:2s]" />

      {/* Header */}
      <div className="mb-12">
        <div className="flex items-center gap-4 mb-4">
          <div className="w-12 h-12 rounded-2xl bg-primary shadow-lg shadow-primary/20 flex items-center justify-center">
             <span className="material-symbols-outlined text-white text-2xl icon-filled">dashboard</span>
          </div>
          <div>
            <h1 className="font-headline text-4xl font-black text-on-surface tracking-tight">Hệ thống YoloFarm</h1>
            <p className="text-sm font-bold text-primary uppercase tracking-[0.3em]">Administrator Dashboard</p>
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-6 mb-12">
        {cards.map((card, index) => (
          <div 
            key={card.label} 
            onClick={() => card.link ? window.location.href = card.link : handleCardClick(card.type as any)}
            className={`premium-card p-6 cursor-pointer ${card.accent} animate-slide-up`}
            style={{ animationDelay: `${index * 0.1}s` }}
          >
            <div className="flex justify-between items-start mb-6">
              <div className={`w-14 h-14 rounded-2xl ${card.iconBg} flex items-center justify-center shadow-inner`}>
                <span className="material-symbols-outlined text-2xl icon-filled">{card.icon}</span>
              </div>
              <div className="flex items-center gap-1 bg-surface-container px-2 py-1 rounded-full">
                 <span className="text-[10px] font-black text-on-surface-variant">Chi tiết</span>
                 <span className="material-symbols-outlined text-xs">open_in_new</span>
              </div>
            </div>
            
            <div className="space-y-1">
              <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-[0.2em] mb-1">
                {card.label}
              </p>
              <div className="flex items-baseline gap-2">
                <span className={`text-5xl font-black bg-gradient-to-br ${card.gradient} bg-clip-text text-transparent`}>
                  {card.value}
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Detailed Sections */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Server Health Status */}
        <div className="lg:col-span-2 premium-card p-10 animate-slide-up" style={{ animationDelay: '0.6s' }}>
          <div className="flex items-center justify-between mb-10">
            <div className="flex items-center gap-4">
              <div className="w-14 h-14 rounded-3xl bg-surface-container flex items-center justify-center">
                <span className="material-symbols-outlined text-primary text-3xl icon-filled">settings_input_component</span>
              </div>
              <div>
                <h3 className="font-headline text-2xl font-black text-on-surface">Cấu trúc hạ tầng</h3>
                <p className="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Trạng thái hệ thống thời gian thực</p>
              </div>
            </div>
            <div className="flex items-center gap-2 px-6 py-2 bg-emerald-50 text-emerald-600 rounded-2xl border border-emerald-100 shadow-sm">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
              <span className="text-xs font-black uppercase tracking-tighter">Healthy & Stable</span>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {[
              { 
                name: 'API Gateway', 
                status: stats?.api_status ? 'Hoạt động' : 'Mất kết nối', 
                icon: 'webhook', 
                color: stats?.api_status ? 'text-emerald-500' : 'text-rose-500', 
                bg: stats?.api_status ? 'bg-emerald-50' : 'bg-rose-50',
                delay: '0.7s' 
              },
              { 
                name: 'MQTT Broker', 
                status: stats?.mqtt_status ? 'Đã kết nối' : 'Mất kết nối', 
                icon: 'hub', 
                color: stats?.mqtt_status ? 'text-blue-500' : 'text-rose-500', 
                bg: stats?.mqtt_status ? 'bg-blue-50' : 'bg-rose-50',
                delay: '0.8s' 
              },
              { 
                name: 'Database Instance', 
                status: stats?.db_status ? 'Hoạt động' : 'Lỗi kết nối', 
                icon: 'database', 
                color: stats?.db_status ? 'text-purple-500' : 'text-rose-500', 
                bg: stats?.db_status ? 'bg-purple-50' : 'bg-rose-50',
                delay: '0.9s' 
              },
            ].map((s) => (
              <div key={s.name} className="group p-6 rounded-[2rem] bg-surface-container-low border border-surface-container-high hover:border-primary transition-all animate-slide-up" style={{ animationDelay: s.delay }}>
                <div className={`w-10 h-10 rounded-xl mb-4 flex items-center justify-center ${s.color} ${s.bg} shadow-sm group-hover:scale-110 transition-transform`}>
                  <span className="material-symbols-outlined">{s.icon}</span>
                </div>
                <h4 className="text-xs font-black text-on-surface-variant uppercase mb-1 tracking-wider">{s.name}</h4>
                <p className={`text-sm font-black ${s.color}`}>{s.status}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Global Action Banner */}
        <div className="relative overflow-hidden bg-primary p-10 rounded-[2.5rem] shadow-2xl shadow-primary/30 flex flex-col justify-between animate-slide-up" style={{ animationDelay: '0.8s' }}>
          {/* Decorative Pattern */}
          <div className="absolute top-0 right-0 p-4 opacity-10 rotate-12">
            <span className="material-symbols-outlined text-[10rem]">notifications</span>
          </div>
          
          <div className="relative z-10">
             <div className="w-16 h-16 bg-white/20 rounded-3xl flex items-center justify-center mb-8 backdrop-blur-md">
               <span className="material-symbols-outlined text-white text-3xl icon-filled">mark_chat_unread</span>
             </div>
             <h3 className="text-3xl font-black text-white mb-4">Lưu ý hệ thống</h3>
             <p className="text-white/80 text-base font-medium leading-relaxed mb-8">
               Có <span className="text-white font-black text-xl px-2 py-0.5 bg-white/20 rounded-lg">{stats?.pending_requests || 0}</span> yêu cầu thiết bị đang chờ Admin phê duyệt. Vui lòng xử lý sớm để nông dân có thể sử dụng.
             </p>
          </div>
          
          <a 
            href="/admin/device-requests" 
            className="group relative z-10 w-full bg-white text-primary font-black py-5 rounded-2xl flex items-center justify-center gap-2 hover:bg-surface-container-lowest transition-all hover:scale-[1.03] active:scale-[0.98] shadow-lg"
          >
            PHÊ DUYỆT NGAY
            <span className="material-symbols-outlined text-lg transition-transform group-hover:translate-x-1">arrow_forward</span>
          </a>
        </div>
      </div>

      <AdminDetailDrawer 
        isOpen={isDrawerOpen} 
        onClose={() => setIsDrawerOpen(false)} 
        type={detailType}
        title={cards.find(c => c.type === detailType)?.label || 'Báo cáo chi tiết'}
      />
    </div>
  )
}
