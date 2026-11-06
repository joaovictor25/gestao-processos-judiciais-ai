import { emAndamento, normalizarTexto } from './processoFormModel';

export function ProcessStatusBadge({ status }: { status: string | null }) {
  const normalized = normalizarTexto(status);
  const tom = emAndamento(status) ? 'green' : normalized === 'concluido' ? 'blue' : normalized === 'suspenso' ? 'gold' : 'gray';
  return <span className={`process-badge process-badge--${tom}`}><i />{emAndamento(status) ? 'Em andamento' : status || 'Não informado'}</span>;
}
