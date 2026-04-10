import { useState, useEffect } from 'react'
import toast from 'react-hot-toast'
import api, { getApiErrorMessage } from '@/lib/axios'
import type { DeviceWithModel, RuleResponse } from '@/types'
import RuleToggleSwitch from './RuleToggleSwitch'
import RuleFormModal from './RuleFormModal'
import ConfirmDialog from '@/components/ui/ConfirmDialog'
import EmptyState from '@/components/ui/EmptyState'
import LoadingSpinner from '@/components/ui/LoadingSpinner'

interface RulesTabProps {
  farmId: string
  devices: DeviceWithModel[]
}

type RuleDiagnostic = {
  canActivate: boolean
  reason: string | null
}

function parseScheduleTimeFromCron(cronExpression: string): { hour: number; minute: number } | null {
  const parts = cronExpression.trim().split(/\s+/)

  let minuteRaw: string | undefined
  let hourRaw: string | undefined

  if (parts.length === 5) {
    ;[minuteRaw, hourRaw] = parts
  } else if (parts.length === 6) {
    ;[, minuteRaw, hourRaw] = parts
  } else {
    return null
  }

  const minute = Number.parseInt(minuteRaw ?? '', 10)
  const hour = Number.parseInt(hourRaw ?? '', 10)

  if (Number.isNaN(hour) || Number.isNaN(minute)) return null
  if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null

  return { hour, minute }
}

