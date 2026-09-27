package br.com.axionweb.backend.exception;

import org.springframework.web.client.RestClientException;

public class ConsultaExternaException extends RuntimeException {
    public enum Servico { DATAJUD, DJEN }

    private final Servico servico;

    public ConsultaExternaException(Servico servico, RestClientException causa) {
        super("Falha ao consultar " + servico, causa);
        this.servico = servico;
    }

    public Servico getServico() { return servico; }
}
