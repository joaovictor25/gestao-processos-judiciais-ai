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
  const origem = useRef<HTMLButtonElement | null>(null);
  const filtros = useRef<HTMLInputElement | null>(null);
  const comunicacoes = dados ?? [];
  const filtradas = useMemo(() => (dados ?? []).filter(item => normalizarTexto(textoComunicacao(item.texto)).includes(normalizarTexto(busca))), [dados, busca]);
  const paginaAtual = Math.min(pagina, Math.max(1, Math.ceil(filtradas.length / porPagina)));
  const selecionado = comunicacoes.find(item => item.id === selecionada);
  useEffect(() => { setResultado(null); }, [processoId, versao]);
  function abrir(item: Comunicacao, botao: HTMLButtonElement) { origem.current = botao; setSelecionada(item.id); }
  function fechar() { setSelecionada(null); if (origem.current?.isConnected) origem.current.focus({ preventScroll: true }); else filtros.current?.focus(); }

  return <div className="communications-analysis-layout">
    <AnaliseComunicacoesCard key={`${processoId}-${versao}`} processoId={processoId} quantidade={comunicacoes.length} aoResultado={setResultado} />
    <div className="communications-main">
    {resultado && <AnaliseComunicacoesResultado resultado={resultado} />}
    {carregando ? <ProcessoLoading mensagem="Carregando comunicações salvas…" /> : erro ? <ProcessoFeedback titulo="Não foi possível carregar as comunicações" mensagem={erro} retry={tentarNovamente} />
      : comunicacoes.length === 0 ? <ProcessoFeedback titulo="Nenhuma comunicação foi encontrada para este processo." mensagem="Após solicitar uma análise, os resultados disponíveis serão exibidos aqui." />
      : <section className="detail-card communications-results"><div className="communication-filter"><h2>Filtros</h2><Input id="communication-search" label="Buscar no texto das comunicações" icon="search" ref={filtros} placeholder="Buscar no texto das comunicações" value={busca} onChange={event => { setBusca(event.target.value); setPagina(1); }} /></div>
          {filtradas.length ? <ComunicacoesTable comunicacoes={filtradas.slice((paginaAtual - 1) * porPagina, paginaAtual * porPagina)} selecionada={selecionada ?? undefined} abrir={abrir} /> : <ProcessoFeedback titulo="Nenhuma comunicação corresponde à busca." mensagem="Tente outro trecho do texto." />}
          <Pagination total={filtradas.length} pagina={paginaAtual} porPagina={porPagina} mudarPagina={setPagina} mudarQuantidade={valor => { setPorPagina(valor); setPagina(1); }} itens="comunicações" quantidades={[20, 50, 100]} />
        </section>}
    {!carregando && !erro && selecionado && <ComunicacaoDetailsModal comunicacao={selecionado} fechar={fechar} />}
    </div>
  </div>;
}
