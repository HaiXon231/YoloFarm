import { useState, useEffect } from 'react'
import api, { getApiErrorMessage } from '@/lib/axios'
import LoadingSpinner from '@/components/ui/LoadingSpinner'
import toast from 'react-hot-toast'
import { format } from 'date-fns'
import type { AdminFarmerResponse, AdminFarmResponse, AdminDeviceResponse } from '@/types'

interface AdminDetailDrawerProps {
  isOpen: boolean
  onClose: () => void
  type: 'farmers' | 'farms' | 'devices' | 'active_devices' | null
  title: string
}

export default function AdminDetailDrawer({ isOpen, onClose, type, title }: AdminDetailDrawerProps) {
  const [data, setData] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    if (isOpen && type) {
      fetchData()
    }
  }, [isOpen, type])

  const fetchData = async () => {
    setIsLoading(true)
    try {
      let endpoint = ''
      if (type === 'farmers') endpoint = '/admin/farmers'
      else if (type === 'farms') endpoint = '/admin/farms'
      else if (type === 'devices' || type === 'active_devices') endpoint = '/admin/devices'

      const res = await api.get(endpoint)
      let result = res.data
      
      if (type === 'active_devices') {
        result = result.filter((d: AdminDeviceResponse) => d.status === 'ACTIVE')
      }

      setData(result)
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  if (!isOpen) return null

  return (
    <>
      {/* Backdrop */}
      <div 
        className="fixed inset-0 bg-black/40 backdrop-blur-sm z-[60] transition-opacity duration-300"
        onClick={onClose}
      />
      
      {/* Drawer */}
      <div className={`fixed right-0 top-0 h-full w-full max-w-2xl bg-surface-container-lowest z-[70] shadow-2xl transition-transform duration-500 ease-out transform ${isOpen ? 'translate-x-0' : 'translate-x-full'}`}>
        <div className="flex flex-col h-full">
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-surface-container-low">
            <h2 className="font-headline text-xl font-bold text-on-surface">{title}</h2>
            <button 
              onClick={onClose}
              className="p-2 rounded-full hover:bg-surface-container transition-colors"
            >
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>

          {/* Content */}
          <div className="flex-1 overflow-y-auto p-6">
            {isLoading ? (
              <LoadingSpinner size="lg" text="Đang tải danh sách..." />
            ) : data.length === 0 ? (
              <div className="text-center py-12">
                <span className="material-symbols-outlined text-4xl text-outline-variant mb-2">inventory_2</span>
                <p className="text-on-surface-variant font-medium">Không có dữ liệu</p>
              </div>
            ) : (
              <div className="bg-surface-container-low rounded-2xl overflow-hidden border border-surface-container-low">
                <table className="w-full text-left text-sm">
                  <thead className="bg-surface-container text-on-surface-variant font-bold uppercase tracking-wider text-[10px]">
                    {type === 'farmers' && (
                      <tr>
                        <th className="px-4 py-3">Tên đăng nhập</th>
                        <th className="px-4 py-3">Email</th>
                        <th className="px-4 py-3">Số trang trại</th>
                        <th className="px-4 py-3">Ngày tạo</th>
                      </tr>
                    )}
                    {type === 'farms' && (
                      <tr>
                        <th className="px-4 py-3">Tên nông trại</th>
                        <th className="px-4 py-3">Chủ sở hữu</th>
                        <th className="px-4 py-3">Thiết bị</th>
                        <th className="px-4 py-3">Ngày tạo</th>
                      </tr>
                    )}
                    {(type === 'devices' || type === 'active_devices') && (
                      <tr>
                        <th className="px-4 py-3">Tên thiết bị</th>
                        <th className="px-4 py-3">Loại</th>
                        <th className="px-4 py-3">Nông trại</th>
                        <th className="px-4 py-3">Trạng thái</th>
                      </tr>
                    )}
                  </thead>
                  <tbody className="divide-y divide-surface-container-low">
                    {type === 'farmers' && (data as AdminFarmerResponse[]).map((farmer) => (
                      <tr key={farmer.id} className="hover:bg-surface-container-high transition-colors">
                        <td className="px-4 py-3 font-bold text-on-surface">{farmer.username}</td>
                        <td className="px-4 py-3 text-on-surface-variant">{farmer.email}</td>
                        <td className="px-4 py-3 font-semibold text-primary">{farmer.farm_count}</td>
                        <td className="px-4 py-3 text-xs text-on-surface-variant italic">
                          {format(new Date(farmer.created_at), 'dd/MM/yyyy')}
                        </td>
                      </tr>
                    ))}
                    {type === 'farms' && (data as AdminFarmResponse[]).map((farm) => (
                      <tr key={farm.id} className="hover:bg-surface-container-high transition-colors">
                        <td className="px-4 py-3">
                          <p className="font-bold text-on-surface">{farm.name}</p>
                          <p className="text-[10px] text-on-surface-variant">{farm.location || 'N/A'}</p>
                        </td>
                        <td className="px-4 py-3 text-on-surface-variant">{farm.owner_name}</td>
                        <td className="px-4 py-3 font-semibold text-secondary">{farm.device_count}</td>
                        <td className="px-4 py-3 text-xs text-on-surface-variant italic">
                          {format(new Date(farm.created_at), 'dd/MM/yyyy')}
                        </td>
                      </tr>
                    ))}
                    {(type === 'devices' || type === 'active_devices') && (data as AdminDeviceResponse[]).map((device) => (
                      <tr key={device.id} className="hover:bg-surface-container-high transition-colors">
                        <td className="px-4 py-3 font-bold text-on-surface">{device.name}</td>
                        <td className="px-4 py-3 text-on-surface-variant">{device.model_name}</td>
                        <td className="px-4 py-3 text-on-surface-variant">{device.farm_name}</td>
                        <td className="px-4 py-3">
                          <span className={device.status === 'ACTIVE' ? 'badge-online' : 'badge-pending'}>
                            {device.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  )
}
