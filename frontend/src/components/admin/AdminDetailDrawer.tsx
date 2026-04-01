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
    // CRITICAL: Clear data whenever the modal opens or type changes to prevent TypeErrors
    setData([])
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
        className="fixed inset-0 bg-black/60 backdrop-blur-md z-[60] transition-all duration-500 animate-fade-in"
        onClick={onClose}
      />
      
      {/* Modal - Centered */}
      <div className="fixed inset-0 flex items-center justify-center z-[70] p-4 pointer-events-none">
        <div className={`w-full max-w-5xl bg-white shadow-2xl rounded-[3rem] overflow-hidden pointer-events-auto transition-all duration-500 transform ${isOpen ? 'scale-100 opacity-100' : 'scale-95 opacity-0'}`}>
          <div className="flex flex-col max-h-[90vh] bg-surface-container-lowest">
            {/* Header */}
            <div className="flex items-center justify-between px-10 py-8 bg-primary/5 border-b border-surface-container">
              <div>
                <h2 className="font-headline text-3xl font-black text-on-surface tracking-tight mb-1">{title}</h2>
                <div className="flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-primary animate-pulse" />
                  <p className="text-xs font-bold text-primary uppercase tracking-widest">{data.length} bản ghi đồng bộ</p>
                </div>
              </div>
              <button 
                onClick={onClose}
                className="w-12 h-12 rounded-2xl bg-white border border-surface-container shadow-sm flex items-center justify-center hover:bg-surface-container transition-all"
              >
                <span className="material-symbols-outlined text-2xl font-bold">close</span>
              </button>
            </div>

            {/* Scrollable Content area */}
            <div className="flex-1 overflow-y-auto p-10">
              {isLoading ? (
                <div className="h-60 flex items-center justify-center">
                   <LoadingSpinner size="lg" text="Đang đồng bộ dữ liệu..." />
                </div>
              ) : data.length === 0 ? (
                <div className="text-center py-20">
                  <div className="w-24 h-24 rounded-full bg-surface-container mx-auto flex items-center justify-center mb-6">
                     <span className="material-symbols-outlined text-5xl text-outline-variant">inventory_2</span>
                  </div>
                  <p className="text-on-surface-variant font-black uppercase tracking-widest text-sm">Kho lưu trữ rỗng hoặc đang chờ tải</p>
                </div>
              ) : (
                <div className="premium-card !p-0 !rounded-3xl border border-surface-container-high shadow-sm overflow-hidden">
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm min-w-full border-collapse">
                      <thead className="bg-surface-container text-on-surface font-black uppercase tracking-[0.15em] text-[11px] border-b border-surface-container-high">
                        <tr className="whitespace-nowrap">
                          {type === 'farmers' && (
                            <>
                              <th className="px-8 py-6">Tên nông dân</th>
                              <th className="px-8 py-6">Email liên hệ</th>
                              <th className="px-8 py-6 text-center">Số trang trại</th>
                              <th className="px-8 py-6">Ngày gia nhập</th>
                            </>
                          )}
                          {type === 'farms' && (
                            <>
                              <th className="px-8 py-6">Tên nông trại</th>
                              <th className="px-8 py-6">Chủ sở hữu</th>
                              <th className="px-8 py-6 text-center">Số thiết bị</th>
                              <th className="px-8 py-6">Ngày khởi tạo</th>
                            </>
                          )}
                          {(type === 'devices' || type === 'active_devices') && (
                            <>
                              <th className="px-8 py-6">Tên thiết bị</th>
                              <th className="px-8 py-6">Loại Model</th>
                              <th className="px-8 py-6">Thuộc nông trại</th>
                              <th className="px-8 py-6">Trạng thái</th>
                            </>
                          )}
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-surface-container">
                        {type === 'farmers' && (data as AdminFarmerResponse[]).map((farmer) => (
                          <tr key={farmer.id} className="hover:bg-primary/5 transition-colors group whitespace-nowrap">
                            <td className="px-8 py-6">
                               <div className="flex items-center gap-4">
                                  <div className="w-10 h-10 rounded-2xl bg-emerald-100 text-emerald-600 flex items-center justify-center font-black shadow-inner">
                                     {farmer.username?.charAt(0).toUpperCase() || '?'}
                                  </div>
                                  <span className="font-black text-on-surface group-hover:text-primary transition-colors">{farmer.username}</span>
                               </div>
                            </td>
                            <td className="px-8 py-6 text-on-surface-variant font-medium">{farmer.email}</td>
                            <td className="px-8 py-6 text-center">
                               <span className="px-4 py-1.5 bg-emerald-50 text-emerald-700 rounded-xl font-black text-xs border border-emerald-100">{farmer.farm_count}</span>
                            </td>
                            <td className="px-8 py-6 text-[11px] text-on-surface-variant font-black uppercase tracking-tighter italic">
                              {farmer.created_at ? format(new Date(farmer.created_at), 'dd MMM yyyy') : 'N/A'}
                            </td>
                          </tr>
                        ))}
                        {type === 'farms' && (data as AdminFarmResponse[]).map((farm) => (
                          <tr key={farm.id} className="hover:bg-blue-50/50 transition-colors group whitespace-nowrap">
                            <td className="px-8 py-6">
                              <p className="font-black text-on-surface group-hover:text-secondary">{farm.name}</p>
                              <p className="text-[10px] font-bold text-on-surface-variant uppercase tracking-widest">{farm.location || 'Hệ thống Global'}</p>
                            </td>
                            <td className="px-8 py-6">
                               <div className="flex flex-col">
                                 <span className="font-bold text-on-surface-variant">{farm.owner_name}</span>
                                 <span className="text-[10px] italic text-outline">{farm.owner_email}</span>
                               </div>
                            </td>
                            <td className="px-8 py-6 text-center">
                              <span className="px-5 py-2 bg-blue-100 text-blue-600 rounded-2xl font-black text-xs border border-blue-200">{farm.device_count}</span>
                            </td>
                            <td className="px-8 py-6 text-[11px] text-on-surface-variant font-black uppercase italic">
                              {farm.created_at ? format(new Date(farm.created_at), 'dd/MM/yyyy') : 'N/A'}
                            </td>
                          </tr>
                        ))}
                        {(type === 'devices' || type === 'active_devices') && (data as AdminDeviceResponse[]).map((device) => (
                          <tr key={device.id} className="hover:bg-purple-50/50 transition-colors group whitespace-nowrap">
                            <td className="px-8 py-6 font-black text-on-surface group-hover:text-primary">{device.name}</td>
                            <td className="px-8 py-6">
                               <span className="px-4 py-2 bg-surface-container rounded-xl text-[10px] font-black text-on-surface-variant uppercase tracking-wider">{device.model_name}</span>
                            </td>
                            <td className="px-8 py-6 text-on-surface font-bold">{device.farm_name}</td>
                            <td className="px-8 py-6">
                              <span className={device.status === 'ACTIVE' ? 'badge-online !px-5 !py-2 shadow-sm' : 'badge-pending !px-5 !py-2 shadow-sm'}>
                                {device.status}
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
            
            {/* Footer */}
            <div className="px-10 py-6 border-t border-surface-container bg-surface-container-low flex items-center justify-between">
               <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-[0.3em]">YoloFarm Admin System • Security Verified</p>
               <button 
                 onClick={onClose}
                 className="btn-primary-gradient !px-6 !py-2 !rounded-xl !text-xs"
               >
                 Đóng lại
               </button>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
