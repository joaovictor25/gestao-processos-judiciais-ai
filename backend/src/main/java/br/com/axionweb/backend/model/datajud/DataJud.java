package br.com.axionweb.backend.model.datajud;

import br.com.axionweb.backend.model.processo.Processo;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "datajud_estatistica", indexes = @jakarta.persistence.Index(name = "idx_datajud_sinteses", columnList = "processo_id,sintese_gerada_em,id"))
public class DataJud {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @Version
    private Long versao;

    private Instant executadaEm;
    private String tribunalConsulta;
    private String classeConsulta;
    private String grauConsulta;
    private Integer limiteConsulta;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    @Column(columnDefinition = "TEXT")
    private String sintese;
    private Instant sinteseGeradaEm;
    private String statusSintese = "SEM_SINTESE";

    @Column(name = "quantidade_processos")
    private Integer quantidadeProcessos;

    @Column(name = "processos_pendentes")
    private Integer processosPendentes;

    // Mantém os nomes das colunas existentes. Colunas de métricas antigas ficam
    // preservadas no banco, sem leitura ou escrita pelo novo modelo.
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "quantidade", column = @Column(name = "primeira_baixa_quantidade")),
            @AttributeOverride(name = "medianaDias", column = @Column(name = "primeira_baixa_mediana_dias"))
    })
    private TempoEstatistica tempoAtePrimeiraBaixa;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "quantidade", column = @Column(name = "julgamento_quantidade")),
            @AttributeOverride(name = "medianaDias", column = @Column(name = "julgamento_mediana_dias"))
    })
    private TempoEstatistica tempoAtePrimeiroJulgamento;

    public Long getId() { return id; }
    public Instant getExecutadaEm() { return executadaEm; }
    public void setExecutadaEm(Instant valor) { executadaEm = valor; }
    public String getTribunalConsulta() { return tribunalConsulta; }
    public void setTribunalConsulta(String valor) { tribunalConsulta = valor; }
    public String getClasseConsulta() { return classeConsulta; }
    public void setClasseConsulta(String valor) { classeConsulta = valor; }
    public String getGrauConsulta() { return grauConsulta; }
    public void setGrauConsulta(String valor) { grauConsulta = valor; }
    public Integer getLimiteConsulta() { return limiteConsulta; }
    public void setLimiteConsulta(Integer valor) { limiteConsulta = valor; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate valor) { dataInicio = valor; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate valor) { dataFim = valor; }
    public Long getVersao() { return versao; }
    public String getSintese() { return sintese; }
    public Instant getSinteseGeradaEm() { return sinteseGeradaEm; }
    public String getStatusSintese() { return statusSintese == null ? "SEM_SINTESE" : statusSintese; }
    public void salvarSintese(String texto, Instant instante) {
        sintese = texto;
        sinteseGeradaEm = instante;
        statusSintese = "CONCLUIDA";
    }
    public Processo getProcesso() { return processo; }
    public void setProcesso(Processo processo) { this.processo = processo; }
    public Integer getQuantidadeProcessos() { return quantidadeProcessos; }
    public void setQuantidadeProcessos(Integer quantidadeProcessos) { this.quantidadeProcessos = quantidadeProcessos; }
    public Integer getProcessosPendentes() { return processosPendentes; }
    public void setProcessosPendentes(Integer processosPendentes) { this.processosPendentes = processosPendentes; }
    public TempoEstatistica getTempoAtePrimeiraBaixa() { return tempoAtePrimeiraBaixa; }
    public void setTempoAtePrimeiraBaixa(TempoEstatistica tempo) { this.tempoAtePrimeiraBaixa = tempo; }
    public TempoEstatistica getTempoAtePrimeiroJulgamento() { return tempoAtePrimeiroJulgamento; }
    public void setTempoAtePrimeiroJulgamento(TempoEstatistica tempo) { this.tempoAtePrimeiroJulgamento = tempo; }
}
