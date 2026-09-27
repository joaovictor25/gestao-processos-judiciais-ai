export interface Usuario {
  id: number;
  nome: string;
  email: string;
  role: 'ADMIN' | 'USER';
}

export interface LoginRequest { email: string; senha: string }
export interface CadastroUsuarioRequest extends LoginRequest { nome: string }
export interface LoginResponse {
  token: string;
  tipo: string;
  usuarioId: number;
  nome: string;
  email: string;
  role: Usuario['role'];
}

export interface AuthContextType {
  usuario: Usuario | null;
  autenticado: boolean;
  carregando: boolean;
  erroInicial: string | null;
  login: (dados: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  atualizarUsuario: () => Promise<void>;
}
