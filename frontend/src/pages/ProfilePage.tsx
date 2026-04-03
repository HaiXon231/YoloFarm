import { useState, useEffect } from 'react'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { UserProfile, AdminStatsResponse } from '@/types'
import { useAuthStore } from '@/stores/authStore'
import LoadingSpinner from '@/components/ui/LoadingSpinner'
import toast from 'react-hot-toast'
import { format } from 'date-fns'

export default function ProfilePage() {
  const { role } = useAuthStore()
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [allUsers, setAllUsers] = useState<UserProfile[]>([])
  const [stats, setStats] = useState<AdminStatsResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isUpdating, setIsUpdating] = useState(false)

  // Form states
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')

  const fetchProfile = async () => {
    try {
      const res = await api.get<UserProfile>('/users/profile')
      setProfile(res.data)
      setUsername(res.data.username)
      setEmail(res.data.email)
      
      // Nếu là Admin, tải thêm danh sách người dùng và thống kê hệ thống
      if (res.data.role === 'ADMIN') {
        fetchAdminData()
      }
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  const fetchAdminData = async () => {
     try {
        const [usersRes, statsRes] = await Promise.all([
           api.get<UserProfile[]>('/admin/users'),
           api.get<AdminStatsResponse>('/admin/stats')
        ])
        setAllUsers(usersRes.data)
        setStats(statsRes.data)
     } catch (error) {
        console.error('Error fetching admin data:', error)
     }
  }

  useEffect(() => {
    fetchProfile()
  }, [])

  const handleUpdateInfo = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsUpdating(true)
    try {
      const res = await api.patch<UserProfile>('/users/profile', { username, email })
      setProfile(res.data)
      toast.success('Cập nhật thông tin thành công!')
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsUpdating(false)
    }
  }

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault()
    if (newPassword !== confirmPassword) {
      return toast.error('Mật khẩu xác nhận không khớp!')
    }

    setIsUpdating(true)
    try {
      await api.patch('/users/profile', { 
        current_password: currentPassword, 
        new_password: newPassword 
      })
      toast.success('Đổi mật khẩu thành công!')
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsUpdating(false)
    }
  }

  if (isLoading) return <LoadingSpinner size="lg" text={`Đang tải hồ sơ ${role === 'ADMIN' ? 'quản trị viên' : 'nông dân'}...`} />

  return (
    <div className="animate-fade-in pb-12 relative">
      {/* Background Blobs */}
      <div className="fixed top-40 left-0 w-80 h-80 bg-indigo-500/10 rounded-full blur-[100px] -z-10 animate-blob" />
      <div className="fixed top-0 right-0 w-96 h-96 bg-emerald-500/10 rounded-full blur-[120px] -z-10 animate-blob [animation-delay:2s]" />

      <div className="mb-10">
        <h1 className="text-4xl font-black text-on-surface tracking-tight mb-2">Hồ sơ cá nhân</h1>
        <p className="text-on-surface-variant font-medium">Quản lý thông tin tài khoản và bảo mật của bạn.</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: User Card & System Health (Admin) */}
        <div className="lg:col-span-1 space-y-6">
          <div className="premium-card p-10 flex flex-col items-center text-center">
             <div className={`w-32 h-32 rounded-[2.5rem] bg-gradient-to-br ${role === 'ADMIN' ? 'from-indigo-600 to-purple-800' : 'from-primary to-teal-700'} p-1 mb-6 shadow-xl shadow-primary/20`}>
                <div className="w-full h-full rounded-[2.2rem] bg-white flex items-center justify-center">
                   <span className={`text-5xl font-black ${role === 'ADMIN' ? 'text-indigo-600' : 'text-primary'}`}>
                     {profile?.username.charAt(0).toUpperCase()}
                   </span>
                </div>
             </div>
             
             <h2 className="text-2xl font-black text-on-surface mb-1">{profile?.username}</h2>
             <span className={role === 'ADMIN' ? 'badge-pending !px-4 !py-1.5 mb-6 !bg-indigo-50 !text-indigo-700' : 'badge-online !px-4 !py-1.5 mb-6'}>
                {profile?.role} Account
             </span>
             
             <div className="w-full h-[1px] bg-surface-container-high mb-6" />
             
             <div className="w-full space-y-4">
                <div className="flex items-center gap-3 text-left">
                   <div className="w-10 h-10 rounded-xl bg-surface-container flex items-center justify-center">
                      <span className="material-symbols-outlined text-outline">mail</span>
                   </div>
                   <div>
                      <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Email</p>
                      <p className="text-sm font-bold text-on-surface">{profile?.email}</p>
                   </div>
                </div>
                <div className="flex items-center gap-3 text-left">
                   <div className="w-10 h-10 rounded-xl bg-surface-container flex items-center justify-center">
                      <span className="material-symbols-outlined text-outline">calendar_today</span>
                   </div>
                   <div>
                      <p className="text-[10px] font-black text-on-surface-variant uppercase tracking-widest">Gia nhập</p>
                      <p className="text-sm font-bold text-on-surface">
                        {profile?.created_at ? format(new Date(profile.created_at), 'dd/MM/yyyy') : 'N/A'}
                      </p>
                   </div>
                </div>
             </div>
          </div>

          {role === 'ADMIN' && stats && (
             <div className="premium-card p-8 border-indigo-100 bg-indigo-50/30">
                <div className="flex items-center gap-3 mb-6">
                   <span className="material-symbols-outlined text-indigo-600">analytics</span>
                   <h4 className="font-black text-indigo-900 text-sm uppercase tracking-wider">Trạng thái hệ thống</h4>
                </div>
                <div className="space-y-4">
                   <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-on-surface-variant">API Gateway</span>
                      <span className={`w-2 h-2 rounded-full ${stats.api_status ? 'bg-emerald-500' : 'bg-rose-500'}`} />
                   </div>
                   <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-on-surface-variant">MQTT Broker</span>
                      <span className={`w-2 h-2 rounded-full ${stats.mqtt_status ? 'bg-emerald-500' : 'bg-rose-500'}`} />
                   </div>
                   <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-on-surface-variant">Database Info</span>
                      <span className={`w-2 h-2 rounded-full ${stats.db_status ? 'bg-emerald-500' : 'bg-rose-500'}`} />
                   </div>
                </div>
             </div>
          )}

          <div className="premium-card p-8 bg-primary/5 border-primary/20">
             <div className="flex items-center gap-3 mb-4">
                <span className="material-symbols-outlined text-primary">info</span>
                <h4 className="font-black text-primary text-sm uppercase tracking-wider">Hỗ trợ tài khoản</h4>
             </div>
             <p className="text-xs text-on-surface-variant leading-relaxed">
               Nếu bạn gặp vấn đề trong việc cập nhật thông tin hoặc mất quyền truy cập, vui lòng liên hệ Ban Quản Trị qua email support@yolofarm.com
             </p>
          </div>
        </div>

        {/* Right Column: Edit Forms & User Management */}
        <div className="lg:col-span-2 space-y-8">
          {/* General Information */}
          <div className="premium-card p-10">
             <div className="flex items-center gap-4 mb-8">
                <div className="w-12 h-12 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                   <span className="material-symbols-outlined icon-filled">account_circle</span>
                </div>
                <div>
                   <h3 className="text-xl font-black text-on-surface">Thông tin cá nhân</h3>
                   <p className="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Cập nhật hồ sơ công khai</p>
                </div>
             </div>

             <form onSubmit={handleUpdateInfo} className="space-y-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                   <div className="space-y-2">
                      <label className="label-text">Tên hiển thị</label>
                      <input 
                        className="input-field" 
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        placeholder="Nhập tên mới"
                      />
                   </div>
                   <div className="space-y-2">
                      <label className="label-text">Địa chỉ Email</label>
                      <input 
                        className="input-field" 
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        placeholder="Nhập email mới"
                      />
                   </div>
                </div>
                <div className="flex justify-end">
                   <button 
                     type="submit" 
                     disabled={isUpdating}
                     className="btn-primary-gradient !rounded-[1.2rem] flex items-center gap-2 group"
                   >
                     {isUpdating ? 'Đang lưu...' : 'Lưu thay đổi'}
                     <span className="material-symbols-outlined text-lg group-hover:translate-x-0.5 transition-transform">done_all</span>
                   </button>
                </div>
             </form>
          </div>

          {/* Password Change */}
          <div className="premium-card p-10">
             <div className="flex items-center gap-4 mb-8">
                <div className="w-12 h-12 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center">
                   <span className="material-symbols-outlined icon-filled">lock_reset</span>
                </div>
                <div>
                   <h3 className="text-xl font-black text-on-surface">Bảo mật</h3>
                   <p className="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Thay đổi mật khẩu đăng nhập</p>
                </div>
             </div>

             <form onSubmit={handleChangePassword} className="space-y-6">
                <div className="space-y-2">
                   <label className="label-text">Mật khẩu hiện tại</label>
                   <input 
                     type="password"
                     className="input-field" 
                     value={currentPassword}
                     onChange={(e) => setCurrentPassword(e.target.value)}
                     placeholder="••••••••"
                   />
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                   <div className="space-y-2">
                      <label className="label-text">Mật khẩu mới</label>
                      <input 
                        type="password"
                        className="input-field" 
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        placeholder="Tối thiểu 6 ký tự"
                      />
                   </div>
                   <div className="space-y-2">
                      <label className="label-text">Xác nhận mật khẩu mới</label>
                      <input 
                        type="password"
                        className="input-field" 
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        placeholder="Nhập lại mật khẩu mới"
                      />
                   </div>
                </div>
                <div className="flex justify-end">
                   <button 
                     type="submit" 
                     disabled={isUpdating}
                     className="bg-on-surface text-white font-black py-3 px-8 rounded-[1.2rem] hover:bg-on-surface/90 transition-all active:scale-95 flex items-center gap-2 group"
                   >
                     {isUpdating ? 'Đang đổi...' : 'Đổi mật khẩu'}
                     <span className="material-symbols-outlined text-lg group-hover:rotate-12 transition-transform">shield</span>
                   </button>
                </div>
             </form>
          </div>

          {/* User Management (Admin Only) */}
          {role === 'ADMIN' && (
             <div className="premium-card p-10">
                <div className="flex items-center gap-4 mb-8">
                   <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
                      <span className="material-symbols-outlined icon-filled">manage_accounts</span>
                   </div>
                   <div>
                      <h3 className="text-xl font-black text-on-surface">Quản lý người dùng</h3>
                      <p className="text-xs font-bold text-on-surface-variant uppercase tracking-wider">Danh sách tài khoản hệ thống</p>
                   </div>
                </div>

                <div className="overflow-x-auto border border-surface-container rounded-2xl">
                   <table className="w-full text-left text-sm border-collapse">
                      <thead className="bg-surface-container text-on-surface font-black uppercase tracking-widest text-[10px]">
                         <tr>
                            <th className="px-6 py-4">Tài khoản</th>
                            <th className="px-6 py-4">Email</th>
                            <th className="px-6 py-4">Vai trò</th>
                            <th className="px-6 py-4">Gia nhập</th>
                         </tr>
                      </thead>
                      <tbody className="divide-y divide-surface-container">
                         {allUsers.map((u) => (
                            <tr key={u.id} className="hover:bg-surface-container-low transition-colors">
                               <td className="px-6 py-4 font-bold text-on-surface">{u.username}</td>
                               <td className="px-6 py-4 text-on-surface-variant">{u.email}</td>
                               <td className="px-6 py-4 text-xs">
                                  <span className={`px-3 py-1 rounded-full font-black ${
                                     u.role === 'ADMIN' ? 'bg-indigo-50 text-indigo-700' : 'bg-emerald-50 text-emerald-700'
                                  }`}>
                                     {u.role}
                                  </span>
                               </td>
                               <td className="px-6 py-4 text-xs text-on-surface-variant italic">
                                  {format(new Date(u.created_at), 'dd/MM/yyyy')}
                               </td>
                            </tr>
                         ))}
                      </tbody>
                   </table>
                </div>
             </div>
          )}
        </div>
      </div>
    </div>
  )
}
