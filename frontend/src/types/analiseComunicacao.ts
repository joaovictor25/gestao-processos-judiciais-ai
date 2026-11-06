import type { Comunicacao } from './comunicacao';

export interface PesquisaPublicacoesRequest {
  texto: string;
  dataDisponibilizacaoInicio?: string;
  dataDisponibilizacaoFim?: string;
}
export interface ComunicacaoConsultada extends Omit<Comunicacao, 'id' | 'processoId'> { numero: number }

export const tiposAnaliseComunicacao = [
  { codigo: 'SINTESE_GERAL', titulo: 'Síntese geral' },
  { codigo: 'FUNDAMENTOS_DECISOES', titulo: 'Fundamentos e decisões' },
  { codigo: 'VALORES', titulo: 'Valores' },
] as const;

export type TipoAnaliseComunicacao = typeof tiposAnaliseComunicacao[number]['codigo'];
export interface EvidenciaComunicacao { comunicacao: number; trecho: string }
export interface ItemAnaliseComunicacao {
  titulo: string;
  descricao: string;
  contexto: string | null;
  decisaoRelacionada: string | null;
  evidencias: EvidenciaComunicacao[];
}
export interface CategoriaAnaliseComunicacao {
  tipo: TipoAnaliseComunicacao;
  itens: ItemAnaliseComunicacao[];
  limitacao: string | null;
}
export interface AnaliseComunicacao {
  comunicacoesConsideradas: number;
  duplicadasIgnoradas: number;
  categorias: CategoriaAnaliseComunicacao[];
  comunicacoes: ComunicacaoConsultada[];
}

export interface PesquisaComunicacao {
  id: number;
  textoBusca: string | null;
  tribunal: string | null;
  dataInicio: string | null;
  dataFim: string | null;
  pesquisadaEm: string;
  quantidade: number;
  comunicacaoIds: number[];
  sintese: string | null;
  geradaEm: string | null;
  comunicacoesConsideradas: number | null;
  duplicadasIgnoradas: number | null;
  evidencias: EvidenciaComunicacao[];
}
export interface HistoricoSintesesComunicacoes { itens: PesquisaComunicacao[]; pagina: number; temProxima: boolean }
export interface DetalhePesquisaComunicacao { pesquisa: PesquisaComunicacao; publicacoes: Comunicacao[] }
