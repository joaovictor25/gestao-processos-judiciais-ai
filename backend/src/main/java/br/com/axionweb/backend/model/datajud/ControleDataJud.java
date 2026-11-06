package br.com.axionweb.backend.model.datajud;

import jakarta.persistence.*;

/** Coordenação compartilhada por todas as instâncias que usam o mesmo banco. */
@Entity
@Table(name = "datajud_controle")
public class ControleDataJud {
    @Id public Integer id;
    public long proximaConsulta;
    public long ocupadoAte;
    @Column(length = 36) public String reserva;
}
