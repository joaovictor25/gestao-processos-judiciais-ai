package br.com.axionweb.backend.model;

import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;


public class Processo {
    private Long id;
    private String numeroProcesso;
    private String clienteVinculado;
    private String posicaoCliente;
    private String tribunal;
    private String orgaoJulgador;
    private String comarca;
    private String uf;
    private String classeProcessual;
    private String assunto;
    private String areaAtuacao;
    private String status;
    private BigDecimal valorCausa;
    private String nomeMagistrado;
    private LocalDate dataDistribuicao;

    public Long getId(){
        return id;
    }

    public void setNumeroProcesso(String numeroProcesso){
        this.numeroProcesso = numeroProcesso;
    }
    public String getNumeroProcesso(){
        return numeroProcesso;
    }

    public void setClienteVinculado(String clienteVinculado){
        this.clienteVinculado = clienteVinculado;
    }
    public String getClienteVinculado(){
        return clienteVinculado;
    }

    public void setPosicaoCliente(String posicaoCliente){
        this.posicaoCliente = posicaoCliente;
    }
    public String getPosicaoCliente(){
        return posicaoCliente;
    }

    public void setTribunal(String tribunal){
        this.tribunal = tribunal;
    }
    public String getTribunal(){
        return tribunal;
    }

    public void setOrgaoJulgador(String orgaoJulgador){
        this.orgaoJulgador = orgaoJulgador;
    }
    public String getOrgaoJulgador(){
        return orgaoJulgador;
    }

    public void setComarca(String comarca){
        this.comarca = comarca;
    }
    public String getComarca(){
        return comarca;
    }

    public void setUf(String uf){
        this.uf = uf;
    }
    public String getUf(){
        return uf;
    }

    public void setClasseProcessual(String classeProcessual){
        this.classeProcessual = classeProcessual;
    }
    public String getClasseProcessual(){
        return classeProcessual;
    }

    public void setAssunto(String assunto){
        this.assunto = assunto;
    }
    public String getAssunto(){
        return assunto;
    }

    public void setAreaAtuacao(String areaAtuacao){
        this.areaAtuacao = areaAtuacao;
    }
    public String getAreaAtuacao(){
        return areaAtuacao;
    }

    public void setStatus(String status){
        this.status = status;
    }
    public String getStatus(){
        return status;
    }

    public void setValorCausa(BigDecimal valorCausa){
        this.valorCausa = valorCausa;
    }
    public BigDecimal getValorCausa(){
        return valorCausa;
    }

    public void setNomeMagistrado(String nomeMagistrado){
        this.nomeMagistrado = nomeMagistrado;
    }
    public String getNomeMagistrado(){
        return nomeMagistrado;
    }

    public void setDataDistribuicao(LocalDate dataDistribuicao){
        this.dataDistribuicao = dataDistribuicao;
    }
    public LocalDate getDataDistribuicao(){
        return dataDistribuicao;
    }
}
