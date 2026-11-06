import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

export function ProcessoPageHeader({ titulo, descricao, acao, breadcrumb }: {
  titulo: string; descricao: string; acao?: ReactNode; breadcrumb?: string;
}) {
  return <header className="process-page-heading">
    <div>{breadcrumb && <nav className="process-breadcrumb" aria-label="Localização"><Link to="/processos">Processos</Link><span aria-hidden="true">/</span><span aria-current="page">{breadcrumb}</span></nav>}
      <h1>{titulo}</h1><p>{descricao}</p>
    </div>{acao}
  </header>;
}
