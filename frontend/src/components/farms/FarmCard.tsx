import { useNavigate } from 'react-router-dom'
import { format } from 'date-fns'
import type { FarmResponse } from '@/types'
import { useState, useRef, useEffect } from 'react'

interface FarmCardProps {
  farm: FarmResponse
  onEdit: () => void
  onDelete: () => void
}

export default function FarmCard({ farm, onEdit, onDelete }: FarmCardProps) {
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false)
      }
    }
    if (menuOpen) document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [menuOpen])

  const farmIcons = ['eco', 'grass', 'local_florist', 'psychiatry', 'spa']
  const iconIndex = farm.name.length % farmIcons.length

  return (
    <div
      className="card cursor-pointer group relative hover:scale-[1.01] transition-transform duration-300"
      onClick={() => navigate(`/farms/${farm.id}`)}
    >
      {/* Icon */}
      <div className="w-14 h-14 rounded-2xl bg-primary-container/20 flex items-center justify-center mb-4">
        <span className="material-symbols-outlined text-primary text-2xl">{farmIcons[iconIndex]}</span>
      </div>

      {/* Content */}
      <h3 className="font-headline text-lg font-bold text-on-surface mb-1 pr-8">{farm.name}</h3>

      {farm.location && (
        <div className="flex items-center gap-1.5 mb-3">
          <span className="material-symbols-outlined text-sm text-on-surface-variant">location_on</span>
          <span className="text-sm text-on-surface-variant">{farm.location}</span>
        </div>
      )}

      <div className="flex items-center gap-1.5 mt-3">
        <span className="material-symbols-outlined text-sm text-on-surface-variant">calendar_today</span>
        <span className="text-xs text-on-surface-variant font-label">
          {format(new Date(farm.created_at), 'dd/MM/yyyy')}
        </span>
      </div>

      {/* Status Badge */}
      <div className="absolute top-6 right-6">
        <span className="badge-active">Active</span>
      </div>

      {/* Menu Button */}
      <div className="absolute top-14 right-5" ref={menuRef}>
        <button
          onClick={(e) => { e.stopPropagation(); setMenuOpen(!menuOpen) }}
          className="p-1.5 rounded-lg hover:bg-surface-container transition-colors opacity-0 group-hover:opacity-100"
        >
          <span className="material-symbols-outlined text-on-surface-variant text-lg">more_vert</span>
        </button>

        {menuOpen && (
          <div className="absolute right-0 top-10 w-40 bg-surface-container-lowest rounded-xl shadow-modal border border-surface-container-low py-1 animate-scale-in z-10">
            <button
              onClick={(e) => { e.stopPropagation(); setMenuOpen(false); onEdit() }}
              className="w-full flex items-center gap-2 px-4 py-2.5 text-sm text-on-surface hover:bg-surface-container-low transition-colors"
            >
              <span className="material-symbols-outlined text-lg">edit</span>
              Chỉnh sửa
            </button>
            <button
              onClick={(e) => { e.stopPropagation(); setMenuOpen(false); onDelete() }}
              className="w-full flex items-center gap-2 px-4 py-2.5 text-sm text-error hover:bg-error-container/10 transition-colors"
            >
              <span className="material-symbols-outlined text-lg">delete</span>
              Xóa
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
