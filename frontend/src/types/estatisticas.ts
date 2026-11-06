export interface SinteseDataJud { estatisticaId: number; sintese: string; geradaEm: string }

export interface SolicitacaoEstatistica {
  id: string;
  processoId: number;
  status: 'AGUARDANDO' | 'CONSULTANDO' | 'REAGENDADA' | 'CONCLUIDA' | 'FALHOU';
  tentativas: number;
  proximaTentativa: string;
  codigoErro: string | null;
  dataInicio: string | null;
  dataFim: string | null;
  resultados: EstatisticasProcessuais[];
}

export interface ConsultaEstatistica {
  tribunal: string | null;
  classeCodigo: string | null;
  grau: string | null;
  limiteResultados: number | null;
  dataInicio: string | null;
  dataFim: string | null;
}

export interface HistoricoSinteses {
  itens: { estatisticaId: number; geradaEm: string; parametros: ConsultaEstatistica | null }[];
  pagina: number;
  temProxima: boolean;
}

export interface TempoEstatistica {
  quantidade: number | null;
  medianaDias: number | null;
}

// Corresponde a DataJudResponseDTO, inclusive campos opcionais das amostras vazias.
export interface EstatisticasProcessuais {
  id: number;
  processoId: number;
  quantidadeProcessos: number | null;
  processosPendentes: number | null;
  tempoAtePrimeiraBaixa: TempoEstatistica | null;
  tempoAtePrimeiroJulgamento: TempoEstatistica | null;
  executadaEm: string | null;
  parametros: ConsultaEstatistica | null;
  statusSintese: 'SEM_SINTESE' | 'CONCLUIDA';
  sintese: SinteseDataJud | null;
}
