package br.com.axionweb.backend.dto.processo;

import br.com.axionweb.backend.model.processo.Assunto;
import br.com.axionweb.backend.model.processo.ClasseProcessual;
import br.com.axionweb.backend.model.processo.OrgaoJulgador;
import br.com.axionweb.backend.model.processo.Processo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProcessoResponseDTO(
        Long id, String numeroProcesso, String clienteVinculado, String posicaoCliente,
        String tribunal, OrgaoJulgador orgaoJulgador, String grau, String comarca, String uf,
        ClasseProcessual classeProcessual, Assunto assunto, String areaAtuacao, String status,
        BigDecimal valorCausa, String nomeMagistrado, LocalDate dataInicio, LocalDate dataFim) {

    public static ProcessoResponseDTO de(Processo processo) {
        return new ProcessoResponseDTO(processo.getId(), processo.getNumeroProcesso(), processo.getClienteVinculado(),
                processo.getPosicaoCliente(), processo.getTribunal(), processo.getOrgaoJulgador(), processo.getGrau(),
                processo.getComarca(), processo.getUf(), processo.getClasseProcessual(), processo.getAssunto(),
                processo.getAreaAtuacao(), processo.getStatus(), processo.getValorCausa(), processo.getNomeMagistrado(),
                processo.getDataInicio(), processo.getDataFim());
    }
}
