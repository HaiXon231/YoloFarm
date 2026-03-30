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

  const renderCondition = (rule: RuleResponse) => {
    if (rule.rule_type === 'CONDITION') {
      const triggerName = getDeviceName(rule.trigger_device_id)
      return `${triggerName} ${rule.operator} ${rule.threshold_value}`
    }
    if (rule.cron_expression) {
      // Parse simple cron to human-readable
      const parts = rule.cron_expression.split(' ')
      if (parts.length >= 2) {
        return `Mỗi ngày lúc ${parts[1].padStart(2, '0')}:${parts[0].padStart(2, '0')}`
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
                      <span className={`material-symbols-outlined text-sm ${
                        rule.rule_type === 'CONDITION' ? 'text-primary' : 'text-tertiary'
                      }`}>
                        {rule.rule_type === 'CONDITION' ? 'sensors' : 'schedule'}
                      </span>
                      <span className="font-semibold text-on-surface text-sm">{rule.rule_name}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 text-sm text-on-surface-variant">{renderCondition(rule)}</td>
                  <td className="px-6 py-4 text-sm">
                    <span className="text-on-surface font-medium">
                      {rule.action_command === 'ON' ? 'BẬT' : 'TẮT'} {getDeviceName(rule.action_device_id)}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-center">
                    <RuleToggleSwitch rule={rule} onUpdate={fetchRules} />
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
