import { useEffect, useRef, useState } from 'react';
import type { ProcessoResumo } from '../../types/processo';
import { mensagemErro } from '../../services/api';
import { Button } from '../ui/Button';
import { Icon } from '../ui/Icon';

export function ExcluirProcessoModal({ processo, excluir, fechar }: {
  processo: ProcessoResumo;
  excluir: () => Promise<void>;
  fechar: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const cancelButton = useRef<HTMLButtonElement>(null);
  const emAndamento = useRef(false);
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState('');

  useEffect(() => {
    const modal = dialog.current;
    const anterior = document.activeElement;
    const overflowAnterior = document.body.style.overflow;
    modal?.showModal();
    document.body.style.overflow = 'hidden';
    cancelButton.current?.focus();
    return () => {
      modal?.close();
      document.body.style.overflow = overflowAnterior;
      if (anterior instanceof HTMLElement && anterior.isConnected) anterior.focus();
      else document.getElementById('buscar-processos')?.focus();
    };
  }, []);

  async function confirmar() {
    if (emAndamento.current) return;
    emAndamento.current = true;
    setLoading(true);
    setErro('');
    try { await excluir(); }
    catch (error) { setErro(mensagemErro(error)); }
    finally { emAndamento.current = false; setLoading(false); }
  }

  return <dialog ref={dialog} className="process-delete-modal" aria-labelledby="delete-process-title" aria-describedby="delete-process-description" aria-busy={loading}
    onCancel={event => { event.preventDefault(); if (!emAndamento.current) fechar(); }}>
    <h2 id="delete-process-title">Excluir processo?</h2>
    <p id="delete-process-description">O processo <strong>{processo.numeroProcesso || processo.id}</strong> e seus dados vinculados serão excluídos. Esta ação não pode ser desfeita.</p>
    {erro && <p className="notice notice--error" role="alert">{erro}</p>}
    <div className="process-delete-actions">
      <button ref={cancelButton} type="button" className="button button--secondary" disabled={loading} onClick={fechar}>Cancelar</button>
      <Button variant="secondary" className="process-delete-button" loading={loading} onClick={() => void confirmar()}>
        {!loading && <Icon name="trash" />}{loading ? 'Excluindo…' : 'Excluir processo'}
      </Button>
    </div>
  </dialog>;
}
