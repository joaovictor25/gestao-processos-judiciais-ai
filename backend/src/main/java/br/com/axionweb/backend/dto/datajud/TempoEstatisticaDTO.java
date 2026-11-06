package br.com.axionweb.backend.dto.datajud;

public class TempoEstatisticaDTO {
    private Integer quantidade;
    private Double medianaDias;

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public Double getMedianaDias() { return medianaDias; }
    public void setMedianaDias(Double medianaDias) { this.medianaDias = medianaDias; }
}
