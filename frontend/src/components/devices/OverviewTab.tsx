import { useState, useEffect, useCallback } from 'react'
import type { DeviceWithModel, DeviceModelResponse, TelemetryMessage } from '@/types'
import SensorCard from './SensorCard'
import ActuatorCard from './ActuatorCard'
import AddDeviceModal from './AddDeviceModal'
import { connectToFarm, disconnectWebSocket, type DeviceStatusEvent } from '@/lib/websocket'

interface OverviewTabProps {
  farmId: string
  devices: DeviceWithModel[]
  deviceModels: DeviceModelResponse[]
  onDevicesChange: () => void
  onDeviceEvents: (events: DeviceStatusEvent[]) => void
}

export default function OverviewTab({ farmId, devices, deviceModels, onDevicesChange, onDeviceEvents }: OverviewTabProps) {
  const [isAddDeviceOpen, setIsAddDeviceOpen] = useState(false)
  const [realtimeValues, setRealtimeValues] = useState<Record<string, { value: number; timestamp: string }>>({})
  const [activeInSession, setActiveInSession] = useState<Set<string>>(new Set())
  const [flashDeviceId, setFlashDeviceId] = useState<string | null>(null)
  const [wsConnected, setWsConnected] = useState(false)

  const sensors = devices.filter((d) => d.device_type === 'SENSOR' && d.status === 'ACTIVE')
  const actuators = devices.filter((d) => d.device_type === 'ACTUATOR' && d.status === 'ACTIVE')
  const pendingDevices = devices.filter((d) => d.status === 'PENDING')
  const pendingRemovalDevices = devices.filter((d) => d.status === 'PENDING_REMOVAL')

  const onlineCount = devices.filter((d) =>
    d.status === 'ACTIVE' && (d.connection_status === 'ONLINE' || activeInSession.has(d.id))
  ).length

  const handleTelemetry = useCallback((data: TelemetryMessage) => {
    setRealtimeValues((prev) => ({
      ...prev,
      [data.deviceId]: { value: data.value, timestamp: data.timestamp },
    }))
    setActiveInSession((prev) => new Set(prev).add(data.deviceId))
    setFlashDeviceId(data.deviceId)
    setTimeout(() => setFlashDeviceId(null), 600)
  }, [onDeviceEvents])

  // Nhận push event từ backend khi thiết bị ONLINE/OFFLINE — cập nhật state ngay, không cần refetch
  const handleDeviceStatus = useCallback((events: DeviceStatusEvent[]) => {
    onDeviceEvents(events)
    events.forEach((ev) => {
      setActiveInSession((prev) => {
        const next = new Set(prev)
        if (ev.connectionStatus === 'ONLINE') next.add(ev.deviceId)
        else if (ev.connectionStatus === 'OFFLINE') next.delete(ev.deviceId)
        return next
      })
    })
  }, [])

  useEffect(() => {
    setWsConnected(false)
    connectToFarm(
      farmId,
      handleTelemetry,
      () => setWsConnected(true),
      () => setWsConnected(false),
      () => setWsConnected(false),
      handleDeviceStatus
    )
    return () => disconnectWebSocket()
  }, [farmId, handleTelemetry, handleDeviceStatus])

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Devices Online */}
        <div className="card">
          <div className="flex justify-between items-start mb-4">
            <span className="label-text">Thiết bị trực tuyến</span>
            <div className="relative w-14 h-14">
              <svg className="w-full h-full -rotate-90">
                <circle className="text-surface-container-low" cx="28" cy="28" fill="transparent" r="22" stroke="currentColor" strokeWidth="5" />
                <circle className="text-primary" cx="28" cy="28" fill="transparent" r="22" stroke="currentColor"
                  strokeDasharray={`${(onlineCount / Math.max(devices.filter(d => d.status === 'ACTIVE').length, 1)) * 138} 138`}
                  strokeWidth="5" strokeLinecap="round"
                />
              </svg>
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="material-symbols-outlined text-primary text-lg">router</span>
              </div>
            </div>
          </div>
          <div className="flex items-baseline gap-1">
            <h2 className="font-headline text-3xl font-extrabold text-on-surface">{onlineCount}</h2>
            <span className="text-lg text-on-surface-variant font-medium">/{devices.filter(d => d.status === 'ACTIVE').length}</span>
          </div>
          <div className="flex items-center gap-1.5 mt-2">
            <span className={`w-2 h-2 rounded-full ${wsConnected ? 'bg-primary animate-pulse' : 'bg-outline-variant'}`} />
            <span className="text-xs font-medium text-on-surface-variant">
              {wsConnected ? 'WebSocket kết nối' : 'Đang kết nối...'}
            </span>
          </div>
        </div>

        {/* Sensors Count */}
        <div className="card">
          <div className="flex justify-between items-start mb-4">
            <span className="label-text">Cảm biến</span>
            <span className="material-symbols-outlined text-primary bg-primary-container/20 p-2 rounded-xl">sensors</span>
          </div>
          <h2 className="font-headline text-3xl font-extrabold text-on-surface">{sensors.length}</h2>
          <span className="text-xs text-on-surface-variant mt-1 block">Đang hoạt động</span>
        </div>

        {/* Actuators Count */}
        <div className="card">
          <div className="flex justify-between items-start mb-4">
            <span className="label-text">Thiết bị điều khiển</span>
            <span className="material-symbols-outlined text-tertiary bg-tertiary-container/20 p-2 rounded-xl">water_pump</span>
          </div>
          <h2 className="font-headline text-3xl font-extrabold text-on-surface">{actuators.length}</h2>
          <span className="text-xs text-on-surface-variant mt-1 block">Bơm / Đèn</span>
        </div>

        {/* Pending */}
        <div className="card">
          <div className="flex justify-between items-start mb-4">
            <span className="label-text">Chờ duyệt</span>
            <span className="material-symbols-outlined text-amber-600 bg-amber-100 p-2 rounded-xl">pending_actions</span>
          </div>
          <h2 className="font-headline text-3xl font-extrabold text-on-surface">{pendingDevices.length + pendingRemovalDevices.length}</h2>
          <span className="text-xs text-on-surface-variant mt-1 block">{pendingDevices.length} cấp phát, {pendingRemovalDevices.length} thu hồi</span>
        </div>
      </div>

      {/* Sensors Section */}
      {sensors.length > 0 && (
        <section>
          <h3 className="font-headline text-lg font-bold text-on-surface mb-4 flex items-center gap-2">
            <span className="material-symbols-outlined text-primary">sensors</span>
            Cảm biến
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {sensors.map((device) => (
              <SensorCard
                key={device.id}
                device={device}
                realtimeValue={realtimeValues[device.id]}
                isFlashing={flashDeviceId === device.id}
                onRenameSuccess={onDevicesChange}
              />
            ))}
          </div>
        </section>
      )}

      {/* Actuators Section */}
      {actuators.length > 0 && (
        <section>
          <h3 className="font-headline text-lg font-bold text-on-surface mb-4 flex items-center gap-2">
            <span className="material-symbols-outlined text-tertiary">water_pump</span>
            Thiết bị điều khiển
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {actuators.map((device) => (
              <ActuatorCard key={device.id} device={device} onUpdate={onDevicesChange} />
            ))}
          </div>
        </section>
      )}

      {/* Pending Devices */}
      {(pendingDevices.length > 0 || pendingRemovalDevices.length > 0) && (
        <section>
          <h3 className="font-headline text-lg font-bold text-on-surface mb-4 flex items-center gap-2">
            <span className="material-symbols-outlined text-amber-600">pending</span>
            Đang chờ duyệt
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {pendingDevices.map((device) => (
              <div key={device.id} className="card opacity-60">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center">
                    <span className="material-symbols-outlined text-amber-600">hourglass_top</span>
                  </div>
                  <div>
                    <p className="font-semibold text-on-surface">{device.name}</p>
                    <p className="text-xs text-on-surface-variant">{device.model_name || 'Chờ Admin duyệt'}</p>
                  </div>
                  <span className="badge-pending ml-auto">Pending</span>
                </div>
              </div>
            ))}
            {pendingRemovalDevices.map((device) => (
              <div key={device.id} className="card opacity-70 border border-rose-200">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-rose-100 flex items-center justify-center">
                    <span className="material-symbols-outlined text-rose-600">delete</span>
                  </div>
                  <div>
                    <p className="font-semibold text-on-surface">{device.name}</p>
                    <p className="text-xs text-on-surface-variant">Yêu cầu gỡ bỏ đang chờ Admin duyệt</p>
                  </div>
                  <span className="ml-auto px-3 py-1 rounded-full text-xs font-semibold bg-rose-100 text-rose-700">
                    Pending Removal
                  </span>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Add Device Button */}
      <button
        onClick={() => setIsAddDeviceOpen(true)}
        className="fixed bottom-8 right-8 w-14 h-14 rounded-full btn-primary-gradient text-white shadow-2xl flex items-center justify-center hover:scale-110 active:scale-95 transition-transform z-30"
        id="add-device-fab"
      >
        <span className="material-symbols-outlined text-2xl">add</span>
      </button>

      <AddDeviceModal
        isOpen={isAddDeviceOpen}
        onClose={() => setIsAddDeviceOpen(false)}
        farmId={farmId}
        deviceModels={deviceModels}
        onSuccess={() => { setIsAddDeviceOpen(false); onDevicesChange() }}
      />
    </div>
  )
}
