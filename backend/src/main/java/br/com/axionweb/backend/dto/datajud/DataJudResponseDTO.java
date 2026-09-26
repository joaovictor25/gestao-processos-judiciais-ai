package br.com.axionweb.backend.dto.datajud;

import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.model.datajud.TempoEstatistica;

public record DataJudResponseDTO(Long id, Long processoId, Integer quantidadeProcessos,
                                 Integer processosBaixados, Integer processosPendentes,
                                 Integer processosComJulgamento, TempoEstatisticaDTO tempoAtePrimeiraBaixa,
                                 TempoEstatisticaDTO tempoPendente, TempoEstatisticaDTO tempoAtePrimeiroJulgamento) {

    public static DataJudResponseDTO de(DataJud dados) {
        return new DataJudResponseDTO(dados.getId(), dados.getProcesso().getId(), dados.getQuantidadeProcessos(),
                dados.getProcessosBaixados(), dados.getProcessosPendentes(), dados.getProcessosComJulgamento(),
                converter(dados.getTempoAtePrimeiraBaixa()), converter(dados.getTempoPendente()),
                converter(dados.getTempoAtePrimeiroJulgamento()));
    }

    private static TempoEstatisticaDTO converter(TempoEstatistica tempo) {
        if (tempo == null) {
            return null;
        }
        TempoEstatisticaDTO dto = new TempoEstatisticaDTO();
        dto.setQuantidade(tempo.getQuantidade());
        dto.setMediaDias(tempo.getMediaDias());
        dto.setMedianaDias(tempo.getMedianaDias());
        dto.setMinimoDias(tempo.getMinimoDias());
        dto.setMaximoDias(tempo.getMaximoDias());
        dto.setAmplitudeDias(tempo.getAmplitudeDias());
        dto.setDesvioPadraoDias(tempo.getDesvioPadraoDias());
        dto.setPercentil25Dias(tempo.getPercentil25Dias());
        dto.setPercentil50Dias(tempo.getPercentil50Dias());
        dto.setPercentil75Dias(tempo.getPercentil75Dias());
        return dto;
    }
}
