package br.com.axionweb.backend.model.datajud;

import jakarta.persistence.*;

@Entity
@Table(name = "datajud_cache", indexes = @Index(name = "idx_datajud_cache_expira", columnList = "expiraEm"))
public class CacheConsultaDataJud {
    @Id @Column(length = 64) public String chave;
    public long expiraEm;
    @Lob @Column(columnDefinition = "LONGTEXT") public String resposta;
}
