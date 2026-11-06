package br.com.axionweb.backend.dto.comunicacao;

import jakarta.validation.constraints.*;
import java.util.List;

public record SinteseComunicacaoRequestDTO(@Positive Long pesquisaId,
        @NotEmpty List<@NotNull @Positive Long> comunicacaoIds) {}
