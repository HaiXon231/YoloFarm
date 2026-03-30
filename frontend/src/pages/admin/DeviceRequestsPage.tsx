import { useState, useEffect } from 'react'
import toast from 'react-hot-toast'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { DeviceResponse } from '@/types'
import ApproveDeviceModal from '@/components/admin/ApproveDeviceModal'
import RejectDeviceModal from '@/components/admin/RejectDeviceModal'
import LoadingSpinner from '@/components/ui/LoadingSpinner'
import EmptyState from '@/components/ui/EmptyState'

export default function DeviceRequestsPage() {
  const [requests, setRequests] = useState<DeviceResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [approveDevice, setApproveDevice] = useState<DeviceResponse | null>(null)
  const [rejectDevice, setRejectDevice] = useState<DeviceResponse | null>(null)

  const fetchRequests = async () => {
    try {
      const res = await api.get<DeviceResponse[]>('/admin/devices/requests', {
        params: { status: 'PENDING' },
      })
      setRequests(res.data)
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { fetchRequests() }, [])

  if (isLoading) return <LoadingSpinner size="lg" text="Đang tải yêu cầu thiết bị..." />

  return (
    <div className="animate-fade-in">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-headline text-2xl font-bold text-on-surface">Yêu cầu cấp phát thiết bị</h1>
          <p className="text-sm text-on-surface-variant mt-1 font-label">
            Duyệt và cấp Feed Key Adafruit cho Nông dân
          </p>
        </div>
        <div className="flex items-center gap-2 px-4 py-2 bg-amber-50 rounded-full">
          <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
          <span className="text-xs font-semibold text-amber-700">{requests.length} đang chờ</span>
        </div>
      </div>

      {requests.length === 0 ? (
        <EmptyState
          icon="check_circle"
          title="Không có yêu cầu nào"
          description="Tất cả yêu cầu cấp phát thiết bị đã được xử lý."
        />
      ) : (
        <div className="card overflow-hidden p-0">
          <table className="w-full">
            <thead>
              <tr className="border-b border-surface-container-low">
                <th className="text-left px-6 py-4 label-text">Tên thiết bị</th>
                <th className="text-left px-6 py-4 label-text">Nông trại</th>
                <th className="text-left px-6 py-4 label-text">Trạng thái</th>
                <th className="text-right px-6 py-4 label-text">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {requests.map((device) => (
                <tr key={device.id} className="border-b border-surface-container-low/50 hover:bg-surface-container-low/30 transition-colors">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center">
                        <span className="material-symbols-outlined text-amber-600">devices</span>
                      </div>
                      <div>
                        <p className="font-semibold text-on-surface text-sm">{device.name}</p>
                        <p className="text-xs text-on-surface-variant">ID: {device.id.slice(0, 8)}...</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-sm text-on-surface-variant">
                    {device.farm_id?.slice(0, 8)}...
                  </td>
                  <td className="px-6 py-4">
                    <span className="badge-pending">Pending</span>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2 justify-end">
                      <button
                        onClick={() => setApproveDevice(device)}
                        className="px-4 py-2 bg-primary text-white text-sm font-bold rounded-xl hover:bg-primary-dim transition-colors flex items-center gap-1"
                      >
                        <span className="material-symbols-outlined text-sm">check</span>
                        Duyệt
                      </button>
                      <button
                        onClick={() => setRejectDevice(device)}
                        className="px-4 py-2 bg-error text-white text-sm font-bold rounded-xl hover:bg-error-dim transition-colors flex items-center gap-1"
                      >
                        <span className="material-symbols-outlined text-sm">close</span>
                        Từ chối
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ApproveDeviceModal
        device={approveDevice}
        onClose={() => setApproveDevice(null)}
        onSuccess={() => { setApproveDevice(null); fetchRequests() }}
      />
      <RejectDeviceModal
        device={rejectDevice}
        onClose={() => setRejectDevice(null)}
        onSuccess={() => { setRejectDevice(null); fetchRequests() }}
      />
    </div>
  )
}
