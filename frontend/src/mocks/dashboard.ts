import type { DashboardResumo } from '../types/processo';

// Somente apresentação. Estes valores nunca são apresentados como dados da conta.
export const dashboardDemo: DashboardResumo = {
  cadastrados: 183, emAndamento: 147, analisados: 135, aguardando: 12,
  areas: [
    { nome: 'Cível', quantidade: 56, cor: 'var(--color-primary)' },
    { nome: 'Trabalhista', quantidade: 35, cor: 'var(--color-secondary)' },
    { nome: 'Previdenciário', quantidade: 26, cor: 'var(--color-accent)' },
    { nome: 'Consumidor', quantidade: 18, cor: 'var(--color-cyan)' },
    { nome: 'Outros', quantidade: 12, cor: 'var(--color-chart-other)' },
  ],
  atividades: [
    { id: 'a1', processoId: 1, numeroProcesso: '0051611-36.2025.8.16.0182', classe: 'Recurso Inominado Cível', tipo: 'analise', titulo: 'Análise concluída', descricao: 'Estatísticas e comunicações atualizadas.', data: '2026-09-26T18:43:00-03:00' },
    { id: 'a2', processoId: 2, numeroProcesso: '0032181-03.2021.8.19.0038', classe: 'Apelação Cível', tipo: 'pendente', titulo: 'Aguardando análise', descricao: 'Processo aguardando uma nova análise.', data: '2026-09-26T09:17:00-03:00' },
    { id: 'a3', processoId: 3, numeroProcesso: '1027436-62.2024.8.26.0100', classe: 'Procedimento Comum Cível', tipo: 'cadastro', titulo: 'Processo cadastrado', descricao: 'Um novo processo adicionado à carteira.', data: '2026-09-25T16:03:00-03:00' },
    { id: 'a4', processoId: 4, numeroProcesso: '0001234-56.2025.8.19.0001', classe: 'Procedimento Comum Cível', tipo: 'comunicacao', titulo: 'Comunicações atualizadas', descricao: 'Novas publicações disponíveis para consulta.', data: '2026-09-24T14:21:00-03:00' },
  ],
};
