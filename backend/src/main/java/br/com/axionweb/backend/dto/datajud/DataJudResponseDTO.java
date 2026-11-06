package br.com.axionweb.backend.dto.datajud;

import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.model.datajud.TempoEstatistica;

public record DataJudResponseDTO(Long id, Long processoId, Integer quantidadeProcessos,
                                 Integer processosPendentes, TempoEstatisticaDTO tempoAtePrimeiraBaixa,
                                 TempoEstatisticaDTO tempoAtePrimeiroJulgamento,
                                 java.time.Instant executadaEm, ConsultaEstatisticaDTO parametros,
                                 String statusSintese, SinteseDataJudDTO sintese) {

    public static DataJudResponseDTO de(DataJud dados) {
        return new DataJudResponseDTO(dados.getId(), dados.getProcesso().getId(), dados.getQuantidadeProcessos(),
                dados.getProcessosPendentes(), converter(dados.getTempoAtePrimeiraBaixa()),
                converter(dados.getTempoAtePrimeiroJulgamento()), dados.getExecutadaEm(),
                ConsultaEstatisticaDTO.de(dados), dados.getStatusSintese(),
                dados.getSintese() == null ? null : SinteseDataJudDTO.de(dados));
    }

    private static TempoEstatisticaDTO converter(TempoEstatistica tempo) {
        if (tempo == null) {
            return null;
        }
        TempoEstatisticaDTO dto = new TempoEstatisticaDTO();
        dto.setQuantidade(tempo.getQuantidade());
        // Registros antigos sem subamostra válida não representam prazo zero.
        dto.setMedianaDias(tempo.getQuantidade() == null || tempo.getQuantidade() <= 0
                ? null : tempo.getMedianaDias());
        return dto;
    }
}
