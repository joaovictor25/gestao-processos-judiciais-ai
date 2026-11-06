package br.com.axionweb.backend.dto.datajud;

import br.com.axionweb.backend.model.datajud.DataJud;
import java.time.Instant;
import java.util.List;

public record HistoricoSintesesDTO(List<Item> itens, int pagina, boolean temProxima) {
    public record Item(Long estatisticaId, Instant geradaEm, ConsultaEstatisticaDTO parametros) {
        public static Item de(DataJud dados) {
            return new Item(dados.getId(), dados.getSinteseGeradaEm(), ConsultaEstatisticaDTO.de(dados));
        }
    }
}
