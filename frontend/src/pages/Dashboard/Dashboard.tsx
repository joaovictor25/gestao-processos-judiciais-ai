import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Icon, type IconName } from '../../components/ui/Icon';
import type { AtividadeRecente } from '../../types/processo';
import type { DashboardResumo } from '../../types/dashboard';
import { dashboardService } from '../../services/dashboardService';
import { mensagemErro } from '../../services/api';
import './Dashboard.css';

const metricas: { campo: keyof Pick<DashboardResumo, 'cadastrados' | 'emAnalise' | 'analisados' | 'aguardando'>; titulo: string; legenda: string; icon: IconName; tom: string }[] = [
  { campo: 'cadastrados', titulo: 'Processos cadastrados', legenda: 'Toda a sua carteira', icon: 'folder', tom: 'blue' },
  { campo: 'emAnalise', titulo: 'Em análise', legenda: 'Análises em execução', icon: 'scales', tom: 'gold' },
  { campo: 'analisados', titulo: 'Processos analisados', legenda: 'Com análise concluída', icon: 'check', tom: 'green' },
  { campo: 'aguardando', titulo: 'Aguardando análise', legenda: 'Acompanhe as pendências', icon: 'clock', tom: 'red' },
];
const activityIcons: Record<AtividadeRecente['tipo'], { icon: IconName; tom: string }> = {
  analise: { icon: 'check', tom: 'green' }, cadastro: { icon: 'plus', tom: 'blue' },
  comunicacao: { icon: 'file', tom: 'violet' }, pendente: { icon: 'clock', tom: 'gold' },
};

function CarteiraChart({ areas }: { areas: DashboardResumo['areas'] }) {
  const total = areas.reduce((sum, area) => sum + area.quantidade, 0);
  let offset = 0;
  const gradient = areas.map(area => {
    const start = offset;
    offset += total ? area.quantidade / total * 100 : 0;
    return `${area.cor} ${start}% ${offset}%`;
  }).join(', ');
  return <div className="portfolio-content">
    <div className="donut-chart" style={{ background: total ? `conic-gradient(${gradient})` : 'var(--color-border)' }} role="img" aria-label={`Carteira por área: ${total} processos. Valores detalhados na legenda.`}>
      <div className="donut-center"><strong>{total}</strong><span>processos</span></div>
    </div>
    <ul className="chart-legend">{areas.map(area => <li key={area.nome}>
      <i style={{ backgroundColor: area.cor }} /><span>{area.nome}</span>
      <strong>{total ? Math.round(area.quantidade / total * 100) : 0}%</strong><small>{area.quantidade} processos</small>
    </li>)}</ul>
  </div>;
}

export function Dashboard() {
  const [dados, setDados] = useState<DashboardResumo | null>(null);
  const [pagina, setPagina] = useState(1);
  const [tentativa, setTentativa] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  useEffect(() => {
    let pedido: AbortController | undefined;
    async function carregar() {
      pedido?.abort();
      const controller = new AbortController(); pedido = controller;
      setCarregando(true);
      try {
        const resposta = await dashboardService.consultar(pagina, controller.signal);
        if (!controller.signal.aborted) { setDados(resposta); setErro(''); }
      } catch (error) { if (!controller.signal.aborted) setErro(mensagemErro(error)); }
      finally { if (!controller.signal.aborted) setCarregando(false); }
    }
    void carregar();
    const intervalo = window.setInterval(() => { if (!document.hidden) void carregar(); }, 10000);
    const aoFocar = () => { void carregar(); };
    window.addEventListener('focus', aoFocar);
    return () => { pedido?.abort(); clearInterval(intervalo); window.removeEventListener('focus', aoFocar); };
  }, [pagina, tentativa]);

  return <>
    <div className="dashboard-heading"><div><span className="eyebrow">VISÃO GERAL</span><h1>Seu escritório em perspectiva</h1><p>Os principais movimentos da sua carteira, em um só lugar.</p></div>
      <button className="text-button" type="button" disabled={carregando} onClick={() => setTentativa(valor => valor + 1)}>{carregando ? 'Atualizando…' : 'Atualizar painel'}</button>
    </div>
    {erro && <div className="notice notice--error" role="alert">{erro} {dados && 'Os dados exibidos são da última atualização.'}</div>}
    <section className="metrics-grid" aria-label="Resumo dos processos">
      {metricas.map(metrica => <article className="metric-card" key={metrica.campo}>
        <span className={`icon-tile tone-${metrica.tom}`}><Icon name={metrica.icon} /></span>
        <div><h2>{metrica.titulo}</h2><strong className="metric-value">{dados ? dados[metrica.campo] : '—'}</strong><p>{metrica.legenda}</p></div>
      </article>)}
    </section>
    <div className="dashboard-middle">
      <section className="panel portfolio-panel" aria-labelledby="portfolio-title">
        <div className="panel-heading"><h2 id="portfolio-title">Carteira por área</h2><span>{dados ? `${dados.cadastrados} processos` : '—'}</span></div>
        {dados ? <><CarteiraChart areas={dados.areas} />{dados.cadastrados === 0 && <p className="dashboard-footnote">Cadastre um processo para começar sua carteira.</p>}</> : <p className="dashboard-footnote">{carregando ? 'Carregando carteira…' : 'Não foi possível carregar a carteira.'}</p>}
      </section>
      <section className="panel activities-panel" aria-labelledby="activities-title">
      <div className="panel-heading"><h2 id="activities-title"><Icon name="clock" />Atividades recentes</h2><span>Última ação por processo</span></div>
      {dados && dados.atividades.length > 0 ? <ul className="activity-list">{dados.atividades.map(atividade => <li key={atividade.id}>
        <span className={`activity-icon tone-${activityIcons[atividade.tipo].tom}`}><Icon name={activityIcons[atividade.tipo].icon} /></span>
        <div className="activity-copy"><p><Link to={`/processos/${atividade.processoId}`}>{atividade.numeroProcesso}</Link><span className="activity-divider"> — </span><span>{atividade.titulo.toLowerCase()}</span></p><small>{atividade.classe} · {atividade.descricao}</small></div>
        <time dateTime={atividade.data ?? undefined}>{atividade.data ? new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(atividade.data)) : 'Data não registrada'}</time>
      </li>)}</ul> : <div className="empty-state empty-state--compact"><Icon name="clock" /><h3>Acompanhe cada movimento</h3><p>As atividades dos seus processos aparecerão aqui.</p></div>}
      {dados && dados.totalAtividades > 0 && <nav className="dashboard-pagination" aria-label="Paginação das atividades">
        <button type="button" className="text-button" disabled={carregando || dados.pagina <= 1} onClick={() => setPagina(dados.pagina - 1)}>Anterior</button>
        <span aria-live="polite">Página {dados.pagina} de {dados.totalPaginas} · {dados.totalAtividades} processos</span>
        <button type="button" className="text-button" disabled={carregando || dados.pagina >= dados.totalPaginas} onClick={() => setPagina(dados.pagina + 1)}>Próximo</button>
      </nav>}
      </section>
    </div>
    <p className="dashboard-footnote"><Icon name="info" />Dados da sua conta, atualizados automaticamente a cada 10 segundos. “Em análise” indica uma execução de DataJud ou IA; os demais processos são classificados como analisados ou aguardando análise.</p>
  </>;
}
