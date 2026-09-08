package br.com.axionweb.backend.dto.datajud;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DadosDataJudDTO {

    private String numeroProcesso;

    private String tribunal;

    private String grau;

    private String dataAjuizamento;

    private String dataHoraUltimaAtualizacao;


    private ClasseDataJudDTO classe;

    private List<AssuntoDataJudDTO> assuntos;

    private OrgaoJulgadorDataJudDTO orgaoJulgador;

    private List<MovimentoDataJudDTO> movimentos;

    private FormatoDataJudDTO formato;

    private SistemaDataJudDTO sistema;

}