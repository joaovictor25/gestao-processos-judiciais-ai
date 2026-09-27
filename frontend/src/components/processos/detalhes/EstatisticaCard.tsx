import type { IconName } from '../../ui/Icon';
import { Icon } from '../../ui/Icon';

export function EstatisticaCard({ titulo, subtitulo, valor, badge, descricao, icon, tom, abrir }: {
  titulo: string; subtitulo: string; valor: string; badge: string; descricao: string; icon: IconName; tom: 'blue' | 'green' | 'gold'; abrir: () => void;
}) {
  return <article className="statistic-card"><div className="statistic-heading"><span className={`process-summary-icon process-summary-icon--${tom}`}><Icon name={icon} /></span><div><h3>{titulo}</h3><p>{subtitulo}</p></div></div>
    <strong className="statistic-value">{valor}</strong><span className={`process-badge process-badge--${badge === 'Mediana' ? 'gold' : 'gray'}`}>{badge}</span><p className="statistic-description">{descricao}</p>
    <button type="button" className="text-button" onClick={abrir}>Ver todas as estatísticas<span className="sr-only"> de {titulo.toLowerCase()}</span></button>
  </article>;
}
