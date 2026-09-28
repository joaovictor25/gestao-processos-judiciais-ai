package br.com.axionweb.backend.exception;

import org.springframework.http.HttpStatus;

public class AnaliseComunicacaoException extends RuntimeException {
    public enum Motivo {
        PESQUISA_DESATUALIZADA(HttpStatus.CONFLICT, "As publicações foram atualizadas. Pesquise novamente antes de analisar."),
        SEM_TEXTOS(HttpStatus.UNPROCESSABLE_ENTITY, "Nenhuma comunicação com texto foi encontrada para os filtros informados."),
        EXCESSO_DE_TEXTO(HttpStatus.UNPROCESSABLE_ENTITY, "Os textos encontrados excedem o limite desta análise. Refine o texto ou o período da busca."),
        CONFIGURACAO_AUSENTE(HttpStatus.SERVICE_UNAVAILABLE, "Configure GEMINI_API_KEY no backend para habilitar a análise das comunicações."),
        LIMITE_EXTERNO(HttpStatus.TOO_MANY_REQUESTS, "O serviço de IA limitou as solicitações. Aguarde antes de tentar novamente."),
        RESPOSTA_INCOMPLETA(HttpStatus.BAD_GATEWAY, "A IA interrompeu a resposta antes de concluir a análise. Selecione menos categorias e tente novamente."),
        RESPOSTA_INVALIDA(HttpStatus.BAD_GATEWAY, "A análise da IA não apresentou evidências verificáveis nas comunicações. Tente novamente."),
        SERVICO_INDISPONIVEL(HttpStatus.BAD_GATEWAY, "Não foi possível consultar o serviço de IA. Tente novamente mais tarde.");
        private final HttpStatus status;
        private final String mensagem;
        Motivo(HttpStatus status, String mensagem) { this.status = status; this.mensagem = mensagem; }
    }
    private final Motivo motivo;
    public AnaliseComunicacaoException(Motivo motivo) { super(motivo.mensagem); this.motivo = motivo; }
    public HttpStatus getStatus() { return motivo.status; }
    public String getCodigo() { return motivo.name(); }
}
