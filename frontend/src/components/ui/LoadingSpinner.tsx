interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  text?: string
}

export default function LoadingSpinner({ size = 'md', text }: LoadingSpinnerProps) {
  const sizeClasses = {
    sm: 'w-5 h-5 border-2',
    md: 'w-10 h-10 border-3',
    lg: 'w-14 h-14 border-4',
  }

  return (
    <div className="flex flex-col items-center justify-center gap-3 py-12">
      <div
        className={`${sizeClasses[size]} border-primary/20 border-t-primary rounded-full animate-spin`}
      />
      {text && (
        <span className="text-on-surface-variant font-label text-sm">{text}</span>
      )}
    </div>
  )
}
