package br.com.axionweb.backend.model.datajud;

import jakarta.persistence.*;

@Entity
@Table(name = "datajud_solicitacao", indexes = {
        @Index(name = "idx_datajud_fila", columnList = "status,proximaTentativa"),
        @Index(name = "idx_datajud_solicitacao_processo", columnList = "processoId,criadaEm")})
public class SolicitacaoEstatistica {
    @Id @Column(length = 36) public String id;
    @Column(nullable = false) public Long processoId;
    @Column(nullable = false, length = 254) public String usuario;
    @Column(nullable = false, length = 64) public String chave;
    @Column(nullable = false, length = 4000) public String parametros;
    @Column(nullable = false, length = 20) public String status;
    public long criadaEm;
    public long atualizadaEm;
    public long proximaTentativa;
    public long prazoFinal;
    public long ocupadaAte;
    @Column(length = 36) public String reserva;
    public int tentativas;
    public Long estatisticaId;
    @Column(length = 64) public String codigoErro;
}
