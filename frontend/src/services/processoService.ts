import type { AnaliseProcesso, Processo, ProcessoRequest, ConsultaCnjResponse } from '../types/processo';
import type { EstatisticasProcessuais, SinteseDataJud, HistoricoSinteses, SolicitacaoEstatistica } from '../types/estatisticas';
import type { Comunicacao } from '../types/comunicacao';
import type { AnaliseComunicacao, TipoAnaliseComunicacao, PesquisaPublicacoesRequest, PesquisaComunicacao, HistoricoSintesesComunicacoes, DetalhePesquisaComunicacao } from '../types/analiseComunicacao';
import { ApiError, apiRequest } from './api';
import { authService } from './authService';

function token() {
  const atual = authService.tokenAtual();
  if (!atual) throw new ApiError('Sua sessão expirou. Entre novamente.', 401);
  return atual;
}

export const processoService = {
  pesquisaAtualComunicacoes: (id: number, signal?: AbortSignal) =>
    apiRequest<PesquisaComunicacao | undefined>(`/api/dados/comunicacoes/${id}/pesquisas/atual`, { token: token(), signal }),
  sintetizarComunicacoes: (id: number, comunicacaoIds: number[], pesquisaId?: number, signal?: AbortSignal) =>
    apiRequest<PesquisaComunicacao>(`/api/dados/comunicacoes/${id}/sintese-ia`, { method: 'POST', token: token(), body: { pesquisaId, comunicacaoIds }, signal }),
  historicoSintesesComunicacoes: (id: number, pagina = 0, signal?: AbortSignal) =>
    apiRequest<HistoricoSintesesComunicacoes>(`/api/dados/comunicacoes/${id}/sinteses?pagina=${pagina}`, { token: token(), signal }),
  pesquisaComunicacoes: (id: number, pesquisaId: number, signal?: AbortSignal) =>
    apiRequest<DetalhePesquisaComunicacao>(`/api/dados/comunicacoes/${id}/pesquisas/${pesquisaId}`, { token: token(), signal }),
  historicoSinteses: (id: number, pagina = 0, signal?: AbortSignal) =>
    apiRequest<HistoricoSinteses>(`/api/dados/estatisticas/${id}/sinteses?pagina=${pagina}`, { token: token(), signal }),
  execucaoEstatistica: (id: number, estatisticaId: number, signal?: AbortSignal) =>
    apiRequest<EstatisticasProcessuais>(`/api/dados/estatisticas/${id}/${estatisticaId}`, { token: token(), signal }),
  sintetizarEstatisticas: (id: number, estatisticaId: number, signal?: AbortSignal) =>
    apiRequest<SinteseDataJud>(`/api/dados/estatisticas/${id}/sintese-ia`, { method: 'POST', token: token(), body: { estatisticaId }, signal }),
  gerarEstatisticas: (id: number, filtros: { dataInicio?: string; dataFim?: string }, signal?: AbortSignal) =>
    apiRequest<SolicitacaoEstatistica>(`/api/dados/estatisticas/${id}/solicitacoes`, { method: 'POST', token: token(), body: filtros, signal }),
  geracaoAtual: (id: number, signal?: AbortSignal) =>
    apiRequest<SolicitacaoEstatistica | undefined>(`/api/dados/estatisticas/${id}/solicitacoes/atual`, { token: token(), signal }),
  consultarGeracao: (id: number, solicitacaoId: string, signal?: AbortSignal) =>
    apiRequest<SolicitacaoEstatistica>(`/api/dados/estatisticas/${id}/solicitacoes/${encodeURIComponent(solicitacaoId)}`, { token: token(), signal }),
  consultarCnj: (numeroProcesso: string, signal?: AbortSignal) => apiRequest<ConsultaCnjResponse>(`/api/processo/consultar-datajud?numeroProcesso=${encodeURIComponent(numeroProcesso)}`, { token: token(), signal }),
  listar: (signal?: AbortSignal) => apiRequest<Processo[]>('/api/processo/listar-usuario', { token: token(), signal }),
  buscar: (id: number, signal?: AbortSignal) => apiRequest<Processo>(`/api/processo/${id}`, { token: token(), signal }),
  cadastrar: (dados: ProcessoRequest) => apiRequest<Processo>('/api/processo/cadastrar', { method: 'POST', body: dados, token: token() }),
  atualizar: (id: number, dados: ProcessoRequest) => apiRequest<Processo>(`/api/processo/atualizar/${id}`, { method: 'PUT', body: dados, token: token() }),
  excluir: (id: number) => apiRequest<string>(`/api/processo/deletar/${id}`, { method: 'DELETE', token: token(), responseType: 'text' }),
  consultarAnalise: (id: number, signal?: AbortSignal) => apiRequest<AnaliseProcesso>(`/api/dados/analise/${id}`, { token: token(), signal }),
  analisar: (id: number) => apiRequest<string>(`/api/dados/analisar/${id}`, { method: 'POST', token: token(), responseType: 'text' }),
  estatisticas: (id: number, signal?: AbortSignal) => apiRequest<EstatisticasProcessuais[]>(`/api/dados/estatisticas/${id}`, { token: token(), signal }),
  comunicacoes: (id: number, signal?: AbortSignal) => apiRequest<Comunicacao[]>(`/api/dados/comunicacoes/${id}`, { token: token(), signal }),
  pesquisarPublicacoes: (id: number, filtros: PesquisaPublicacoesRequest, signal?: AbortSignal) =>
    apiRequest<Comunicacao[]>(`/api/dados/comunicacoes/${id}/pesquisar`, { method: 'POST', body: filtros, token: token(), signal }),
  analisarComunicacoes: (id: number, tiposAnalise: TipoAnaliseComunicacao[], comunicacaoIds: number[], signal?: AbortSignal) =>
    apiRequest<AnaliseComunicacao>(`/api/dados/comunicacoes/${id}/analise-ia`,
      { method: 'POST', body: { tiposAnalise, comunicacaoIds }, token: token(), signal }),
};
