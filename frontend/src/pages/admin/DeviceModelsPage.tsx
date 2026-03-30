import { useState, useEffect } from 'react'
import toast from 'react-hot-toast'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { DeviceModelResponse } from '@/types'
import ModelFormModal from '@/components/admin/ModelFormModal'
import LoadingSpinner from '@/components/ui/LoadingSpinner'
import EmptyState from '@/components/ui/EmptyState'

export default function DeviceModelsPage() {
  const [models, setModels] = useState<DeviceModelResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isFormOpen, setIsFormOpen] = useState(false)

  const fetchModels = async () => {
    try {
      const res = await api.get<DeviceModelResponse[]>('/device-models')
      setModels(res.data)
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { fetchModels() }, [])

  if (isLoading) return <LoadingSpinner size="lg" text="Đang tải danh mục thiết bị..." />

  return (
    <div className="animate-fade-in">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-headline text-2xl font-bold text-on-surface">Danh mục thiết bị</h1>
          <p className="text-sm text-on-surface-variant mt-1 font-label">
            Khai báo các khuôn mẫu cảm biến và thiết bị điều khiển
          </p>
        </div>
        <button onClick={() => setIsFormOpen(true)} className="btn-primary-gradient flex items-center gap-2" id="create-model-btn">
          <span className="material-symbols-outlined text-lg">add</span>
          Thêm mẫu mới
        </button>
      </div>

      {models.length === 0 ? (
        <EmptyState
          icon="inventory_2"
          title="Chưa có khuôn mẫu nào"
          description="Thêm khuôn mẫu thiết bị để Nông dân có thể yêu cầu cấp phát."
          action={
            <button onClick={() => setIsFormOpen(true)} className="btn-primary flex items-center gap-2">
              <span className="material-symbols-outlined text-lg">add</span>
              Thêm mẫu mới
            </button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {models.map((model, index) => (
            <div key={model.id} className="card animate-slide-up" style={{ animationDelay: `${index * 0.05}s` }}>
              <div className="flex items-start gap-4">
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                  model.device_type === 'SENSOR'
                    ? 'bg-primary-container/20'
                    : 'bg-tertiary-container/20'
                }`}>
                  <span className={`material-symbols-outlined ${
                    model.device_type === 'SENSOR' ? 'text-primary' : 'text-tertiary'
                  }`}>
                    {model.device_type === 'SENSOR' ? 'sensors' : 'water_pump'}
                  </span>
                </div>
                <div className="flex-1">
                  <h3 className="font-semibold text-on-surface">{model.model_name}</h3>
                  <div className="flex flex-wrap items-center gap-2 mt-2">
                    <span className={model.device_type === 'SENSOR' ? 'badge-online' : 'badge-auto'}>
                      {model.device_type}
                    </span>
                    <span className="badge bg-surface-container-high text-on-surface-variant">
                      {model.metric_type}
                    </span>
                  </div>
                  {model.manufacturer && (
                    <p className="text-xs text-on-surface-variant mt-2">
                      <span className="material-symbols-outlined text-xs align-middle mr-1">factory</span>
                      {model.manufacturer}
                    </p>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <ModelFormModal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        onSuccess={() => { setIsFormOpen(false); fetchModels() }}
      />
    </div>
  )
}
