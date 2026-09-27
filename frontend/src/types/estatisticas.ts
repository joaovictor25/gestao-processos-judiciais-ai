export interface TempoEstatistica {
  quantidade: number | null;
  mediaDias: number | null;
  medianaDias: number | null;
  minimoDias: number | null;
  maximoDias: number | null;
  amplitudeDias: number | null;
  desvioPadraoDias: number | null;
  percentil25Dias: number | null;
  percentil50Dias: number | null;
  percentil75Dias: number | null;
}

// Corresponde a DataJudResponseDTO, inclusive campos opcionais das amostras vazias.
export interface EstatisticasProcessuais {
  id: number;
  processoId: number;
  quantidadeProcessos: number | null;
  processosBaixados: number | null;
  processosPendentes: number | null;
  processosComJulgamento: number | null;
  tempoAtePrimeiraBaixa: TempoEstatistica | null;
  tempoPendente: TempoEstatistica | null;
  tempoAtePrimeiroJulgamento: TempoEstatistica | null;
}
