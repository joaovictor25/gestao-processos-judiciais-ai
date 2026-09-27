export interface ClassificacaoProcessual {
  nome: string | null;
  codigo: string | null;
}

// Campos utilizados na listagem, conforme ProcessoResponseDTO.
export interface ProcessoResumo {
  id: number;
  numeroProcesso: string | null;
  clienteVinculado: string | null;
  posicaoCliente: string | null;
  classeProcessual: ClassificacaoProcessual | null;
  tribunal: string | null;
  status: string | null;
  areaAtuacao: string | null;
  dataInicio: string | null;
  dataFim: string | null;
}

export interface Processo extends ProcessoResumo {
  orgaoJulgador: ClassificacaoProcessual | null;
  assunto: ClassificacaoProcessual | null;
  grau: string | null;
  comarca: string | null;
  uf: string | null;
  valorCausa: number | null;
  nomeMagistrado: string | null;
}

export type ProcessoRequest = Omit<Processo, 'id'>;

export interface RegistroDataJudCadastro {
  tribunal: string | null;
  grau: string | null;
  orgaoJulgador: ClassificacaoProcessual | null;
  classeProcessual: ClassificacaoProcessual | null;
  assuntos: ClassificacaoProcessual[];
  dataInicio: string | null;
}
export interface ConsultaCnjResponse {
  numeroProcesso: string;
  tribunalConsultado: string;
  registros: RegistroDataJudCadastro[];
  limiteAtingido: boolean;
}

// Projeção da resposta existente: basta a presença dos registros para indicar disponibilidade.
export interface AnaliseProcesso {
  processoId: number;
  estatisticas: { id: number; processoId: number }[];
  comunicacoes: { id: number; processoId: number }[];
}

export interface EstadoAnalise {
  status: 'consultando' | 'nao-analisado' | 'disponivel' | 'analisando' | 'falha' | 'indisponivel';
  possuiResultados?: boolean;
  iniciadaEm?: string;
}

// Modelo de apresentação; a API atual não fornece um feed de atividades.
export interface AtividadeRecente {
  id: string;
  processoId: number;
  numeroProcesso: string;
  classe: string;
  tipo: 'analise' | 'cadastro' | 'comunicacao' | 'pendente';
  titulo: string;
  descricao: string;
  data: string;
}

export interface DashboardResumo {
  cadastrados: number;
  emAndamento: number;
  analisados: number;
  aguardando: number;
  areas: { nome: string; quantidade: number; cor: string }[];
  atividades: AtividadeRecente[];
}
