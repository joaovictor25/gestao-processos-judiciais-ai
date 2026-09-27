import type { AnaliseProcesso, Processo, ProcessoRequest, ConsultaCnjResponse } from '../types/processo';
import type { EstatisticasProcessuais } from '../types/estatisticas';
import type { Comunicacao } from '../types/comunicacao';
import type { AnaliseComunicacao, TipoAnaliseComunicacao } from '../types/analiseComunicacao';
import { ApiError, apiRequest } from './api';
import { authService } from './authService';

function token() {
  const atual = authService.tokenAtual();
  if (!atual) throw new ApiError('Sua sessão expirou. Entre novamente.', 401);
  return atual;
}

export const processoService = {
  consultarCnj: (numeroProcesso: string, signal?: AbortSignal) => apiRequest<ConsultaCnjResponse>(`/api/processo/consultar-datajud?numeroProcesso=${encodeURIComponent(numeroProcesso)}`, { token: token(), signal }),
  listar: (signal?: AbortSignal) => apiRequest<Processo[]>('/api/processo/listar-usuario', { token: token(), signal }),
  buscar: (id: number, signal?: AbortSignal) => apiRequest<Processo>(`/api/processo/${id}`, { token: token(), signal }),
  cadastrar: (dados: ProcessoRequest) => apiRequest<Processo>('/api/processo/cadastrar', { method: 'POST', body: dados, token: token() }),
  atualizar: (id: number, dados: ProcessoRequest) => apiRequest<Processo>(`/api/processo/atualizar/${id}`, { method: 'PUT', body: dados, token: token() }),
  consultarAnalise: (id: number, signal?: AbortSignal) => apiRequest<AnaliseProcesso>(`/api/dados/analise/${id}`, { token: token(), signal }),
  analisar: (id: number) => apiRequest<string>(`/api/dados/analisar/${id}`, { method: 'POST', token: token(), responseType: 'text' }),
  estatisticas: (id: number, signal?: AbortSignal) => apiRequest<EstatisticasProcessuais[]>(`/api/dados/estatisticas/${id}`, { token: token(), signal }),
  comunicacoes: (id: number, signal?: AbortSignal) => apiRequest<Comunicacao[]>(`/api/dados/comunicacoes/${id}`, { token: token(), signal }),
  analisarComunicacoes: (id: number, tiposAnalise: TipoAnaliseComunicacao[], signal?: AbortSignal) =>
    apiRequest<AnaliseComunicacao>(`/api/dados/comunicacoes/${id}/analise-ia`,
      { method: 'POST', body: { tiposAnalise }, token: token(), signal }),
};
