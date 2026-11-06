import { useCallback, useEffect, useRef, useState } from 'react';
import type { PesquisaComunicacao } from '../../../types/analiseComunicacao';
import { processoService } from '../../../services/processoService';
import { useDadosProcesso } from '../../../pages/Processos/useDadosProcesso';
import { Button } from '../../ui/Button';
import { ProcessoFeedback, ProcessoLoading } from '../ProcessoFeedback';
import { dataSintese } from './HistoricoSinteses';
import { textoComunicacao } from './formatacao';

function FiltrosPesquisa({ pesquisa }: { pesquisa: PesquisaComunicacao }) {
  const data = (valor: string) => valor.split('-').reverse().join('/');
  return <p className="field-hint">
    {pesquisa.textoBusca ? `Busca: “${pesquisa.textoBusca}”` : 'Comunicações anteriores — filtros originais não registrados'}
    {pesquisa.tribunal && ` · ${pesquisa.tribunal}`}<br />
    {pesquisa.dataInicio || pesquisa.dataFim
      ? `Período: ${pesquisa.dataInicio ? data(pesquisa.dataInicio) : 'sem início'} a ${pesquisa.dataFim ? data(pesquisa.dataFim) : 'sem fim'}`
      : pesquisa.textoBusca ? 'Sem recorte de data' : 'Período original não registrado'}
    {` · ${pesquisa.quantidade} publicações`}
  </p>;
}

export function SinteseComunicacoesResultado({ pesquisa }: { pesquisa: PesquisaComunicacao }) {
  return <section className="detail-card" aria-label="Síntese da busca de comunicações">
    <h2>Síntese das comunicações</h2>
    <FiltrosPesquisa pesquisa={pesquisa} />
    {pesquisa.geradaEm && <p className="field-hint">Gerada em <time dateTime={pesquisa.geradaEm}>{dataSintese(pesquisa.geradaEm)}</time></p>}
    <p className="datajud-history-text">{pesquisa.sintese}</p>
    <p className="field-hint">{pesquisa.comunicacoesConsideradas ?? 0} publicações com texto consideradas{pesquisa.duplicadasIgnoradas ? `; ${pesquisa.duplicadasIgnoradas} duplicadas ignoradas` : ''}.</p>
    {pesquisa.evidencias.length > 0 && <details><summary>Trechos de apoio</summary>
      {pesquisa.evidencias.map((e, i) => <blockquote key={i}>{e.trecho}</blockquote>)}
    </details>}
  </section>;
}

export function HistoricoSintesesComunicacoes({ processoId, versao }: { processoId: number; versao: string }) {
  const [pagina, setPagina] = useState(0);
  const [selecionada, setSelecionada] = useState<number | null>(null);
  const carregar = useCallback((id: number, signal?: AbortSignal) => processoService.historicoSintesesComunicacoes(id, pagina, signal), [pagina, versao]);
  const { dados, carregando, erro, tentarNovamente } = useDadosProcesso(processoId, carregar);
  return <section className="detail-card datajud-history" aria-label="Histórico de sínteses das comunicações">
    <h2>Histórico de sínteses</h2>
    {carregando ? <ProcessoLoading mensagem="Carregando histórico…" /> : erro
      ? <ProcessoFeedback titulo="Não foi possível carregar o histórico" mensagem={erro} retry={tentarNovamente} />
      : !dados?.itens.length ? <p>Nenhuma síntese salva para este processo.</p>
        : <ul>{dados.itens.map(item => <li key={item.id}>
          {item.geradaEm && <time dateTime={item.geradaEm}>{dataSintese(item.geradaEm)}</time>}
          <FiltrosPesquisa pesquisa={item} />
          <Button variant="secondary" onClick={() => setSelecionada(item.id)}>Ver síntese e busca</Button>
        </li>)}</ul>}
    <div className="datajud-history-pages">
      {pagina > 0 && <Button variant="secondary" disabled={carregando} onClick={() => setPagina(p => p - 1)}>Anteriores</Button>}
      {!erro && dados?.temProxima && <Button variant="secondary" disabled={carregando} onClick={() => setPagina(p => p + 1)}>Ver mais</Button>}
    </div>
    {selecionada != null && <Historica key={selecionada} processoId={processoId} pesquisaId={selecionada} fechar={() => setSelecionada(null)} />}
  </section>;
}

function Historica({ processoId, pesquisaId, fechar }: { processoId: number; pesquisaId: number; fechar: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const carregar = useCallback((id: number, signal?: AbortSignal) => processoService.pesquisaComunicacoes(id, pesquisaId, signal), [pesquisaId]);
  const { dados, carregando, erro, tentarNovamente } = useDadosProcesso(processoId, carregar);
  useEffect(() => {
    const modal = dialog.current, anterior = document.activeElement, overflow = document.body.style.overflow;
    modal?.showModal(); document.body.style.overflow = 'hidden';
    return () => { modal?.close(); document.body.style.overflow = overflow; if (anterior instanceof HTMLElement && anterior.isConnected) anterior.focus(); };
  }, []);
  return <dialog ref={dialog} className="datajud-history-modal" aria-label="Síntese e publicações da busca" onCancel={e => { e.preventDefault(); fechar(); }}>
    {carregando ? <ProcessoLoading mensagem="Carregando síntese…" /> : erro
      ? <ProcessoFeedback titulo="Não foi possível abrir a síntese" mensagem={erro} retry={tentarNovamente} />
        : dados && <>
          <SinteseComunicacoesResultado pesquisa={dados.pesquisa} />
          <details><summary>Publicações desta busca ({dados.publicacoes.length})</summary>
            {dados.publicacoes.map((p, i) => <article key={p.id}><h3>Publicação {i + 1}</h3>
              <p className="field-hint">{[p.siglaTribunal, p.dataDisponibilizacao, p.tipoComunicacao].filter(Boolean).join(' · ')}</p>
              <p className="datajud-history-text">{textoComunicacao(p.texto)}</p></article>)}
          </details>
        </>}
    <Button variant="secondary" onClick={fechar}>Fechar</Button>
  </dialog>;
}
