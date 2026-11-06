import { useCallback, useEffect, useRef, useState } from 'react';
import { processoService } from '../../../services/processoService';
import { useDadosProcesso } from '../../../pages/Processos/useDadosProcesso';
import type { ConsultaEstatistica } from '../../../types/estatisticas';
import { ProcessoFeedback, ProcessoLoading } from '../ProcessoFeedback';
import { Button } from '../../ui/Button';
import { numero } from './formatacao';

export function periodoConsulta(parametros: ConsultaEstatistica | null | undefined) {
  if (!parametros) return 'Período não registrado nesta execução';
  const data = (valor: string) => valor.split('-').reverse().join('/');
  if (parametros.dataInicio && parametros.dataFim) return `Período: ${data(parametros.dataInicio)} a ${data(parametros.dataFim)}`;
  if (parametros.dataInicio) return `Período: a partir de ${data(parametros.dataInicio)}`;
  if (parametros.dataFim) return `Período: até ${data(parametros.dataFim)}`;
  return 'Sem recorte de data';
}

export function dataSintese(valor: string) {
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(valor));
}

export function HistoricoSinteses({ processoId }: { processoId: number }) {
  const [pagina, setPagina] = useState(0);
  const [selecionada, setSelecionada] = useState<number | null>(null);
  const carregar = useCallback((id: number, signal?: AbortSignal) => processoService.historicoSinteses(id, pagina, signal), [pagina]);
  const { dados, carregando, erro, tentarNovamente } = useDadosProcesso(processoId, carregar);
  return <section className="detail-card datajud-history" aria-labelledby="datajud-history-title">
    <h2 id="datajud-history-title">Histórico de sínteses</h2>
    {carregando ? <ProcessoLoading mensagem="Carregando histórico…" /> : erro
      ? <ProcessoFeedback titulo="Não foi possível carregar o histórico" mensagem={erro} retry={tentarNovamente} />
      : !dados?.itens.length ? <p>Nenhuma síntese anterior disponível.</p>
      : <ul>{dados.itens.map(item => <li key={item.estatisticaId}>
        <time dateTime={item.geradaEm}>{dataSintese(item.geradaEm)}</time>
        <p>{periodoConsulta(item.parametros)}</p>
        <Button variant="secondary" onClick={() => setSelecionada(item.estatisticaId)}>Ver síntese</Button>
      </li>)}</ul>}
    <div className="datajud-history-pages">
      {pagina > 0 && <Button variant="secondary" disabled={carregando} onClick={() => setPagina(atual => atual - 1)}>Anteriores</Button>}
      {!erro && dados?.temProxima && <Button variant="secondary" disabled={carregando} onClick={() => setPagina(atual => atual + 1)}>Ver mais</Button>}
    </div>
    {selecionada != null && <SinteseHistoricaModal key={selecionada} processoId={processoId} estatisticaId={selecionada} fechar={() => setSelecionada(null)} />}
  </section>;
}

function SinteseHistoricaModal({ processoId, estatisticaId, fechar }: { processoId: number; estatisticaId: number; fechar: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const carregar = useCallback((id: number, signal?: AbortSignal) => processoService.execucaoEstatistica(id, estatisticaId, signal), [estatisticaId]);
  const { dados, carregando, erro, tentarNovamente } = useDadosProcesso(processoId, carregar);
  useEffect(() => {
    const modal = dialog.current;
    const anterior = document.activeElement;
    const overflow = document.body.style.overflow;
    modal?.showModal();
    document.body.style.overflow = 'hidden';
    return () => {
      modal?.close();
      document.body.style.overflow = overflow;
      if (anterior instanceof HTMLElement && anterior.isConnected) anterior.focus();
    };
  }, []);
  return <dialog ref={dialog} className="datajud-history-modal" aria-labelledby="historical-summary-title"
    onCancel={event => { event.preventDefault(); fechar(); }}>
    <h2 id="historical-summary-title">Síntese histórica</h2>
    {carregando ? <ProcessoLoading mensagem="Carregando síntese…" /> : erro
      ? <ProcessoFeedback titulo="Não foi possível abrir a síntese" mensagem={erro} retry={tentarNovamente} />
      : dados?.sintese && <>
        <p className="field-hint"><time dateTime={dados.sintese.geradaEm}>{dataSintese(dados.sintese.geradaEm)}</time><br />{periodoConsulta(dados.parametros)}</p>
        <p>Amostra consultada: {numero(dados.quantidadeProcessos)} processos.</p>
        <p className="datajud-history-text">{dados.sintese.sintese}</p>
      </>}
    <Button variant="secondary" onClick={fechar}>Fechar</Button>
  </dialog>;
}
