import { Outlet } from 'react-router-dom';
import { Icon } from '../ui/Icon';
import reference from '../../assets/auth-reference.png';
import './AuthLayout.css';

export function AuthLayout() {
  return <main className="auth-layout">
    <div className="auth-visual" aria-hidden="true">
      {/* A janela SVG utiliza somente a fotografia à esquerda da referência fornecida. */}
      <svg className="auth-photo" viewBox="0 0 1083 870" preserveAspectRatio="xMidYMid slice">
        <image href={reference} width="1808" height="870" />
      </svg>
      <div className="auth-brand"><Icon name="scales" /><span>AXION<small>INTELIGÊNCIA JURÍDICA</small></span></div>
      <div className="auth-caption"><span className="auth-caption-line" />Clareza para decidir. Tempo para ir além.</div>
    </div>
    <section className="auth-content" aria-label="Acesso à plataforma">
      <div className="auth-mobile-brand"><Icon name="scales" /><span>AXION</span></div>
      <Outlet />
    </section>
  </main>;
}
