package br.com.axionweb.backend.model.processo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ClasseProcessual {
    @Column(name="classe_processual_nome")
    private String nome;
    @Column(name="classe_processual_codigo")
    private String codigo;

    public void setNome(String nome){
        this.nome = nome;
    }
    public String getNome(){
        return nome;
    }

    public void setCodigo(String codigo){
        this.codigo = codigo;
    }
    public String getCodigo(){
        return codigo;
    }
}
