package br.com.axionweb.backend.dto.datajud;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SolicitacaoEstatisticaDTO(String id, Long processoId, String status, int tentativas,
        Instant proximaTentativa, String codigoErro, LocalDate dataInicio, LocalDate dataFim,
        List<DataJudResponseDTO> resultados) { }
