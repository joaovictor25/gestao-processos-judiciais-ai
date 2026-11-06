import { useEffect, useRef, useState } from 'react';
import { processoService } from '../../../services/processoService';
import { useDadosProcesso } from '../../../pages/Processos/useDadosProcesso';
import type { TempoEstatistica, EstatisticasProcessuais, SinteseDataJud, SolicitacaoEstatistica } from '../../../types/estatisticas';
import { ProcessoFeedback, ProcessoLoading } from '../ProcessoFeedback';
import { Button } from '../../ui/Button';
import { Icon } from '../../ui/Icon';
import { AISummaryCard } from './AISummaryCard';
import { EstatisticaCard } from './EstatisticaCard';
import { numero } from './formatacao';
import { Input } from '../../forms/Input';
import { ApiError, mensagemErro } from '../../../services/api';
import { HistoricoSinteses, dataSintese, periodoConsulta } from './HistoricoSinteses';

export function DataJudTab({ processoId, versao, analisando }: { processoId: number; versao: number; analisando: boolean }) {
  // Trocar processo/versão interrompe o acompanhamento; a geração continua no servidor.
  return <DataJudConteudo key={`${processoId}-${versao}`} processoId={processoId} versao={versao} analisando={analisando} />;
}

function DataJudConteudo({ processoId, versao, analisando }: { processoId: number; versao: number; analisando: boolean }) {
  const [resultadoGerado, setResultadoGerado] = useState<{ processoId: number; versao: number; dados: EstatisticasProcessuais[] } | null>(null);
  const [inicio, setInicio] = useState('');
  const [fim, setFim] = useState('');
  const [gerando, setGerando] = useState(false);
  const [conferindo, setConferindo] = useState(true);
  const [sintetizando, setSintetizando] = useState(false);
  const [sinteseGerada, setSinteseGerada] = useState<SinteseDataJud | null>(null);
  const [versaoHistorico, setVersaoHistorico] = useState(0);
  const [aviso, setAviso] = useState('');
  const [erroGeracao, setErroGeracao] = useState('');
  const pedido = useRef<AbortController | null>(null);
  useEffect(() => {
    const controller = new AbortController();
    pedido.current = controller;
    void (async () => {
      try {
        const atual = await processoService.geracaoAtual(processoId, controller.signal);
        if (!controller.signal.aborted && atual) {
          setInicio(atual.dataInicio ?? ''); setFim(atual.dataFim ?? '');
          setGerando(true); await acompanhar(atual, controller.signal);
        }
      } catch (error) { if (!controller.signal.aborted) setErroGeracao(mensagemErro(error)); }
      finally { if (!controller.signal.aborted) { pedido.current = null; setGerando(false); setConferindo(false); } }
    })();
    return () => { controller.abort(); pedido.current?.abort(); };
  }, [processoId]);

  async function acompanhar(inicial: SolicitacaoEstatistica, signal: AbortSignal) {
    let atual = inicial;
    const limite = Date.now() + 8 * 60_000;
    while (!signal.aborted) {
      if (atual.processoId !== processoId || !atual.id) throw new ApiError('O servidor retornou uma solicitação inválida.');
      if (atual.status === 'CONCLUIDA') {
        if (!Array.isArray(atual.resultados) || !atual.resultados.length || atual.resultados.some(item => item.processoId !== processoId))
          throw new ApiError('O servidor não retornou as estatísticas do processo. Recarregue a aba para conferir o resultado salvo.');
        setResultadoGerado({ processoId, versao, dados: atual.resultados });
        setAviso('Estatísticas disponíveis e salvas. Consultas recentes podem ser reutilizadas.');
        return;
      }
      if (atual.status === 'FALHOU') {
        setAviso('');
        const mensagens: Record<string, string> = {
          DATAJUD_SEM_PROCESSOS: 'Nenhum processo válido foi encontrado com os filtros informados. As estatísticas anteriores foram preservadas.',
          CONSULTA_DATAJUD_LIMITADA: 'O DataJud continuou recusando as consultas após as tentativas automáticas. Aguarde antes de solicitar novamente. As estatísticas anteriores foram preservadas.',
          DATAJUD_PRAZO_EXCEDIDO: 'A consulta não pôde ser concluída no prazo. Tente novamente mais tarde. As estatísticas anteriores foram preservadas.',
        };
        throw new ApiError(mensagens[atual.codigoErro ?? ''] ?? 'Não foi possível concluir a consulta ao DataJud. As estatísticas anteriores foram preservadas. Tente novamente mais tarde.');
      }
      const mensagens = {
        AGUARDANDO: 'Solicitação recebida. Aguardando a vez de consultar o DataJud.',
        CONSULTANDO: 'Consultando o DataJud e preparando as estatísticas…',
        REAGENDADA: 'Aguardando disponibilidade do DataJud. A consulta será retomada automaticamente.',
      };
      if (!(atual.status in mensagens)) throw new ApiError('O servidor retornou um estado de geração desconhecido.');
      setAviso(mensagens[atual.status] + ' Você pode sair desta aba; a solicitação continua no servidor.');
      if (Date.now() >= limite) throw new ApiError('O acompanhamento demorou mais que o esperado. Reabra esta aba para consultar o andamento.');
      await esperar(2000, signal);
      if (signal.aborted) return;
      atual = await processoService.consultarGeracao(processoId, atual.id, signal);
    }
  }
  const periodoInvalido = Boolean(inicio && fim && fim < inicio);
  async function gerar() {
    if (pedido.current || conferindo || analisando || sintetizando || periodoInvalido) return;
    const controller = new AbortController();
    pedido.current = controller; setGerando(true); setAviso(''); setErroGeracao('');
    try {
      const resultado = await processoService.gerarEstatisticas(processoId, { dataInicio: inicio || undefined, dataFim: fim || undefined }, controller.signal);
      if (controller.signal.aborted) return;
      await acompanhar(resultado, controller.signal);
    } catch (error) { if (!controller.signal.aborted) setErroGeracao(mensagemErro(error)); }
    finally { if (!controller.signal.aborted) { pedido.current = null; setGerando(false); } }
  }
  const { dados, carregando, erro, tentarNovamente } = useDadosProcesso(processoId, processoService.estatisticas, versao);
  const dadosGerados = resultadoGerado?.processoId === processoId && resultadoGerado.versao === versao ? resultadoGerado.dados : null;
  const estatistica = (dadosGerados ?? dados)?.at(-1);
  const sintese = sinteseGerada?.estatisticaId === estatistica?.id ? sinteseGerada : estatistica?.sintese;
  const mediana = (tempo: TempoEstatistica | null | undefined) => (tempo?.quantidade ?? 0) <= 0 ? null : tempo?.medianaDias ?? null;
  const julgamento = mediana(estatistica?.tempoAtePrimeiroJulgamento);
  const baixa = mediana(estatistica?.tempoAtePrimeiraBaixa);

  return <div className="datajud-tab">
    {erroGeracao && <div className="notice notice--error" role="alert"><strong>Não foi possível gerar as estatísticas.</strong> {erroGeracao}</div>}
    <div className="datajud-columns"><div className="datajud-main"><section className="datajud-panel detail-card">
    <div className="detail-section-heading"><span className="process-summary-icon process-summary-icon--blue"><Icon name="chart" /></span><div><h2>Análise DataJud</h2><p>A consulta usa o tribunal, o código da classe e o grau, quando disponíveis. A amostra pode incluir diferentes assuntos e órgãos julgadores.</p></div>
    </div>
    {carregando && !dadosGerados ? <ProcessoLoading mensagem="Carregando estatísticas salvas…" /> : erro && !dadosGerados ? <ProcessoFeedback titulo="Não foi possível carregar o DataJud" mensagem={erro} retry={tentarNovamente} />
      : !estatistica ? <ProcessoFeedback titulo="Nenhuma análise DataJud foi realizada para este processo." mensagem="Use o card Gerar estatística para consultar a amostra no DataJud." />
      : <><div className="statistic-cards">
        <EstatisticaCard titulo="Primeiro julgamento" subtitulo="Tempo observado até o primeiro julgamento" valor={julgamento == null ? 'Indisponível' : `${numero(julgamento)} dias`} badge="Mediana" icon="gavel" tom="blue"
          amostra={`${numero(estatistica.tempoAtePrimeiroJulgamento?.quantidade)} processos com duração válida`}
          descricao={julgamento == null ? 'Não há dados suficientes para calcular a mediana do primeiro julgamento.' : `Em pelo menos metade da subamostra válida, o primeiro julgamento ocorreu em até ${numero(julgamento)} dias.`} />
        <EstatisticaCard titulo="Primeira baixa" subtitulo="Tempo observado até a primeira baixa" valor={baixa == null ? 'Indisponível' : `${numero(baixa)} dias`} badge="Mediana" icon="file" tom="green"
          amostra={`${numero(estatistica.tempoAtePrimeiraBaixa?.quantidade)} processos com duração válida`}
          descricao={baixa == null ? 'Não há dados suficientes para calcular a mediana da primeira baixa.' : `Em pelo menos metade da subamostra válida, a primeira baixa ocorreu em até ${numero(baixa)} dias.`} />
        <EstatisticaCard titulo="Sem baixa definitiva identificada" subtitulo="Processos na amostra consultada" valor={`${numero(estatistica.processosPendentes)} processos`} badge="Sem baixa identificada" icon="clock" tom="gold"
          descricao="Contagem dos processos sem baixa definitiva identificada na amostra consultada." />
      </div><p className="datajud-sample">Amostra consultada: {numero(estatistica.quantidadeProcessos)} processos. Os indicadores descrevem a amostra, não uma previsão de resultado.</p></>}
  </section>
  {estatistica && <section className="detail-card datajud-current-summary" aria-labelledby="current-summary-title" aria-live="polite">
    <h2 id="current-summary-title"><Icon name="sparkles" />Síntese das estatísticas com IA</h2>
    {sintese ? <div className="datajud-ai-result"><h3>Síntese atual</h3><p>{sintese.sintese}</p>
      <small><time dateTime={sintese.geradaEm}>{dataSintese(sintese.geradaEm)}</time> · {periodoConsulta(estatistica.parametros)}</small>
    </div> : <p>Esta estatística ainda não possui síntese.</p>}
  </section>}</div><aside className="datajud-sidebar"><form className="detail-card publication-search" onSubmit={event => { event.preventDefault(); void gerar(); }}>
    <h2>Gerar estatística</h2>
    <p className="field-hint">Filtre pela data de ajuizamento. Sem datas, a consulta não aplica recorte temporal.</p>
    <fieldset disabled={gerando || conferindo || analisando || sintetizando}>
      <legend className="sr-only">Período opcional da consulta DataJud</legend>
      <Input id="datajud-inicio" label="Data início (opcional)" type="date" value={inicio} max={fim || '9999-12-31'} onChange={event => setInicio(event.target.value)} />
      <Input id="datajud-fim" label="Data fim (opcional)" type="date" value={fim} min={inicio || undefined} max="9999-12-31" onChange={event => setFim(event.target.value)} error={periodoInvalido ? 'A data fim não pode ser anterior à data início.' : undefined} />
    </fieldset>
    <Button type="submit" loading={gerando} disabled={gerando || conferindo || analisando || sintetizando || periodoInvalido}>Gerar estatística</Button>
    {aviso && <p role="status" className="field-hint">{aviso}</p>}
  </form><AISummaryCard key={`${processoId}-${versao}-${estatistica?.id ?? 'vazio'}`} processoId={processoId} estatisticaId={estatistica?.id}
    habilitado={!gerando && !conferindo && !analisando && Boolean(dadosGerados || (!carregando && !erro)) && (estatistica?.quantidadeProcessos ?? 0) > 0}
    salva={Boolean(sintese)} aoSalvar={resultado => { setSinteseGerada(resultado); setVersaoHistorico(atual => atual + 1); }}
    aoCarregar={setSintetizando} />
    <HistoricoSinteses key={versaoHistorico} processoId={processoId} /></aside></div>
  </div>;
}

function esperar(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise(resolve => {
    if (signal.aborted) { resolve(); return; }
    const terminar = () => { clearTimeout(timer); signal.removeEventListener('abort', terminar); resolve(); };
    const timer = setTimeout(terminar, ms);
    signal.addEventListener('abort', terminar, { once: true });
  });
}
