import type { Comunicacao } from '../../../types/comunicacao';
import { Icon } from '../../ui/Icon';
import { data, textoComunicacao } from './formatacao';

export function ComunicacoesTable({ comunicacoes, selecionada, abrir }: {
  comunicacoes: Comunicacao[]; selecionada?: number; abrir: (item: Comunicacao, origem: HTMLButtonElement) => void;
}) {
  return <div className="communications-table-scroll"><table className="communications-table"><caption className="sr-only">Comunicações salvas para este processo</caption>
    <thead><tr>{['Data', 'Tipo', 'Meio', 'Documento', 'Status', 'Texto', 'Ações'].map(titulo => <th key={titulo} scope="col">{titulo}</th>)}</tr></thead>
    <tbody>{comunicacoes.map(item => <tr key={item.id} className={selecionada === item.id ? 'communication-selected' : undefined}>
      <td>{data(item.dataDisponibilizacao || item.dataDisponibilizacaoFormatada)}</td><td><span className="process-badge process-badge--blue">{item.tipoComunicacao || 'Não informado'}</span></td>
      <td>{item.meio || '—'}</td><td>{item.tipoDocumento || '—'}</td><td>{item.status || '—'}</td>
      <td><span className="communication-excerpt">{textoComunicacao(item.texto) || 'Texto não informado'}</span></td>
      <td><button type="button" className="button button--secondary communication-view" aria-label={`Ver comunicação de ${data(item.dataDisponibilizacao || item.dataDisponibilizacaoFormatada)}`} aria-haspopup="dialog" aria-controls={selecionada === item.id ? 'communication-details' : undefined} onClick={event => abrir(item, event.currentTarget)}><Icon name="eye" />Ver</button></td>
    </tr>)}</tbody>
  </table></div>;
}
