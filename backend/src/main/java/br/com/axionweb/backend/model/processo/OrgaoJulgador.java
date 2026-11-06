package br.com.axionweb.backend.model.processo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OrgaoJulgador {
    @Column(name="orgao_julgador_nome")
    private String nome;
    @Column(name="orgao_julgador_codigo")
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