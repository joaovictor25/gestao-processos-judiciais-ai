import { useEffect, useRef, useState } from 'react';
import { Icon } from '../../ui/Icon';
import { Button } from '../../ui/Button';
import { processoService } from '../../../services/processoService';
import { ApiError, mensagemErro } from '../../../services/api';
import type { SinteseDataJud } from '../../../types/estatisticas';

export function AISummaryCard({ processoId, estatisticaId, habilitado, aoCarregar, salva, aoSalvar }: {
  processoId: number; estatisticaId?: number; habilitado: boolean; aoCarregar: (valor: boolean) => void;
  salva: boolean; aoSalvar: (sintese: SinteseDataJud) => void;
}) {
  const [erro, setErro] = useState('');
  const [gerando, setGerando] = useState(false);
  const pedido = useRef<AbortController | null>(null);
  useEffect(() => () => { pedido.current?.abort(); aoCarregar(false); }, [aoCarregar]);

  async function gerar() {
    if (pedido.current || !habilitado || estatisticaId == null) return;
    const controller = new AbortController();
    pedido.current = controller;
    setGerando(true); aoCarregar(true); setErro('');
    try {
      const resultado = await processoService.sintetizarEstatisticas(processoId, estatisticaId, controller.signal);
      if (controller.signal.aborted) return;
      if (resultado.estatisticaId !== estatisticaId || typeof resultado.sintese !== 'string' || !resultado.sintese.trim()
          || typeof resultado.geradaEm !== 'string' || !Number.isFinite(Date.parse(resultado.geradaEm)))
        throw new ApiError('Não foi possível obter a síntese destas estatísticas. Tente novamente.');
      aoSalvar(resultado);
    } catch (falha) {
      if (!controller.signal.aborted) setErro(mensagemErro(falha));
    } finally {
      if (!controller.signal.aborted) { pedido.current = null; setGerando(false); aoCarregar(false); }
    }
  }

  return <section className="ai-summary-card" aria-labelledby="datajud-ai-title" aria-busy={gerando}>
    <div><h2 id="datajud-ai-title"><Icon name="sparkles" />Síntese das estatísticas com IA</h2>
      <p>Uma explicação breve dos indicadores e das limitações da amostra consultada.</p>
    </div>
    <div className="ai-summary-action">
      <Button onClick={() => void gerar()} loading={gerando} disabled={!habilitado || estatisticaId == null || gerando}>
        <Icon name="sparkles" />{gerando ? 'Gerando síntese...' : 'Gerar síntese'}
      </Button>
      {!habilitado && !gerando && <small>Gere ou aguarde o carregamento das estatísticas com processos na amostra para solicitar a síntese.</small>}
    </div>
    {estatisticaId != null && <p className="datajud-summary-status" role="status">{salva ? 'Síntese atual salva' : 'Esta estatística ainda não possui síntese.'}</p>}
    {erro && <p className="notice notice--error" role="alert">{erro}</p>}
  </section>;
}
