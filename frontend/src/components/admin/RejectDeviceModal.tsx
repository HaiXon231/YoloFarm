import { useState } from 'react'
import toast from 'react-hot-toast'
import Modal from '@/components/ui/Modal'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { DeviceResponse } from '@/types'

interface RejectDeviceModalProps {
  device: DeviceResponse | null
  onClose: () => void
  onSuccess: () => void
}

export default function RejectDeviceModal({ device, onClose, onSuccess }: RejectDeviceModalProps) {
  const [reason, setReason] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!device || !reason.trim()) return

    setIsLoading(true)
    try {
      await api.post(`/admin/devices/${device.id}/reject`, { reject_reason: reason })
      toast.success('Đã từ chối yêu cầu và gửi thông báo cho Nông dân.')
      setReason('')
      onSuccess()
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Modal isOpen={!!device} onClose={onClose} title="Từ chối yêu cầu thiết bị">
      {device && (
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="bg-error-container/5 border border-error/10 rounded-xl p-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-error-container/20 flex items-center justify-center">
                <span className="material-symbols-outlined text-error">devices</span>
              </div>
              <div>
                <p className="font-bold text-on-surface">{device.name}</p>
                <p className="text-xs text-on-surface-variant">Từ chối cấp phát</p>
              </div>
            </div>
          </div>

          <div>
            <label htmlFor="reject-reason" className="label-text block mb-2">Lý do từ chối *</label>
            <textarea
              id="reject-reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="input-field min-h-[100px] resize-y"
              placeholder="VD: Vượt quá số lượng thiết bị của gói cước hiện tại."
              required
            />
          </div>

          <div className="flex items-center gap-3 justify-end pt-2">
            <button type="button" onClick={onClose} className="btn-secondary" disabled={isLoading}>Hủy</button>
            <button type="submit" className="btn-danger" disabled={isLoading} id="reject-device-submit">
              {isLoading ? (
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Đang xử lý...
                </div>
              ) : 'Xác nhận từ chối'}
            </button>
          </div>
        </form>
      )}
    </Modal>
  )
}
