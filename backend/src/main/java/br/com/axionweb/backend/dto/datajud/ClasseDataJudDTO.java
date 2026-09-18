package br.com.axionweb.backend.dto.datajud;

public class ClasseDataJudDTO {

    private Integer codigo;
    private String nome;

    public void setCodigo(Integer codigo){
        this.codigo = codigo;
    }
    public Integer getCodigo(){
        return codigo;
    }

    public void setNome(String nome){
        this.nome = nome;
    }
    public String getNome(){
        return nome;
    }
}