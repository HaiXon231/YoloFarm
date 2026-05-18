import { useState } from 'react'
import toast from 'react-hot-toast'
import Modal from '@/components/ui/Modal'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { DeviceType, MetricType } from '@/types'

interface ModelFormModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
}

export default function ModelFormModal({ isOpen, onClose, onSuccess }: ModelFormModalProps) {
  const [modelName, setModelName] = useState('')
  const [deviceType, setDeviceType] = useState<DeviceType>('SENSOR')
  const [metricType, setMetricType] = useState<MetricType>('TEMP')
  const [manufacturer, setManufacturer] = useState('')
  const [displayUnit, setDisplayUnit] = useState('')
  const [minValue, setMinValue] = useState('')
  const [maxValue, setMaxValue] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const sensorMetrics: MetricType[] = ['TEMP', 'HUMIDITY', 'SOIL_MOISTURE', 'LIGHT', 'PRESSURE', 'CO2']
  const actuatorMetrics: MetricType[] = ['PUMP', 'VALVE', 'RELAY', 'FAN']

  const metricOptions = deviceType === 'SENSOR' ? sensorMetrics : actuatorMetrics

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)
    try {
      await api.post('/admin/device-models', {
        model_name: modelName,
        device_type: deviceType,
        metric_type: metricType,
        manufacturer: manufacturer || undefined,
        display_unit: displayUnit || undefined,
        min_value: minValue === '' ? null : Number(minValue),
        max_value: maxValue === '' ? null : Number(maxValue),
      })
      toast.success('Thêm khuôn mẫu thiết bị thành công!')
      setModelName('')
      setDeviceType('SENSOR')
      setMetricType('TEMP')
      setManufacturer('')
      setDisplayUnit('')
      setMinValue('')
      setMaxValue('')
      onSuccess()
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Thêm khuôn mẫu thiết bị">
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="label-text block mb-2">Tên mẫu *</label>
          <input
            type="text"
            value={modelName}
            onChange={(e) => setModelName(e.target.value)}
            className="input-field"
            placeholder="VD: Cảm biến độ ẩm đất v1"
            required
          />
        </div>

        <div>
          <label className="label-text block mb-3">Loại thiết bị *</label>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => { setDeviceType('SENSOR'); setMetricType('TEMP') }}
              className={`flex-1 py-3 rounded-xl font-bold text-sm transition-all ${
                deviceType === 'SENSOR' ? 'bg-primary text-white' : 'bg-surface-container-low text-on-surface-variant'
              }`}
            >
              <span className="material-symbols-outlined text-sm align-middle mr-1">sensors</span>
              Cảm biến
            </button>
            <button
              type="button"
              onClick={() => { setDeviceType('ACTUATOR'); setMetricType('PUMP') }}
              className={`flex-1 py-3 rounded-xl font-bold text-sm transition-all ${
                deviceType === 'ACTUATOR' ? 'bg-tertiary text-white' : 'bg-surface-container-low text-on-surface-variant'
              }`}
            >
              <span className="material-symbols-outlined text-sm align-middle mr-1">water_pump</span>
              Điều khiển
            </button>
          </div>
        </div>

        <div>
          <label className="label-text block mb-2">Loại chỉ số *</label>
          <select
            value={metricType}
            onChange={(e) => setMetricType(e.target.value as MetricType)}
            className="input-field"
          >
            {metricOptions.map((m) => (
              <option key={m} value={m}>{m}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="label-text block mb-2">Nhà sản xuất</label>
          <input
            type="text"
            value={manufacturer}
            onChange={(e) => setManufacturer(e.target.value)}
            className="input-field"
            placeholder="VD: AgriTech VN"
          />
        </div>

        <div className="grid grid-cols-3 gap-3">
          <div>
            <label className="label-text block mb-2">Đơn vị</label>
            <input
              type="text"
              value={displayUnit}
              onChange={(e) => setDisplayUnit(e.target.value)}
              className="input-field"
              placeholder="%, C, lux"
            />
          </div>
          <div>
            <label className="label-text block mb-2">Min khuyến nghị</label>
            <input
              type="number"
              step="any"
              value={minValue}
              onChange={(e) => setMinValue(e.target.value)}
              className="input-field"
            />
          </div>
          <div>
            <label className="label-text block mb-2">Max khuyến nghị</label>
            <input
              type="number"
              step="any"
              value={maxValue}
              onChange={(e) => setMaxValue(e.target.value)}
              className="input-field"
            />
          </div>
        </div>

        <div className="flex items-center gap-3 justify-end pt-2">
          <button type="button" onClick={onClose} className="btn-secondary" disabled={isLoading}>Hủy</button>
          <button type="submit" className="btn-primary" disabled={isLoading} id="model-form-submit">
            {isLoading ? (
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Đang lưu...
              </div>
            ) : 'Thêm khuôn mẫu'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
