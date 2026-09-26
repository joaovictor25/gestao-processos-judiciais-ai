package br.com.axionweb.backend.model.datajud;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TempoEstatistica {

    @Column(name="quantidade")
    private Integer quantidade;

    @Column(name="media_dias")
    private Double mediaDias;

    @Column(name="mediana_dias")
    private Double medianaDias;

    @Column(name="minimo_dias")
    private Long minimoDias;

    @Column(name="maximo_dias")
    private Long maximoDias;

    @Column(name="amplitude_dias")
    private Long amplitudeDias;

    @Column(name="desvio_padrao_dias")
    private Double desvioPadraoDias;

    @Column(name="percentil_25dias")
    private Double percentil25Dias;

    @Column(name="percentil_150dias")
    private Double percentil50Dias;

    @Column(name="percentil_75dias")
    private Double percentil75Dias;


    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public Double getMediaDias() {
        return mediaDias;
    }

    public void setMediaDias(Double mediaDias) {
        this.mediaDias = mediaDias;
    }

    public Double getMedianaDias() {
        return medianaDias;
    }

    public void setMedianaDias(Double medianaDias) {
        this.medianaDias = medianaDias;
    }

    public Long getMinimoDias() {
        return minimoDias;
    }

    public void setMinimoDias(Long minimoDias) {
        this.minimoDias = minimoDias;
    }

    public Long getMaximoDias() {
        return maximoDias;
    }

    public void setMaximoDias(Long maximoDias) {
        this.maximoDias = maximoDias;
    }

    public Long getAmplitudeDias() {
        return amplitudeDias;
    }

    public void setAmplitudeDias(Long amplitudeDias) {
        this.amplitudeDias = amplitudeDias;
    }

    public Double getDesvioPadraoDias() {
        return desvioPadraoDias;
    }

    public void setDesvioPadraoDias(Double desvioPadraoDias) {
        this.desvioPadraoDias = desvioPadraoDias;
    }

    public Double getPercentil25Dias() {
        return percentil25Dias;
    }

    public void setPercentil25Dias(Double percentil25Dias) {
        this.percentil25Dias = percentil25Dias;
    }

    public Double getPercentil50Dias() {
        return percentil50Dias;
    }

    public void setPercentil50Dias(Double percentil50Dias) {
        this.percentil50Dias = percentil50Dias;
    }

    public Double getPercentil75Dias() {
        return percentil75Dias;
    }

    public void setPercentil75Dias(Double percentil75Dias) {
        this.percentil75Dias = percentil75Dias;
    }
}