package br.com.axionweb.backend.dto.comunicacao;

import java.util.List;

public record AnaliseComunicacaoDTO(int comunicacoesConsideradas, int duplicadasIgnoradas,
                                    List<Categoria> categorias) {
    public record Categoria(TipoAnaliseComunicacao tipo, List<Item> itens, String limitacao) {}
    public record Item(String titulo, String descricao, String contexto, String decisaoRelacionada,
                       List<Evidencia> evidencias) {}
    public record Evidencia(int comunicacao, String trecho) {}
}
