import { useEffect, useRef, useState } from 'react';
import type { PesquisaComunicacao } from '../../../types/analiseComunicacao';
import { processoService } from '../../../services/processoService';
import { mensagemErro } from '../../../services/api';
import { Button } from '../../ui/Button';
import { Icon } from '../../ui/Icon';

export function AnaliseComunicacoesCard({ processoId, comunicacaoIds, habilitado, aoCarregar, aoResultado }: {
  processoId: number; comunicacaoIds: number[]; habilitado: boolean; aoCarregar: (carregando: boolean) => void;
  aoResultado: (resultado: PesquisaComunicacao | null) => void;
}) {
  const [pesquisa, setPesquisa] = useState<PesquisaComunicacao>();
  const [carregando, setCarregando] = useState(true);
  const [analisando, setAnalisando] = useState(false);
  const [erro, setErro] = useState('');
  const [tentativa, setTentativa] = useState(0);
  const pedido = useRef<AbortController | null>(null);
  const ids = comunicacaoIds.join(',');
  useEffect(() => {
    const controller = new AbortController();
    setCarregando(true); setErro(''); setPesquisa(undefined); aoResultado(null);
    void processoService.pesquisaAtualComunicacoes(processoId, controller.signal).then(atual => {
      if (controller.signal.aborted) return;
      if (atual?.comunicacaoIds.join(',') === ids) { setPesquisa(atual); aoResultado(atual); }
    }).catch(falha => { if (!controller.signal.aborted) setErro(mensagemErro(falha)); })
      .finally(() => { if (!controller.signal.aborted) setCarregando(false); });
    return () => { controller.abort(); pedido.current?.abort(); };
  }, [processoId, ids, tentativa, aoResultado]);

  async function analisar() {
    if (pedido.current || carregando || !habilitado || !comunicacaoIds.length || pesquisa?.sintese) return;
    const controller = new AbortController(); pedido.current = controller;
    setAnalisando(true); aoCarregar(true); setErro('');
    try {
      const resultado = await processoService.sintetizarComunicacoes(processoId, comunicacaoIds, pesquisa?.id, controller.signal);
      if (!controller.signal.aborted) { setPesquisa(resultado); aoResultado(resultado); }
    } catch (falha) {
      if (!controller.signal.aborted) setErro(mensagemErro(falha));
    } finally {
      if (!controller.signal.aborted) { setAnalisando(false); pedido.current = null; aoCarregar(false); }
    }
  }

  return <aside className="detail-card communication-ai-options" aria-labelledby="communication-ai-title">
    <h2 id="communication-ai-title"><Icon name="sparkles" />Síntese das comunicações</h2>
    <p>Gere uma síntese breve e integrada das publicações desta busca.</p>
    <Button type="button" loading={analisando} disabled={!habilitado || !comunicacaoIds.length || carregando || analisando || Boolean(pesquisa?.sintese)} onClick={analisar}>
      <Icon name="sparkles" />{analisando ? 'Gerando síntese...' : pesquisa?.sintese ? 'Síntese gerada' : 'Gerar síntese com IA'}
    </Button>
    <p className="field-hint">{carregando ? 'Carregando a busca vinculada...'
      : !habilitado ? 'Aguarde o carregamento das comunicações.'
        : !comunicacaoIds.length ? 'Pesquise publicações para gerar uma síntese.'
          : 'A síntese fica salva no histórico junto aos filtros e às publicações desta busca.'}</p>
    {erro && <div className="notice notice--error" role="alert">{erro} <button type="button" className="text-button" disabled={analisando} onClick={() => setTentativa(v => v + 1)}>Recarregar busca</button></div>}
  </aside>;
}
