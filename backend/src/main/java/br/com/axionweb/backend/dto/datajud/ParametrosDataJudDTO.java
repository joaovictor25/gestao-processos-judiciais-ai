package br.com.axionweb.backend.dto.datajud;

public class ParametrosDataJudDTO {
        private String tribunal;

        private Integer codigoClasse;

        private List<Integer> codigosAssuntos;

        private String grau;

        private Integer codigoOrgaoJulgador;

        private LocalDate dataAjuizamentoInicio;

        private LocalDate dataAjuizamentoFim;

        private Integer quantidadeResultados;
}
