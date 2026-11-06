import { Link, Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Button } from '../components/ui/Button';
import { Icon } from '../components/ui/Icon';

export function SessionLoading() {
  return <div className="session-screen" role="status"><span className="spinner" />Carregando sua conta…</div>;
}

export function ProtectedRoute() {
  const { autenticado, carregando, erroInicial, atualizarUsuario } = useAuth();
  if (carregando) return <SessionLoading />;
  if (erroInicial) return <main className="session-screen"><Icon name="alert" />
    <h1>Não foi possível carregar sua conta</h1><p role="alert">{erroInicial}</p>
    <Button onClick={() => void atualizarUsuario()}>Tentar novamente</Button><Link to="/login">Voltar para o login</Link>
  </main>;
  return autenticado ? <Outlet /> : <Navigate to="/login" replace />;
}

export function PublicRoute() {
  const { autenticado, carregando } = useAuth();
  if (carregando) return <SessionLoading />;
  return autenticado ? <Navigate to="/dashboard" replace /> : <Outlet />;
}
