import { useState, useEffect } from 'react'
import Modal from '@/components/ui/Modal'
import api, { getApiErrorMessage } from '@/lib/axios'
import toast from 'react-hot-toast'

interface RenameDeviceModalProps {
  isOpen: boolean
  onClose: () => void
  deviceId: string
  currentName: string
  onSuccess: () => void
}

export default function RenameDeviceModal({ 
  isOpen, 
  onClose, 
  deviceId, 
  currentName, 
  onSuccess 
}: RenameDeviceModalProps) {
  const [name, setName] = useState(currentName)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    setName(currentName)
  }, [currentName, isOpen])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) {
      toast.error('Tên thiết bị không được để trống')
      return
    }
    
    setIsLoading(true)
    try {
      await api.patch(`/devices/${deviceId}`, { name: name.trim() })
      toast.success('Đổi tên thiết bị thành công!')
      onSuccess()
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Đổi tên thiết bị">
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label htmlFor="device-name" className="label-text block mb-2">Tên thiết bị mới</label>
          <input
            id="device-name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="input-field"
            placeholder="VD: Cảm biến độ ẩm Zone A"
            autoFocus
            required
          />
        </div>
        
        <div className="flex items-center gap-3 justify-end pt-4">
          <button type="button" onClick={onClose} className="btn-secondary" disabled={isLoading}>
            Hủy
          </button>
          <button type="submit" className="btn-primary" disabled={isLoading} id="rename-device-submit">
            {isLoading ? 'Đang lưu...' : 'Lưu thay đổi'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
