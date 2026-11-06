package br.com.axionweb.backend.exception;

import org.springframework.web.client.RestClientException;

public class DataJudAguardarException extends RestClientException {
    private final long tentarEm;
    private final boolean consultou;
    private final Integer statusExterno;

    public DataJudAguardarException(long tentarEm, boolean consultou, Integer statusExterno, Throwable causa) {
        super("DataJud temporariamente indisponível; aguarde a próxima tentativa", causa);
        this.tentarEm = tentarEm;
        this.consultou = consultou;
        this.statusExterno = statusExterno;
    }
    public long getTentarEm() { return tentarEm; }
    public boolean isConsultou() { return consultou; }
    public Integer getStatusExterno() { return statusExterno; }
}
