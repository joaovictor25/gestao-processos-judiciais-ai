package br.com.axionweb.backend.dto.datajud;

import java.util.List;

public class MovimentoDataJudDTO {

    private Integer codigo;

    private String dataHora;

    private String nome;

    private List<ComplementoTabeladoDTO> complementosTabelados;

    private OrgaoJulgadorDataJudDTO orgaoJulgador;


    public Integer getCodigo() {
        return codigo;
    }

    public void setCodigo(Integer codigo) {
        this.codigo = codigo;
    }

    public String getDataHora() {
        return dataHora;
    }

    public void setDataHora(String dataHora) {
        this.dataHora = dataHora;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public List<ComplementoTabeladoDTO> getComplementosTabelados() {
        return complementosTabelados;
    }

    public void setComplementosTabelados(List<ComplementoTabeladoDTO> complementosTabelados) {
        this.complementosTabelados = complementosTabelados;
    }

    public OrgaoJulgadorDataJudDTO getOrgaoJulgador() {
        return orgaoJulgador;
    }

    public void setOrgaoJulgador(OrgaoJulgadorDataJudDTO orgaoJulgador) {
        this.orgaoJulgador = orgaoJulgador;
    }
}