import { useState, useEffect } from 'react'
import Modal from '@/components/ui/Modal'
import api, { getApiErrorMessage } from '@/lib/axios'
import toast from 'react-hot-toast'

interface ThresholdEditModalProps {
  isOpen: boolean
  onClose: () => void
  deviceId: string
  deviceName: string
  currentMin: number | null
  currentMax: number | null
  unit: string
  onSuccess: () => void
}

export default function ThresholdEditModal({
  isOpen,
  onClose,
  deviceId,
  deviceName,
  currentMin,
  currentMax,
  unit,
  onSuccess,
}: ThresholdEditModalProps) {
  const [minValue, setMinValue] = useState<string>(currentMin?.toString() ?? '')
  const [maxValue, setMaxValue] = useState<string>(currentMax?.toString() ?? '')
  const [isSaving, setIsSaving] = useState(false)

  useEffect(() => {
    if (isOpen) {
      setMinValue(currentMin?.toString() ?? '')
      setMaxValue(currentMax?.toString() ?? '')
    }
  }, [isOpen, currentMin, currentMax])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    const min = minValue === '' ? null : parseFloat(minValue)
    const max = maxValue === '' ? null : parseFloat(maxValue)

    if (min !== null && max !== null && min >= max) {
      toast.error('Giá trị min phải nhỏ hơn max')
      return
    }

    setIsSaving(true)
    try {
      await api.patch(`/devices/${deviceId}/threshold`, {
        minValue: min,
        maxValue: max,
      })
      toast.success('Cập nhật ngưỡng thành công!')
      onSuccess()
      onClose()
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Cấu hình ngưỡng cảm biến" maxWidth="max-w-sm">
      <form onSubmit={handleSubmit} className="space-y-5">
        <p className="text-sm text-on-surface-variant -mt-2">
          Đặt dải giá trị hợp lệ cho{' '}
          <span className="font-semibold text-on-surface">{deviceName}</span>
        </p>

        <div>
          <label htmlFor="threshold-min-input" className="label-text block mb-2">
            Giá trị tối thiểu{unit && <span className="text-on-surface-variant ml-1">({unit})</span>}
          </label>
          <input
            id="threshold-min-input"
            type="number"
            step="any"
            value={minValue}
            onChange={(e) => setMinValue(e.target.value)}
            placeholder="Không giới hạn"
            className="input-field"
          />
        </div>

        <div>
          <label htmlFor="threshold-max-input" className="label-text block mb-2">
            Giá trị tối đa{unit && <span className="text-on-surface-variant ml-1">({unit})</span>}
          </label>
          <input
            id="threshold-max-input"
            type="number"
            step="any"
            value={maxValue}
            onChange={(e) => setMaxValue(e.target.value)}
            placeholder="Không giới hạn"
            className="input-field"
          />
        </div>

        <div className="flex items-center gap-3 justify-end pt-2">
          <button
            type="button"
            onClick={onClose}
            className="btn-secondary"
            disabled={isSaving}
          >
            Hủy
          </button>
          <button
            type="submit"
            className="btn-primary"
            disabled={isSaving}
            id="threshold-save-button"
          >
            {isSaving ? 'Đang lưu...' : 'Lưu ngưỡng'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
