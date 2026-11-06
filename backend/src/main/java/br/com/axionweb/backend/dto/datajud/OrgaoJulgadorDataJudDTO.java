package br.com.axionweb.backend.dto.datajud;

public class OrgaoJulgadorDataJudDTO {

    private String codigo;
    private String nome;
    private Integer codigoMunicipioIBGE;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getCodigoMunicipioIBGE() {
        return codigoMunicipioIBGE;
    }

    public void setCodigoMunicipioIBGE(Integer codigoMunicipioIBGE) {
        this.codigoMunicipioIBGE = codigoMunicipioIBGE;
    }
}