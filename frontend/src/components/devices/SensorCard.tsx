import { useState } from 'react'
import toast from 'react-hot-toast'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { DeviceWithModel } from '@/types'
import RenameDeviceModal from './RenameDeviceModal'
import ConfirmDialog from '@/components/ui/ConfirmDialog'

interface SensorCardProps {
  device: DeviceWithModel
  realtimeValue?: { value: number; timestamp: string }
  isFlashing: boolean
  onRenameSuccess?: () => void
}

const metricIcons: Record<string, string> = {
  TEMP: 'device_thermostat',
  HUMIDITY: 'humidity_mid',
  SOIL_MOISTURE: 'water_drop',
}

const metricLabels: Record<string, string> = {
  TEMP: 'Nhiệt độ',
  HUMIDITY: 'Độ ẩm',
  SOIL_MOISTURE: 'Độ ẩm đất',
}

const metricUnits: Record<string, string> = {
  TEMP: '°C',
  HUMIDITY: '%',
  SOIL_MOISTURE: '%',
}

export default function SensorCard({ device, realtimeValue, isFlashing, onRenameSuccess }: SensorCardProps) {
  const [isRenameOpen, setIsRenameOpen] = useState(false)
  const [isRemoveConfirmOpen, setIsRemoveConfirmOpen] = useState(false)
  const [isRemoving, setIsRemoving] = useState(false)
  const icon = metricIcons[device.metric_type || ''] || 'sensors'
  const label = metricLabels[device.metric_type || ''] || 'Cảm biến'
  const unit = metricUnits[device.metric_type || ''] || ''
  const isOnline = device.connection_status === 'ONLINE' || realtimeValue !== undefined
  const value = realtimeValue?.value

  const handleRequestRemoval = async () => {
    setIsRemoving(true)
    try {
      const response = await api.post<{ message: string }>(`/devices/${device.id}/remove-requests`)
      toast.success(response.data.message || 'Đã gửi yêu cầu gỡ bỏ thiết bị.')
      setIsRemoveConfirmOpen(false)
      onRenameSuccess?.()
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsRemoving(false)
    }
  }

  return (
    <div
      className={`card transition-all duration-300 ${isFlashing ? 'animate-flash-green ring-2 ring-primary-container' : ''}`}
    >
      <div className="flex justify-between items-start mb-4">
        <span className="label-text">{label}</span>
        <span className={`material-symbols-outlined p-2 rounded-xl ${isOnline
            ? 'text-primary bg-primary-container/20'
            : 'text-outline-variant bg-surface-container-high'
          }`}>
          {icon}
        </span>
      </div>

      <div className="flex items-baseline gap-2 mb-2">
        <h2 className="font-headline text-3xl font-extrabold text-on-surface">
          {value !== undefined ? `${value.toFixed(1)}` : '--'}
        </h2>
        <span className="text-lg text-on-surface-variant font-medium">{unit}</span>
      </div>

      <div className="flex items-center justify-between mt-3">
        <div className="flex items-center gap-1 group/name">
          <p className="text-xs text-on-surface-variant truncate max-w-[120px]">{device.name}</p>
          <button
            onClick={() => setIsRenameOpen(true)}
            className="p-1 rounded-md hover:bg-surface-container opacity-0 group-hover/name:opacity-100 transition-all"
            title="Đổi tên"
          >
            <span className="material-symbols-outlined text-xs text-on-surface-variant">edit</span>
          </button>
          <button
            onClick={() => setIsRemoveConfirmOpen(true)}
            className="p-1 rounded-md hover:bg-error-container/10 opacity-0 group-hover/name:opacity-100 transition-all"
            title="Yêu cầu thu hồi thiết bị"
          >
            <span className="material-symbols-outlined text-xs text-error">delete</span>
          </button>
        </div>
        <span className={isOnline ? 'badge-online' : 'badge-offline'}>
          {isOnline ? 'Online' : 'Offline'}
        </span>
      </div>

      <RenameDeviceModal
        isOpen={isRenameOpen}
        onClose={() => setIsRenameOpen(false)}
        deviceId={device.id}
        currentName={device.name}
        onSuccess={() => {
          setIsRenameOpen(false)
          onRenameSuccess?.()
        }}
      />

      <ConfirmDialog
        isOpen={isRemoveConfirmOpen}
        onClose={() => setIsRemoveConfirmOpen(false)}
        onConfirm={handleRequestRemoval}
        isLoading={isRemoving}
        title="Yêu cầu gỡ bỏ thiết bị"
        message={`Gửi yêu cầu gỡ bỏ thiết bị [${device.name}] tới Admin? Sau khi được duyệt, thiết bị và feed Adafruit tương ứng sẽ bị thu hồi.`}
        confirmText="Gửi yêu cầu"
        variant="danger"
      />

      {/* Mini sparkline bar */}
      {value !== undefined && (
        <div className="mt-4 w-full bg-surface-container-low h-1.5 rounded-full overflow-hidden">
          <div
            className="bg-primary h-full rounded-full transition-all duration-500"
            style={{ width: `${Math.min(Math.max(value, 0), 100)}%` }}
          />
        </div>
      )}
    </div>
  )
}
