import type { ButtonHTMLAttributes } from 'react';

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean;
  variant?: 'primary' | 'secondary' | 'ghost';
}

export function Button({ children, loading, disabled, className = '', variant = 'primary', ...props }: Props) {
  return <button type="button" {...props} className={`button button--${variant} ${className}`}
    disabled={disabled || loading} aria-busy={loading || undefined}>
    {loading && <span className="spinner" aria-hidden="true" />}{children}
  </button>;
}
