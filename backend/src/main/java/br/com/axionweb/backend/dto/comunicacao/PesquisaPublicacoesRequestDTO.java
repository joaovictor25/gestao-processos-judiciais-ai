package br.com.axionweb.backend.dto.comunicacao;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record PesquisaPublicacoesRequestDTO(
        @NotBlank(message = "Informe o texto da busca.") String texto,
        LocalDate dataDisponibilizacaoInicio,
        LocalDate dataDisponibilizacaoFim) {
    @AssertTrue(message = "A data final não pode ser anterior à data inicial.")
    public boolean isPeriodoValido() {
        return dataDisponibilizacaoInicio == null || dataDisponibilizacaoFim == null
                || !dataDisponibilizacaoFim.isBefore(dataDisponibilizacaoInicio);
    }
}
