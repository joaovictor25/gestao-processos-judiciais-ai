import { Link } from 'react-router-dom';
import type { EstadoAnalise, ProcessoResumo } from '../../types/processo';
import { Button } from '../ui/Button';
import { Icon } from '../ui/Icon';
import { normalizarTexto } from './processoFormModel';
import { ProcessStatusBadge } from './ProcessStatusBadge';

function PositionBadge({ posicao }: { posicao: string | null }) {
  const normalized = normalizarTexto(posicao);
  const tom = normalized === 'autor' ? 'blue' : normalized === 'reu' ? 'red' : 'gray';
  return <span className={`position-badge process-badge--${tom}`}>{normalized === 'reu' ? 'RÉU' : posicao?.toUpperCase() || '—'}</span>;
}

function AnalysisStatus({ analise, consultar }: { analise?: EstadoAnalise; consultar: () => void }) {
  if (!analise || analise.status === 'consultando') return <div className="process-analysis process-analysis--muted"><span className="spinner" /><span>Consultando…</span></div>;
  if (analise.status === 'analisando') return <div className="process-analysis process-analysis--blue" role="status"><span className="spinner" /><div><strong>Análise em andamento</strong><small>{analise.iniciadaEm ? `Iniciada às ${new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(new Date(analise.iniciadaEm))}` : 'Aguarde a conclusão'}</small></div></div>;
  if (analise.status === 'disponivel') return <div className="process-analysis process-analysis--green"><span className="analysis-check"><Icon name="check" /></span><div><strong>Análise disponível</strong><small>Resultados salvos</small></div></div>;
  if (analise.status === 'falha') return <div className="process-analysis process-analysis--red"><Icon name="alert" /><div><strong>Falha na análise</strong><small>{analise.possuiResultados ? 'Resultados anteriores disponíveis' : 'Tente novamente'}</small></div></div>;
  if (analise.status === 'indisponivel') return <div className="process-analysis process-analysis--muted"><Icon name="info" /><div><strong>Status indisponível</strong><button type="button" className="text-button" onClick={consultar}>Consultar novamente</button></div></div>;
  return <div className="process-analysis process-analysis--muted"><span className="analysis-minus"><Icon name="minus" /></span><strong>Não analisado</strong></div>;
}

export function ProcessoTable({ processos, analises, analisar, consultar }: {
  processos: ProcessoResumo[]; analises: Record<number, EstadoAnalise>;
  analisar: (processo: ProcessoResumo) => void; consultar: (id: number) => void;
}) {
  return <div className="process-table-scroll" role="region" aria-label="Tabela de processos" tabIndex={0}>
    <table className="process-table"><caption className="sr-only">Processos da sua conta e disponibilidade das análises</caption>
      <thead><tr><th scope="col">Processo</th><th scope="col">Posição</th><th scope="col">Status</th><th scope="col">Análise</th><th scope="col">Ações</th></tr></thead>
      <tbody>{processos.map(processo => {
        const estado = analises[processo.id];
        const analisando = estado?.status === 'analisando';
        return <tr key={processo.id} data-processo-id={processo.id}>
          <td className="process-identification"><Link to={`/processos/${processo.id}`}>{processo.numeroProcesso || 'Número não informado'}</Link><small>Cliente: {processo.clienteVinculado || 'Não informado'}</small></td>
          <td><span className="mobile-cell-label">Posição</span><PositionBadge posicao={processo.posicaoCliente} /></td>
          <td><span className="mobile-cell-label">Status</span><ProcessStatusBadge status={processo.status} /></td>
          <td className="process-analysis-cell"><span className="mobile-cell-label">Análise</span><AnalysisStatus analise={estado} consultar={() => consultar(processo.id)} /></td>
          <td className="process-actions-cell"><div className="process-row-actions">
            <Link className="button button--secondary" to={`/processos/${processo.id}`} aria-label={`Ver processo ${processo.numeroProcesso ?? processo.id}`}><Icon name="eye" />Ver</Link>
            <Button className="process-analyze-button" variant="secondary" loading={analisando} disabled={!estado || estado.status === 'consultando'}
              onClick={() => analisar(processo)} aria-label={`${analisando ? 'Analisando' : 'Analisar'} processo ${processo.numeroProcesso ?? processo.id}`}>
              {!analisando && <Icon name="sparkles" />}{analisando ? 'Analisando…' : 'Analisar'}
            </Button>
          </div></td>
        </tr>;
      })}</tbody>
    </table>
  </div>;
}
