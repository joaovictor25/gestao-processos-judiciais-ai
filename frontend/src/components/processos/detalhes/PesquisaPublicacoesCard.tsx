import { useEffect, useRef, useState } from 'react';
import type { Comunicacao } from '../../../types/comunicacao';
import { processoService } from '../../../services/processoService';
import { mensagemErro } from '../../../services/api';
import { Input } from '../../forms/Input';
import { Button } from '../../ui/Button';
import { Icon } from '../../ui/Icon';

export function PesquisaPublicacoesCard({ processoId, bloqueado, aoAlterar, aoCarregar, aoResultado }: {
  processoId: number; bloqueado: boolean;
  aoAlterar: () => void; aoCarregar: (carregando: boolean) => void;
  aoResultado: (publicacoes: Comunicacao[]) => void;
}) {
  const [texto, setTexto] = useState('');
  const [inicio, setInicio] = useState('');
  const [fim, setFim] = useState('');
  const [pesquisando, setPesquisando] = useState(false);
  const [erro, setErro] = useState('');
  const pedido = useRef<AbortController | null>(null);
  const periodoInvalido = Boolean(inicio && fim && fim < inicio);
  useEffect(() => () => pedido.current?.abort(), []);
  function alterar() { setErro(''); aoAlterar(); }

  async function pesquisar() {
    if (pedido.current || bloqueado || !texto.trim() || periodoInvalido) return;
    const controller = new AbortController();
    pedido.current = controller;
    setPesquisando(true); setErro(''); aoAlterar(); aoCarregar(true);
    try {
      const publicacoes = await processoService.pesquisarPublicacoes(processoId, {
        texto: texto.trim(), dataDisponibilizacaoInicio: inicio || undefined, dataDisponibilizacaoFim: fim || undefined,
      }, controller.signal);
      if (!controller.signal.aborted) aoResultado(publicacoes);
    } catch (falha) {
      if (!controller.signal.aborted) setErro(mensagemErro(falha));
    } finally {
      if (!controller.signal.aborted) { pedido.current = null; setPesquisando(false); aoCarregar(false); }
    }
  }

  return <form className="detail-card publication-search" aria-labelledby="publication-search-title"
    onSubmit={event => { event.preventDefault(); void pesquisar(); }}>
    <h2 id="publication-search-title"><Icon name="search" />Pesquisar por publicações</h2>
    <fieldset disabled={pesquisando || bloqueado}>
      <legend className="sr-only">Filtros da pesquisa de publicações</legend>
      <Input id="publication-text" label="Texto da busca" required value={texto} placeholder="Ex.: indenização por dano moral"
        onChange={event => { setTexto(event.target.value); alterar(); }} />
      <Input id="publication-start" label="Data inicial (opcional)" type="date" value={inicio} max={fim || '9999-12-31'}
        onChange={event => { setInicio(event.target.value); alterar(); }} />
      <Input id="publication-end" label="Data final (opcional)" type="date" value={fim} min={inicio || undefined} max="9999-12-31"
        error={periodoInvalido ? 'A data final não pode ser anterior à data inicial.' : undefined}
        onChange={event => { setFim(event.target.value); alterar(); }} />
    </fieldset>
    <p className="field-hint">Pesquise pelo texto no tribunal deste processo. As datas filtram a disponibilização das publicações e são opcionais.</p>
    <Button type="submit" loading={pesquisando} disabled={bloqueado || pesquisando || !texto.trim() || periodoInvalido}>
      <Icon name="search" />{pesquisando ? 'Pesquisando publicações...' : 'Pesquisar publicações'}
    </Button>
    <p className="field-hint">A pesquisa consulta até 200 publicações.</p>
    {erro && <p className="notice notice--error" role="alert">{erro}</p>}
  </form>;
}
