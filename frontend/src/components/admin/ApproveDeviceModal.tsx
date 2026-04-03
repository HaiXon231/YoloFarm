import { useState } from 'react'
import toast from 'react-hot-toast'
import Modal from '@/components/ui/Modal'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { DeviceResponse } from '@/types'

const FEED_KEY_PATTERN = /^[a-z0-9-]{1,64}$/

function normalizeFeedKeyInput(rawInput: string): string {
  let cleaned = rawInput.trim()
  const marker = '/feeds/'
  const markerIndex = cleaned.toLowerCase().indexOf(marker)
  if (markerIndex >= 0) {
    cleaned = cleaned.substring(markerIndex + marker.length)
  }

  cleaned = cleaned.trim()
  if (cleaned.endsWith('/')) {
    cleaned = cleaned.substring(0, cleaned.length - 1).trim()
  }

  return cleaned.toLowerCase()
}

interface ApproveDeviceModalProps {
  device: DeviceResponse | null
  onClose: () => void
  onSuccess: () => void
}

export default function ApproveDeviceModal({ device, onClose, onSuccess }: ApproveDeviceModalProps) {
  const [feedKey, setFeedKey] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const normalizedFeedKey = normalizeFeedKeyInput(feedKey)
  const isFeedKeyValid = FEED_KEY_PATTERN.test(normalizedFeedKey)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!device || !feedKey.trim()) return

    if (!isFeedKeyValid) {
      toast.error("Feed key phải đúng chuẩn Adafruit: chỉ gồm a-z, 0-9 và dấu '-' (1-64 ký tự).")
      return
    }

    setIsLoading(true)
    try {
      await api.post(`/admin/devices/${device.id}/approve`, { adafruit_feed_key: normalizedFeedKey })
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
              className={`input-field ${feedKey.trim() && !isFeedKeyValid ? '!border-red-500 !ring-red-500/20' : ''}`}
              placeholder="VD: yolofarm/feeds/pump-01"
              required
            />
            {feedKey.trim() && !isFeedKeyValid ? (
              <p className="text-xs text-red-500 mt-2">
                Feed key không hợp lệ. Chỉ dùng a-z, 0-9, dấu '-', độ dài 1-64 ký tự.
              </p>
            ) : (
              <p className="text-xs text-on-surface-variant mt-2">
                Có thể paste dạng đầy đủ như username/feeds/pump-01. Hệ thống sẽ tự lấy phần key cuối.
              </p>
            )}
          </div>

          <div className="flex items-center gap-3 justify-end pt-2">
            <button type="button" onClick={onClose} className="btn-secondary" disabled={isLoading}>Hủy</button>
            <button type="submit" className="btn-primary" disabled={isLoading || !isFeedKeyValid} id="approve-device-submit">
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
