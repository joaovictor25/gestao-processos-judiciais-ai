import { Link } from 'react-router-dom';
import type { EstadoAnalise, ProcessoResumo } from '../../types/processo';
import { Icon } from '../ui/Icon';
import { ProcessStatusBadge } from './ProcessStatusBadge';

function AnalysisStatus({ analise, consultar }: { analise?: EstadoAnalise; consultar: () => void }) {
  if (!analise || analise.status === 'consultando') return <div className="process-analysis process-analysis--muted"><span className="spinner" /><span>Consultando…</span></div>;
  if (analise.status === 'analisando') return <div className="process-analysis process-analysis--blue" role="status"><span className="spinner" /><div><strong>Análise em andamento</strong><small>{analise.iniciadaEm ? `Iniciada às ${new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(new Date(analise.iniciadaEm))}` : 'Aguarde a conclusão'}</small></div></div>;
  if (analise.status === 'disponivel') return <div className="process-analysis process-analysis--green"><span className="analysis-check"><Icon name="check" /></span><div><strong>Análise disponível</strong><small>Resultados salvos</small></div></div>;
  if (analise.status === 'falha') return <div className="process-analysis process-analysis--red"><Icon name="alert" /><div><strong>Falha na análise</strong><small>{analise.possuiResultados ? 'Resultados anteriores disponíveis' : 'Tente novamente'}</small></div></div>;
  if (analise.status === 'indisponivel') return <div className="process-analysis process-analysis--muted"><Icon name="info" /><div><strong>Status indisponível</strong><button type="button" className="text-button" onClick={consultar}>Consultar novamente</button></div></div>;
  return <div className="process-analysis process-analysis--muted"><span className="analysis-minus"><Icon name="minus" /></span><strong>Não analisado</strong></div>;
}

export function ProcessoTable({ processos, analises, consultar }: {
  processos: ProcessoResumo[]; analises: Record<number, EstadoAnalise>;
  consultar: (id: number) => void;
}) {
  return <div className="process-table-scroll" role="region" aria-label="Tabela de processos" tabIndex={0}>
    <table className="process-table"><caption className="sr-only">Processos da sua conta e disponibilidade das análises</caption>
      <thead><tr><th scope="col">Processo</th><th scope="col">Cliente</th><th scope="col">Status</th><th scope="col">Análise</th><th scope="col">Ações</th></tr></thead>
      <tbody>{processos.map(processo => {
        const estado = analises[processo.id];
        return <tr key={processo.id} data-processo-id={processo.id}>
          <td className="process-identification"><Link to={`/processos/${processo.id}`}>{processo.numeroProcesso || 'Número não informado'}</Link></td>
          <td className="process-client"><span className="mobile-cell-label">Cliente</span>{processo.clienteVinculado || 'Não informado'}</td>
          <td><span className="mobile-cell-label">Status</span><ProcessStatusBadge status={processo.status} /></td>
          <td className="process-analysis-cell"><span className="mobile-cell-label">Análise</span><AnalysisStatus analise={estado} consultar={() => consultar(processo.id)} /></td>
          <td className="process-actions-cell"><div className="process-row-actions">
            <Link className="button button--secondary" to={`/processos/${processo.id}`} aria-label={`Ver processo ${processo.numeroProcesso ?? processo.id}`}><Icon name="eye" />Ver</Link>
          </div></td>
        </tr>;
      })}</tbody>
    </table>
  </div>;
}
