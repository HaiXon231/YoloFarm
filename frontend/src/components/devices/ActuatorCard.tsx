import { useState } from 'react'
import toast from 'react-hot-toast'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { DeviceWithModel } from '@/types'
import RenameDeviceModal from './RenameDeviceModal'

interface ActuatorCardProps {
  device: DeviceWithModel
  onUpdate: () => void
}

export default function ActuatorCard({ device, onUpdate }: ActuatorCardProps) {
  const [isModeLoading, setIsModeLoading] = useState(false)
  const [isCommandLoading, setIsCommandLoading] = useState(false)
  const [localMode, setLocalMode] = useState(device.operating_mode)
  const [isOn, setIsOn] = useState(device.is_active)
  const [isRenameOpen, setIsRenameOpen] = useState(false)

  const isOnline = device.connection_status === 'ONLINE'
  const isAuto = localMode === 'AUTO'
  const icon = device.metric_type === 'PUMP' ? 'water_pump' : 'light_mode'

  const handleModeToggle = async () => {
    const newMode = isAuto ? 'MANUAL' : 'AUTO'
    setIsModeLoading(true)
    try {
      await api.patch(`/devices/${device.id}/mode`, { operating_mode: newMode })
      setLocalMode(newMode)
      toast.success(`Đã chuyển sang chế độ ${newMode === 'AUTO' ? 'Tự động' : 'Thủ công'}`)
      onUpdate()
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsModeLoading(false)
    }
  }

  const handleCommand = async (command: 'ON' | 'OFF') => {
    if (isAuto) {
      toast.error('Vui lòng chuyển sang Thủ Công (MANUAL) trước khi tự điều khiển.', { icon: '⚠️' })
      return
    }
    setIsCommandLoading(true)
    try {
      await api.post(`/devices/${device.id}/command`, { command })
      setIsOn(command === 'ON')
      toast.success(`Lệnh [${command}] đã được gửi tới thiết bị thành công.`)
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsCommandLoading(false)
    }
  }

  return (
    <div className="card">
      <div className="flex items-start justify-between mb-4">
        <div className="flex items-center gap-3">
          <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${isOn ? 'bg-primary-container/30' : 'bg-surface-container-high'
            }`}>
            <span className={`material-symbols-outlined text-xl ${isOn ? 'text-primary' : 'text-on-surface-variant'}`}>
              {icon}
            </span>
          </div>
          <div>
            <div className="flex items-center gap-1 group/name">
              <p className="font-bold text-on-surface truncate max-w-[140px]">{device.name}</p>
              <button
                onClick={() => setIsRenameOpen(true)}
                className="p-1 rounded-md hover:bg-surface-container opacity-0 group-hover/name:opacity-100 transition-all"
                title="Đổi tên"
              >
                <span className="material-symbols-outlined text-xs text-on-surface-variant">edit</span>
              </button>
            </div>
            <div className="flex items-center gap-2 mt-0.5">
              <span className={isOnline ? 'badge-online' : 'badge-offline'}>
                {isOnline ? 'Online' : 'Offline'}
              </span>
              <span className={isAuto ? 'badge-auto' : 'badge-manual'}>
                {isAuto ? 'Auto' : 'Manual'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Mode Toggle */}
      <div className="flex items-center justify-between py-3 px-4 bg-surface-container-low rounded-xl mb-3">
        <span className="text-sm font-medium text-on-surface-variant">Chế độ</span>
        <div className="flex items-center gap-3">
          <span className="text-xs font-semibold text-on-surface-variant">
            {isAuto ? 'Tự động' : 'Thủ công'}
          </span>
          <button
            onClick={handleModeToggle}
            disabled={isModeLoading}
            className="relative"
          >
            <div className={`w-11 h-6 rounded-full transition-colors duration-300 ${isAuto ? 'bg-primary' : 'bg-outline-variant'
              }`}>
              <div className={`absolute top-[2px] w-5 h-5 bg-white rounded-full shadow-md transition-transform duration-300 ${isAuto ? 'translate-x-[22px]' : 'translate-x-[2px]'
                }`} />
            </div>
          </button>
        </div>
      </div>

      {/* Command Buttons */}
      <div className="flex gap-2">
        <button
          onClick={() => handleCommand('ON')}
          disabled={isCommandLoading || isAuto}
          className={`flex-1 py-2.5 rounded-xl font-bold text-sm flex items-center justify-center gap-1.5 transition-all duration-200 ${isAuto
              ? 'bg-surface-container-low text-outline-variant cursor-not-allowed'
              : isOn
                ? 'bg-primary text-white shadow-md'
                : 'bg-surface-container-low text-on-surface hover:bg-primary hover:text-white'
            }`}
          title={isAuto ? 'Chuyển sang MANUAL để điều khiển' : ''}
        >
          <span className="material-symbols-outlined text-lg">power_settings_new</span>
          BẬT
        </button>
        <button
          onClick={() => handleCommand('OFF')}
          disabled={isCommandLoading || isAuto}
          className={`flex-1 py-2.5 rounded-xl font-bold text-sm flex items-center justify-center gap-1.5 transition-all duration-200 ${isAuto
              ? 'bg-surface-container-low text-outline-variant cursor-not-allowed'
              : !isOn
                ? 'bg-on-surface text-white shadow-md'
                : 'bg-surface-container-low text-on-surface hover:bg-on-surface hover:text-white'
            }`}
          title={isAuto ? 'Chuyển sang MANUAL để điều khiển' : ''}
        >
          <span className="material-symbols-outlined text-lg">power_off</span>
          TẮT
        </button>
      </div>

      <RenameDeviceModal
        isOpen={isRenameOpen}
        onClose={() => setIsRenameOpen(false)}
        deviceId={device.id}
        currentName={device.name}
        onSuccess={() => {
          setIsRenameOpen(false)
          onUpdate()
        }}
      />
    </div>
  )
}
