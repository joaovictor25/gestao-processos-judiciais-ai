package br.com.axionweb.backend.dto.comunicacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

//Refazer para praticar o DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComunicacaoDTO {

    private Long id;

    @JsonProperty("data_disponibilizacao")
    private String dataDisponibilizacao;

    private String siglaTribunal;

    private String tipoComunicacao;

    private String nomeOrgao;

    private Integer idOrgao;

    private String texto;

    @JsonProperty("numero_processo")
    private String numeroProcesso;

    private String meio;

    private String link;

    private String tipoDocumento;

    private String nomeClasse;

    private String codigoClasse;

    private Integer numeroComunicacao;

    private Boolean ativo;

    private String hash;

    private String status;

    private String motivoCancelamento;

    private String dataCancelamento;

    private String datadisponibilizacao;

    private String meiocompleto;

    @JsonProperty("numeroprocessocommascara")
    private String numeroProcessoComMascara;

    public ComunicacaoDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDataDisponibilizacao() {
        return dataDisponibilizacao;
    }

    public void setDataDisponibilizacao(String dataDisponibilizacao) {
        this.dataDisponibilizacao = dataDisponibilizacao;
    }

    public String getSiglaTribunal() {
        return siglaTribunal;
    }

    public void setSiglaTribunal(String siglaTribunal) {
        this.siglaTribunal = siglaTribunal;
    }

    public String getTipoComunicacao() {
        return tipoComunicacao;
    }

    public void setTipoComunicacao(String tipoComunicacao) {
        this.tipoComunicacao = tipoComunicacao;
    }

    public String getNomeOrgao() {
        return nomeOrgao;
    }

    public void setNomeOrgao(String nomeOrgao) {
        this.nomeOrgao = nomeOrgao;
    }

    public Integer getIdOrgao() {
        return idOrgao;
    }

    public void setIdOrgao(Integer idOrgao) {
        this.idOrgao = idOrgao;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getNumeroProcesso() {
        return numeroProcesso;
    }

    public void setNumeroProcesso(String numeroProcesso) {
        this.numeroProcesso = numeroProcesso;
    }

    public String getMeio() {
        return meio;
    }

    public void setMeio(String meio) {
        this.meio = meio;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNomeClasse() {
        return nomeClasse;
    }

    public void setNomeClasse(String nomeClasse) {
        this.nomeClasse = nomeClasse;
    }

    public String getCodigoClasse() {
        return codigoClasse;
    }

    public void setCodigoClasse(String codigoClasse) {
        this.codigoClasse = codigoClasse;
    }

    public Integer getNumeroComunicacao() {
        return numeroComunicacao;
    }

    public void setNumeroComunicacao(Integer numeroComunicacao) {
        this.numeroComunicacao = numeroComunicacao;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public void setMotivoCancelamento(String motivoCancelamento) {
        this.motivoCancelamento = motivoCancelamento;
    }

    public String getDataCancelamento() {
        return dataCancelamento;
    }

    public void setDataCancelamento(String dataCancelamento) {
        this.dataCancelamento = dataCancelamento;
    }

    public String getDatadisponibilizacao() {
        return datadisponibilizacao;
    }

    public void setDatadisponibilizacao(String datadisponibilizacao) {
        this.datadisponibilizacao = datadisponibilizacao;
    }

    public String getMeiocompleto() {
        return meiocompleto;
    }

    public void setMeiocompleto(String meiocompleto) {
        this.meiocompleto = meiocompleto;
    }

    public String getNumeroProcessoComMascara() {
        return numeroProcessoComMascara;
    }

    public void setNumeroProcessoComMascara(String numeroProcessoComMascara) {
        this.numeroProcessoComMascara = numeroProcessoComMascara;
    }
}