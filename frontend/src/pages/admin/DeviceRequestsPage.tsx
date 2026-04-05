import { useState, useEffect } from 'react'
import toast from 'react-hot-toast'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { DeviceResponse } from '@/types'
import ApproveDeviceModal from '@/components/admin/ApproveDeviceModal'
import RejectDeviceModal from '@/components/admin/RejectDeviceModal'
import ConfirmDialog from '@/components/ui/ConfirmDialog'
import LoadingSpinner from '@/components/ui/LoadingSpinner'
import EmptyState from '@/components/ui/EmptyState'

export default function DeviceRequestsPage() {
  const [requests, setRequests] = useState<DeviceResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [approveDevice, setApproveDevice] = useState<DeviceResponse | null>(null)
  const [rejectDevice, setRejectDevice] = useState<DeviceResponse | null>(null)
  const [approveRemovalDevice, setApproveRemovalDevice] = useState<DeviceResponse | null>(null)
  const [isApprovingRemoval, setIsApprovingRemoval] = useState(false)

  const fetchRequests = async () => {
    try {
      const res = await api.get<DeviceResponse[]>('/admin/devices/requests')
      setRequests(res.data)
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { fetchRequests() }, [])

  const pendingProvisionCount = requests.filter((item) => item.status === 'PENDING').length
  const pendingRemovalCount = requests.filter((item) => item.status === 'PENDING_REMOVAL').length

  const handleApproveRemoval = async () => {
    if (!approveRemovalDevice) return

    setIsApprovingRemoval(true)
    try {
      await api.post(`/admin/devices/${approveRemovalDevice.id}/approve`)
      toast.success('Đã duyệt thu hồi thiết bị và xóa feed Adafruit tương ứng.')
      setApproveRemovalDevice(null)
      fetchRequests()
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsApprovingRemoval(false)
    }
  }

  if (isLoading) return <LoadingSpinner size="lg" text="Đang tải yêu cầu thiết bị..." />

  return (
    <div className="animate-fade-in">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-headline text-2xl font-bold text-on-surface">Yêu cầu thiết bị</h1>
          <p className="text-sm text-on-surface-variant mt-1 font-label">
            Duyệt cấp phát mới hoặc thu hồi thiết bị theo yêu cầu từ Nông dân
          </p>
        </div>
        <div className="flex items-center gap-2">
          <div className="px-4 py-2 bg-amber-50 rounded-full text-xs font-semibold text-amber-700">
            + Cấp phát: {pendingProvisionCount}
          </div>
          <div className="px-4 py-2 bg-rose-50 rounded-full text-xs font-semibold text-rose-700">
            - Thu hồi: {pendingRemovalCount}
          </div>
        </div>
      </div>

      {requests.length === 0 ? (
        <EmptyState
          icon="check_circle"
          title="Không có yêu cầu nào"
          description="Tất cả yêu cầu cấp phát và thu hồi thiết bị đã được xử lý."
        />
      ) : (
        <div className="card overflow-hidden p-0">
          <table className="w-full">
            <thead>
              <tr className="border-b border-surface-container-low">
                <th className="text-left px-6 py-4 label-text">Tên thiết bị</th>
                <th className="text-left px-6 py-4 label-text">Nông trại</th>
                <th className="text-left px-6 py-4 label-text">Loại yêu cầu</th>
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
                  <td className="px-6 py-4 text-sm text-on-surface-variant">
                    {device.status === 'PENDING_REMOVAL' ? 'Thu hồi thiết bị' : 'Cấp phát thiết bị'}
                  </td>
                  <td className="px-6 py-4">
                    {device.status === 'PENDING_REMOVAL' ? (
                      <span className="px-3 py-1 rounded-full text-xs font-semibold bg-rose-100 text-rose-700">
                        Pending Removal
                      </span>
                    ) : (
                      <span className="badge-pending">Pending</span>
                    )}
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2 justify-end">
                      {device.status === 'PENDING_REMOVAL' ? (
                        <button
                          onClick={() => setApproveRemovalDevice(device)}
                          className="px-4 py-2 bg-rose-600 text-white text-sm font-bold rounded-xl hover:bg-rose-700 transition-colors flex items-center gap-1"
                        >
                          <span className="material-symbols-outlined text-sm">delete</span>
                          Duyệt thu hồi
                        </button>
                      ) : (
                        <button
                          onClick={() => setApproveDevice(device)}
                          className="px-4 py-2 bg-primary text-white text-sm font-bold rounded-xl hover:bg-primary-dim transition-colors flex items-center gap-1"
                        >
                          <span className="material-symbols-outlined text-sm">check</span>
                          Duyệt
                        </button>
                      )}
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

      <ConfirmDialog
        isOpen={!!approveRemovalDevice}
        onClose={() => setApproveRemovalDevice(null)}
        onConfirm={handleApproveRemoval}
        isLoading={isApprovingRemoval}
        title="Duyệt thu hồi thiết bị"
        message={approveRemovalDevice
          ? `Bạn có chắc muốn thu hồi thiết bị [${approveRemovalDevice.name}]? Hệ thống sẽ xóa feed Adafruit tương ứng và xóa thiết bị khỏi hệ thống.`
          : ''}
        confirmText="Xác nhận thu hồi"
        variant="danger"
      />
    </div>
  )
}
