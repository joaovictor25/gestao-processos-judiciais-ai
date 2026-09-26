package br.com.axionweb.backend.dto.datajud;

import br.com.axionweb.backend.model.datajud.TempoEstatistica;

public class EstatisticasProcessuaisDTO {

    private Integer quantidadeProcessos;

    private Integer processosBaixados;

    private Integer processosPendentes;

    private Integer processosComJulgamento;

    //private Integer processosComPendenteLiquidoCalculavel;


    private TempoEstatisticaDTO tempoAtePrimeiraBaixa;

    private TempoEstatisticaDTO tempoPendente;

    private TempoEstatisticaDTO tempoAtePrimeiroJulgamento;

    //private TempoEstatisticaDTO tempoPendenteLiquido;


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

/*    public Integer getProcessosComPendenteLiquidoCalculavel() {
        return processosComPendenteLiquidoCalculavel;
    }

    public void setProcessosComPendenteLiquidoCalculavel(
            Integer processosComPendenteLiquidoCalculavel) {
        this.processosComPendenteLiquidoCalculavel =
                processosComPendenteLiquidoCalculavel;
    }*/

    public TempoEstatisticaDTO getTempoAtePrimeiraBaixa() {
        return tempoAtePrimeiraBaixa;
    }

    public void setTempoAtePrimeiraBaixa(
            TempoEstatisticaDTO tempoAtePrimeiraBaixa) {
        this.tempoAtePrimeiraBaixa = tempoAtePrimeiraBaixa;
    }

    public TempoEstatisticaDTO getTempoPendente() {
        return tempoPendente;
    }

    public void setTempoPendente(TempoEstatisticaDTO tempoPendente) {
        this.tempoPendente = tempoPendente;
    }

    public TempoEstatisticaDTO getTempoAtePrimeiroJulgamento() {
        return tempoAtePrimeiroJulgamento;
    }

    public void setTempoAtePrimeiroJulgamento(
            TempoEstatisticaDTO tempoAtePrimeiroJulgamento) {
        this.tempoAtePrimeiroJulgamento = tempoAtePrimeiroJulgamento;
    }

/*    public TempoEstatisticaDTO getTempoPendenteLiquido() {
        return tempoPendenteLiquido;
    }

    public void setTempoPendenteLiquido(
            TempoEstatisticaDTO tempoPendenteLiquido) {
        this.tempoPendenteLiquido = tempoPendenteLiquido;
    }*/
}