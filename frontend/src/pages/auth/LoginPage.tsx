import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { LoginResponse } from '@/types'

export default function LoginPage() {
  const navigate = useNavigate()
  const { login, fetchProfile } = useAuthStore()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage('')
    setIsLoading(true)

    try {
      const response = await api.post<LoginResponse>('/auth/login', { username, password })
      const { access_token, role } = response.data
      login(access_token, role)
      await fetchProfile()

      if (role === 'ADMIN') {
        navigate('/admin/device-requests', { replace: true })
      } else {
        navigate('/farms', { replace: true })
      }
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-sidebar flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background decoration */}
      <div className="absolute inset-0">
        <div className="absolute top-0 right-0 w-96 h-96 bg-primary/10 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-0 w-80 h-80 bg-accent/10 rounded-full blur-3xl" />
      </div>

      <div className="relative w-full max-w-md animate-fade-in">
        {/* Header */}
        <div className="text-center mb-10">
          <h1 className="font-headline text-4xl font-black text-accent mb-3">YoloFarm</h1>
          <p className="text-gray-400 font-label text-sm">Hệ thống quản lý nông trại thông minh</p>
        </div>

        {/* Card */}
        <div className="bg-surface-container-lowest rounded-3xl shadow-modal p-8">
          <h2 className="font-headline text-xl font-bold text-on-surface mb-6">Đăng nhập</h2>

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Username */}
            <div>
              <label htmlFor="login-username" className="label-text block mb-2">
                Tên đăng nhập
              </label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 material-symbols-outlined text-outline-variant text-lg">
                  person
                </span>
                <input
                  id="login-username"
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="input-field pl-12"
                  placeholder="Nhập tên đăng nhập"
                  required
                />
              </div>
            </div>

            {/* Password */}
            <div>
              <label htmlFor="login-password" className="label-text block mb-2">
                Mật khẩu
              </label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 material-symbols-outlined text-outline-variant text-lg">
                  lock
                </span>
                <input
                  id="login-password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="input-field pl-12 pr-12"
                  placeholder="Nhập mật khẩu"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-outline-variant hover:text-on-surface transition-colors"
                >
                  <span className="material-symbols-outlined text-lg">
                    {showPassword ? 'visibility_off' : 'visibility'}
                  </span>
                </button>
              </div>
            </div>

            {/* Error */}
            {errorMessage && (
              <div className="flex items-center gap-2 px-4 py-3 bg-error-container/10 border border-error/20 rounded-xl animate-fade-in">
                <span className="material-symbols-outlined text-error text-lg">error</span>
                <p className="text-error text-sm font-medium">{errorMessage}</p>
              </div>
            )}

            {/* Submit */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full btn-primary-gradient py-3.5 flex items-center justify-center gap-2 text-base"
              id="login-submit-btn"
            >
              {isLoading ? (
                <>
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Đang đăng nhập...
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined text-lg">login</span>
                  Đăng nhập
                </>
              )}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-on-surface-variant">
              Chưa có tài khoản?{' '}
              <Link to="/register" className="text-primary font-bold hover:underline">
                Đăng ký ngay
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
