import { useEffect, useRef, useState } from 'react';
import type { AnaliseComunicacao, TipoAnaliseComunicacao } from '../../../types/analiseComunicacao';
import { tiposAnaliseComunicacao } from '../../../types/analiseComunicacao';
import { processoService } from '../../../services/processoService';
import { mensagemErro } from '../../../services/api';
import { Button } from '../../ui/Button';
import { Icon } from '../../ui/Icon';

export function AnaliseComunicacoesCard({ processoId, quantidade, aoResultado }: {
  processoId: number; quantidade: number; aoResultado: (resultado: AnaliseComunicacao | null) => void;
}) {
  const [tipos, setTipos] = useState<TipoAnaliseComunicacao[]>([]);
  const [analisando, setAnalisando] = useState(false);
  const [erro, setErro] = useState('');
  const emAndamento = useRef(false);
  const pedido = useRef<AbortController | null>(null);
  useEffect(() => () => { pedido.current?.abort(); }, []);

  function alternar(tipo: TipoAnaliseComunicacao) {
    if (emAndamento.current) return;
    setTipos(atuais => atuais.includes(tipo) ? atuais.filter(item => item !== tipo) : [...atuais, tipo]);
    setErro('');
    aoResultado(null);
  }

  async function analisar() {
    if (emAndamento.current || !quantidade || !tipos.length) return;
    emAndamento.current = true;
    setAnalisando(true);
    setErro('');
    aoResultado(null);
    const controller = new AbortController();
    pedido.current = controller;
    try {
      const resultado = await processoService.analisarComunicacoes(processoId, tipos, controller.signal);
      if (!controller.signal.aborted) aoResultado(resultado);
    } catch (falha) {
      if (!controller.signal.aborted) setErro(mensagemErro(falha));
    } finally {
      if (!controller.signal.aborted) { emAndamento.current = false; setAnalisando(false); pedido.current = null; }
    }
  }

  return <aside className="detail-card communication-ai-options" aria-labelledby="communication-ai-title">
    <h2 id="communication-ai-title"><Icon name="sparkles" />Análise das comunicações</h2>
    <p>Selecione as informações que deseja identificar nas comunicações encontradas.</p>
    <fieldset disabled={analisando}>
      <legend className="sr-only">Tipos de análise</legend>
      {tiposAnaliseComunicacao.map(item => <label key={item.codigo} className="communication-ai-choice">
        <input type="checkbox" checked={tipos.includes(item.codigo)} onChange={() => alternar(item.codigo)} />
        <span>{item.titulo}</span>
      </label>)}
    </fieldset>
    <Button type="button" loading={analisando} disabled={!quantidade || !tipos.length || analisando} onClick={analisar}>
      <Icon name="sparkles" />{analisando ? 'Analisando comunicações...' : 'Analisar com IA'}
    </Button>
    {!quantidade && <p className="field-hint">É preciso ter comunicações salvas para iniciar a análise.</p>}
    {erro && <p className="notice notice--error" role="alert">{erro}</p>}
  </aside>;
}