export default function RulesTab({ farmId, devices }: RulesTabProps) {
  const [rules, setRules] = useState<RuleResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [isDeleteOpen, setIsDeleteOpen] = useState(false)
  const [editingRule, setEditingRule] = useState<RuleResponse | null>(null)
  const [deletingRuleId, setDeletingRuleId] = useState<string | null>(null)

  const fetchRules = async () => {
    try {
      const res = await api.get<RuleResponse[]>(`/farms/${farmId}/rules`)
      setRules(res.data)
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { fetchRules() }, [farmId])

  const handleCreate = () => { setEditingRule(null); setIsFormOpen(true) }
  const handleEdit = (rule: RuleResponse) => { setEditingRule(rule); setIsFormOpen(true) }
  const handleDeleteClick = (ruleId: string) => { setDeletingRuleId(ruleId); setIsDeleteOpen(true) }

  const handleDelete = async () => {
    if (!deletingRuleId) return
    try {
      await api.delete(`/rules/${deletingRuleId}`)
      toast.success('Đã xóa luật thành công!')
      setIsDeleteOpen(false)
      fetchRules()
    } catch (error) {
      toast.error(getApiErrorMessage(error))
    }
  }

  const getDeviceName = (deviceId: string | null) => {
    if (!deviceId) return '—'
    return devices.find((d) => d.id === deviceId)?.name || deviceId.slice(0, 8)
  }

  const normalizeOperator = (operator: string | null) => (operator ?? '').trim()
  const isLowOperator = (operator: string) => operator === '<' || operator === '<='
  const isHighOperator = (operator: string) => operator === '>' || operator === '>='

  const normalizeCron = (cronExpression: string | null) => {
    if (!cronExpression) return ''
    const parts = cronExpression.trim().split(/\s+/)
    if (parts.length === 5) return `0 ${parts.join(' ')}`
    return parts.join(' ')
  }

  const buildRuleDiagnostics = (allRules: RuleResponse[]): Record<string, RuleDiagnostic> => {
    const diagnostics: Record<string, RuleDiagnostic> = {}

    const keyFor = (rule: RuleResponse) =>
      rule.rule_type === 'CONDITION'
        ? `CONDITION:${rule.action_device_id}:${rule.trigger_device_id ?? ''}`
        : `SCHEDULE:${rule.action_device_id}`

    const grouped = new Map<string, RuleResponse[]>()
    for (const rule of allRules) {
      const key = keyFor(rule)
      if (!grouped.has(key)) grouped.set(key, [])
      grouped.get(key)!.push(rule)
    }

    for (const [, group] of grouped) {
      const onRules = group.filter((r) => r.action_command === 'ON')
      const offRules = group.filter((r) => r.action_command === 'OFF')

      const bothInactive = group.length === 2 && group.every((r) => !r.is_active)

      let canActivate = false
      let reason: string | null = null

      if (onRules.length !== 1 || offRules.length !== 1) {
        reason = 'Thiếu hoặc dư cặp ON/OFF cho cùng nhóm rule.'
      } else {
        const onRule = onRules[0]
        const offRule = offRules[0]

        if (onRule.rule_type === 'CONDITION') {
          const onOperator = normalizeOperator(onRule.operator)
          const offOperator = normalizeOperator(offRule.operator)
          const onThreshold = onRule.threshold_value
          const offThreshold = offRule.threshold_value

          if (onThreshold == null || offThreshold == null) {
            reason = 'Rule CONDITION thiếu threshold để ghép cặp hợp lệ.'
          } else {
            const validLowToHigh = isLowOperator(onOperator) && isHighOperator(offOperator) && onThreshold < offThreshold
            const validHighToLow = isHighOperator(onOperator) && isLowOperator(offOperator) && offThreshold < onThreshold
            canActivate = validLowToHigh || validHighToLow
            if (!canActivate) {
              reason = 'Cặp CONDITION mâu thuẫn logic (không đạt hysteresis ON/OFF).'
            }
          }
        } else {
          const onCron = normalizeCron(onRule.cron_expression)
          const offCron = normalizeCron(offRule.cron_expression)
          canActivate = onCron.length > 0 && offCron.length > 0 && onCron !== offCron
          if (!canActivate) {
            reason = 'Cặp SCHEDULE không hợp lệ: ON/OFF bị trùng lịch.'
          }
        }
      }

      for (const rule of group) {
        diagnostics[rule.id] = {
          canActivate,
          reason: !canActivate
            ? reason
            : bothInactive
              ? 'Rule đang tắt theo cặp ON/OFF.'
              : null,
        }
      }
    }

    return diagnostics
  }

  const diagnosticsByRuleId = buildRuleDiagnostics(rules)

  const getRuleHint = (rule: RuleResponse) => diagnosticsByRuleId[rule.id]?.reason ?? null

  const isToggleBlocked = (rule: RuleResponse) => {
    if (rule.is_active) return false
    const diagnostic = diagnosticsByRuleId[rule.id]
    return diagnostic ? !diagnostic.canActivate : false
  }

  const renderCondition = (rule: RuleResponse) => {
    if (rule.rule_type === 'CONDITION') {
      const triggerName = getDeviceName(rule.trigger_device_id)
      return `${triggerName} ${rule.operator} ${rule.threshold_value}`
    }
    if (rule.cron_expression) {
      const parsedTime = parseScheduleTimeFromCron(rule.cron_expression)
      if (parsedTime) {
        return `Mỗi ngày lúc ${String(parsedTime.hour).padStart(2, '0')}:${String(parsedTime.minute).padStart(2, '0')}`
      }
    }
    return rule.cron_expression || '—'
  }

  if (isLoading) return <LoadingSpinner text="Đang tải luật tự động hóa..." />

  return (
    <div className="animate-fade-in">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h3 className="font-headline text-lg font-bold text-on-surface">Luật tự động hóa</h3>
          <p className="text-xs text-on-surface-variant font-label mt-1">
            Thiết lập bộ não cho nông trại của bạn
          </p>
        </div>
        <button onClick={handleCreate} className="btn-primary-gradient flex items-center gap-2" id="create-rule-btn">
          <span className="material-symbols-outlined text-lg">add</span>
          Tạo luật mới
        </button>
      </div>

      {rules.length === 0 ? (
        <EmptyState
          icon="robot_2"
          title="Chưa có luật tự động hóa"
          description="Tạo luật đầu tiên để tự động điều khiển thiết bị dựa trên cảm biến hoặc lịch trình."
          action={
            <button onClick={handleCreate} className="btn-primary flex items-center gap-2">
              <span className="material-symbols-outlined text-lg">add</span>
              Tạo luật mới
            </button>
          }
        />
      ) : (
        <div className="card overflow-hidden p-0">
          <table className="w-full">
            <thead>
              <tr className="border-b border-surface-container-low">
                <th className="text-left px-6 py-4 label-text">Tên luật</th>
                <th className="text-left px-6 py-4 label-text">Điều kiện</th>
                <th className="text-left px-6 py-4 label-text">Hành động</th>
                <th className="text-center px-6 py-4 label-text">Trạng thái</th>
                <th className="text-right px-6 py-4 label-text">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {rules.map((rule) => (
                <tr key={rule.id} className="border-b border-surface-container-low/50 hover:bg-surface-container-low/30 transition-colors">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2">
                      <span className={`material-symbols-outlined text-sm ${rule.rule_type === 'CONDITION' ? 'text-primary' : 'text-tertiary'
                        }`}>
                        {rule.rule_type === 'CONDITION' ? 'sensors' : 'schedule'}
                      </span>
                      <div className="flex flex-col">
                        <span className="font-semibold text-on-surface text-sm">{rule.rule_name}</span>
                        {getRuleHint(rule) && (
                          <span className={`text-xs mt-0.5 ${isToggleBlocked(rule) ? 'text-error' : 'text-on-surface-variant'}`}>
                            {getRuleHint(rule)}
                          </span>
                        )}
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-sm text-on-surface-variant">{renderCondition(rule)}</td>
                  <td className="px-6 py-4 text-sm">
                    <span className="text-on-surface font-medium">
                      {rule.action_command === 'ON' ? 'BẬT' : 'TẮT'} {getDeviceName(rule.action_device_id)}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-center">
                    <RuleToggleSwitch
                      rule={rule}
                      onUpdate={fetchRules}
                      disabled={isToggleBlocked(rule)}
                      disabledReason={isToggleBlocked(rule) ? getRuleHint(rule) : null}
                    />
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex items-center gap-1 justify-end">
                      <button
                        onClick={() => handleEdit(rule)}
                        className="p-2 rounded-lg hover:bg-surface-container transition-colors"
                        title="Sửa"
                      >
                        <span className="material-symbols-outlined text-on-surface-variant text-lg">edit</span>
                      </button>
                      <button
                        onClick={() => handleDeleteClick(rule.id)}
                        className="p-2 rounded-lg hover:bg-error-container/10 transition-colors"
                        title="Xóa"
                      >
                        <span className="material-symbols-outlined text-error text-lg">delete</span>
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <RuleFormModal
        isOpen={isFormOpen}
        onClose={() => { setIsFormOpen(false); setEditingRule(null) }}
        farmId={farmId}
        devices={devices}
        rule={editingRule}
        onSuccess={() => { setIsFormOpen(false); setEditingRule(null); fetchRules() }}
      />

      <ConfirmDialog
        isOpen={isDeleteOpen}
        onClose={() => { setIsDeleteOpen(false); setDeletingRuleId(null) }}
        onConfirm={handleDelete}
        title="Xóa luật tự động hóa"
        message="Bạn có chắc chắn muốn xóa luật này? Thiết bị sẽ không còn được điều khiển tự động bởi luật này."
        confirmText="Xóa luật"
        variant="danger"
      />
    </div>
  )
}
