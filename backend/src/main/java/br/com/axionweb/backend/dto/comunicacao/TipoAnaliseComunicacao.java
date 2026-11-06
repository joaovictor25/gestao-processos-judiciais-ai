package br.com.axionweb.backend.dto.comunicacao;

public enum TipoAnaliseComunicacao {
    SINTESE_GERAL("Síntese geral", "Sintetize o conjunto, sem resumir cada comunicação individualmente."),
    FUNDAMENTOS_DECISOES("Fundamentos e decisões", "Associe fundamentos e decisões somente quando a relação estiver expressa nos textos."),
    VALORES("Valores", "Identifique apenas valores monetários expressos e o contexto documentado, sem estimar valores futuros.");

    private final String titulo;
    private final String instrucao;
    TipoAnaliseComunicacao(String titulo, String instrucao) { this.titulo = titulo; this.instrucao = instrucao; }
    public String getTitulo() { return titulo; }
    public String getInstrucao() { return instrucao; }
}
