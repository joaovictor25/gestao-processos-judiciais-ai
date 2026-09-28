import { Link } from 'react-router-dom';
import type { Processo } from '../../../types/processo';
import { ProcessStatusBadge } from '../ProcessStatusBadge';
import { Icon } from '../../ui/Icon';
import { posicao } from './formatacao';

export function ProcessHeader({ processo }: { processo: Processo }) {
  return <header className="process-details-header">
    <nav className="process-breadcrumb" aria-label="Localização"><Link to="/processos">Processos</Link><Icon name="chevron" width="12" /><span aria-current="page">Detalhes do processo</span></nav>
    <div className="details-header-row"><div className="details-identity"><div><h1>{processo.numeroProcesso || 'Número não informado'}</h1><ProcessStatusBadge status={processo.status} /></div>
      <p>{processo.clienteVinculado || 'Cliente não informado'}<span>·</span>{posicao(processo.posicaoCliente)}<span>·</span>{processo.tribunal || 'Tribunal não informado'}</p>
    </div><div className="details-header-actions"><Link className="button button--secondary" to={`/processos/${processo.id}/editar`}><Icon name="edit" />Editar processo</Link></div></div>
  </header>;
}
