export type ProcessoAba = 'geral' | 'datajud' | 'comunicacoes';
const abas: { id: ProcessoAba; nome: string }[] = [{ id: 'geral', nome: 'Informações gerais' }, { id: 'datajud', nome: 'DataJud' }, { id: 'comunicacoes', nome: 'Comunicações' }];

export function ProcessTabs({ ativa, selecionar }: { ativa: ProcessoAba; selecionar: (aba: ProcessoAba) => void }) {
  return <div className="process-tabs" role="tablist" aria-label="Informações do processo">{abas.map((aba, index) => <button
    key={aba.id} id={`tab-${aba.id}`} type="button" role="tab" aria-selected={ativa === aba.id} aria-controls={`panel-${aba.id}`}
    tabIndex={ativa === aba.id ? 0 : -1} onClick={() => selecionar(aba.id)} onKeyDown={event => {
      const next = event.key === 'ArrowRight' ? (index + 1) % abas.length : event.key === 'ArrowLeft' ? (index + abas.length - 1) % abas.length : event.key === 'Home' ? 0 : event.key === 'End' ? abas.length - 1 : null;
      if (next === null) return;
      event.preventDefault(); selecionar(abas[next].id); document.getElementById(`tab-${abas[next].id}`)?.focus();
    }}>{aba.nome}</button>)}</div>;
}
