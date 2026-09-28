import { useEffect, useMemo, useRef, useState } from 'react';
import type { Comunicacao } from '../../../types/comunicacao';
import { processoService } from '../../../services/processoService';
import { useDadosProcesso } from '../../../pages/Processos/useDadosProcesso';
import { Input } from '../../forms/Input';
import { Pagination } from '../Pagination';
import { normalizarTexto } from '../processoFormModel';
import { ProcessoFeedback, ProcessoLoading } from '../ProcessoFeedback';
import type { AnaliseComunicacao } from '../../../types/analiseComunicacao';
import { AnaliseComunicacoesCard } from './AnaliseComunicacoesCard';
import { PesquisaPublicacoesCard } from './PesquisaPublicacoesCard';
import { AnaliseComunicacoesResultado } from './AnaliseComunicacoesResultado';
import { ComunicacoesTable } from './ComunicacoesTable';
import { ComunicacaoDetailsModal } from './ComunicacaoDetailsModal';
import { textoComunicacao } from './formatacao';

export function ComunicacoesTab({ processoId, versao }: { processoId: number; versao: number }) {
  const { dados, carregando, erro, tentarNovamente } = useDadosProcesso(processoId, processoService.comunicacoes, versao);
  const [busca, setBusca] = useState('');
  const [pagina, setPagina] = useState(1);
  const [porPagina, setPorPagina] = useState(20);
  const [selecionada, setSelecionada] = useState<number | null>(null);
  const [resultado, setResultado] = useState<AnaliseComunicacao | null>(null);
  const [pesquisa, setPesquisa] = useState<Comunicacao[] | null>(null);
  const [pesquisaAtual, setPesquisaAtual] = useState(false);
  const [pesquisando, setPesquisando] = useState(false);
  const [analisando, setAnalisando] = useState(false);
  const origem = useRef<HTMLButtonElement | null>(null);
  const filtros = useRef<HTMLInputElement | null>(null);
  const comunicacaoIds = useMemo(() => (pesquisa ?? []).map(item => item.id), [pesquisa]);
  const comunicacoes = useMemo(() => resultado
    ? resultado.comunicacoes.map(({ numero, ...item }) => ({ ...item, id: numero, processoId }))
    : pesquisa ?? dados ?? [], [resultado, pesquisa, dados, processoId]);
  const filtradas = useMemo(() => comunicacoes.filter(item => normalizarTexto(resultado ? item.texto ?? '' : textoComunicacao(item.texto)).includes(normalizarTexto(busca))), [comunicacoes, busca, resultado]);
  const paginaAtual = Math.min(pagina, Math.max(1, Math.ceil(filtradas.length / porPagina)));
  const selecionado = comunicacoes.find(item => item.id === selecionada);
  useEffect(() => { setResultado(null); }, [processoId, versao]);
  function limparResultado() { setResultado(null); setBusca(''); setPagina(1); setSelecionada(null); }
  function alterarPesquisa() { setPesquisaAtual(false); limparResultado(); }
  function receberPesquisa(publicacoes: Comunicacao[]) {
    setPesquisa(publicacoes); setPesquisaAtual(true); limparResultado();
  }
  function receberAnalise(novo: AnaliseComunicacao | null) {
    limparResultado(); setResultado(novo);
  }
  function abrir(item: Comunicacao, botao: HTMLButtonElement) { origem.current = botao; setSelecionada(item.id); }
  function fechar() { setSelecionada(null); if (origem.current?.isConnected) origem.current.focus({ preventScroll: true }); else filtros.current?.focus(); }

  return <div className="communications-analysis-layout">
    <div className="communications-sidebar">
      <PesquisaPublicacoesCard processoId={processoId} bloqueado={analisando} aoAlterar={alterarPesquisa}
        aoCarregar={setPesquisando} aoResultado={receberPesquisa} />
      <AnaliseComunicacoesCard key={`${processoId}-${versao}-${comunicacaoIds.join(',')}`} processoId={processoId}
        comunicacaoIds={comunicacaoIds} habilitado={pesquisaAtual && !pesquisando} aoCarregar={setAnalisando} aoResultado={receberAnalise} />
    </div>
    <div className="communications-main">
    {resultado && <AnaliseComunicacoesResultado resultado={resultado} />}
    {pesquisando ? <ProcessoLoading mensagem="Pesquisando publicações…" />
      : pesquisa === null && carregando ? <ProcessoLoading mensagem="Carregando comunicações salvas…" /> : pesquisa === null && erro ? <ProcessoFeedback titulo="Não foi possível carregar as comunicações" mensagem={erro} retry={tentarNovamente} />
      : comunicacoes.length === 0 ? <ProcessoFeedback titulo={pesquisa === null ? 'Nenhuma comunicação foi encontrada para este processo.' : 'Nenhuma publicação encontrada para os filtros informados.'} mensagem="Informe o texto e, se desejar, as datas na caixa Pesquisar por publicações." />
      : <section className="detail-card communications-results"><div className="communication-filter"><h2>{resultado ? 'Publicações usadas nesta análise' : pesquisa !== null ? 'Publicações da última pesquisa' : 'Comunicações salvas'}</h2><Input id="communication-search" label="Buscar no texto das comunicações" icon="search" ref={filtros} placeholder="Buscar no texto das comunicações" value={busca} onChange={event => { setBusca(event.target.value); setPagina(1); }} /></div>
          {filtradas.length ? <ComunicacoesTable comunicacoes={filtradas.slice((paginaAtual - 1) * porPagina, paginaAtual * porPagina)} fontesAnalise={Boolean(resultado)} selecionada={selecionada ?? undefined} abrir={abrir} /> : <ProcessoFeedback titulo="Nenhuma comunicação corresponde à busca." mensagem="Tente outro trecho do texto." />}
          <Pagination total={filtradas.length} pagina={paginaAtual} porPagina={porPagina} mudarPagina={setPagina} mudarQuantidade={valor => { setPorPagina(valor); setPagina(1); }} itens="comunicações" quantidades={[20, 50, 100]} />
        </section>}
    {!pesquisando && (pesquisa !== null || (!carregando && !erro)) && selecionado && <ComunicacaoDetailsModal comunicacao={selecionado} textoNormalizado={Boolean(resultado)} fechar={fechar} />}
    </div>
  </div>;
}
