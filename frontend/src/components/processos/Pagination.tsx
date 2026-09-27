import { Icon } from '../ui/Icon';

export function Pagination({ total, pagina, porPagina, mudarPagina, mudarQuantidade, itens = 'processos', quantidades = [10, 20, 50] }: {
  total: number; pagina: number; porPagina: number; mudarPagina: (pagina: number) => void; mudarQuantidade: (quantidade: number) => void;
  itens?: string; quantidades?: readonly number[];
}) {
  const paginas = Math.max(1, Math.ceil(total / porPagina));
  const primeiro = Math.max(1, Math.min(pagina - 2, paginas - 4));
  const numeros = Array.from({ length: Math.min(5, paginas) }, (_, index) => primeiro + index);
  return <footer className="process-pagination">
    <span className="pagination-count" aria-live="polite">{total ? (pagina - 1) * porPagina + 1 : 0}–{Math.min(pagina * porPagina, total)} de {total} {itens}</span>
    <nav aria-label={`Paginação de ${itens}`} className="pagination-pages">
      <button type="button" disabled={pagina <= 1} onClick={() => mudarPagina(pagina - 1)}><Icon className="pagination-prev" name="chevron" /><span>Anterior</span></button>
      {numeros.map(numero => <button type="button" key={numero} className={pagina === numero ? 'selected' : ''} aria-current={pagina === numero ? 'page' : undefined} aria-label={`Página ${numero}`} onClick={() => mudarPagina(numero)}>{numero}</button>)}
      <button type="button" disabled={pagina >= paginas} onClick={() => mudarPagina(pagina + 1)}><span>Próximo</span><Icon name="chevron" /></button>
    </nav>
    <div className="pagination-size" role="group" aria-label={`${itens} por página`}>{quantidades.map(quantidade => <button type="button" key={quantidade} aria-pressed={porPagina === quantidade} onClick={() => mudarQuantidade(quantidade)}>{quantidade}</button>)}<span>por página</span></div>
  </footer>;
}
