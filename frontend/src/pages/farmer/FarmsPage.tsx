import { useState, useEffect } from 'react'
import toast from 'react-hot-toast'
import api, { getApiErrorMessage, getApiErrorCode } from '@/lib/axios'
import type { FarmResponse } from '@/types'
import FarmCard from '@/components/farms/FarmCard'
import FarmFormModal from '@/components/farms/FarmFormModal'
import DeleteConfirmModal from '@/components/farms/DeleteConfirmModal'
import EmptyState from '@/components/ui/EmptyState'
import LoadingSpinner from '@/components/ui/LoadingSpinner'

export default function FarmsPage() {
  const [farms, setFarms] = useState<FarmResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [isDeleteOpen, setIsDeleteOpen] = useState(false)
  const [selectedFarm, setSelectedFarm] = useState<FarmResponse | null>(null)

  const fetchFarms = async () => {
    try {
      const res = await api.get<FarmResponse[]>('/farms')
      setFarms(res.data)
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { fetchFarms() }, [])

  const handleCreate = () => {
    setSelectedFarm(null)
    setIsModalOpen(true)
  }

  const handleEdit = (farm: FarmResponse) => {
    setSelectedFarm(farm)
    setIsModalOpen(true)
  }

  const handleDelete = (farm: FarmResponse) => {
    setSelectedFarm(farm)
    setIsDeleteOpen(true)
  }

  const handleFormSuccess = () => {
    setIsModalOpen(false)
    setSelectedFarm(null)
    fetchFarms()
  }

  const handleDeleteConfirm = async () => {
    if (!selectedFarm) return
    try {
      await api.delete(`/farms/${selectedFarm.id}`)
      toast.success('Đã xóa nông trại thành công!')
      setIsDeleteOpen(false)
      setSelectedFarm(null)
      fetchFarms()
    } catch (error) {
      const code = getApiErrorCode(error)
      if (code === 409) {
        toast.error('Không thể xóa: Nông trại hiện vẫn còn thiết bị hoặc tự động hóa đang liên kết.', { duration: 5000 })
      } else {
        toast.error(getApiErrorMessage(error))
      }
    }
  }

  if (isLoading) return <LoadingSpinner size="lg" text="Đang tải nông trại..." />

  return (
    <div className="animate-fade-in">
      {/* Page Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="font-headline text-2xl font-bold text-on-surface">Nông trại của tôi</h1>
          <p className="text-sm text-on-surface-variant mt-1 font-label">
            Quản lý các khu vực canh tác và triển khai IoT
          </p>
        </div>
        <button onClick={handleCreate} className="btn-primary-gradient flex items-center gap-2" id="create-farm-btn">
          <span className="material-symbols-outlined text-lg">add</span>
          Tạo nông trại mới
        </button>
      </div>

      {/* Farm Grid */}
      {farms.length === 0 ? (
        <EmptyState
          icon="yard"
          title="Chưa có nông trại nào"
          description="Bắt đầu bằng cách tạo nông trại đầu tiên để triển khai hệ thống IoT."
          action={
            <button onClick={handleCreate} className="btn-primary flex items-center gap-2">
              <span className="material-symbols-outlined text-lg">add</span>
              Tạo nông trại mới
            </button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {farms.map((farm, index) => (
            <div key={farm.id} className="animate-slide-up" style={{ animationDelay: `${index * 0.05}s` }}>
              <FarmCard farm={farm} onEdit={() => handleEdit(farm)} onDelete={() => handleDelete(farm)} />
            </div>
          ))}
        </div>
      )}

      {/* Modals */}
      <FarmFormModal
        isOpen={isModalOpen}
        onClose={() => { setIsModalOpen(false); setSelectedFarm(null) }}
        farm={selectedFarm}
        onSuccess={handleFormSuccess}
      />
      <DeleteConfirmModal
        isOpen={isDeleteOpen}
        onClose={() => { setIsDeleteOpen(false); setSelectedFarm(null) }}
        onConfirm={handleDeleteConfirm}
        farmName={selectedFarm?.name || ''}
      />
    </div>
  )
}
