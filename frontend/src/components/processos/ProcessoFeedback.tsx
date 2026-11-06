import { Button } from '../ui/Button';
import { Icon } from '../ui/Icon';
import type { ReactNode } from 'react';

export function ProcessoLoading({ mensagem = 'Carregando processos…' }: { mensagem?: string }) {
  return <div className="process-state" role="status"><span className="spinner" /><p>{mensagem}</p></div>;
}

export function ProcessoFeedback({ titulo, mensagem, retry, children }: {
  titulo: string; mensagem: string; retry?: () => void; children?: ReactNode;
}) {
  return <div className="process-state"><Icon name={retry ? 'alert' : 'folder'} /><h2>{titulo}</h2><p>{mensagem}</p>
    {retry && <Button variant="secondary" onClick={retry}>Tentar novamente</Button>}{children}
  </div>;
}
