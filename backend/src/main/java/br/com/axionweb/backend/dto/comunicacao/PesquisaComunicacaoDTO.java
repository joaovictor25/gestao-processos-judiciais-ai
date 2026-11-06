package br.com.axionweb.backend.dto.comunicacao;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PesquisaComunicacaoDTO(Long id, String textoBusca, String tribunal, LocalDate dataInicio,
        LocalDate dataFim, Instant pesquisadaEm, int quantidade, List<Long> comunicacaoIds,
        String sintese, Instant geradaEm, Integer comunicacoesConsideradas, Integer duplicadasIgnoradas,
        List<AnaliseComunicacaoDTO.Evidencia> evidencias) {
    public record Historico(List<PesquisaComunicacaoDTO> itens, int pagina, boolean temProxima) {}
    public record Detalhe(PesquisaComunicacaoDTO pesquisa, List<ComunicacaoSalvaDTO> publicacoes) {}
}
