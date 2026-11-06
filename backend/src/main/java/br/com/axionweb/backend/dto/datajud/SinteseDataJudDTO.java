package br.com.axionweb.backend.dto.datajud;

import br.com.axionweb.backend.model.datajud.DataJud;
import java.time.Instant;

public record SinteseDataJudDTO(Long estatisticaId, String sintese, Instant geradaEm) {
    public static SinteseDataJudDTO de(DataJud dados) {
        return new SinteseDataJudDTO(dados.getId(), dados.getSintese(), dados.getSinteseGeradaEm());
    }
}
