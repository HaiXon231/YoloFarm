import type { DeviceWithModel } from '@/types'

interface SensorCardProps {
  device: DeviceWithModel
  realtimeValue?: { value: number; timestamp: string }
  isFlashing: boolean
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

export default function SensorCard({ device, realtimeValue, isFlashing }: SensorCardProps) {
  const icon = metricIcons[device.metric_type || ''] || 'sensors'
  const label = metricLabels[device.metric_type || ''] || 'Cảm biến'
  const unit = metricUnits[device.metric_type || ''] || ''
  const isOnline = device.connection_status === 'ONLINE'
  const value = realtimeValue?.value

  return (
    <div
      className={`card transition-all duration-300 ${isFlashing ? 'animate-flash-green ring-2 ring-primary-container' : ''}`}
    >
      <div className="flex justify-between items-start mb-4">
        <span className="label-text">{label}</span>
        <span className={`material-symbols-outlined p-2 rounded-xl ${
          isOnline
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
        <p className="text-xs text-on-surface-variant truncate mr-3">{device.name}</p>
        <span className={isOnline ? 'badge-online' : 'badge-offline'}>
          {isOnline ? 'Online' : 'Offline'}
        </span>
      </div>

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
