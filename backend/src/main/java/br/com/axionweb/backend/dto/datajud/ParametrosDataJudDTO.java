package br.com.axionweb.backend.dto.datajud;

import java.time.LocalDate;

public class ParametrosDataJudDTO {
    //Similaridade
    private String tribunal;
    private String classeProcessual;
    private String assunto;
    private String orgaoJulgador;
    private String comarca;
    private String uf;

    //Amostra temporal
    private LocalDate dataInicio;
    private LocalDate dataFim;

    //Quantidade da amostra
    private Integer quantidadeResultado;


    //Definido os Getters e Setters
    public void setTribunal(String tribunal){
        this.tribunal = tribunal;
    }
    public String getTribunal(){
        return tribunal;
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

    public void setDataInicio(LocalDate dataInicio){
        this.dataInicio = dataInicio;
    }
    public LocalDate getDataInicio(){
        return dataInicio;
    }

    public void setDataFim(LocalDate dataFim){
        this.dataFim = dataFim;
    }
    public LocalDate getDataFim(){
        return dataFim;
    }

    public void setQuantidadeResultado(Integer quantidadeResultado){
        this.quantidadeResultado = quantidadeResultado;
    }
    public Integer getQuantidadeResultado(){
        return quantidadeResultado;
    }
}
