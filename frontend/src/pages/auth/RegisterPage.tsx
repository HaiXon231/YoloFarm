import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import api, { getApiErrorMessage, getApiErrorCode } from '@/lib/axios'

interface FormErrors {
  username?: string
  email?: string
  password?: string
  confirmPassword?: string
}

export default function RegisterPage() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [formErrors, setFormErrors] = useState<FormErrors>({})

  const validate = (): boolean => {
    const errors: FormErrors = {}
    if (!username.trim()) errors.username = 'Tên đăng nhập không được để trống'
    if (!email.trim()) errors.email = 'Email không được để trống'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = 'Email không đúng định dạng'
    if (!password) errors.password = 'Mật khẩu không được để trống'
    else if (password.length < 8) errors.password = 'Mật khẩu phải chứa ít nhất 8 ký tự'
    if (password !== confirmPassword) errors.confirmPassword = 'Mật khẩu xác nhận không khớp'
    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!validate()) return

    setIsLoading(true)
    try {
      await api.post('/auth/register', { username, password, email })
      toast.success('Đăng ký thành công! Vui lòng đăng nhập.', { duration: 4000 })
      navigate('/login', { replace: true })
    } catch (error) {
      const code = getApiErrorCode(error)
      const msg = getApiErrorMessage(error)
      if (code === 409) {
        setFormErrors({ username: msg })
      } else {
        toast.error(msg)
      }
    } finally {
      setIsLoading(false)
    }
  }

  const renderFieldError = (error?: string) =>
    error ? (
      <p className="mt-1.5 text-error text-xs font-medium flex items-center gap-1 animate-fade-in">
        <span className="material-symbols-outlined text-xs">error</span>
        {error}
      </p>
    ) : null

  return (
    <div className="min-h-screen bg-sidebar flex items-center justify-center p-4 relative overflow-hidden">
      <div className="absolute inset-0">
        <div className="absolute top-0 left-0 w-96 h-96 bg-primary/10 rounded-full blur-3xl" />
        <div className="absolute bottom-0 right-0 w-80 h-80 bg-accent/10 rounded-full blur-3xl" />
      </div>

      <div className="relative w-full max-w-md animate-fade-in">
        <div className="text-center mb-10">
          <h1 className="font-headline text-4xl font-black text-accent mb-3">YoloFarm</h1>
          <p className="text-gray-400 font-label text-sm">Tạo tài khoản nông dân mới</p>
        </div>

        <div className="bg-surface-container-lowest rounded-3xl shadow-modal p-8">
          <h2 className="font-headline text-xl font-bold text-on-surface mb-6">Đăng ký</h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Username */}
            <div>
              <label htmlFor="reg-username" className="label-text block mb-2">Tên đăng nhập</label>
              <input
                id="reg-username"
                type="text"
                value={username}
                onChange={(e) => { setUsername(e.target.value); setFormErrors((p) => ({ ...p, username: undefined })) }}
                className={`input-field ${formErrors.username ? 'input-error' : ''}`}
                placeholder="VD: nongdan_01"
              />
              {renderFieldError(formErrors.username)}
            </div>

            {/* Email */}
            <div>
              <label htmlFor="reg-email" className="label-text block mb-2">Email</label>
              <input
                id="reg-email"
                type="email"
                value={email}
                onChange={(e) => { setEmail(e.target.value); setFormErrors((p) => ({ ...p, email: undefined })) }}
                className={`input-field ${formErrors.email ? 'input-error' : ''}`}
                placeholder="nongdan@gmail.com"
              />
              {renderFieldError(formErrors.email)}
            </div>

            {/* Password */}
            <div>
              <label htmlFor="reg-password" className="label-text block mb-2">Mật khẩu</label>
              <div className="relative">
                <input
                  id="reg-password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => { setPassword(e.target.value); setFormErrors((p) => ({ ...p, password: undefined })) }}
                  className={`input-field pr-12 ${formErrors.password ? 'input-error' : ''}`}
                  placeholder="Ít nhất 8 ký tự"
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
              {renderFieldError(formErrors.password)}
            </div>

            {/* Confirm Password */}
            <div>
              <label htmlFor="reg-confirm-password" className="label-text block mb-2">Xác nhận mật khẩu</label>
              <input
                id="reg-confirm-password"
                type="password"
                value={confirmPassword}
                onChange={(e) => { setConfirmPassword(e.target.value); setFormErrors((p) => ({ ...p, confirmPassword: undefined })) }}
                className={`input-field ${formErrors.confirmPassword ? 'input-error' : ''}`}
                placeholder="Nhập lại mật khẩu"
              />
              {renderFieldError(formErrors.confirmPassword)}
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full btn-primary-gradient py-3.5 flex items-center justify-center gap-2 text-base mt-6"
              id="register-submit-btn"
            >
              {isLoading ? (
                <>
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Đang xử lý...
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined text-lg">person_add</span>
                  Đăng ký
                </>
              )}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-on-surface-variant">
              Đã có tài khoản?{' '}
              <Link to="/login" className="text-primary font-bold hover:underline">Đăng nhập</Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
