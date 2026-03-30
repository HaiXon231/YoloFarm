interface EmptyStateProps {
  icon: string
  title: string
  description: string
  action?: React.ReactNode
}

export default function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-20 animate-fade-in">
      <div className="w-20 h-20 rounded-3xl bg-surface-container-low flex items-center justify-center mb-6">
        <span className="material-symbols-outlined text-4xl text-outline-variant">{icon}</span>
      </div>
      <h3 className="font-headline text-lg font-bold text-on-surface mb-2">{title}</h3>
      <p className="text-sm text-on-surface-variant font-label text-center max-w-sm mb-6">
        {description}
      </p>
      {action}
    </div>
  )
}
