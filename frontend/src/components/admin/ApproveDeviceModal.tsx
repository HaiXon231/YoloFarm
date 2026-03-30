import { useState } from 'react'
import toast from 'react-hot-toast'
import Modal from '@/components/ui/Modal'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { DeviceResponse } from '@/types'

interface ApproveDeviceModalProps {
  device: DeviceResponse | null
  onClose: () => void
  onSuccess: () => void
}

export default function ApproveDeviceModal({ device, onClose, onSuccess }: ApproveDeviceModalProps) {
  const [feedKey, setFeedKey] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!device || !feedKey.trim()) return

    setIsLoading(true)
    try {
      await api.post(`/admin/devices/${device.id}/approve`, { adafruit_feed_key: feedKey })
      toast.success('Cấp phát thành công! Thiết bị đã được kích hoạt.')
      setFeedKey('')
      onSuccess()
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Modal isOpen={!!device} onClose={onClose} title="Duyệt cấp phát thiết bị">
      {device && (
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="bg-surface-container-low rounded-xl p-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-xl bg-primary-container/20 flex items-center justify-center">
                <span className="material-symbols-outlined text-primary">devices</span>
              </div>
              <div>
                <p className="font-bold text-on-surface">{device.name}</p>
                <p className="text-xs text-on-surface-variant">ID: {device.id}</p>
              </div>
            </div>
          </div>

          <div>
            <label htmlFor="feed-key" className="label-text block mb-2">Adafruit Feed Key *</label>
            <input
              id="feed-key"
              type="text"
              value={feedKey}
              onChange={(e) => setFeedKey(e.target.value)}
              className="input-field"
              placeholder="VD: yolofarm/feeds/pump-01"
              required
            />
            <p className="text-xs text-on-surface-variant mt-2">
              Tạo Feed trên Adafruit IO trước, rồi paste tên Feed vào đây.
            </p>
          </div>

          <div className="flex items-center gap-3 justify-end pt-2">
            <button type="button" onClick={onClose} className="btn-secondary" disabled={isLoading}>Hủy</button>
            <button type="submit" className="btn-primary" disabled={isLoading} id="approve-device-submit">
              {isLoading ? (
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Đang xử lý...
                </div>
              ) : 'Xác nhận duyệt'}
            </button>
          </div>
        </form>
      )}
    </Modal>
  )
}
