import { Outlet } from 'react-router-dom';
import { Icon } from '../ui/Icon';
import logoClara from '../../assets/prosint-logo-clara.png';
import logoEscura from '../../assets/prosint-logo-escura.png';
import './AuthLayout.css';

export function AuthLayout() {
  return <main className="auth-layout">
    <aside className="auth-visual" aria-label="Sobre a plataforma ProSint">
      {/* "P" decorativo da marca ao fundo do painel. */}
      <svg className="auth-monogram" viewBox="231 226 429 564" aria-hidden="true" focusable="false">
        <path fillRule="evenodd" d="M231 226h224c125 0 205 74 205 185s-80 185-205 185H347v194H231V226Zm116 98v174h100c63 0 99-34 99-87s-36-87-99-87H347Z" />
      </svg>
      <img className="auth-logo" src={logoClara} alt="ProSint" width="3000" height="712" />
      <div className="auth-pitch">
        <span className="auth-badge"><i aria-hidden="true" />Plataforma com IA</span>
        <p className="auth-headline">Gestão processual com estatísticas e <em>sínteses por IA</em></p>
        <ul className="auth-features">
          <li><span className="auth-feature-icon"><Icon name="chart" /></span>
            <div><strong>Ferramentas</strong>
              <p>Consulta ao DataJud e às comunicações processuais do DJEN, com geração de indicadores estatísticos.</p></div>
          </li>
          <li><span className="auth-feature-icon"><Icon name="sparkles" /></span>
            <div><strong>Sínteses com IA</strong>
              <p>Geração de sínteses de estatísticas e comunicações processuais para facilitar a compreensão das informações.</p></div>
          </li>
        </ul>
      </div>
    </aside>
    <section className="auth-content" aria-label="Acesso à plataforma">
      <div className="auth-mobile-brand"><img src={logoEscura} alt="ProSint" width="3000" height="712" /></div>
      <Outlet />
    </section>
  </main>;
}
