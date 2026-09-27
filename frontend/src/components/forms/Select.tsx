import type { SelectHTMLAttributes } from 'react';
import { Icon } from '../ui/Icon';

interface Props extends SelectHTMLAttributes<HTMLSelectElement> {
  id: string;
  label: string;
  options: readonly string[];
  error?: string;
  placeholder?: string;
}

export function Select({ id, label, options, error, placeholder = 'Selecione', value, ...props }: Props) {
  const atual = String(value ?? '');
  return <div className={`field ${error ? 'field--invalid' : ''}`}>
    <label htmlFor={id}>{label}{props.required && <span className="required-mark" aria-hidden="true"> *</span>}</label>
    <div className="input-wrap process-select">
      <select {...props} id={id} value={atual} aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined}>
        <option value="">{placeholder}</option>
        {atual && !options.includes(atual) && <option value={atual}>{atual}</option>}
        {options.map(option => <option key={option} value={option}>{option}</option>)}
      </select><Icon name="down" />
    </div>
    {error && <p id={`${id}-error`} className="field-error">{error}</p>}
  </div>;
}
