import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { FarmResponse, DeviceResponse, DeviceModelResponse, DeviceWithModel } from '@/types'
import OverviewTab from '@/components/devices/OverviewTab'
import TelemetryTab from '@/components/telemetry/TelemetryTab'
import RulesTab from '@/components/rules/RulesTab'
import LoadingSpinner from '@/components/ui/LoadingSpinner'

const tabs = [
  { id: 'overview', label: 'Tổng quan & Thiết bị', icon: 'dashboard' },
  { id: 'telemetry', label: 'Biểu đồ thống kê', icon: 'show_chart' },
  { id: 'rules', label: 'Tự động hóa', icon: 'robot_2' },
]

export default function FarmDetailPage() {
  const { farmId } = useParams<{ farmId: string }>()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('overview')
  const [farm, setFarm] = useState<FarmResponse | null>(null)
  const [devices, setDevices] = useState<DeviceWithModel[]>([])
  const [deviceModels, setDeviceModels] = useState<DeviceModelResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)

  const fetchData = async () => {
    if (!farmId) return
    try {
      const [farmRes, devicesRes, modelsRes] = await Promise.all([
        api.get<FarmResponse>(`/farms/${farmId}`),
        api.get<DeviceResponse[]>(`/farms/${farmId}/devices`),
        api.get<DeviceModelResponse[]>('/device-models'),
      ])

      setFarm(farmRes.data)
      setDeviceModels(modelsRes.data)

      // Enrich devices with model info
      const enriched: DeviceWithModel[] = devicesRes.data.map((d) => {
        const model = modelsRes.data.find((m) => m.id === d.model_id)
        return {
          ...d,
          device_type: model?.device_type,
          metric_type: model?.metric_type,
          model_name: model?.model_name,
        }
      })
      setDevices(enriched)
    } catch (error) {
      toast.error(getApiErrorMessage(error))
      navigate('/farms')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { 
    fetchData() 
    const interval = setInterval(fetchData, 30000) // Tự động đồng bộ ngầm mỗi 30s
    return () => clearInterval(interval)
  }, [farmId])

  if (isLoading) return <LoadingSpinner size="lg" text="Đang tải chi tiết nông trại..." />
  if (!farm || !farmId) return null

  return (
    <div className="animate-fade-in">
      {/* Breadcrumb */}
      <div className="flex items-center gap-2 mb-6 text-sm">
        <button onClick={() => navigate('/farms')} className="text-on-surface-variant hover:text-primary transition-colors font-label">
          Nông trại
        </button>
        <span className="material-symbols-outlined text-outline-variant text-sm">chevron_right</span>
        <span className="text-on-surface font-semibold">{farm.name}</span>
      </div>

      {/* Farm Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-headline text-2xl font-bold text-on-surface">{farm.name}</h1>
          {farm.location && (
            <div className="flex items-center gap-1.5 mt-1">
              <span className="material-symbols-outlined text-sm text-on-surface-variant">location_on</span>
              <span className="text-sm text-on-surface-variant">{farm.location}</span>
            </div>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 mb-8 bg-surface-container-low rounded-2xl p-1.5">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-2 px-5 py-3 rounded-xl text-sm font-semibold transition-all duration-200 ${
              activeTab === tab.id
                ? 'bg-surface-container-lowest text-primary shadow-card'
                : 'text-on-surface-variant hover:text-on-surface hover:bg-surface-container/50'
            }`}
          >
            <span className="material-symbols-outlined text-lg">{tab.icon}</span>
            {tab.label}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      {activeTab === 'overview' && (
        <OverviewTab
          farmId={farmId}
          devices={devices}
          deviceModels={deviceModels}
          onDevicesChange={fetchData}
        />
      )}
      {activeTab === 'telemetry' && (
        <TelemetryTab farmId={farmId} devices={devices} />
      )}
      {activeTab === 'rules' && (
        <RulesTab farmId={farmId} devices={devices} />
      )}
    </div>
  )
}
