import { useEffect, useRef } from 'react';
import type { Comunicacao } from '../../../types/comunicacao';
import { Button } from '../../ui/Button';
import { Icon } from '../../ui/Icon';
import { data, textoComunicacao } from './formatacao';

export function ComunicacaoDetailsModal({ comunicacao, fechar, textoNormalizado = false }: { comunicacao: Comunicacao; fechar: () => void; textoNormalizado?: boolean }) {
  const dialog = useRef<HTMLDialogElement>(null);
  const titulo = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    const modal = dialog.current;
    const overflowAnterior = document.body.style.overflow;
    modal?.showModal();
    document.body.style.overflow = 'hidden';
    titulo.current?.focus({ preventScroll: true });
    return () => { modal?.close(); document.body.style.overflow = overflowAnterior; };
  }, []);
  function fecharModal() { dialog.current?.close(); }
  const campos = [
    ['Data de disponibilização', data(comunicacao.dataDisponibilizacao || comunicacao.dataDisponibilizacaoFormatada)],
    ['Tribunal', comunicacao.siglaTribunal], ['Órgão', comunicacao.idOrgao != null ? `Código ${comunicacao.idOrgao}` : null],
    ['Código da classe', comunicacao.codigoClasse], ['Tipo de comunicação', comunicacao.tipoComunicacao],
    ['Tipo de documento', comunicacao.tipoDocumento], ['Meio', comunicacao.meioCompleto || comunicacao.meio], ['Status', comunicacao.status],
  ];
  return <dialog ref={dialog} id="communication-details" className="detail-card communication-modal" aria-labelledby="communication-details-title"
    onClose={() => { if (dialog.current?.isConnected && !dialog.current.open) fechar(); }}
    onClick={event => {
      if (event.target !== event.currentTarget) return;
      const rect = event.currentTarget.getBoundingClientRect();
      if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) fecharModal();
    }}>
    <div className="communication-modal-heading"><h2 id="communication-details-title" ref={titulo} tabIndex={-1}>Detalhes da comunicação</h2><button type="button" className="icon-button" aria-label="Fechar detalhes da comunicação" onClick={fecharModal}><Icon name="close" /></button></div>
    <dl>{campos.map(([label, valor]) => <div key={label}><dt>{label}</dt><dd>{valor || 'Não informado'}</dd></div>)}</dl>
    <h3 id="communication-text-title">Texto da comunicação</h3><div className="communication-full-text" tabIndex={0} role="region" aria-labelledby="communication-text-title">{(textoNormalizado ? comunicacao.texto : textoComunicacao(comunicacao.texto)) || 'Texto não informado'}</div>
    <Button variant="secondary" onClick={fecharModal}>Fechar</Button>
  </dialog>;
}
