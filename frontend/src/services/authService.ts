import type { CadastroUsuarioRequest, LoginRequest, LoginResponse, Usuario } from '../types/auth';
import { ApiError, apiRequest } from './api';

const TOKEN_KEY = 'axion.auth.token';

export const authService = {
  cadastrar: (dados: CadastroUsuarioRequest) => apiRequest<Usuario>('/api/auth/cadastro', { method: 'POST', body: dados }),
  login: (dados: LoginRequest) => apiRequest<LoginResponse>('/api/auth/login', { method: 'POST', body: dados }),
  me: (token: string, signal?: AbortSignal) => apiRequest<Usuario>('/api/auth/me', { token, signal }),
  async logout(token: string) {
    try { await apiRequest<void>('/api/auth/logout', { method: 'POST', token }); }
    catch (error) {
      // Um token já inválido também pode ser descartado pelo cliente.
      if (!(error instanceof ApiError && error.status === 401)) throw error;
    }
  },
  tokenAtual() { return sessionStorage.getItem(TOKEN_KEY); },
  guardarToken(token: string) { sessionStorage.setItem(TOKEN_KEY, token); },
  limparToken() { sessionStorage.removeItem(TOKEN_KEY); },
};
