import { useState, useEffect } from 'react'
import toast from 'react-hot-toast'
import Modal from '@/components/ui/Modal'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { FarmResponse } from '@/types'

interface FarmFormModalProps {
  isOpen: boolean
  onClose: () => void
  farm: FarmResponse | null
  onSuccess: () => void
}

export default function FarmFormModal({ isOpen, onClose, farm, onSuccess }: FarmFormModalProps) {
  const [name, setName] = useState('')
  const [location, setLocation] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const isEditing = !!farm

  useEffect(() => {
    if (farm) {
      setName(farm.name)
      setLocation(farm.location || '')
    } else {
      setName('')
      setLocation('')
    }
  }, [farm, isOpen])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) {
      toast.error('Tên nông trại không được để trống')
      return
    }
    setIsLoading(true)
    try {
      if (isEditing) {
        await api.put(`/farms/${farm.id}`, { name, location })
        toast.success('Cập nhật nông trại thành công!')
      } else {
        await api.post('/farms', { name, location })
        toast.success('Tạo nông trại mới thành công!')
      }
      onSuccess()
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEditing ? 'Chỉnh sửa nông trại' : 'Tạo nông trại mới'}>
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="farm-name" className="label-text block mb-2">Tên nông trại *</label>
          <input
            id="farm-name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="input-field"
            placeholder="VD: Vườn Cà Chua Thủy Canh"
            required
          />
        </div>
        <div>
          <label htmlFor="farm-location" className="label-text block mb-2">Vị trí</label>
          <input
            id="farm-location"
            type="text"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            className="input-field"
            placeholder="VD: Khu nhà màng A"
          />
        </div>
        <div className="flex items-center gap-3 justify-end pt-4">
          <button type="button" onClick={onClose} className="btn-secondary" disabled={isLoading}>Hủy</button>
          <button type="submit" className="btn-primary" disabled={isLoading} id="farm-form-submit">
            {isLoading ? (
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                {isEditing ? 'Đang lưu...' : 'Đang tạo...'}
              </div>
            ) : (
              isEditing ? 'Lưu thay đổi' : 'Tạo nông trại'
            )}
          </button>
        </div>
      </form>
    </Modal>
  )
}
