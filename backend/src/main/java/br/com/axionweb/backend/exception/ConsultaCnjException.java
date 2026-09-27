package br.com.axionweb.backend.exception;

import org.springframework.http.HttpStatus;

public class ConsultaCnjException extends RuntimeException {
    public enum Motivo {
        CNJ_INVALIDO(HttpStatus.BAD_REQUEST, "Informe um número CNJ completo com dígitos verificadores válidos."),
        CNJ_TRIBUNAL_NAO_SUPORTADO(HttpStatus.BAD_REQUEST, "O tribunal de origem desse CNJ não está disponível para esta consulta DataJud."),
        CNJ_NAO_ENCONTRADO(HttpStatus.NOT_FOUND, "Nenhum registro público desse número foi encontrado no DataJud do tribunal de origem. Você pode continuar o cadastro manualmente."),
        CNJ_CONSULTA_LIMITADA(HttpStatus.TOO_MANY_REQUESTS, "O DataJud limitou as consultas. Aguarde antes de tentar novamente ou continue o cadastro manualmente."),
        CNJ_CONSULTA_INDISPONIVEL(HttpStatus.BAD_GATEWAY, "Não foi possível consultar o DataJud. Tente novamente mais tarde ou continue o cadastro manualmente.");

        private final HttpStatus status;
        private final String mensagem;
        Motivo(HttpStatus status, String mensagem) { this.status = status; this.mensagem = mensagem; }
    }

    private final Motivo motivo;
    public ConsultaCnjException(Motivo motivo) { super(motivo.mensagem); this.motivo = motivo; }
    public HttpStatus getStatus() { return motivo.status; }
    public String getCodigo() { return motivo.name(); }
}
