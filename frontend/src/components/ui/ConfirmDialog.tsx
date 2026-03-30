import Modal from './Modal'

interface ConfirmDialogProps {
  isOpen: boolean
  onClose: () => void
  onConfirm: () => void
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  variant?: 'danger' | 'primary'
  isLoading?: boolean
}

export default function ConfirmDialog({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = 'Xác nhận',
  cancelText = 'Hủy',
  variant = 'danger',
  isLoading = false,
}: ConfirmDialogProps) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} maxWidth="max-w-md">
      <p className="text-on-surface-variant text-sm mb-8">{message}</p>
      <div className="flex items-center gap-3 justify-end">
        <button
          onClick={onClose}
          className="btn-secondary"
          disabled={isLoading}
        >
          {cancelText}
        </button>
        <button
          onClick={onConfirm}
          className={variant === 'danger' ? 'btn-danger' : 'btn-primary'}
          disabled={isLoading}
          id="confirm-action-btn"
        >
          {isLoading ? (
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              Đang xử lý...
            </div>
          ) : (
            confirmText
          )}
        </button>
      </div>
    </Modal>
  )
}
