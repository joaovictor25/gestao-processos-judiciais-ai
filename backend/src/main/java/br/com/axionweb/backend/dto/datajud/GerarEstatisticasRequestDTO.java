package br.com.axionweb.backend.dto.datajud;

import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;

public record GerarEstatisticasRequestDTO(LocalDate dataInicio, LocalDate dataFim) {
    @AssertTrue(message = "A data final não pode ser anterior à data inicial.")
    public boolean isPeriodoValido() {
        return dataInicio == null || dataFim == null || !dataFim.isBefore(dataInicio);
    }
}
