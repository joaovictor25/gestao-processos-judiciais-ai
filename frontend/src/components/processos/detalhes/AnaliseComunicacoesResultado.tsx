import type { AnaliseComunicacao } from '../../../types/analiseComunicacao';
import { tiposAnaliseComunicacao } from '../../../types/analiseComunicacao';
import { Icon } from '../../ui/Icon';

export function AnaliseComunicacoesResultado({ resultado }: { resultado: AnaliseComunicacao }) {
  return <section className="detail-card communication-ai-result" aria-labelledby="communication-ai-result-title">
    <h2 id="communication-ai-result-title"><Icon name="sparkles" />Análise por IA</h2>
    <p className="field-hint">{resultado.comunicacoesConsideradas} {resultado.comunicacoesConsideradas === 1 ? 'comunicação analisada' : 'comunicações analisadas'}{resultado.duplicadasIgnoradas > 0 ? ` · ${resultado.duplicadasIgnoradas} duplicadas ignoradas` : ''}</p>
    {resultado.categorias.map(categoria => <section key={categoria.tipo} className="communication-ai-category">
      <h3>{tiposAnaliseComunicacao.find(item => item.codigo === categoria.tipo)?.titulo ?? categoria.tipo}</h3>
      {categoria.itens.map((item, indice) => <article className="communication-ai-item" key={indice}>
        <h4>{item.titulo}</h4><p>{item.descricao}</p>
        {item.contexto && <p><strong>Contexto:</strong> {item.contexto}</p>}
        {item.decisaoRelacionada && <p><strong>Decisão relacionada:</strong> {item.decisaoRelacionada}</p>}
        <ul>{item.evidencias.map((evidencia, posicao) => <li key={`${evidencia.comunicacao}-${posicao}`}>
          <strong>Comunicação {evidencia.comunicacao}:</strong> “{evidencia.trecho}”
        </li>)}</ul>
      </article>)}
      {categoria.limitacao && <p className="communication-ai-limitation">{categoria.limitacao}</p>}
    </section>)}
    <p className="communication-ai-disclaimer">Análise gerada exclusivamente a partir das comunicações recuperadas para este processo. Os resultados representam padrões e informações observados na amostra e não constituem previsão de decisão judicial ou recomendação jurídica.</p>
  </section>;
}
