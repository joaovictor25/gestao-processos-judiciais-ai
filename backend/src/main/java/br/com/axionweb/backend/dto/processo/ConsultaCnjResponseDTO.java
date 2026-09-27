package br.com.axionweb.backend.dto.processo;

import java.time.LocalDate;
import java.util.List;

public record ConsultaCnjResponseDTO(String numeroProcesso, String tribunalConsultado,
                                    List<Registro> registros, boolean limiteAtingido) {
    public record Classificacao(String nome, String codigo) {}
    public record Registro(String tribunal, String grau, Classificacao orgaoJulgador,
                           Classificacao classeProcessual, List<Classificacao> assuntos, LocalDate dataInicio) {}
}
