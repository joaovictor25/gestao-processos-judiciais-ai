package br.com.axionweb.backend.dto.datajud;

import br.com.axionweb.backend.model.datajud.DataJud;
import java.time.LocalDate;

public record ConsultaEstatisticaDTO(String tribunal, String classeCodigo, String grau,
                                    Integer limiteResultados, LocalDate dataInicio, LocalDate dataFim) {
    public static ConsultaEstatisticaDTO de(DataJud dados) {
        // Não inventa filtros de execuções legadas que não os registravam.
        if (dados.getExecutadaEm() == null) return null;
        return new ConsultaEstatisticaDTO(dados.getTribunalConsulta(), dados.getClasseConsulta(),
                dados.getGrauConsulta(), dados.getLimiteConsulta(), dados.getDataInicio(), dados.getDataFim());
    }
}
