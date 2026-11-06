import { useState, type InputHTMLAttributes, type Ref } from 'react';
import { Icon, type IconName } from '../ui/Icon';

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  ref?: Ref<HTMLInputElement>;
  id: string;
  label: string;
  icon?: IconName;
  error?: string;
  hint?: string;
}

export function Input({ id, label, icon, error, hint, type = 'text', ...props }: InputProps) {
  const [visible, setVisible] = useState(false);
  const password = type === 'password';
  return <div className={`field ${error ? 'field--invalid' : ''}`}>
    <label htmlFor={id}>{label}{props.required && <span className="required-mark" aria-hidden="true"> *</span>}</label>
    <div className="input-wrap">
      {icon && <Icon name={icon} className="input-icon" />}
      <input {...props} id={id} type={password && visible ? 'text' : type}
        className={`${icon ? 'input--icon' : ''} ${password ? 'input--password' : ''}`}
        aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : hint ? `${id}-hint` : undefined} />
      {password && <button className="password-toggle" type="button" disabled={props.disabled}
        aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'} aria-pressed={visible}
        onClick={() => setVisible(!visible)}><Icon name={visible ? 'eye' : 'eyeOff'} /></button>}
    </div>
    {error ? <p id={`${id}-error`} className="field-error">{error}</p>
      : hint && <p id={`${id}-hint`} className="field-hint">{hint}</p>}
  </div>;
}
