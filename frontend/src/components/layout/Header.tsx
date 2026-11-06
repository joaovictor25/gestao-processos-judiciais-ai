import { useLocation } from 'react-router-dom';
import { Icon } from '../ui/Icon';

export function Header({ menuOpen, toggleMenu }: { menuOpen: boolean; toggleMenu: () => void }) {
  const { pathname } = useLocation();
  const dashboard = pathname === '/dashboard';
  const titulo = dashboard ? 'Dashboard' : 'Processos';
  const etapa = dashboard ? 'Visão geral' : pathname === '/processos/novo' ? 'Novo processo' : pathname.endsWith('/editar') ? 'Editar processo' : /^\/processos\/[^/]+$/.test(pathname) ? 'Detalhes do processo' : 'Lista de processos';

  return <header className="app-header">
    <button type="button" className="icon-button menu-toggle" onClick={toggleMenu} aria-label={menuOpen ? 'Recolher menu' : 'Abrir menu'}
      aria-expanded={menuOpen} aria-controls="main-sidebar"><Icon name="menu" /></button>
    <div className="breadcrumb"><strong>{titulo}</strong><Icon name="chevron" /><span>{etapa}</span></div>
    <div className="header-tools">
      <span className="session-status"><i />Sessão ativa</span>
    </div>
  </header>;
}
