import { useState } from 'react'
import toast from 'react-hot-toast'
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine } from 'recharts'
import { format } from 'date-fns'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { DeviceWithModel, TelemetryDataPoint, AggregateInterval } from '@/types'
import LoadingSpinner from '@/components/ui/LoadingSpinner'
import EmptyState from '@/components/ui/EmptyState'

interface TelemetryTabProps {
  farmId: string
  devices: DeviceWithModel[]
}

export default function TelemetryTab({ devices }: TelemetryTabProps) {
  const chartDevices = devices.filter((d) => d.status === 'ACTIVE')
  const [selectedDeviceId, setSelectedDeviceId] = useState('')
  const [startTime, setStartTime] = useState(() => {
    const d = new Date(); d.setHours(d.getHours() - 24)
    return d.toISOString().slice(0, 16)
  })
  const [endTime, setEndTime] = useState(() => new Date().toISOString().slice(0, 16))
  const [aggregate, setAggregate] = useState<AggregateInterval>('')
  const [data, setData] = useState<TelemetryDataPoint[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [hasQueried, setHasQueried] = useState(false)

  const handleQuery = async () => {
    if (!selectedDeviceId) {
      toast.error('Vui lòng chọn một thiết bị')
      return
    }
    setIsLoading(true)
    setHasQueried(true)
    const formatDate = (dateStr: string) => {
      // Chuyển sang định dạng ISO Local (yyyy-MM-ddTHH:mm:ss) cho Backend
      return format(new Date(dateStr), "yyyy-MM-dd'T'HH:mm:ss")
    }

    try {
      const res = await api.get<TelemetryDataPoint[]>(`/devices/${selectedDeviceId}/telemetry`, {
        params: {
          start_time: formatDate(startTime),
          end_time: formatDate(endTime),
          ...(aggregate ? { aggregate } : {}),
        },
      })
      setData(res.data)
      if (res.data.length === 0) toast('Không có dữ liệu trong khoảng thời gian này', { icon: 'ℹ️' })
    } catch (error) {
      toast.error(getApiErrorMessage(error))
      setData([])
    } finally {
      setIsLoading(false)
    }
  }

  const selectedDevice = chartDevices.find((s) => s.id === selectedDeviceId)
  const isActuator = selectedDevice?.device_type === 'ACTUATOR'
  const metricUnit = selectedDevice?.display_unit || (selectedDevice?.metric_type === 'TEMP' ? '°C' : selectedDevice?.device_type === 'ACTUATOR' ? '' : '%')

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Filter Bar */}
      <div className="card">
        <h3 className="font-headline text-lg font-bold text-on-surface mb-5">Bộ lọc dữ liệu</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 items-end">
          <div>
            <label className="label-text block mb-2">Cảm biến *</label>
            <select
              value={selectedDeviceId}
              onChange={(e) => setSelectedDeviceId(e.target.value)}
              className="input-field"
              id="telemetry-sensor-select"
            >
              <option value="">-- Chọn thiết bị --</option>
              {chartDevices.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="label-text block mb-2">Từ ngày</label>
            <input
              type="datetime-local"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              className="input-field"
            />
          </div>
          <div>
            <label className="label-text block mb-2">Đến ngày</label>
            <input
              type="datetime-local"
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              className="input-field"
            />
          </div>
          <div>
            <label className="label-text block mb-2">Độ chia</label>
            <select
              value={aggregate}
              onChange={(e) => setAggregate(e.target.value as AggregateInterval)}
              className="input-field"
            >
              <option value="">Mặc định (Tất cả)</option>
              <option value="15m">15 phút</option>
              <option value="1h">1 giờ</option>
              <option value="1d">1 ngày</option>
            </select>
          </div>
          <button
            onClick={handleQuery}
            disabled={isLoading}
            className="btn-primary flex items-center justify-center gap-2"
            id="telemetry-query-btn"
          >
            <span className="material-symbols-outlined text-lg">show_chart</span>
            Xem biểu đồ
          </button>
        </div>
      </div>

      {/* Chart */}
      {isLoading ? (
        <LoadingSpinner text="Đang tải dữ liệu..." />
      ) : hasQueried && data.length > 0 && isActuator ? (
        <div className="card">
          <div className="flex items-center justify-between mb-5">
            <h3 className="font-headline text-lg font-bold text-on-surface">
              {selectedDevice?.name || 'Lịch sử hoạt động'}
            </h3>
            <span className="badge-active">{data.length} bản ghi</span>
          </div>
          <div className="divide-y divide-surface-container-low">
            {data.slice().reverse().map((point, index) => {
              const isOn = point.value > 0.5
              return (
                <div key={`${point.time}-${index}`} className="flex items-center justify-between py-3">
                  <div className="flex items-center gap-3">
                    <span className={`material-symbols-outlined ${isOn ? 'text-primary' : 'text-on-surface-variant'}`}>
                      {isOn ? 'toggle_on' : 'toggle_off'}
                    </span>
                    <span className="text-sm font-bold text-on-surface">{isOn ? 'ON' : 'OFF'}</span>
                  </div>
                  <span className="text-xs text-on-surface-variant">
                    {format(new Date(point.time), 'dd/MM/yyyy HH:mm:ss')}
                  </span>
                </div>
              )
            })}
          </div>
        </div>
      ) : hasQueried && data.length > 0 ? (
        <div className="card">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="font-headline text-lg font-bold text-on-surface">
                {selectedDevice?.name || 'Biểu đồ'}
              </h3>
              <p className="text-xs text-on-surface-variant font-label mt-1">
                Dữ liệu cảm biến theo thời gian
              </p>
            </div>
            <span className="badge-active">{data.length} điểm dữ liệu</span>
          </div>
          <div className="h-80">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={data} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#006947" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#006947" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e9eb" />
                <XAxis
                  dataKey="time"
                  tickFormatter={(t) => format(new Date(t), 'HH:mm')}
                  tick={{ fontSize: 11, fill: '#595c5e' }}
                  axisLine={{ stroke: '#e5e9eb' }}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fontSize: 11, fill: '#595c5e' }}
                  axisLine={false}
                  tickLine={false}
                  unit={metricUnit}
                />
                {selectedDevice?.min_value != null && (
                  <ReferenceLine y={selectedDevice.min_value} stroke="#b45309" strokeDasharray="5 5" label="Min" />
                )}
                {selectedDevice?.max_value != null && (
                  <ReferenceLine y={selectedDevice.max_value} stroke="#b91c1c" strokeDasharray="5 5" label="Max" />
                )}
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#fff',
                    border: '1px solid #e5e9eb',
                    borderRadius: '12px',
                    boxShadow: '0 10px 25px rgba(0,0,0,0.08)',
                    fontSize: '13px',
                  }}
                  labelFormatter={(t) => format(new Date(t), 'dd/MM/yyyy HH:mm')}
                  formatter={(value: number) => [`${value.toFixed(1)}${metricUnit}`, 'Giá trị']}
                />
                <Area
                  type="monotone"
                  dataKey="value"
                  stroke="#006947"
                  strokeWidth={2.5}
                  fillOpacity={1}
                  fill="url(#colorValue)"
                  dot={false}
                  activeDot={{ r: 6, fill: '#006947', stroke: '#fff', strokeWidth: 2 }}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
      ) : hasQueried && data.length === 0 ? (
        <EmptyState
          icon="show_chart"
          title="Không có dữ liệu"
          description="Cảm biến chưa ghi nhận dữ liệu trong khoảng thời gian đã chọn."
        />
      ) : (
        <EmptyState
          icon="query_stats"
          title="Thống kê cảm biến"
          description="Chọn cảm biến và khoảng thời gian, sau đó bấm 'Xem biểu đồ' để hiển thị dữ liệu."
        />
      )}
    </div>
  )
}
