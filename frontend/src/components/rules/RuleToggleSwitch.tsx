import { useEffect, useState } from 'react'
import toast from 'react-hot-toast'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { RuleResponse } from '@/types'

interface RuleToggleSwitchProps {
  rule: RuleResponse
  onUpdate: () => void | Promise<void>
  disabled?: boolean
  disabledReason?: string | null
}

export default function RuleToggleSwitch({ rule, onUpdate, disabled = false, disabledReason = null }: RuleToggleSwitchProps) {
  const [isActive, setIsActive] = useState(rule.is_active)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    setIsActive(rule.is_active)
  }, [rule.is_active])

  const handleToggle = async () => {
    if (disabled || isLoading) return

    const prev = isActive
    // Optimistic update
    setIsActive(!isActive)
    setIsLoading(true)
    try {
      await api.patch(`/rules/${rule.id}/toggle`, { is_active: !prev })
      await onUpdate()
    } catch (error) {
      // Revert on failure
      setIsActive(prev)
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <button
      onClick={handleToggle}
      disabled={isLoading || disabled}
      className="relative inline-flex"
      title={disabled && disabledReason ? disabledReason : isActive ? 'Tắt luật' : 'Bật luật'}
    >
      <div className={`w-11 h-6 rounded-full transition-colors duration-300 ${isActive ? 'bg-primary' : 'bg-outline-variant'
        }`}>
        <div className={`absolute top-[2px] w-5 h-5 bg-white rounded-full shadow-md transition-transform duration-300 ${isActive ? 'translate-x-[22px]' : 'translate-x-[2px]'
          }`} />
      </div>
    </button>
  )
}
