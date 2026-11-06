package br.com.axionweb.backend.dto.datajud;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SinteseDataJudRequestDTO(@NotNull @Positive Long estatisticaId) {}
