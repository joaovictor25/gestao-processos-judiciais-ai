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


    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public String getTribunal() {
        return tribunal;
    }

    public void setTribunal(String tribunal) {
        this.tribunal = tribunal;
    }

    public String getGrau() {
        return grau;
    }

    public void setGrau(String grau) {
        this.grau = grau;
    }

    public String getDataAjuizamento() {
        return dataAjuizamento;
    }

    public void setDataAjuizamento(String dataAjuizamento) {
        this.dataAjuizamento = dataAjuizamento;
    }

    public String getDataHoraUltimaAtualizacao() {
        return dataHoraUltimaAtualizacao;
    }

    public void setDataHoraUltimaAtualizacao(String dataHoraUltimaAtualizacao) {
        this.dataHoraUltimaAtualizacao = dataHoraUltimaAtualizacao;
    }

    public ClasseDataJudDTO getClasse() {
        return classe;
    }

    public void setClasse(ClasseDataJudDTO classe) {
        this.classe = classe;
    }

    public List<AssuntoDataJudDTO> getAssuntos() {
        return assuntos;
    }

    public void setAssuntos(List<AssuntoDataJudDTO> assuntos) {
        this.assuntos = assuntos;
    }

    public OrgaoJulgadorDataJudDTO getOrgaoJulgador() {
        return orgaoJulgador;
    }

    public void setOrgaoJulgador(OrgaoJulgadorDataJudDTO orgaoJulgador) {
        this.orgaoJulgador = orgaoJulgador;
    }

    public List<MovimentoDataJudDTO> getMovimentos() {
        return movimentos;
    }

    public void setMovimentos(List<MovimentoDataJudDTO> movimentos) {
        this.movimentos = movimentos;
    }

    public FormatoDataJudDTO getFormato() {
        return formato;
    }

    public void setFormato(FormatoDataJudDTO formato) {
        this.formato = formato;
    }

    public SistemaDataJudDTO getSistema() {
        return sistema;
    }

    public void setSistema(SistemaDataJudDTO sistema) {
        this.sistema = sistema;
    }
}