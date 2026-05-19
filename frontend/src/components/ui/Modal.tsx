import { ReactNode, useEffect, useRef } from 'react'

interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title: string
  children: ReactNode
  maxWidth?: string
}

export default function Modal({
  isOpen,
  onClose,
  title,
  children,
  maxWidth = 'max-w-lg',
}: ModalProps) {
  const overlayRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    if (isOpen) {
      document.addEventListener('keydown', handleEsc)
      document.body.style.overflow = 'hidden'
    }
    return () => {
      document.removeEventListener('keydown', handleEsc)
      document.body.style.overflow = ''
    }
  }, [isOpen, onClose])

  if (!isOpen) return null

  return (
    <div
      ref={overlayRef}
      className="fixed inset-0 z-[100] flex items-center justify-center p-4"
      onClick={(e) => {
        if (e.target === overlayRef.current) onClose()
      }}
    >
      {/* Backdrop */}
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm animate-fade-in" />
      
      {/* Modal Content */}
      <div
        className={`relative flex max-h-[calc(100dvh-2rem)] w-full flex-col overflow-hidden bg-surface-container-lowest rounded-3xl shadow-modal ${maxWidth} animate-scale-in`}
      >
        {/* Header */}
        <div className="flex shrink-0 items-center justify-between px-8 pt-8 pb-4">
          <h2 className="font-headline text-xl font-bold text-on-surface">{title}</h2>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-surface-container transition-colors"
            id="modal-close-btn"
          >
            <span className="material-symbols-outlined text-on-surface-variant">close</span>
          </button>
        </div>
        
        {/* Body */}
        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-8 pb-8">{children}</div>
      </div>
    </div>
  )
}
