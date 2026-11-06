package br.com.axionweb.backend.exception;

public class EstatisticasSemProcessosException extends RuntimeException {
    public EstatisticasSemProcessosException() {
        super("Nenhum processo foi encontrado no DataJud com os filtros informados. Revise os dados do processo ou amplie o período. As estatísticas anteriores foram preservadas.");
    }
}
