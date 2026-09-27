import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import type { AuthContextType, LoginRequest, Usuario } from '../types/auth';
import { authService } from '../services/authService';
import { ApiError, mensagemErro, SESSION_EXPIRED_EVENT } from '../services/api';

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erroInicial, setErroInicial] = useState<string | null>(null);

  const limparSessao = useCallback(() => {
    authService.limparToken();
    setUsuario(null);
    setErroInicial(null);
  }, []);

  const carregarUsuario = useCallback(async (signal?: AbortSignal) => {
    setCarregando(true);
    setErroInicial(null);
    try {
      const token = authService.tokenAtual();
      if (!token) { setUsuario(null); return; }
      const dados = await authService.me(token, signal);
      if (!signal?.aborted) setUsuario(dados);
    } catch (error) {
      if (signal?.aborted) return;
      if (error instanceof ApiError && error.status === 401) limparSessao();
      else setErroInicial(mensagemErro(error));
    } finally {
      if (!signal?.aborted) setCarregando(false);
    }
  }, [limparSessao]);

  useEffect(() => {
    const controller = new AbortController();
    window.addEventListener(SESSION_EXPIRED_EVENT, limparSessao);
    void carregarUsuario(controller.signal);
    return () => {
      controller.abort();
      window.removeEventListener(SESSION_EXPIRED_EVENT, limparSessao);
    };
  }, [carregarUsuario, limparSessao]);

  async function login(dados: LoginRequest) {
    const resposta = await authService.login(dados);
    authService.guardarToken(resposta.token);
    setUsuario({ id: resposta.usuarioId, nome: resposta.nome, email: resposta.email, role: resposta.role });
    setErroInicial(null);
  }

  async function logout() {
    const token = authService.tokenAtual();
    if (token) await authService.logout(token);
    limparSessao();
  }

  return <AuthContext.Provider value={{ usuario, autenticado: usuario !== null, carregando, erroInicial,
    login, logout, atualizarUsuario: carregarUsuario }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth deve ser utilizado dentro de AuthProvider');
  return context;
}
