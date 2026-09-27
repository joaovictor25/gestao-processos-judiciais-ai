import { useEffect, useRef, useState } from 'react';
import { processoService } from '../../../services/processoService';
import { useDadosProcesso } from '../../../pages/Processos/useDadosProcesso';
import type { TempoEstatistica } from '../../../types/estatisticas';
import { ProcessoFeedback, ProcessoLoading } from '../ProcessoFeedback';
import { Button } from '../../ui/Button';
import { Icon } from '../../ui/Icon';
import { AISummaryCard } from './AISummaryCard';
import { EstatisticaCard } from './EstatisticaCard';
import { numero } from './formatacao';

const medidas: { chave: keyof TempoEstatistica; label: string }[] = [
  { chave: 'quantidade', label: 'Processos na amostra' }, { chave: 'mediaDias', label: 'Média' }, { chave: 'medianaDias', label: 'Mediana' },
  { chave: 'minimoDias', label: 'Mínimo' }, { chave: 'maximoDias', label: 'Máximo' }, { chave: 'amplitudeDias', label: 'Amplitude' },
  { chave: 'desvioPadraoDias', label: 'Desvio padrão' }, { chave: 'percentil25Dias', label: 'Percentil 25' },
  { chave: 'percentil50Dias', label: 'Percentil 50' }, { chave: 'percentil75Dias', label: 'Percentil 75' },
];

export function DataJudTab({ processoId, versao, analisando, solicitar }: { processoId: number; versao: number; analisando: boolean; solicitar: () => void }) {
  const { dados, carregando, erro, tentarNovamente } = useDadosProcesso(processoId, processoService.estatisticas, versao);
  const [detalhe, setDetalhe] = useState<'julgamento' | 'baixa' | 'pendentes' | null>(null);
  const dialog = useRef<HTMLDialogElement>(null);
  const estatistica = dados?.at(-1);
  useEffect(() => { if (detalhe) dialog.current?.showModal(); else dialog.current?.close(); }, [detalhe]);
  const mediana = (tempo: TempoEstatistica | null | undefined) => tempo?.quantidade === 0 ? null : tempo?.medianaDias ?? null;
  const julgamento = mediana(estatistica?.tempoAtePrimeiroJulgamento);
  const baixa = mediana(estatistica?.tempoAtePrimeiraBaixa);
  const selecionado = detalhe === 'julgamento' ? estatistica?.tempoAtePrimeiroJulgamento : detalhe === 'baixa' ? estatistica?.tempoAtePrimeiraBaixa : estatistica?.tempoPendente;
  const titulo = detalhe === 'julgamento' ? 'Primeiro julgamento' : detalhe === 'baixa' ? 'Primeira baixa' : 'Processos pendentes';

  return <div className="datajud-tab"><section className="datajud-panel detail-card">
    <div className="detail-section-heading"><span className="process-summary-icon process-summary-icon--blue"><Icon name="chart" /></span><div><h2>Análise DataJud</h2><p>Estatísticas calculadas a partir de processos com características semelhantes ao processo analisado.</p></div>
      <div className="datajud-refresh"><span><Icon name="clock" />Atualização não informada</span><Button variant="secondary" loading={analisando} onClick={solicitar}>{!analisando && <Icon name="refresh" />}{analisando ? 'Analisando…' : 'Atualizar análise'}</Button></div>
    </div>
    {carregando ? <ProcessoLoading mensagem="Carregando estatísticas salvas…" /> : erro ? <ProcessoFeedback titulo="Não foi possível carregar o DataJud" mensagem={erro} retry={tentarNovamente} />
      : !estatistica ? <ProcessoFeedback titulo="Nenhuma análise DataJud foi realizada para este processo." mensagem="Solicite uma análise para consultar as estatísticas de processos semelhantes."><Button loading={analisando} onClick={solicitar}>Solicitar análise</Button></ProcessoFeedback>
      : <><div className="statistic-cards">
        <EstatisticaCard titulo="Primeiro julgamento" subtitulo="Tempo observado até o primeiro julgamento" valor={julgamento == null ? '—' : `${numero(julgamento)} dias`} badge={julgamento == null ? 'Sem amostra' : 'Mediana'} icon="gavel" tom="blue"
          descricao={julgamento == null ? 'Não há dados suficientes para calcular a mediana do primeiro julgamento.' : `Metade dos processos com julgamento identificado chegou ao primeiro julgamento em até ${numero(julgamento)} dias.`} abrir={() => setDetalhe('julgamento')} />
        <EstatisticaCard titulo="Primeira baixa" subtitulo="Tempo observado até a primeira baixa" valor={baixa == null ? '—' : `${numero(baixa)} dias`} badge={baixa == null ? 'Sem amostra' : 'Mediana'} icon="file" tom="green"
          descricao={baixa == null ? 'Não há dados suficientes para calcular a mediana da primeira baixa.' : `Metade dos processos considerados apresentou a primeira baixa em até ${numero(baixa)} dias.`} abrir={() => setDetalhe('baixa')} />
        <EstatisticaCard titulo="Processos pendentes" subtitulo="Processos que ainda não apresentavam baixa" valor={`${numero(estatistica.processosPendentes)} processos`} badge="Ainda pendentes" icon="clock" tom="gold"
          descricao="Esses processos permaneciam sem baixa identificada no momento da consulta." abrir={() => setDetalhe('pendentes')} />
      </div><p className="datajud-sample">Amostra consultada: {numero(estatistica.quantidadeProcessos)} processos. Os indicadores descrevem a amostra, não uma previsão de resultado.</p></>}
  </section><AISummaryCard />
    <dialog ref={dialog} className="statistics-dialog" aria-labelledby="statistics-title" onClose={() => setDetalhe(null)} onKeyDown={event => { if (event.key === 'Escape') event.stopPropagation(); }}>
      <div className="statistics-dialog-heading"><h2 id="statistics-title">{titulo}</h2><button type="button" className="icon-button" aria-label="Fechar estatísticas" onClick={() => setDetalhe(null)}><Icon name="close" /></button></div>
      <dl className="statistics-measures">{medidas.map(({ chave, label }) => {
        const value = chave !== 'quantidade' && selecionado?.quantidade === 0 ? null : selecionado?.[chave];
        return <div key={chave}><dt>{label}</dt><dd>{numero(value)}{value != null && chave !== 'quantidade' ? ' dias' : ''}</dd></div>;
      })}</dl><Button variant="secondary" onClick={() => setDetalhe(null)}>Fechar</Button>
    </dialog>
  </div>;
}
