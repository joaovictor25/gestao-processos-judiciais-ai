package br.com.axionweb.backend.dto.comunicacao;

public class DadosComunicacaoDTO {

    private String codigoClasse;

    private String data_disponibilizacao;

    private String datadisponibilizacao;

    private Integer idOrgao;

    private String meio;

    private String meiocompleto;

    private String siglaTribunal;

    private String status;

    private String texto;

    private String tipoComunicacao;

    private String tipoDocumento;

    public String getCodigoClasse() {
        return codigoClasse;
    }

    public void setCodigoClasse(String codigoClasse) {
        this.codigoClasse = codigoClasse;
    }

    public String getData_disponibilizacao() {
        return data_disponibilizacao;
    }

    public void setData_disponibilizacao(String data_disponibilizacao) {
        this.data_disponibilizacao = data_disponibilizacao;
    }

    public String getDatadisponibilizacao() {
        return datadisponibilizacao;
    }

    public void setDatadisponibilizacao(String datadisponibilizacao) {
        this.datadisponibilizacao = datadisponibilizacao;
    }

    public Integer getIdOrgao() {
        return idOrgao;
    }

    public void setIdOrgao(Integer idOrgao) {
        this.idOrgao = idOrgao;
    }

    public String getMeio() {
        return meio;
    }

    public void setMeio(String meio) {
        this.meio = meio;
    }

    public String getMeiocompleto() {
        return meiocompleto;
    }

    public void setMeiocompleto(String meiocompleto) {
        this.meiocompleto = meiocompleto;
    }

    public String getSiglaTribunal() {
        return siglaTribunal;
    }

    public void setSiglaTribunal(String siglaTribunal) {
        this.siglaTribunal = siglaTribunal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getTipoComunicacao() {
        return tipoComunicacao;
    }

    public void setTipoComunicacao(String tipoComunicacao) {
        this.tipoComunicacao = tipoComunicacao;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }
}
