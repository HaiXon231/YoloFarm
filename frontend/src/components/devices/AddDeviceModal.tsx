import { useState } from 'react'
import toast from 'react-hot-toast'
import Modal from '@/components/ui/Modal'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { DeviceModelResponse } from '@/types'

interface AddDeviceModalProps {
  isOpen: boolean
  onClose: () => void
  farmId: string
  deviceModels: DeviceModelResponse[]
  onSuccess: () => void
}

export default function AddDeviceModal({ isOpen, onClose, farmId, deviceModels, onSuccess }: AddDeviceModalProps) {
  const [modelId, setModelId] = useState('')
  const [name, setName] = useState('')
  const [minValue, setMinValue] = useState('')
  const [maxValue, setMaxValue] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!modelId || !name.trim()) {
      toast.error('Vui lòng chọn loại thiết bị và nhập tên')
      return
    }
    setIsLoading(true)
    try {
      await api.post('/devices/requests', {
        farm_id: farmId,
        model_id: modelId,
        name,
        min_value: minValue === '' ? null : Number(minValue),
        max_value: maxValue === '' ? null : Number(maxValue),
      })
      toast.success('Đã gửi yêu cầu thêm thiết bị! Vui lòng chờ Admin duyệt.')
      setModelId('')
      setName('')
      setMinValue('')
      setMaxValue('')
      onSuccess()
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  const selectedModel = deviceModels.find((m) => m.id === modelId)

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Yêu cầu thêm thiết bị">
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="device-model" className="label-text block mb-2">Loại thiết bị *</label>
          <select
            id="device-model"
            value={modelId}
            onChange={(e) => setModelId(e.target.value)}
            className="input-field"
            required
          >
            <option value="">-- Chọn mẫu thiết bị --</option>
            {deviceModels.map((model) => (
              <option key={model.id} value={model.id}>
                {model.model_name} ({model.device_type})
              </option>
            ))}
          </select>
          {selectedModel && (
            <div className="mt-2 flex items-center gap-2 text-xs text-on-surface-variant">
              <span className={selectedModel.device_type === 'SENSOR' ? 'badge-online' : 'badge-auto'}>
                {selectedModel.device_type}
              </span>
              <span>•</span>
              <span>{selectedModel.metric_type}</span>
              {selectedModel.manufacturer && <><span>•</span><span>{selectedModel.manufacturer}</span></>}
              {(selectedModel.min_value != null || selectedModel.max_value != null) && (
                <><span>•</span><span>Khuyến nghị {selectedModel.min_value ?? '-'} - {selectedModel.max_value ?? '-'} {selectedModel.display_unit ?? ''}</span></>
              )}
            </div>
          )}
        </div>

        <div>
          <label htmlFor="device-name" className="label-text block mb-2">Tên thiết bị *</label>
          <input
            id="device-name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="input-field"
            placeholder="VD: Cảm biến đất khu A"
            required
          />
        </div>

        {selectedModel?.device_type === 'SENSOR' && (
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="label-text block mb-2">Ngưỡng cảnh báo min</label>
              <input
                type="number"
                step="any"
                value={minValue}
                onChange={(e) => setMinValue(e.target.value)}
                className="input-field"
                placeholder={selectedModel.min_value?.toString() ?? ''}
              />
            </div>
            <div>
              <label className="label-text block mb-2">Ngưỡng cảnh báo max</label>
              <input
                type="number"
                step="any"
                value={maxValue}
                onChange={(e) => setMaxValue(e.target.value)}
                className="input-field"
                placeholder={selectedModel.max_value?.toString() ?? ''}
              />
            </div>
          </div>
        )}

        <div className="bg-surface-container-low rounded-xl p-4 text-xs text-on-surface-variant">
          <p className="flex items-center gap-2">
            <span className="material-symbols-outlined text-sm text-amber-600">info</span>
            Sau khi gửi yêu cầu, Admin sẽ duyệt và cấp phát Feed Key Adafruit cho thiết bị.
          </p>
        </div>

        <div className="flex items-center gap-3 justify-end pt-2">
          <button type="button" onClick={onClose} className="btn-secondary" disabled={isLoading}>Hủy</button>
          <button type="submit" className="btn-primary" disabled={isLoading} id="add-device-submit">
            {isLoading ? (
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Đang gửi...
              </div>
            ) : 'Gửi yêu cầu'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
