import { useEffect, useRef, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Header } from './Header';
import { Sidebar } from './Sidebar';
import './AppLayout.css';

export function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(() => window.matchMedia('(min-width: 1024px)').matches);
  const { pathname } = useLocation();
  const main = useRef<HTMLElement>(null);

  useEffect(() => {
    const media = window.matchMedia('(min-width: 1024px)');
    const resize = () => setMenuOpen(media.matches);
    const escape = (event: KeyboardEvent) => { if (event.key === 'Escape') setMenuOpen(false); };
    media.addEventListener('change', resize);
    window.addEventListener('keydown', escape);
    return () => { media.removeEventListener('change', resize); window.removeEventListener('keydown', escape); };
  }, []);

  useEffect(() => { main.current?.focus({ preventScroll: true }); }, [pathname]);

  function closeMobileMenu() {
    if (!window.matchMedia('(min-width: 1024px)').matches) setMenuOpen(false);
  }

  return <div className={`app-layout ${menuOpen ? 'app-layout--sidebar-open' : ''}`}>
    <a className="skip-link" href="#main-content">Pular para o conteúdo</a>
    <Sidebar close={closeMobileMenu} />
    {menuOpen && <button type="button" className="sidebar-backdrop" onClick={() => setMenuOpen(false)} aria-label="Fechar menu lateral" tabIndex={-1} />}
    <div className="app-body">
      <Header menuOpen={menuOpen} toggleMenu={() => setMenuOpen(!menuOpen)} />
      <main className="app-main" id="main-content" ref={main} tabIndex={-1}><Outlet /></main>
    </div>
  </div>;
}
