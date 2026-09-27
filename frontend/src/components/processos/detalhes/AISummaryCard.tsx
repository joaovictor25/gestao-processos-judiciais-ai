import { Icon } from '../../ui/Icon';
import { Button } from '../../ui/Button';

export function AISummaryCard({ comunicacoes = false }: { comunicacoes?: boolean }) {
  return <section className={`ai-summary-card ${comunicacoes ? 'ai-summary-card--communications' : ''}`}>
    <div><h2><Icon name="sparkles" />{comunicacoes ? 'Síntese das comunicações' : 'Análise da IA'}<span className="ai-availability">Em breve</span></h2>
      <p>{comunicacoes ? 'A síntese automática das comunicações ainda não está disponível. Os registros apresentados abaixo são os resultados salvos da consulta.' : 'Gere uma síntese dos resultados do DataJud para apoiar a compreensão da amostra e a comparação com o processo analisado.'}</p>
    </div>{!comunicacoes && <div className="ai-summary-action"><Button disabled><Icon name="sparkles" />Iniciar análise da IA</Button><small><Icon name="info" />Disponível em uma próxima etapa, com base nas estatísticas do DataJud.</small></div>}
  </section>;
}
