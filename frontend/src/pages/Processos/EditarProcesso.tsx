import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ProcessoPageHeader } from '../../components/processos/ProcessoPageHeader';
import { ProcessoForm } from '../../components/processos/ProcessoForm';
import { ProcessoFeedback, ProcessoLoading } from '../../components/processos/ProcessoFeedback';
import { processoService } from '../../services/processoService';
import { ApiError, mensagemErro } from '../../services/api';
import type { Processo, ProcessoRequest } from '../../types/processo';
import './Processos.css';

export function EditarProcesso() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [processo, setProcesso] = useState<Processo | null>(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState('');
  const [naoEncontrado, setNaoEncontrado] = useState(false);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setProcesso(null);
    setErro('');
    setNaoEncontrado(false);
    setLoading(true);
    const numericId = Number(id);
    if (!id || !/^\d+$/.test(id) || !Number.isSafeInteger(numericId) || numericId <= 0) {
      setNaoEncontrado(true); setLoading(false); return;
    }
    void processoService.buscar(numericId, controller.signal).then(setProcesso).catch((error: unknown) => {
      if (controller.signal.aborted) return;
      if (error instanceof ApiError && error.status === 404) setNaoEncontrado(true);
      else setErro(mensagemErro(error));
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [id, tentativa]);

  async function salvar(dados: ProcessoRequest) {
    if (!processo) return;
    await processoService.atualizar(processo.id, dados);
    navigate('/processos', { replace: true, state: { processoSalvo: 'atualizado' } });
  }

  return <div className="process-module">
    <ProcessoPageHeader titulo="Editar processo" descricao="Atualize as informações cadastradas para este processo." breadcrumb="Editar processo" />
    {loading ? <ProcessoLoading mensagem="Carregando informações do processo…" />
      : naoEncontrado ? <ProcessoFeedback titulo="Processo não encontrado" mensagem="Este processo não existe ou não está disponível para sua conta."><Link className="button button--secondary" to="/processos">Voltar aos processos</Link></ProcessoFeedback>
      : erro ? <ProcessoFeedback titulo="Não foi possível carregar o processo" mensagem={erro} retry={() => setTentativa(current => current + 1)} />
      : processo && <ProcessoForm key={processo.id} modo="edit" processo={processo} salvar={salvar} />}
  </div>;
}
