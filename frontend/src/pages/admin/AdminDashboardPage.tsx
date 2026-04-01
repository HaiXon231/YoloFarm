import { useState, useEffect } from 'react'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { AdminStatsResponse } from '@/types'
import LoadingSpinner from '@/components/ui/LoadingSpinner'
import toast from 'react-hot-toast'

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<AdminStatsResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)

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

  if (isLoading) return <LoadingSpinner size="lg" text="Đang tải dữ liệu hệ thống..." />

  const cards = [
    {
      label: 'Tổng số nông dân',
      value: stats?.total_farmers || 0,
      icon: 'person',
      color: 'bg-primary'
    },
    {
      label: 'Tổng số nông trại',
      value: stats?.total_farms || 0,
      icon: 'yard',
      color: 'bg-secondary'
    },
    {
      label: 'Tổng số thiết bị',
      value: stats?.total_devices || 0,
      icon: 'devices',
      color: 'bg-tertiary'
    },
    {
      label: 'Yêu cầu chờ duyệt',
      value: stats?.pending_requests || 0,
      icon: 'pending_actions',
      color: 'bg-error'
    },
    {
      label: 'Thiết bị đang hoạt động',
      value: stats?.active_devices || 0,
      icon: 'sensors',
      color: 'bg-success'
    }
  ]

  return (
    <div className="animate-fade-in">
      {/* Header */}
      <div className="mb-8">
        <h1 className="font-headline text-2xl font-bold text-on-surface">Tổng quan hệ thống</h1>
        <p className="text-sm text-on-surface-variant mt-1 font-label">
          Báo cáo nhanh tình trạng hoạt động của toàn bộ nền tảng YoloFarm
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {cards.map((card, index) => (
          <div 
            key={card.label} 
            className="card p-6 flex items-center gap-5 animate-slide-up hover:translate-y-[-4px] transition-all duration-300"
            style={{ animationDelay: `${index * 0.1}s` }}
          >
            <div className={`w-14 h-14 rounded-2xl ${card.color} flex items-center justify-center shadow-lg shadow-black/5`}>
              <span className="material-symbols-outlined text-white text-2xl">{card.icon}</span>
            </div>
            <div>
              <p className="text-xs font-bold text-on-surface-variant uppercase tracking-wider mb-1">{card.label}</p>
              <h2 className="text-3xl font-black text-on-surface">{card.value}</h2>
            </div>
          </div>
        ))}
      </div>

      {/* Info Sections */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-8">
        <div className="card animate-slide-up" style={{ animationDelay: '0.5s' }}>
          <h3 className="font-headline text-lg font-bold text-on-surface mb-4 flex items-center gap-2">
            <span className="material-symbols-outlined text-primary">security</span>
            Trạng thái máy chủ
          </h3>
          <div className="space-y-4">
            <div className="flex justify-between items-center p-3 bg-surface-container-low rounded-xl">
              <span className="text-sm font-medium text-on-surface">API Gateway</span>
              <span className="badge-active">Hoạt động</span>
            </div>
            <div className="flex justify-between items-center p-3 bg-surface-container-low rounded-xl">
              <span className="text-sm font-medium text-on-surface">MQTT Broker</span>
              <span className="badge-active">Hoạt động</span>
            </div>
            <div className="flex justify-between items-center p-3 bg-surface-container-low rounded-xl">
              <span className="text-sm font-medium text-on-surface">Database</span>
              <span className="badge-active">Hoạt động</span>
            </div>
          </div>
        </div>

        <div className="card animate-slide-up bg-primary-container/10 border-primary/20" style={{ animationDelay: '0.6s' }}>
          <h3 className="font-headline text-lg font-bold text-primary mb-4 flex items-center gap-2">
            <span className="material-symbols-outlined">info</span>
            Thông báo hệ thống
          </h3>
          <p className="text-sm text-on-surface-variant leading-relaxed mb-6">
            Hệ thống đang hoạt động ổn định. Hiện có <strong>{stats?.pending_requests}</strong> yêu cầu phê duyệt thiết bị mới đang chờ xử lý từ các nông dân.
          </p>
          <a 
            href="/admin/device-requests" 
            className="btn-primary inline-flex items-center gap-2"
          >
            Đi tới phê duyệt
            <span className="material-symbols-outlined text-sm">arrow_forward</span>
          </a>
        </div>
      </div>
    </div>
  )
}
