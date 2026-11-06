import { NavLink, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { Icon } from '../ui/Icon';
import { mensagemErro } from '../../services/api';

export function iniciais(nome: string) {
  const partes = nome.trim().split(/\s+/);
  return ((partes[0]?.[0] ?? '') + (partes.length > 1 ? partes.at(-1)?.[0] ?? '' : '')).toUpperCase();
}

export function Sidebar({ close }: { close: () => void }) {
  const { usuario, logout } = useAuth();
  const navigate = useNavigate();
  const [saindo, setSaindo] = useState(false);
  const [erro, setErro] = useState('');

  async function sair() {
    setSaindo(true);
    setErro('');
    try { await logout(); navigate('/login', { replace: true }); }
    catch (error) { setErro(mensagemErro(error)); }
    finally { setSaindo(false); }
  }

  return <aside className="sidebar" id="main-sidebar" aria-label="Menu principal">
    <div className="sidebar-brand"><Icon name="scales" /><span>AXION<small>GESTÃO JURÍDICA</small></span>
      <button type="button" className="sidebar-close icon-button" aria-label="Fechar menu" onClick={close}><Icon name="close" /></button>
    </div>
    <div className="sidebar-section-label">ESCRITÓRIO</div>
    <nav className="sidebar-nav" aria-label="Navegação principal">
      <NavLink to="/dashboard" onClick={close}><Icon name="grid" /><span>Dashboard</span></NavLink>
      <NavLink to="/processos" onClick={close}><Icon name="scales" /><span>Processos</span></NavLink>
    </nav>
    <div className="sidebar-foot">
      <div className="sidebar-user"><span className="avatar">{iniciais(usuario?.nome ?? '')}</span>
        <div><strong title={usuario?.nome}>{usuario?.nome}</strong><span>{usuario?.role === 'ADMIN' ? 'Administrador' : 'Usuário'}</span></div>
      </div>
      {erro && <p className="sidebar-error" role="alert">{erro}</p>}
      <button type="button" className="logout-button" onClick={() => void sair()} disabled={saindo} aria-busy={saindo}>
        <Icon name="logout" />{saindo ? 'Saindo…' : 'Sair da conta'}
      </button>
    </div>
  </aside>;
}
