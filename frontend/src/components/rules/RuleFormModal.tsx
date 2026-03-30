import { useState, useEffect } from 'react'
import toast from 'react-hot-toast'
import Modal from '@/components/ui/Modal'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { DeviceWithModel, RuleResponse, RuleType, Operator, ActionCommand } from '@/types'

interface RuleFormModalProps {
  isOpen: boolean
  onClose: () => void
  farmId: string
  devices: DeviceWithModel[]
  rule: RuleResponse | null
  onSuccess: () => void
}

export default function RuleFormModal({ isOpen, onClose, farmId, devices, rule, onSuccess }: RuleFormModalProps) {
  const isEditing = !!rule
  const sensors = devices.filter((d) => d.device_type === 'SENSOR' && d.status === 'ACTIVE')
  const actuators = devices.filter((d) => d.device_type === 'ACTUATOR' && d.status === 'ACTIVE')

  const [ruleName, setRuleName] = useState('')
  const [ruleType, setRuleType] = useState<RuleType>('CONDITION')
  const [triggerDeviceId, setTriggerDeviceId] = useState('')
  const [operator, setOperator] = useState<Operator>('<')
  const [thresholdValue, setThresholdValue] = useState<number | ''>('')
  const [scheduleHour, setScheduleHour] = useState(6)
  const [scheduleMinute, setScheduleMinute] = useState(0)
  const [actionDeviceId, setActionDeviceId] = useState('')
  const [actionCommand, setActionCommand] = useState<ActionCommand>('ON')
  const [isLoading, setIsLoading] = useState(false)
  const [apiError, setApiError] = useState('')

  useEffect(() => {
    if (rule) {
      setRuleName(rule.rule_name)
      setRuleType(rule.rule_type)
      setTriggerDeviceId(rule.trigger_device_id || '')
      setOperator((rule.operator as Operator) || '<')
      setThresholdValue(rule.threshold_value ?? '')
      setActionDeviceId(rule.action_device_id)
      setActionCommand(rule.action_command)
      // Parse cron
      if (rule.cron_expression) {
        const parts = rule.cron_expression.split(' ')
        if (parts.length >= 2) {
          setScheduleMinute(parseInt(parts[0]) || 0)
          setScheduleHour(parseInt(parts[1]) || 6)
        }
      }
    } else {
      setRuleName('')
      setRuleType('CONDITION')
      setTriggerDeviceId('')
      setOperator('<')
      setThresholdValue('')
      setScheduleHour(6)
      setScheduleMinute(0)
      setActionDeviceId('')
      setActionCommand('ON')
    }
    setApiError('')
  }, [rule, isOpen])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setApiError('')

    const payload = {
      farm_id: farmId,
      rule_name: ruleName,
      rule_type: ruleType,
      trigger_device_id: ruleType === 'CONDITION' ? triggerDeviceId : null,
      operator: ruleType === 'CONDITION' ? operator : null,
      threshold_value: ruleType === 'CONDITION' ? Number(thresholdValue) : null,
      cron_expression: ruleType === 'SCHEDULE' ? `${scheduleMinute} ${scheduleHour} * * *` : null,
      action_device_id: actionDeviceId,
      action_command: actionCommand,
    }

    setIsLoading(true)
    try {
      if (isEditing) {
        await api.put(`/rules/${rule.id}`, payload)
        toast.success('Cập nhật luật thành công!')
      } else {
        await api.post('/rules', payload)
        toast.success('Tạo luật mới thành công!')
      }
      onSuccess()
    } catch (error) {
      const msg = getApiErrorMessage(error)
      setApiError(msg)
      toast.error(msg)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={isEditing ? 'Chỉnh sửa luật' : 'Tạo luật tự động hóa'} maxWidth="max-w-xl">
      <form onSubmit={handleSubmit} className="space-y-5">
        {/* Rule Name */}
        <div>
          <label className="label-text block mb-2">Tên luật *</label>
          <input
            type="text"
            value={ruleName}
            onChange={(e) => setRuleName(e.target.value)}
            className="input-field"
            placeholder="VD: Tưới tự động khi đất khô"
            required
          />
        </div>

        {/* Rule Type */}
        <div>
          <label className="label-text block mb-3">Loại kích hoạt *</label>
          <div className="flex gap-2 bg-surface-container-low rounded-xl p-1.5">
            <button
              type="button"
              onClick={() => setRuleType('CONDITION')}
              className={`flex-1 py-2.5 rounded-lg text-sm font-semibold transition-all ${
                ruleType === 'CONDITION'
                  ? 'bg-surface-container-lowest text-primary shadow-card'
                  : 'text-on-surface-variant hover:text-on-surface'
              }`}
            >
              <span className="material-symbols-outlined text-sm align-middle mr-1">sensors</span>
              Theo cảm biến
            </button>
            <button
              type="button"
              onClick={() => setRuleType('SCHEDULE')}
              className={`flex-1 py-2.5 rounded-lg text-sm font-semibold transition-all ${
                ruleType === 'SCHEDULE'
                  ? 'bg-surface-container-lowest text-primary shadow-card'
                  : 'text-on-surface-variant hover:text-on-surface'
              }`}
            >
              <span className="material-symbols-outlined text-sm align-middle mr-1">schedule</span>
              Theo lịch trình
            </button>
          </div>
        </div>

        {/* Dynamic Fields: CONDITION */}
        {ruleType === 'CONDITION' && (
          <div className="space-y-4 bg-surface-container-low/50 rounded-xl p-4 animate-fade-in">
            <div>
              <label className="label-text block mb-2">Cảm biến giám sát *</label>
              <select
                value={triggerDeviceId}
                onChange={(e) => setTriggerDeviceId(e.target.value)}
                className="input-field"
                required
              >
                <option value="">-- Chọn cảm biến --</option>
                {sensors.map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="label-text block mb-2">Toán tử *</label>
                <select
                  value={operator}
                  onChange={(e) => setOperator(e.target.value as Operator)}
                  className="input-field"
                >
                  <option value="<">Nhỏ hơn (&lt;)</option>
                  <option value=">">Lớn hơn (&gt;)</option>
                  <option value="==">Bằng (==)</option>
                </select>
              </div>
              <div>
                <label className="label-text block mb-2">Ngưỡng *</label>
                <input
                  type="number"
                  step="0.1"
                  value={thresholdValue}
                  onChange={(e) => setThresholdValue(e.target.value ? parseFloat(e.target.value) : '')}
                  className="input-field"
                  placeholder="VD: 40.5"
                  required
                />
              </div>
            </div>
          </div>
        )}

        {/* Dynamic Fields: SCHEDULE */}
        {ruleType === 'SCHEDULE' && (
          <div className="bg-surface-container-low/50 rounded-xl p-4 animate-fade-in">
            <label className="label-text block mb-3">Thời gian thực hiện *</label>
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2 bg-surface-container-lowest rounded-xl px-4 py-3 border border-outline-variant">
                <input
                  type="number"
                  min={0}
                  max={23}
                  value={scheduleHour}
                  onChange={(e) => setScheduleHour(parseInt(e.target.value) || 0)}
                  className="w-12 text-center text-2xl font-bold text-on-surface bg-transparent focus:outline-none"
                />
                <span className="text-2xl font-bold text-on-surface-variant">:</span>
                <input
                  type="number"
                  min={0}
                  max={59}
                  value={scheduleMinute}
                  onChange={(e) => setScheduleMinute(parseInt(e.target.value) || 0)}
                  className="w-12 text-center text-2xl font-bold text-on-surface bg-transparent focus:outline-none"
                />
              </div>
              <span className="text-sm text-on-surface-variant">mỗi ngày</span>
            </div>
          </div>
        )}

        {/* Action Fields */}
        <div className="space-y-4 bg-primary-container/5 rounded-xl p-4 border border-primary/10">
          <h4 className="text-sm font-bold text-primary flex items-center gap-2">
            <span className="material-symbols-outlined text-sm">bolt</span>
            Hành động thực thi
          </h4>
          <div>
            <label className="label-text block mb-2">Thiết bị điều khiển *</label>
            <select
              value={actionDeviceId}
              onChange={(e) => setActionDeviceId(e.target.value)}
              className="input-field"
              required
            >
              <option value="">-- Chọn thiết bị --</option>
              {actuators.map((a) => (
                <option key={a.id} value={a.id}>{a.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="label-text block mb-2">Lệnh *</label>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setActionCommand('ON')}
                className={`flex-1 py-2.5 rounded-xl font-bold text-sm transition-all ${
                  actionCommand === 'ON'
                    ? 'bg-primary text-white'
                    : 'bg-surface-container-low text-on-surface-variant'
                }`}
              >
                BẬT (ON)
              </button>
              <button
                type="button"
                onClick={() => setActionCommand('OFF')}
                className={`flex-1 py-2.5 rounded-xl font-bold text-sm transition-all ${
                  actionCommand === 'OFF'
                    ? 'bg-on-surface text-white'
                    : 'bg-surface-container-low text-on-surface-variant'
                }`}
              >
                TẮT (OFF)
              </button>
            </div>
          </div>
        </div>

        {/* API Error */}
        {apiError && (
          <div className="flex items-center gap-2 px-4 py-3 bg-error-container/10 border border-error/20 rounded-xl animate-fade-in">
            <span className="material-symbols-outlined text-error text-lg">error</span>
            <p className="text-error text-sm font-medium">{apiError}</p>
          </div>
        )}

        {/* Actions */}
        <div className="flex items-center gap-3 justify-end pt-2">
          <button type="button" onClick={onClose} className="btn-secondary" disabled={isLoading}>Hủy</button>
          <button type="submit" className="btn-primary" disabled={isLoading} id="rule-form-submit">
            {isLoading ? (
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                {isEditing ? 'Đang lưu...' : 'Đang tạo...'}
              </div>
            ) : (
              isEditing ? 'Lưu thay đổi' : 'Tạo luật'
            )}
          </button>
        </div>
      </form>
    </Modal>
  )
}
