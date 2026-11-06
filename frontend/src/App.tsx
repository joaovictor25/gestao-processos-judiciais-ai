import { Navigate, Route, Routes } from 'react-router-dom';
import { AuthLayout } from './components/layout/AuthLayout';
import { AppLayout } from './components/layout/AppLayout';
import { ProtectedRoute, PublicRoute } from './routes/ProtectedRoute';
import { Login } from './pages/Login/Login';
import { Cadastro } from './pages/Cadastro/Cadastro';
import { Dashboard } from './pages/Dashboard/Dashboard';
import { Processos } from './pages/Processos/Processos';
import { NovoProcesso } from './pages/Processos/NovoProcesso';
import { EditarProcesso } from './pages/Processos/EditarProcesso';
import { ProcessoDetalhes } from './pages/Processos/ProcessoDetalhes';
import { NaoEncontrada } from './pages/NaoEncontrada';

export default function App() {
  return <Routes>
    <Route path="/" element={<Navigate to="/dashboard" replace />} />
    <Route element={<PublicRoute />}><Route element={<AuthLayout />}>
      <Route path="/login" element={<Login />} />
      <Route path="/cadastro" element={<Cadastro />} />
    </Route></Route>
    <Route element={<ProtectedRoute />}><Route element={<AppLayout />}>
      <Route path="/dashboard" element={<Dashboard />} />
      <Route path="/processos" element={<Processos />} />
      <Route path="/processos/novo" element={<NovoProcesso />} />
      <Route path="/processos/:id/editar" element={<EditarProcesso />} />
      <Route path="/processos/:id" element={<ProcessoDetalhes />} />
    </Route></Route>
    <Route path="*" element={<NaoEncontrada />} />
  </Routes>;
}
