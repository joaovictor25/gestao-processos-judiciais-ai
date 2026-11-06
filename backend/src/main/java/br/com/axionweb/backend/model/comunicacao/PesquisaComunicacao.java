package br.com.axionweb.backend.model.comunicacao;

import br.com.axionweb.backend.model.processo.Processo;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "comunicacao_pesquisa", indexes = @Index(name = "idx_comunicacao_pesquisa", columnList = "processo_id,id"))
public class PesquisaComunicacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Version public Long versao;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    public Processo processo;
    @Column(columnDefinition = "LONGTEXT") public String textoBusca;
    public String tribunal;
    public LocalDate dataInicio;
    public LocalDate dataFim;
    @Column(nullable = false) public Instant pesquisadaEm;
    public int quantidade;
    @Column(nullable = false, columnDefinition = "LONGTEXT") public String comunicacaoIds;
    @Column(nullable = false, columnDefinition = "LONGTEXT") public String publicacoes;
    @Column(columnDefinition = "LONGTEXT") public String sintese;
    @Column(columnDefinition = "LONGTEXT") public String evidencias;
    public Instant geradaEm;
    public Integer comunicacoesConsideradas;
    public Integer duplicadasIgnoradas;
}
