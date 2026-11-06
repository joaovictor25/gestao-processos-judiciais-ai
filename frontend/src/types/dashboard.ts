import type { AtividadeRecente } from './processo';

export interface DashboardResumo {
  cadastrados: number;
  emAnalise: number;
  analisados: number;
  aguardando: number;
  areas: { nome: string; quantidade: number; cor: string }[];
  atividades: (Omit<AtividadeRecente, 'data'> & { data: string | null })[];
  pagina: number;
  totalPaginas: number;
  totalAtividades: number;
}
