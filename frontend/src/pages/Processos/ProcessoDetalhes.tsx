import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ProcessoFeedback, ProcessoLoading } from '../../components/processos/ProcessoFeedback';
import { ProcessHeader } from '../../components/processos/detalhes/ProcessHeader';
import { ProcessTabs, type ProcessoAba } from '../../components/processos/detalhes/ProcessTabs';
import { InformacoesGeraisTab } from '../../components/processos/detalhes/InformacoesGeraisTab';
import { DataJudTab } from '../../components/processos/detalhes/DataJudTab';
import { ComunicacoesTab } from '../../components/processos/detalhes/ComunicacoesTab';
import { processoService } from '../../services/processoService';
import { mensagemErro } from '../../services/api';
import { useDadosProcesso } from './useDadosProcesso';
import './Processos.css';
import './ProcessoDetalhes.css';

function NaoEncontrado() {
  return <ProcessoFeedback titulo="Processo não encontrado" mensagem="Este processo não existe ou não está disponível para sua conta."><Link className="button button--secondary" to="/processos">Voltar aos processos</Link></ProcessoFeedback>;
}

function Detalhes({ id }: { id: number }) {
  const { dados: processo, carregando, erro, status, tentarNovamente } = useDadosProcesso(id, processoService.buscar);
  const [aba, setAba] = useState<ProcessoAba>('geral');
  const [analisando, setAnalisando] = useState(false);
  const [aviso, setAviso] = useState({ texto: '', erro: false });
  const [versao, setVersao] = useState(0);
  const emCurso = useRef(false);
  const montado = useRef(true);
  useEffect(() => { montado.current = true; return () => { montado.current = false; }; }, []);
  async function solicitar() {
    if (emCurso.current) return;
    emCurso.current = true; setAnalisando(true); setAviso({ texto: '', erro: false });
    try {
      await processoService.analisar(id);
      if (montado.current) { setVersao(valor => valor + 1); setAviso({ texto: 'Análise concluída. Os resultados salvos deste processo foram atualizados.', erro: false }); }
    } catch (error) {
      if (montado.current) setAviso({ texto: mensagemErro(error), erro: true });
    } finally { emCurso.current = false; if (montado.current) setAnalisando(false); }
  }
  if (carregando) return <ProcessoLoading mensagem="Carregando detalhes do processo…" />;
  if (status === 404) return <NaoEncontrado />;
  if (erro) return <ProcessoFeedback titulo="Não foi possível carregar o processo" mensagem={erro} retry={tentarNovamente} />;
  if (!processo) return <NaoEncontrado />;
  return <><ProcessHeader processo={processo} />
    <ProcessTabs ativa={aba} selecionar={setAba} />
    {aviso.texto && <div className={aviso.erro ? 'notice notice--error process-notice' : 'process-success'} role={aviso.erro ? 'alert' : 'status'}>{aviso.texto}</div>}
    {analisando && <p className="process-analysis-notice" role="status">Análise em andamento. Aguarde a conclusão para consultar os novos resultados.</p>}
    <div role="tabpanel" id={`panel-${aba}`} aria-labelledby={`tab-${aba}`} tabIndex={0}>
      {aba === 'geral' && <InformacoesGeraisTab processo={processo} analisando={analisando} solicitar={solicitar} />}
      {aba === 'datajud' && <DataJudTab processoId={id} versao={versao} analisando={analisando} solicitar={solicitar} />}
      {aba === 'comunicacoes' && <ComunicacoesTab key={`${id}-${versao}`} processoId={id} versao={versao} />}
    </div>
  </>;
}

export function ProcessoDetalhes() {
  const { id } = useParams();
  const numeroId = Number(id);
  return <div className="process-module process-details">{id && /^\d+$/.test(id) && Number.isSafeInteger(numeroId) && numeroId > 0 ? <Detalhes key={numeroId} id={numeroId} /> : <NaoEncontrado />}</div>;
}
