import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Icon, type IconName } from '../../components/ui/Icon';
import { dashboardDemo } from '../../mocks/dashboard';
import type { AtividadeRecente, DashboardResumo } from '../../types/processo';
import './Dashboard.css';

const metricas: { campo: keyof Pick<DashboardResumo, 'cadastrados' | 'emAndamento' | 'analisados' | 'aguardando'>; titulo: string; legenda: string; icon: IconName; tom: string }[] = [
  { campo: 'cadastrados', titulo: 'Processos cadastrados', legenda: 'Toda a sua carteira', icon: 'folder', tom: 'blue' },
  { campo: 'emAndamento', titulo: 'Em andamento', legenda: 'Processos ativos', icon: 'scales', tom: 'gold' },
  { campo: 'analisados', titulo: 'Análises realizadas', legenda: 'Resultados disponíveis', icon: 'check', tom: 'green' },
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
    offset += area.quantidade / total * 100;
    return `${area.cor} ${start}% ${offset}%`;
  }).join(', ');
  return <div className="portfolio-content">
    <div className="donut-chart" style={{ background: `conic-gradient(${gradient})` }} role="img" aria-label={`Carteira por área: ${total} processos. Valores detalhados na legenda.`}>
      <div className="donut-center"><strong>{total}</strong><span>processos</span></div>
    </div>
    <ul className="chart-legend">{areas.map(area => <li key={area.nome}>
      <i style={{ backgroundColor: area.cor }} /><span>{area.nome}</span>
      <strong>{Math.round(area.quantidade / total * 100)}%</strong><small>{area.quantidade} processos</small>
    </li>)}</ul>
  </div>;
}

export function Dashboard() {
  const [demonstracao, setDemonstracao] = useState(import.meta.env.VITE_DASHBOARD_DEMO !== 'false');
  const dados: DashboardResumo | null = demonstracao ? dashboardDemo : null;

  return <>
    <div className="dashboard-heading"><div><span className="eyebrow">VISÃO GERAL</span><h1>Seu escritório em perspectiva</h1><p>Os principais movimentos da sua carteira, em um só lugar.</p></div>
      <div className="demo-control"><span className={`demo-badge ${!demonstracao ? 'demo-badge--empty' : ''}`}><Icon name="info" />{demonstracao ? 'Dados de demonstração' : 'Painel em preparação'}</span>
        <button className="text-button" type="button" onClick={() => setDemonstracao(!demonstracao)}>{demonstracao ? 'Ocultar exemplos' : 'Visualizar exemplos'}</button></div>
    </div>
    <section className="metrics-grid" aria-label="Resumo dos processos">
      {metricas.map(metrica => <article className="metric-card" key={metrica.campo}>
        <span className={`icon-tile tone-${metrica.tom}`}><Icon name={metrica.icon} /></span>
        <div><h2>{metrica.titulo}</h2><strong className="metric-value">{dados ? dados[metrica.campo] : '—'}</strong><p>{dados ? metrica.legenda : 'Disponível em breve'}</p></div>
      </article>)}
    </section>
    <div className="dashboard-middle">
      <section className="panel portfolio-panel" aria-labelledby="portfolio-title">
        <div className="panel-heading"><h2 id="portfolio-title">Carteira por área</h2><span>{dados ? `${dados.emAndamento} processos` : 'Em breve'}</span></div>
        {dados ? <CarteiraChart areas={dados.areas} /> : <div className="empty-state"><Icon name="chart" /><h3>Sua carteira em uma visão</h3><p>A distribuição por área aparecerá aqui quando os indicadores estiverem disponíveis.</p></div>}
      </section>
      <section className="panel activities-panel" aria-labelledby="activities-title">
      <div className="panel-heading"><h2 id="activities-title"><Icon name="clock" />Atividades recentes</h2><span>{demonstracao ? 'Exemplos ilustrativos' : 'Em breve'}</span></div>
      {dados ? <ul className="activity-list">{dados.atividades.map(atividade => <li key={atividade.id}>
        <span className={`activity-icon tone-${activityIcons[atividade.tipo].tom}`}><Icon name={activityIcons[atividade.tipo].icon} /></span>
        <div className="activity-copy"><p><Link to={`/processos/${atividade.processoId}`}>{atividade.numeroProcesso}</Link><span className="activity-divider"> — </span><span>{atividade.titulo.toLowerCase()}</span></p><small>{atividade.classe} · {atividade.descricao}</small></div>
        <time dateTime={atividade.data}>{new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(atividade.data))}</time>
      </li>)}</ul> : <div className="empty-state empty-state--compact"><Icon name="clock" /><h3>Acompanhe cada movimento</h3><p>As atividades dos seus processos aparecerão aqui.</p></div>}
      </section>
    </div>
    <p className="dashboard-footnote"><Icon name="info" />{demonstracao ? 'Esta é uma prévia visual. Os indicadores e atividades são fictícios e não representam a sua conta.' : 'Os indicadores e o histórico de atividades estarão disponíveis em uma próxima etapa.'}</p>
  </>;
}
