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
  { codigo: 'PADROES_DECISAO', titulo: 'Padrões de decisão' },
  { codigo: 'ELEMENTOS_PROBATORIOS', titulo: 'Elementos probatórios' },
  { codigo: 'PADROES_ARGUMENTACAO', titulo: 'Padrões de argumentação' },
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
