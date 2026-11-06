package br.com.axionweb.backend.dto.processo;

import br.com.axionweb.backend.model.processo.Assunto;
import br.com.axionweb.backend.model.processo.ClasseProcessual;
import br.com.axionweb.backend.model.processo.OrgaoJulgador;
import br.com.axionweb.backend.model.processo.Processo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProcessoRequestDTO(
        String numeroProcesso, String clienteVinculado, String posicaoCliente,
        String tribunal, OrgaoJulgador orgaoJulgador, String grau, String comarca, String uf,
        ClasseProcessual classeProcessual, Assunto assunto, String areaAtuacao, String status,
        BigDecimal valorCausa, String nomeMagistrado, LocalDate dataInicio, LocalDate dataFim) {

    public void aplicarEm(Processo processo) {
        processo.setNumeroProcesso(numeroProcesso);
        processo.setClienteVinculado(clienteVinculado);
        processo.setPosicaoCliente(posicaoCliente);
        processo.setTribunal(tribunal);
        processo.setOrgaoJulgador(orgaoJulgador);
        processo.setGrau(grau);
        processo.setComarca(comarca);
        processo.setUf(uf);
        processo.setClasseProcessual(classeProcessual);
        processo.setAssunto(assunto);
        processo.setAreaAtuacao(areaAtuacao);
        processo.setStatus(status);
        processo.setValorCausa(valorCausa);
        processo.setNomeMagistrado(nomeMagistrado);
        processo.setDataInicio(dataInicio);
        processo.setDataFim(dataFim);
    }
}
