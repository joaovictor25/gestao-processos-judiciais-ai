package br.com.axionweb.backend.dto.datajud;

public class EstatisticasProcessuaisDTO {
    private Integer quantidadeProcessos;
    private Integer processosPendentes;
    private TempoEstatisticaDTO tempoAtePrimeiraBaixa;
    private TempoEstatisticaDTO tempoAtePrimeiroJulgamento;

    public Integer getQuantidadeProcessos() { return quantidadeProcessos; }
    public void setQuantidadeProcessos(Integer quantidadeProcessos) { this.quantidadeProcessos = quantidadeProcessos; }
    public Integer getProcessosPendentes() { return processosPendentes; }
    public void setProcessosPendentes(Integer processosPendentes) { this.processosPendentes = processosPendentes; }
    public TempoEstatisticaDTO getTempoAtePrimeiraBaixa() { return tempoAtePrimeiraBaixa; }
    public void setTempoAtePrimeiraBaixa(TempoEstatisticaDTO tempo) { this.tempoAtePrimeiraBaixa = tempo; }
    public TempoEstatisticaDTO getTempoAtePrimeiroJulgamento() { return tempoAtePrimeiroJulgamento; }
    public void setTempoAtePrimeiroJulgamento(TempoEstatisticaDTO tempo) { this.tempoAtePrimeiroJulgamento = tempo; }
}
