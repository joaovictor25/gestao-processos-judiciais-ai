import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ProcessoPageHeader } from '../../components/processos/ProcessoPageHeader';
import { ProcessoFeedback, ProcessoLoading } from '../../components/processos/ProcessoFeedback';
import { ProcessoTable } from '../../components/processos/ProcessoTable';
import { Pagination } from '../../components/processos/Pagination';
import { emAndamento, normalizarTexto } from '../../components/processos/processoFormModel';
import { Input } from '../../components/forms/Input';
import { Button } from '../../components/ui/Button';
import { Icon } from '../../components/ui/Icon';
import { ApiError, mensagemErro } from '../../services/api';
import { processoService } from '../../services/processoService';
import type { EstadoAnalise, Processo, ProcessoResumo } from '../../types/processo';
import './Processos.css';

export function Processos() {
  const location = useLocation();
  const navigate = useNavigate();
  const [processos, setProcessos] = useState<Processo[]>([]);
  const [analises, setAnalises] = useState<Record<number, EstadoAnalise>>({});
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState('');
  const [tentativa, setTentativa] = useState(0);
  const [busca, setBusca] = useState('');
  const [pagina, setPagina] = useState(1);
  const [porPagina, setPorPagina] = useState(10);
  const [aviso, setAviso] = useState<{ mensagem: string; erro?: boolean } | null>(null);
  const emExecucao = useRef(new Set<number>());

  useEffect(() => {
    const state: unknown = location.state;
    if (typeof state === 'object' && state !== null && 'processoSalvo' in state) {
      if (state.processoSalvo === 'cadastrado' || state.processoSalvo === 'atualizado') {
        setAviso({ mensagem: `Processo ${state.processoSalvo} com sucesso.` });
        navigate(location.pathname, { replace: true, state: null });
      }
    }
  }, [location.state, location.pathname, navigate]);

  useEffect(() => {
    const controller = new AbortController();
    const signal = controller.signal;
    setLoading(true);
    setErro('');
    async function carregar() {
      try {
        const lista = await processoService.listar(signal);
        if (signal.aborted) return;
        setProcessos(lista);
        setAnalises(Object.fromEntries(lista.map(processo => [processo.id, { status: 'consultando' }])));
        setLoading(false);
        // A API ainda não tem resumo em lote. Limita as consultas a quatro por vez.
        let proximo = 0;
        await Promise.all(Array.from({ length: Math.min(4, lista.length) }, async () => {
          while (!signal.aborted && proximo < lista.length) {
            const processo = lista[proximo++];
            let estado: EstadoAnalise;
            try {
              const resultado = await processoService.consultarAnalise(processo.id, signal);
              const possuiResultados = resultado.estatisticas.length > 0 || resultado.comunicacoes.length > 0;
              estado = { status: possuiResultados ? 'disponivel' : 'nao-analisado', possuiResultados };
            } catch {
              if (signal.aborted) return;
              estado = { status: 'indisponivel' };
            }
            if (!signal.aborted) setAnalises(current => current[processo.id]?.status === 'consultando' ? { ...current, [processo.id]: estado } : current);
          }
        }));
      } catch (error) {
        if (!signal.aborted) { setErro(mensagemErro(error)); setLoading(false); }
      }
    }
    void carregar();
    return () => controller.abort();
  }, [tentativa]);

  async function consultar(id: number) {
    if (emExecucao.current.has(id)) return;
    setAnalises(current => ({ ...current, [id]: { status: 'consultando' } }));
    try {
      const resultado = await processoService.consultarAnalise(id);
      const possuiResultados = resultado.estatisticas.length > 0 || resultado.comunicacoes.length > 0;
      setAnalises(current => ({ ...current, [id]: { status: possuiResultados ? 'disponivel' : 'nao-analisado', possuiResultados } }));
    } catch { setAnalises(current => ({ ...current, [id]: { status: 'indisponivel' } })); }
  }

  async function analisar(processo: ProcessoResumo) {
    if (emExecucao.current.has(processo.id)) return;
    emExecucao.current.add(processo.id);
    const anterior = analises[processo.id];
    setAnalises(current => ({ ...current, [processo.id]: { status: 'analisando', possuiResultados: anterior?.possuiResultados, iniciadaEm: new Date().toISOString() } }));
    setAviso(null);
    try {
      await processoService.analisar(processo.id);
      setAnalises(current => ({ ...current, [processo.id]: { status: 'disponivel', possuiResultados: true } }));
      setAviso({ mensagem: `Análise do processo ${processo.numeroProcesso || processo.id} concluída com sucesso.` });
    } catch (error) {
      const semConfirmacao = error instanceof ApiError && error.status === 0;
      setAnalises(current => ({ ...current, [processo.id]: { status: semConfirmacao ? 'indisponivel' : 'falha', possuiResultados: anterior?.possuiResultados } }));
      setAviso({ erro: true, mensagem: semConfirmacao ? 'A conexão foi interrompida e não foi possível confirmar a conclusão. Consulte novamente o status do processo.' : `Não foi possível analisar o processo ${processo.numeroProcesso || processo.id}. ${mensagemErro(error)}` });
    } finally { emExecucao.current.delete(processo.id); }
  }

  const termo = normalizarTexto(busca);
  const apenasNumero = /\d/.test(busca) && /^[\d.\-/\s]+$/.test(busca.trim());
  const filtrados = processos.filter(processo => !termo || normalizarTexto(processo.numeroProcesso).includes(termo)
    || normalizarTexto(processo.clienteVinculado).includes(termo)
    || (apenasNumero && (processo.numeroProcesso ?? '').replace(/\D/g, '').includes(busca.replace(/\D/g, ''))));
  const paginaAtual = Math.min(pagina, Math.max(1, Math.ceil(filtrados.length / porPagina)));
  const visiveis = filtrados.slice((paginaAtual - 1) * porPagina, paginaAtual * porPagina);
  const semResumo = processos.some(processo => analises[processo.id]?.possuiResultados === undefined);
  const consultandoResumo = processos.some(processo => analises[processo.id]?.status === 'consultando');
  const aguardando = semResumo ? null : processos.filter(processo => !analises[processo.id]?.possuiResultados && analises[processo.id]?.status !== 'analisando').length;

  return <div className="process-module">
    <ProcessoPageHeader titulo="Processos" descricao="Gerencie os processos cadastrados e solicite novas análises."
      acao={<Link className="button button--primary" to="/processos/novo"><Icon name="plus" width="20" />Novo processo</Link>} />
    {aviso && <div className={`notice ${aviso.erro ? 'notice--error' : 'notice--success'} process-notice`} role={aviso.erro ? 'alert' : 'status'}><Icon name={aviso.erro ? 'alert' : 'check'} /><span>{aviso.mensagem}</span><button type="button" aria-label="Fechar mensagem" onClick={() => setAviso(null)}><Icon name="close" /></button></div>}
    <section className="process-summary-grid" aria-label="Resumo dos processos">
      <article className="process-summary"><span className="process-summary-icon process-summary-icon--blue"><Icon name="folder" /></span><div><h2>Processos cadastrados</h2><strong>{loading || erro ? '—' : processos.length}</strong></div></article>
      <article className="process-summary"><span className="process-summary-icon process-summary-icon--green"><Icon name="play" /></span><div><h2>Em andamento</h2><strong>{loading || erro ? '—' : processos.filter(processo => emAndamento(processo.status)).length}</strong></div></article>
      <article className="process-summary"><span className="process-summary-icon process-summary-icon--gold"><Icon name="clock" /></span><div><h2>Aguardando análise</h2><strong>{loading || erro || aguardando === null ? '—' : aguardando}</strong>{!loading && !erro && aguardando === null && <small>{consultandoResumo ? 'Verificando resultados' : 'Resumo indisponível'}</small>}</div></article>
    </section>
    <div className="process-search"><Input id="buscar-processos" label="Buscar por número do processo ou cliente" icon="search" type="search" placeholder="Buscar por número do processo ou cliente" value={busca} onChange={event => { setBusca(event.target.value); setPagina(1); }} disabled={loading || Boolean(erro)} /></div>
    {loading ? <ProcessoLoading /> : erro ? <ProcessoFeedback titulo="Não foi possível carregar os processos" mensagem={erro} retry={() => setTentativa(current => current + 1)} />
      : !processos.length ? <ProcessoFeedback titulo="Nenhum processo cadastrado." mensagem="Adicione seu primeiro processo para começar a organizar sua carteira."><Link className="button button--primary" to="/processos/novo"><Icon name="plus" width="18" />Cadastrar primeiro processo</Link></ProcessoFeedback>
      : !filtrados.length ? <ProcessoFeedback titulo="Nenhum processo encontrado" mensagem="Tente buscar por outro número ou nome de cliente."><Button variant="secondary" onClick={() => { setBusca(''); setPagina(1); }}>Limpar busca</Button></ProcessoFeedback>
      : <section className="process-list-panel" aria-label="Lista de processos">
        <ProcessoTable processos={visiveis} analises={analises} analisar={processo => void analisar(processo)} consultar={id => void consultar(id)} />
        <Pagination total={filtrados.length} pagina={paginaAtual} porPagina={porPagina} mudarPagina={setPagina} mudarQuantidade={quantidade => { setPorPagina(quantidade); setPagina(1); }} />
      </section>}
    {!loading && !erro && processos.length > 0 && <p className="process-analysis-note"><Icon name="info" />Uma nova análise substitui os resultados anteriores somente do processo selecionado.</p>}
  </div>;
}
