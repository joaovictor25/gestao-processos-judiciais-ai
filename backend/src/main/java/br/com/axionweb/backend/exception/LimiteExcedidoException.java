package br.com.axionweb.backend.exception;

import org.springframework.http.HttpStatus;

import java.time.Duration;

/** Limite interno de uso atingido; a resposta HTTP 429 informa Retry-After em segundos. */
public class LimiteExcedidoException extends RuntimeException {
    public enum Motivo {
        LOGIN_LIMITADO("Muitas tentativas de login. Aguarde antes de tentar novamente."),
        CADASTRO_LIMITADO("Muitas tentativas de cadastro. Aguarde antes de tentar novamente."),
        LIMITE_IA_USUARIO("Você atingiu o limite de solicitações de IA. Aguarde antes de tentar novamente."),
        LIMITE_IA_GLOBAL("O limite de solicitações de IA do sistema foi atingido. Aguarde antes de tentar novamente."),
        IA_OCUPADA("A IA está processando outras solicitações. Aguarde alguns segundos e tente novamente.");
        private final String mensagem;
        Motivo(String mensagem) { this.mensagem = mensagem; }
    }

    private final Motivo motivo;
    private final Duration tentarApos;

    public LimiteExcedidoException(Motivo motivo, Duration tentarApos) {
        super(motivo.mensagem);
        this.motivo = motivo;
        this.tentarApos = tentarApos;
    }

    public HttpStatus getStatus() { return HttpStatus.TOO_MANY_REQUESTS; }
    public String getCodigo() { return motivo.name(); }
    public long getRetryAfterSegundos() { return Math.max(1, (tentarApos.toMillis() + 999) / 1000); }
}
