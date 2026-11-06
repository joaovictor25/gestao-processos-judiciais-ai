import type { IconName } from '../../ui/Icon';
import { Icon } from '../../ui/Icon';

export function EstatisticaCard({ titulo, subtitulo, valor, badge, descricao, amostra, icon, tom }: {
  titulo: string; subtitulo: string; valor: string; badge: string; descricao: string; amostra?: string; icon: IconName; tom: 'blue' | 'green' | 'gold';
}) {
  return <article className="statistic-card"><div className="statistic-heading"><span className={`process-summary-icon process-summary-icon--${tom}`}><Icon name={icon} /></span><div><h3>{titulo}</h3><p>{subtitulo}</p></div></div>
    <strong className="statistic-value">{valor}</strong><span className={`process-badge process-badge--${badge === 'Mediana' ? 'gold' : 'gray'}`}>{badge}</span>
    {amostra && <p className="statistic-sample">{amostra}</p>}<p className="statistic-description">{descricao}</p>
  </article>;
}
