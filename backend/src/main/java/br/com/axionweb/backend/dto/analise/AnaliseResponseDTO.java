package br.com.axionweb.backend.dto.analise;

import br.com.axionweb.backend.dto.comunicacao.ComunicacaoSalvaDTO;
import br.com.axionweb.backend.dto.datajud.DataJudResponseDTO;

import java.util.List;

public record AnaliseResponseDTO(Long processoId, List<DataJudResponseDTO> estatisticas,
                                List<ComunicacaoSalvaDTO> comunicacoes) {
}
