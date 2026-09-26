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

@Entity
@Table(name = "datajud_estatistica")
public class DataJud {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @Column(name = "quantidade_processos")
    private Integer quantidadeProcessos;

    @Column(name = "processos_baixados")
    private Integer processosBaixados;

    @Column(name = "processos_pendentes")
    private Integer processosPendentes;

    @Column(name = "processos_julgamento")
    private Integer processosComJulgamento;


    @Embedded
    @AttributeOverrides({

            @AttributeOverride(
                    name = "quantidade",
                    column = @Column(name = "primeira_baixa_quantidade")
            ),

            @AttributeOverride(
                    name = "mediaDias",
                    column = @Column(name = "primeira_baixa_media_dias")
            ),

            @AttributeOverride(
                    name = "medianaDias",
                    column = @Column(name = "primeira_baixa_mediana_dias")
            ),

            @AttributeOverride(
                    name = "minimoDias",
                    column = @Column(name = "primeira_baixa_minimo_dias")
            ),

            @AttributeOverride(
                    name = "maximoDias",
                    column = @Column(name = "primeira_baixa_maximo_dias")
            ),

            @AttributeOverride(
                    name = "amplitudeDias",
                    column = @Column(name = "primeira_baixa_amplitude_dias")
            ),

            @AttributeOverride(
                    name = "desvioPadraoDias",
                    column = @Column(name = "primeira_baixa_desvio_padrao_dias")
            ),

            @AttributeOverride(
                    name = "percentil25Dias",
                    column = @Column(name = "primeira_baixa_percentil_25_dias")
            ),

            @AttributeOverride(
                    name = "percentil50Dias",
                    column = @Column(name = "primeira_baixa_percentil_50_dias")
            ),

            @AttributeOverride(
                    name = "percentil75Dias",
                    column = @Column(name = "primeira_baixa_percentil_75_dias")
            )
    })
    private TempoEstatistica tempoAtePrimeiraBaixa;


    @Embedded
    @AttributeOverrides({

            @AttributeOverride(
                    name = "quantidade",
                    column = @Column(name = "pendente_quantidade")
            ),

            @AttributeOverride(
                    name = "mediaDias",
                    column = @Column(name = "pendente_media_dias")
            ),

            @AttributeOverride(
                    name = "medianaDias",
                    column = @Column(name = "pendente_mediana_dias")
            ),

            @AttributeOverride(
                    name = "minimoDias",
                    column = @Column(name = "pendente_minimo_dias")
            ),

            @AttributeOverride(
                    name = "maximoDias",
                    column = @Column(name = "pendente_maximo_dias")
            ),

            @AttributeOverride(
                    name = "amplitudeDias",
                    column = @Column(name = "pendente_amplitude_dias")
            ),

            @AttributeOverride(
                    name = "desvioPadraoDias",
                    column = @Column(name = "pendente_desvio_padrao_dias")
            ),

            @AttributeOverride(
                    name = "percentil25Dias",
                    column = @Column(name = "pendente_percentil_25_dias")
            ),

            @AttributeOverride(
                    name = "percentil50Dias",
                    column = @Column(name = "pendente_percentil_50_dias")
            ),

            @AttributeOverride(
                    name = "percentil75Dias",
                    column = @Column(name = "pendente_percentil_75_dias")
            )
    })
    private TempoEstatistica tempoPendente;


    @Embedded
    @AttributeOverrides({

            @AttributeOverride(
                    name = "quantidade",
                    column = @Column(name = "julgamento_quantidade")
            ),

            @AttributeOverride(
                    name = "mediaDias",
                    column = @Column(name = "julgamento_media_dias")
            ),

            @AttributeOverride(
                    name = "medianaDias",
                    column = @Column(name = "julgamento_mediana_dias")
            ),

            @AttributeOverride(
                    name = "minimoDias",
                    column = @Column(name = "julgamento_minimo_dias")
            ),

            @AttributeOverride(
                    name = "maximoDias",
                    column = @Column(name = "julgamento_maximo_dias")
            ),

            @AttributeOverride(
                    name = "amplitudeDias",
                    column = @Column(name = "julgamento_amplitude_dias")
            ),

            @AttributeOverride(
                    name = "desvioPadraoDias",
                    column = @Column(name = "julgamento_desvio_padrao_dias")
            ),

            @AttributeOverride(
                    name = "percentil25Dias",
                    column = @Column(name = "julgamento_percentil_25_dias")
            ),

            @AttributeOverride(
                    name = "percentil50Dias",
                    column = @Column(name = "julgamento_percentil_50_dias")
            ),

            @AttributeOverride(
                    name = "percentil75Dias",
                    column = @Column(name = "julgamento_percentil_75_dias")
            )
    })
    private TempoEstatistica tempoAtePrimeiroJulgamento;


    public Long getId() {
        return id;
    }

    public Processo getProcesso() {
        return processo;
    }

    public void setProcesso(Processo processo) {
        this.processo = processo;
    }

    public Integer getQuantidadeProcessos() {
        return quantidadeProcessos;
    }

    public void setQuantidadeProcessos(Integer quantidadeProcessos) {
        this.quantidadeProcessos = quantidadeProcessos;
    }

    public Integer getProcessosBaixados() {
        return processosBaixados;
    }

    public void setProcessosBaixados(Integer processosBaixados) {
        this.processosBaixados = processosBaixados;
    }

    public Integer getProcessosPendentes() {
        return processosPendentes;
    }

    public void setProcessosPendentes(Integer processosPendentes) {
        this.processosPendentes = processosPendentes;
    }

    public Integer getProcessosComJulgamento() {
        return processosComJulgamento;
    }

    public void setProcessosComJulgamento(Integer processosComJulgamento) {
        this.processosComJulgamento = processosComJulgamento;
    }

    public TempoEstatistica getTempoAtePrimeiraBaixa() {
        return tempoAtePrimeiraBaixa;
    }

    public void setTempoAtePrimeiraBaixa(
            TempoEstatistica tempoAtePrimeiraBaixa) {
        this.tempoAtePrimeiraBaixa = tempoAtePrimeiraBaixa;
    }

    public TempoEstatistica getTempoPendente() {
        return tempoPendente;
    }

    public void setTempoPendente(TempoEstatistica tempoPendente) {
        this.tempoPendente = tempoPendente;
    }

    public TempoEstatistica getTempoAtePrimeiroJulgamento() {
        return tempoAtePrimeiroJulgamento;
    }

    public void setTempoAtePrimeiroJulgamento(
            TempoEstatistica tempoAtePrimeiroJulgamento) {
        this.tempoAtePrimeiroJulgamento =
                tempoAtePrimeiroJulgamento;
    }
}
