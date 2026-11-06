import { useEffect, useRef, useState } from 'react';
import type { ClassificacaoProcessual, ConsultaCnjResponse, RegistroDataJudCadastro } from '../../types/processo';
import { processoService } from '../../services/processoService';
import { ApiError, mensagemErro } from '../../services/api';
import { cnjValido, normalizarCnj } from './preenchimentoCnj';
import { Icon } from '../ui/Icon';

export function ConsultaCnj({ numero, desabilitada, preencher, escolherAssunto }: {
  numero: string; desabilitada: boolean;
  preencher: (numero: string, registro: RegistroDataJudCadastro) => void;
  escolherAssunto: (numero: string, assunto: ClassificacaoProcessual | null) => void;
}) {
  const cnj = normalizarCnj(numero);
  const [estado, setEstado] = useState<{ numero: string; carregando: boolean; erro?: string; tentarEm?: number; dados?: ConsultaCnjResponse; indice?: number }>({ numero: '', carregando: false });
  const [tentativa, setTentativa] = useState(0);
  const [assuntoSelecionado, setAssuntoSelecionado] = useState('');
  const cache = useRef(new Map<string, ConsultaCnjResponse>());
  const pausa = useRef<{ ate: number; erro: string } | null>(null);
  const [agora, setAgora] = useState(Date.now);
  const atual = useRef(cnj);
  atual.current = cnj;

  useEffect(() => {
    if (!estado.tentarEm) return;
    setAgora(Date.now());
    const timer = setInterval(() => {
      const instante = Date.now();
      setAgora(instante);
      if (instante >= estado.tentarEm!) clearInterval(timer);
    }, 250);
    return () => clearInterval(timer);
  }, [estado.tentarEm]);

  useEffect(() => {
    if (!cnjValido(cnj) || desabilitada) return;
    const controller = new AbortController();
    let timeout: ReturnType<typeof setTimeout> | undefined;
    let esgotado = false;
    function receber(dados: ConsultaCnjResponse) {
      if (controller.signal.aborted || atual.current !== cnj) return;
      setEstado({ numero: cnj, carregando: false, dados, indice: dados.registros.length === 1 ? 0 : undefined });
      setAssuntoSelecionado('');
      if (dados.registros.length === 1) preencher(cnj, dados.registros[0]);
    }
    const timer = setTimeout(() => {
      const salvo = cache.current.get(cnj);
      if (salvo) { receber(salvo); return; }
      // A pausa vale para qualquer CNJ; mudar o número não deve gerar outra chamada.
      if (pausa.current && pausa.current.ate > Date.now()) {
        setEstado({ numero: cnj, carregando: false, erro: pausa.current.erro, tentarEm: pausa.current.ate });
        return;
      }
      setEstado({ numero: cnj, carregando: true });
      timeout = setTimeout(() => { esgotado = true; controller.abort(); setEstado({ numero: cnj, carregando: false, erro: 'A consulta demorou mais que o esperado. Tente novamente ou continue o cadastro manualmente.' }); }, 60000);
      void processoService.consultarCnj(cnj, controller.signal).then(dados => {
        if (!controller.signal.aborted && atual.current === cnj) { cache.current.set(cnj, dados); receber(dados); }
      }).catch(error => {
        if (controller.signal.aborted || atual.current !== cnj) return;
        const erro = mensagemErro(error);
        const espera = error instanceof ApiError ? (error.retryAfterMs ?? (error.status === 429 ? 30000 : 0)) : 0;
        const tentarEm = espera > 0 ? Date.now() + espera : undefined;
        if (tentarEm) pausa.current = { ate: tentarEm, erro };
        setEstado({ numero: cnj, carregando: false, erro, tentarEm });
      }).finally(() => { if (!esgotado) clearTimeout(timeout); });
    }, 600);
    return () => { clearTimeout(timer); clearTimeout(timeout); controller.abort(); };
  }, [cnj, desabilitada, tentativa, preencher]);

  if (cnj.length < 20) return <p className="field-hint" id="cnj-ajuda">Digite ou cole o CNJ completo para buscar os dados automaticamente.</p>;
  if (!cnjValido(cnj)) return <p className="cnj-feedback cnj-feedback--warning" role="status">Confira os 20 dígitos e os dígitos verificadores do CNJ para realizar a busca automática. O cadastro manual continua disponível.</p>;
  const consulta = estado.numero === cnj ? estado : undefined;
  const segundos = Math.max(0, Math.ceil(((consulta?.tentarEm ?? 0) - agora) / 1000));
  if (!consulta || consulta.carregando) return <p className="cnj-feedback" role="status"><span className="spinner" />Consultando o número CNJ no DataJud…</p>;
  if (consulta.erro) return <div className="cnj-feedback cnj-feedback--warning" role="status"><Icon name="info" /><span>{consulta.erro}</span><button type="button" className="text-button" disabled={desabilitada || segundos > 0} onClick={() => {
    if (pausa.current && pausa.current.ate > Date.now()) return;
    cache.current.delete(cnj); setTentativa(valor => valor + 1);
  }}>{segundos > 0 ? `Tentar novamente em ${segundos}s` : 'Tentar novamente'}</button></div>;
  const dados = consulta.dados;
  const registro = dados && consulta.indice !== undefined ? dados.registros[consulta.indice] : undefined;
  return <div className="cnj-result">
    <p className="cnj-feedback" role="status"><Icon name="check" />{registro ? 'Dados encontrados no DataJud. Os campos disponíveis foram preenchidos; revise antes de cadastrar.' : 'Este CNJ possui mais de um registro no DataJud. Escolha o grau e o órgão que deseja cadastrar.'}</p>
    {dados && dados.registros.length > 1 && <div className="field"><label htmlFor="cnj-registro">Registro do processo no DataJud</label><select id="cnj-registro" value={consulta.indice ?? ''} disabled={desabilitada} onChange={event => {
      if (event.target.value === '') return;
      const indice = Number(event.target.value);
      setEstado(current => ({ ...current, indice })); setAssuntoSelecionado(''); preencher(cnj, dados.registros[indice]);
    }}><option value="" disabled>Selecione o grau e o órgão</option>{dados.registros.map((item, indice) => <option value={indice} key={indice}>{[item.tribunal, item.grau, item.orgaoJulgador?.nome, item.classeProcessual?.nome].filter(Boolean).join(' · ')}</option>)}</select></div>}
    {registro && registro.assuntos.length > 1 && <div className="field"><label htmlFor="cnj-assunto">Assunto a utilizar no cadastro</label><select id="cnj-assunto" value={assuntoSelecionado} disabled={desabilitada} onChange={event => {
      setAssuntoSelecionado(event.target.value); escolherAssunto(cnj, event.target.value === '' ? null : registro.assuntos[Number(event.target.value)]);
    }}><option value="">Selecione entre os assuntos retornados</option>{registro.assuntos.map((item, indice) => <option value={indice} key={indice}>{item.nome || 'Nome não informado'}{item.codigo ? ` — Código ${item.codigo}` : ''}</option>)}</select></div>}
    <p className="field-hint">Campos que você já preencheu são preservados. Complete manualmente os dados que não foram informados.</p>
    {dados?.limiteAtingido && <p className="field-hint">Exibindo os primeiros 100 registros da consulta. Confira o grau e o órgão selecionados.</p>}
  </div>;
}
