package br.com.axionweb.backend.dto.comunicacao;

import br.com.axionweb.backend.model.comunicacao.Comunicacao;

// O número identifica a fonte nesta análise, sem representar um ID persistido.
public record ComunicacaoConsultadaDTO(int numero, String codigoClasse, String dataDisponibilizacao,
                                       String dataDisponibilizacaoFormatada, Integer idOrgao, String meio,
                                       String meioCompleto, String siglaTribunal, String status, String texto,
                                       String tipoComunicacao, String tipoDocumento) {
    public static ComunicacaoConsultadaDTO de(int numero, Comunicacao dados, String texto) {
        return new ComunicacaoConsultadaDTO(numero, dados.getCodigoClasse(), dados.getDataDisponibilizacao(),
                dados.getDataDisponibilizacaoFormatada(), dados.getIdOrgao(), dados.getMeio(), dados.getMeioCompleto(),
                dados.getSiglaTribunal(), dados.getStatus(), texto, dados.getTipoComunicacao(), dados.getTipoDocumento());
    }
}
