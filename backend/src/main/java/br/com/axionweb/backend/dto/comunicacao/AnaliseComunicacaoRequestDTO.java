package br.com.axionweb.backend.dto.comunicacao;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record AnaliseComunicacaoRequestDTO(
        @NotEmpty List<@NotNull TipoAnaliseComunicacao> tiposAnalise,
        @NotEmpty List<@NotNull @Positive Long> comunicacaoIds) {}
