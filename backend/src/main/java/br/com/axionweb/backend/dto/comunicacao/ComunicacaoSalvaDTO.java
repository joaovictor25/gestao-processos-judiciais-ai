package br.com.axionweb.backend.dto.comunicacao;

import br.com.axionweb.backend.model.comunicacao.Comunicacao;

public record ComunicacaoSalvaDTO(Long id, Long processoId, String codigoClasse, String dataDisponibilizacao,
                                  String dataDisponibilizacaoFormatada, Integer idOrgao, String meio,
                                  String meioCompleto, String siglaTribunal, String status, String texto,
                                  String tipoComunicacao, String tipoDocumento) {
    public static ComunicacaoSalvaDTO de(Comunicacao comunicacao) {
        return new ComunicacaoSalvaDTO(comunicacao.getId(), comunicacao.getProcesso().getId(),
                comunicacao.getCodigoClasse(), comunicacao.getDataDisponibilizacao(),
                comunicacao.getDataDisponibilizacaoFormatada(), comunicacao.getIdOrgao(), comunicacao.getMeio(),
                comunicacao.getMeioCompleto(), comunicacao.getSiglaTribunal(), comunicacao.getStatus(),
                comunicacao.getTexto(), comunicacao.getTipoComunicacao(), comunicacao.getTipoDocumento());
    }
}
