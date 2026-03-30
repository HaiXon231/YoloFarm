import ConfirmDialog from '@/components/ui/ConfirmDialog'

interface DeleteConfirmModalProps {
  isOpen: boolean
  onClose: () => void
  onConfirm: () => void
  farmName: string
}

export default function DeleteConfirmModal({ isOpen, onClose, onConfirm, farmName }: DeleteConfirmModalProps) {
  return (
    <ConfirmDialog
      isOpen={isOpen}
      onClose={onClose}
      onConfirm={onConfirm}
      title="Xóa nông trại"
      message={`Bạn có chắc chắn muốn xóa nông trại "${farmName}"? Hành động này không thể hoàn tác.`}
      confirmText="Xóa nông trại"
      variant="danger"
    />
  )
}
